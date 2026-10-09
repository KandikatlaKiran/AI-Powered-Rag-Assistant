package com.example.util;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public class TextExtractor {

    private TextExtractor() {
    }

    public static String extractText(MultipartFile file) throws IOException {

        TikaDocumentReader reader = new TikaDocumentReader(file.getResource());

        List<Document> documents = reader.get();

        StringBuilder text = new StringBuilder();

        for (Document document : documents) {
            if (document.getText() != null) {
                text.append(document.getText());
                text.append("\n");
            }
        }

        return text.toString().trim();
    }
}