# RAG Assistant — Enterprise Document Q&A System

A production-grade, full-stack **Retrieval-Augmented Generation (RAG)** assistant built with **React**, **Spring Boot**, **Spring AI**, **ChromaDB**, and **Ollama**.

---

## Architecture Overview

```text
                               React Frontend
                                     │
                       ┌─────────────┴─────────────┐
                       │                           │
                    Upload                       Question
                       │                           │
                       ▼                           ▼
                 Spring Boot                  Spring Boot
                       │                           │
                       ▼                           ▼
               Document Processing          Embed Question
                       │                           │
                       ▼                           ▼
                 Text Extraction           nomic-embed-text
                  (Apache Tika)                    │
                       │                           │
                       ▼                           │
                    Chunking                       │
             (TokenTextSplitter)                   │
                       │                           │
                       ▼                           ▼
                nomic-embed-text              ChromaDB
                       │                           │
                       ▼                           │
                    ChromaDB ◄──── Similarity Search
                                                   │
                                                   ▼
                                           Relevant Chunks
                                                   │
                                                   ▼
                                               Llama 3.2
                                                   │
                                                   ▼
                                              Final Answer
                                            + Source Citations
                                                   │
                                                   ▼
                                                React UI
```

---

## Technology Stack

| Layer | Technology | Details |
|---|---|---|
| **Frontend** | React 19 + Vite | Component-based, responsive enterprise theme, Axios API client, custom hooks (`useDocuments`, `useChat`) |
| **Backend** | Java 17 + Spring Boot 4.1 / Spring AI 2.0 | RESTful APIs, Spring AI Ollama & Chroma starters, Tika document reader, TokenTextSplitter, Global exception handling |
| **Vector Database** | ChromaDB (Docker) | Vector similarity search, metadata filtering, collection management |
| **LLM (Generation)** | `llama3.2` | Local execution via Ollama |
| **Embedding Model** | `nomic-embed-text` | High-performance embedding generation via Ollama |

---

## Prerequisites

Ensure you have the following installed locally:

