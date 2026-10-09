package com.example.service.impl;

import com.example.dto.ChatRequest;
import com.example.dto.ChatResponse;
import com.example.dto.SourceResponse;
import com.example.exception.RagException;
import com.example.model.DocumentMetadata;
import com.example.model.DocumentStatus;
import com.example.repository.DocumentRepository;
import com.example.service.ChatService;
import com.example.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * RAG chat service implementation with production-level performance profiling and relevance-based retrieval.
 *
 * <p>Pipeline:
 * <ol>
 *   <li>Check for document listing questions ("Which documents do you have?") and answer directly from metadata</li>
 *   <li>Check if active knowledge base contains any completed documents</li>
 *   <li>Embed user question and search ChromaDB using similarity threshold (all-minilm)</li>
 *   <li>Deduplicate chunks, filter relevance, and protect context window</li>
 *   <li>Build structured context from retrieved chunks and pass to Llama 3.2:1b</li>
 *   <li>Return grounded plain-text answer + accurate source citations</li>
 * </ol>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            You are a document-based RAG assistant.

            Answer the user's question using the supplied document context.

            The context comes from documents uploaded by the user.

            Use the retrieved context as the primary source of truth.

            If the answer is present in the context, answer the question directly, thoroughly, and clearly.

            Be structured and comprehensive: if the user asks for multiple categories or items (e.g. software and hardware requirements, technologies, or specifications), scan all excerpts carefully and provide all relevant details found in the context.

            Do not invent facts that are not supported by the context.

            If the retrieved context does not contain enough information to answer the question, say that the information was not found in the currently available uploaded documents.

            Do not mention embeddings, vector databases, similarity search, or internal implementation details unless the user asks.

            When possible, identify the source document used to answer the question.

            Document Context:
            {context}

            User Question:
            {question}
            """;

    private static final Pattern DOCUMENT_LIST_INTENT = Pattern.compile(
            "^(which|what|list|show|tell me about|do you have).*\\b(document|documents|file|files|pdf|pdfs|upload|uploaded|knowledge base)\\b.*",
            Pattern.CASE_INSENSITIVE
    );

    private final DocumentRepository documentRepository;
    private final EmbeddingService embeddingService;
    private final ChatModel chatModel;

    @Value("${app.rag.top-k:4}")
    private int topK;

    @Value("${app.rag.max-context-characters:4000}")
    private int maxContextCharacters;

    @Override
    public ChatResponse chat(ChatRequest request) {
        long requestStartTime = System.currentTimeMillis();
        String question = request.getQuestion().trim();
        log.info("Question:\n{}", question);

        List<DocumentMetadata> completedDocs = getCompletedDocuments();

        // 1. Handle "Which documents do you have?" queries directly & deterministically
        if (isDocumentListingQuery(question)) {
            log.info("Recognized document listing intent for question: '{}'", question);
            return handleDocumentListingQuery(completedDocs);
        }

        // 2. Handle case where no documents exist in the knowledge base
        if (completedDocs.isEmpty()) {
            log.info("No completed documents exist in the repository");
            return ChatResponse.builder()
                    .answer("No documents are currently available. Please upload a PDF, DOCX, or TXT file to get started.")
                    .sources(List.of())
                    .build();
        }

        // 3. Similarity search via ChromaDB (nomic-embed-text embeddings) using topK
        long searchStartTime = System.currentTimeMillis();
        log.info("Question embedding generated.");
        log.info("Searching ChromaDB with topK={}...", topK);
        List<Document> rawChunks;
        try {
            rawChunks = embeddingService.searchSimilar(question, topK);
        } catch (Exception e) {
            log.error("ChromaDB similarity search failed: {}", e.getMessage(), e);
            throw new RagException("Failed to search document knowledge base: " + e.getMessage(), e);
        }
        long searchDuration = System.currentTimeMillis() - searchStartTime;

        // Log all candidate chunks with score (INFO so always visible — key for retrieval diagnosis)
        log.info("--- Retrieved {} candidate chunks (topK={}) ---", rawChunks.size(), topK);
        for (int ci = 0; ci < rawChunks.size(); ci++) {
            Document c = rawChunks.get(ci);
            String src = (String) c.getMetadata().getOrDefault("fileName",
                    c.getMetadata().getOrDefault("documentName", "Unknown"));
            String snippet = c.getText() != null
                    ? c.getText().replaceAll("\\s+", " ").trim()
                    : "";
            snippet = snippet.length() > 100 ? snippet.substring(0, 100) + "..." : snippet;
            log.info("  [{}] score={} | source={} | chunkId={} | text: {}",
                    ci + 1,
                    c.getScore() != null ? String.format("%.4f", c.getScore()) : "n/a",
                    src,
                    c.getId(),
                    snippet);
        }
        log.info("----------------------------------------------");

        // 4. Intelligently rank, filter relevance, deduplicate, and enforce token/character budget (target 4-6 high quality chunks)
        List<Document> relevantChunks = rankFilterAndDeduplicateChunks(question, rawChunks, maxContextCharacters, 6);
        List<String> retrievedDocNames = relevantChunks.stream()
                .map(c -> (String) c.getMetadata().getOrDefault("fileName",
                        c.getMetadata().getOrDefault("documentName", "Unknown Document")))
                .distinct()
                .collect(Collectors.toList());

        List<String> retrievedChunkIds = relevantChunks.stream()
                .map(Document::getId)
                .collect(Collectors.toList());

        log.info("Final chunks sent to LLM: {}", relevantChunks.size());
        log.info("Sources:\n{}", String.join("\n", retrievedDocNames));

        // 5. Handle no relevant context found after similarity search
        if (relevantChunks.isEmpty()) {
            log.info("No relevant chunks retrieved for query: '{}'", question);
            return ChatResponse.builder()
                    .answer("The requested information is not available in the uploaded documents.")
                    .sources(List.of())
                    .build();
        }

        // 6. Build prompt & context
        long promptStartTime = System.currentTimeMillis();
        Prompt prompt = buildPrompt(question, relevantChunks);
        long promptDuration = System.currentTimeMillis() - promptStartTime;
        log.info("Context created.");

        // 7. Call LLM (Llama 3.2:3b)
        long llmStartTime = System.currentTimeMillis();
        log.info("Calling llama3.2:3b...");
        String answer;
        try {
            answer = chatModel.call(prompt).getResult().getOutput().getText();
        } catch (Exception e) {
            log.error("LLM generation failed: {}", e.getMessage(), e);
            throw new RagException("Failed to generate answer: " + e.getMessage(), e);
        }
        long llmDuration = System.currentTimeMillis() - llmStartTime;
        long totalDuration = System.currentTimeMillis() - requestStartTime;
        log.info("Answer generated.");

        // Structured Performance Log
        log.info("\n" +
                "========== RAG PERFORMANCE ==========\n" +
                "Question: {}\n" +
                "Similarity search (embedding + Chroma): {} ms\n" +
                "Candidate chunks retrieved: {}\n" +
                "Final chunks sent to LLM: {}\n" +
                "Retrieved chunk IDs: {}\n" +
                "Retrieved document sources: {}\n" +
                "Prompt construction: {} ms\n" +
                "LLM generation: {} ms\n" +
                "Total response time: {} ms\n" +
                "======================================",
                question, searchDuration, rawChunks.size(), relevantChunks.size(), retrievedChunkIds, retrievedDocNames, promptDuration, llmDuration, totalDuration);

        // 8. Extract source citations
        List<SourceResponse> sources = extractSources(relevantChunks);

        String cleanedAnswer = cleanAnswer(answer);

        return ChatResponse.builder()
                .answer(cleanedAnswer)
                .sources(sources)
                .build();
    }

    private String cleanAnswer(String answer) {
        if (answer == null) return "";
        String cleaned = answer.trim();
        if (cleaned.toLowerCase().startsWith("assistant\n")
                || cleaned.toLowerCase().startsWith("assistant:\n")
                || cleaned.toLowerCase().startsWith("assistant: ")) {
            cleaned = cleaned.replaceFirst("^(?i)assistant\\s*[:\\n]\\s*", "");
        }
        return cleaned.trim();
    }

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "about", "above", "after", "again", "against", "all", "am", "an", "and", "any", "are", "aren't",
            "as", "at", "be", "because", "been", "before", "being", "below", "between", "both", "but", "by",
            "can", "cannot", "could", "did", "do", "does", "doing", "down", "during", "each", "few", "for",
            "from", "further", "had", "has", "have", "having", "he", "her", "here", "hers", "herself", "him",
            "himself", "his", "how", "i", "if", "in", "into", "is", "it", "its", "itself", "let's", "me",
            "more", "most", "must", "my", "myself", "no", "nor", "not", "of", "off", "on", "once", "only",
            "or", "other", "ought", "our", "ours", "ourselves", "out", "over", "own", "same", "she", "should",
            "so", "some", "such", "than", "that", "the", "their", "theirs", "them", "themselves", "then",
            "there", "these", "they", "this", "those", "through", "to", "too", "under", "until", "up", "very",
            "was", "we", "were", "what", "when", "where", "which", "while", "who", "whom", "why", "with",
            "would", "you", "your", "yours", "yourself", "yourselves", "tell", "give", "explain", "describe", "show"
    );

    private static final Set<String> GENERIC_DOC_TERMS = Set.of(
            "smart", "resume", "evaluation", "system", "document", "file", "project", "report", "paper", "application", "platform"
    );

    private List<Document> rankFilterAndDeduplicateChunks(String question, List<Document> chunks, int maxChars, int maxChunksLimit) {
        if (chunks == null || chunks.isEmpty()) {
            return List.of();
        }

        // 1. Extract significant query tokens and specific multi-word query keyphrases
        String cleanQuestion = question.toLowerCase().replaceAll("[^a-z0-9\\s]", " ");
        List<String> queryTokens = java.util.Arrays.stream(cleanQuestion.split("\\s+"))
                .map(String::trim)
                .filter(t -> t.length() >= 3 && !STOP_WORDS.contains(t))
                .distinct()
                .collect(Collectors.toList());

        List<String> keyphrases = new ArrayList<>();
        String[] questionWords = cleanQuestion.split("\\s+");
        for (int i = 0; i < questionWords.length - 1; i++) {
            String w1 = questionWords[i];
            String w2 = questionWords[i + 1];
            // Only form keyphrases that are NOT purely generic document words
            if (!STOP_WORDS.contains(w1) && !STOP_WORDS.contains(w2)
                    && (!GENERIC_DOC_TERMS.contains(w1) || !GENERIC_DOC_TERMS.contains(w2))) {
                keyphrases.add(w1 + " " + w2);
            }
        }
        // Synthesize specific cross-conjunctive topic phrases
        if (cleanQuestion.contains("software") && cleanQuestion.contains("requirements")) {
            keyphrases.add("software requirements");
            keyphrases.add("software requirement");
        }
        if (cleanQuestion.contains("hardware") && cleanQuestion.contains("requirements")) {
            keyphrases.add("hardware requirements");
            keyphrases.add("hardware requirement");
        }
        if (cleanQuestion.contains("functional") && cleanQuestion.contains("requirements")) {
            keyphrases.add("functional requirements");
        }
        keyphrases = keyphrases.stream().distinct().collect(Collectors.toList());

        log.info("Query significant keywords: {}", queryTokens);
        log.info("Query specific keyphrases: {}", keyphrases);

        // 2. Score each candidate chunk
        record ScoredChunk(Document document, double combinedScore, int keywordMatches, int charCount, int estimatedTokens, List<String> matchedPhrases) {}

        List<ScoredChunk> scoredList = new ArrayList<>();
        Set<String> seenContentHashes = new HashSet<>();

        for (Document chunk : chunks) {
            if (chunk.getText() == null || chunk.getText().isBlank()) {
                continue;
            }

            String normalizedText = chunk.getText().trim().replaceAll("\\s+", " ").toLowerCase();
            String contentKey = normalizedText.length() > 80 ? normalizedText.substring(0, 80) : normalizedText;
            if (seenContentHashes.contains(contentKey)) {
                continue; // Skip duplicate content
            }
            seenContentHashes.add(contentKey);

            double vectorScore = chunk.getScore() != null ? chunk.getScore() : 0.5;

            // Specificity-weighted keyword score
            double keywordScore = 0.0;
            int matchCount = 0;
            for (String token : queryTokens) {
                if (normalizedText.contains(token)) {
                    matchCount++;
                    if (token.equals("hardware") || token.equals("software") || token.equals("requirements")
                            || token.equals("requirement") || token.equals("specification") || token.equals("architecture")) {
                        keywordScore += 1.5;
                    } else {
                        keywordScore += 0.5;
                    }
                }
            }

            // Keyphrase matching (e.g. "software requirements", "hardware requirements")
            double phraseScore = 0.0;
            List<String> matchedPhrases = new ArrayList<>();
            for (String kp : keyphrases) {
                if (normalizedText.contains(kp)) {
                    phraseScore += 2.0;
                    matchedPhrases.add(kp);
                }
            }

            double combinedScore = (vectorScore * 1.0) + keywordScore + phraseScore;

            int charCount = chunk.getText().length();
            int estTokens = (int) Math.ceil(charCount / 4.0);

            scoredList.add(new ScoredChunk(chunk, combinedScore, matchCount, charCount, estTokens, matchedPhrases));
        }

        // 3. Sort by combined score descending
        scoredList.sort((a, b) -> Double.compare(b.combinedScore, a.combinedScore));

        // 4. Select top chunks up to maxChunksLimit and within maxChars budget
        List<Document> selected = new ArrayList<>();
        int totalChars = 0;
        int totalEstTokens = 0;

        log.info("--- Reranked Candidate Chunks (target 4-6 high-quality chunks) ---");
        for (int i = 0; i < scoredList.size(); i++) {
            ScoredChunk sc = scoredList.get(i);
            String docName = (String) sc.document.getMetadata().getOrDefault("fileName", "Unknown");

            boolean willFit = (totalChars + sc.charCount) <= maxChars && selected.size() < maxChunksLimit;
            String status = willFit ? "INCLUDED" : (selected.size() >= maxChunksLimit ? "EXCLUDED (chunk limit)" : "EXCLUDED (char limit)");

            log.info("  Rank #{}: id={} | doc={} | vecScore={} | combinedScore={} | kwMatches={}/{} | phrases={} | chars={} (~{} tokens) -> {}",
                    i + 1,
                    sc.document.getId(),
                    docName,
                    sc.document.getScore() != null ? String.format("%.4f", sc.document.getScore()) : "n/a",
                    String.format("%.4f", sc.combinedScore),
                    sc.keywordMatches,
                    queryTokens.size(),
                    sc.matchedPhrases,
                    sc.charCount,
                    sc.estimatedTokens,
                    status);

            if (willFit) {
                selected.add(sc.document);
                totalChars += sc.charCount;
                totalEstTokens += sc.estimatedTokens;
            }
        }
        log.info("Total context selected for LLM: {} chunks, {} characters, ~{} estimated tokens",
                selected.size(), totalChars, totalEstTokens);
        log.info("------------------------------------------------------------------");

        return selected;
    }

    private Prompt buildPrompt(String question, List<Document> chunks) {
        String context = buildContext(chunks);
        SystemPromptTemplate promptTemplate = new SystemPromptTemplate(SYSTEM_PROMPT_TEMPLATE);
        return promptTemplate.create(Map.of(
                "context", context,
                "question", question
        ));
    }

    private List<DocumentMetadata> getCompletedDocuments() {
        return documentRepository.findAll().stream()
                .filter(d -> d.getStatus() == DocumentStatus.COMPLETED)
                .collect(Collectors.toList());
    }

    private boolean isDocumentListingQuery(String question) {
        String clean = question.toLowerCase().trim().replaceAll("[?!.,;:]", "");
        if (clean.equals("which documents do you have")
                || clean.equals("what documents do you have")
                || clean.equals("what files do you have")
                || clean.equals("what files have i uploaded")
                || clean.equals("what documents have i uploaded")
                || clean.equals("list my documents")
                || clean.equals("list documents")
                || clean.equals("list files")
                || clean.equals("what pdfs are available")
                || clean.equals("show uploaded documents")
                || clean.equals("show my documents")
                || clean.equals("what documents are available")
                || clean.equals("what files are available")
                || clean.equals("which files are available")
                || clean.equals("what do you have")
                || clean.equals("available documents")
                || clean.equals("do you have any documents")
                || clean.equals("show documents")) {
            return true;
        }
        return DOCUMENT_LIST_INTENT.matcher(clean).matches();
    }

    private ChatResponse handleDocumentListingQuery(List<DocumentMetadata> completedDocs) {
        if (completedDocs.isEmpty()) {
            return ChatResponse.builder()
                    .answer("No documents are currently available.")
                    .sources(List.of())
                    .build();
        }

        StringBuilder sb = new StringBuilder("Available documents:\n\n");
        for (int i = 0; i < completedDocs.size(); i++) {
            DocumentMetadata doc = completedDocs.get(i);
            sb.append(i + 1).append(". ").append(doc.getOriginalFileName());
            if (doc.getChunkCount() > 0) {
                sb.append(" (").append(doc.getChunkCount()).append(" chunks)");
            }
            sb.append("\n");
        }

        return ChatResponse.builder()
                .answer(sb.toString().trim())
                .sources(List.of())
                .build();
    }

    private String buildContext(List<Document> chunks) {
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            Document chunk = chunks.get(i);
            String docName = (String) chunk.getMetadata().getOrDefault("fileName",
                    chunk.getMetadata().getOrDefault("documentName", "Unknown Document"));
            context.append("--- Excerpt ").append(i + 1).append(" (from ").append(docName).append(") ---\n");
            context.append(chunk.getText().trim()).append("\n\n");
        }
        return context.toString().trim();
    }

    private List<SourceResponse> extractSources(List<Document> chunks) {
        List<SourceResponse> sources = new ArrayList<>();
        List<String> seenDocChunkCombos = new ArrayList<>();

        for (Document chunk : chunks) {
            Map<String, Object> meta = chunk.getMetadata();
            String documentId = (String) meta.get("documentId");
            String documentName = (String) meta.getOrDefault("fileName",
                    meta.getOrDefault("documentName", meta.getOrDefault("source", "Document")));

            Integer chunkIndex = null;
            Object indexObj = meta.get("chunkIndex");
            if (indexObj instanceof Integer) {
                chunkIndex = (Integer) indexObj;
            } else if (indexObj instanceof Number) {
                chunkIndex = ((Number) indexObj).intValue();
            }

            String comboKey = documentName + "_" + chunkIndex;
            if (seenDocChunkCombos.contains(comboKey)) {
                continue; // Deduplicate
            }
            seenDocChunkCombos.add(comboKey);

            Integer pageNumber = null;
            Object pageObj = meta.get("pageNumber");
            if (pageObj instanceof Integer) {
                pageNumber = (Integer) pageObj;
            } else if (pageObj instanceof String && !((String) pageObj).isBlank()) {
                try {
                    pageNumber = Integer.parseInt((String) pageObj);
                } catch (NumberFormatException ignored) {
                }
            }

            String snippet = null;
            if (chunk.getText() != null && !chunk.getText().isBlank()) {
                String text = chunk.getText().replaceAll("\\s+", " ").trim();
                snippet = text.length() > 160 ? text.substring(0, 160) + "..." : text;
            }

            sources.add(SourceResponse.builder()
                    .documentId(documentId)
                    .documentName(documentName)
                    .pageNumber(pageNumber)
                    .chunkIndex(chunkIndex)
                    .snippet(snippet)
                    .build());
        }

        return sources;
    }
}