1. **Java 17+** (`java -version`)
2. **Node.js 18+** & **npm** (`node -v`, `npm -v`)
3. **Docker** (for ChromaDB)
4. **Ollama** ([https://ollama.com](https://ollama.com))

---

## Step-by-Step Setup

### 1. Ollama Setup & Models

Start the Ollama daemon and pull the required models:

```bash
# Pull the generation model
ollama pull llama3.2

# Pull the embedding model
ollama pull nomic-embed-text
```

Verify Ollama is running and accessible at `http://localhost:11434`.

---

### 2. ChromaDB Setup (Docker)

Run ChromaDB as a Docker container:

```bash
docker run -d --name chromadb -p 8000:8000 chromadb/chroma:latest
```

Verify ChromaDB is running and accessible at `http://localhost:8000`.

---

### 3. Backend Setup & Run

The backend configuration is maintained in `Backend/src/main/resources/application.properties`:

```properties
server.port=8080

# Ollama
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.model=llama3.2
spring.ai.ollama.embedding.model=nomic-embed-text

# ChromaDB
spring.ai.vectorstore.chroma.client.host=http://localhost
spring.ai.vectorstore.chroma.client.port=8000
spring.ai.vectorstore.chroma.collection-name=rag_documents
spring.ai.vectorstore.chroma.initialize-schema=true

# Multipart Uploads
spring.servlet.multipart.max-file-size=50MB
spring.servlet.multipart.max-request-size=50MB

# RAG Configuration
app.rag.top-k=5

# CORS Configuration
app.cors.allowed-origin=http://localhost:5173
```

Run the backend:

```bash
cd Backend
./mvnw spring-boot:run
# Or on Windows:
.\mvnw.cmd spring-boot:run
```

---

### 4. Frontend Setup & Run

```bash
cd Frontend
npm install
npm run dev
```

The application will be accessible at: `http://localhost:5173`

---

## API Endpoints

### Documents

| Method | Endpoint | Description | Request | Response |
|---|---|---|---|---|
| `POST` | `/api/documents` | Upload and process document | `multipart/form-data` (file) | `202 Accepted` + `DocumentResponse` |
| `GET` | `/api/documents` | List all uploaded documents | None | `200 OK` + `List<DocumentResponse>` |
| `GET` | `/api/documents/{id}` | Get document by ID | None | `200 OK` + `DocumentResponse` |
| `DELETE`| `/api/documents/{id}` | Delete document and vector chunks | None | `204 No Content` |

### Chat / RAG

| Method | Endpoint | Description | Request | Response |
|---|---|---|---|---|
| `POST` | `/api/chat` | Query documents via RAG | `{"question": "..."}` | `200 OK` + `ChatResponse` |

#### Chat Response Example:
```json
{
  "answer": "Employees are entitled to 15 days of annual paid leave.",
  "sources": [
    {
      "documentId": "4c9d968b-90f7-41fe-8b1e-28c0352ef137",
      "documentName": "employee_handbook.pdf",
      "pageNumber": 4,
      "chunkIndex": 2
    }
  ]
}
```

---

## RAG Ingestion & Query Pipeline

### Ingestion Pipeline
1. **Validation**: Enforces supported formats (`.pdf`, `.docx`, `.txt`) and 50MB file size limits.
2. **Text Extraction**: Uses Apache Tika (`TikaDocumentReader`) to extract raw textual content across document types.
3. **Semantic Chunking**: Spring AI `TokenTextSplitter` segments content into 800-token chunks with sentence boundary preservation.
4. **Metadata Enrichment**: Each chunk is tagged with `documentId`, `documentName`, `chunkId`, `chunkIndex`, `pageNumber`, and timestamps.
5. **Vector Storage**: Embeddings are computed with `nomic-embed-text` and indexed in ChromaDB.

### Query Pipeline
1. **Embedding**: The user's prompt is vectorized using `nomic-embed-text`.
2. **Similarity Search**: ChromaDB retrieves the Top-K (default: 5) most relevant context chunks.
3. **Grounded Prompting**: System prompt strictly constrains `llama3.2` to answer using retrieved context only, preventing hallucinations.
4. **Citation Extraction**: Source documents and page numbers are returned along with the answer.

---

## Project Structure

```text
Chatbot/
├── Backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/
│   │   │   │   ├── config/              # WebConfig, AppConfig, ChromaConfig, OllamaConfig
│   │   │   │   ├── controller/          # DocumentController, ChatController
│   │   │   │   ├── dto/                 # ChatRequest, ChatResponse, DocumentResponse, SourceResponse
│   │   │   │   ├── exception/           # GlobalExceptionHandler, Custom Exceptions, ErrorResponse
│   │   │   │   ├── model/               # DocumentStatus, DocumentMetadata, DocumentChunk
│   │   │   │   ├── service/             # DocumentService, ChatService, EmbeddingService, VectorStoreService
│   │   │   │   │   └── impl/            # Service Implementations
│   │   │   │   ├── util/                # TextChunker, TextExtractor
│   │   │   │   └── ChatbotUsingRagApplication.java
│   │   │   └── resources/
│   │   │       └── application.properties
│   │   └── test/                        # Unit tests for Services, Chunker, Controllers
│   ├── pom.xml
│   └── mvnw / mvnw.cmd
│
├── Frontend/
│   ├── src/
│   │   ├── components/                  # Header, Sidebar, DocumentList, DocumentItem,
│   │   │                                # UploadModal, ChatWindow, ChatMessage, ChatInput,
│   │   │                                # LoadingIndicator, EmptyState
│   │   ├── hooks/                       # useDocuments.js, useChat.js
│   │   ├── pages/                       # ChatPage.jsx
│   │   ├── services/                    # api.js (Centralized Axios client)
│   │   ├── App.jsx                      # Main app composition
│   │   ├── App.css                      # Enterprise dark UI stylesheet
│   │   ├── index.css
│   │   └── main.jsx
│   ├── package.json
│   └── vite.config.js                   # Development proxy configuration
│
└── README.md
```

---

## Troubleshooting

- **Ollama connection failed (`Connection refused: localhost:11434`)**:
  Ensure Ollama is running (`ollama serve` or Ollama desktop app) and models are pulled (`ollama list`).
- **ChromaDB connection error (`Connection refused: localhost:8000`)**:
  Check that the Docker container is active: `docker ps` and restart if needed: `docker start chromadb`.
- **CORS Errors during local development**:
  Ensure the frontend port (`5173`) matches `app.cors.allowed-origin` in `application.properties`, or use the built-in Vite dev proxy.
