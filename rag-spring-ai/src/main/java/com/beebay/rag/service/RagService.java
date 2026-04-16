package com.beebay.rag.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RagService {

    private final VectorStore vectorStore;

    // Chunk size for splitting large documents (in characters)
    private static final int CHUNK_SIZE = 1000;
    private static final int CHUNK_OVERLAP = 200;

    public RagService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public void ingest(String content) {
        List<Document> chunks = splitIntoChunks(content, "api");
        vectorStore.add(chunks);
    }

    public int ingestFile(MultipartFile file) throws IOException {
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        String filename = file.getOriginalFilename();
        List<Document> chunks = splitIntoChunks(content, filename);
        vectorStore.add(chunks);
        return chunks.size();
    }

    public String query(String question) {
        List<Document> results = vectorStore.similaritySearch(
            SearchRequest.query(question).withTopK(5)
        );

        if (results.isEmpty()) {
            return "No relevant documents found.";
        }

        return results.stream()
            .map(Document::getContent)
            .collect(Collectors.joining("\n\n---\n\n"));
    }

    private List<Document> splitIntoChunks(String content, String source) {
        List<Document> chunks = new ArrayList<>();

        if (content.length() <= CHUNK_SIZE) {
            chunks.add(new Document(content, Map.of("source", source, "chunk", 0)));
            return chunks;
        }

        int start = 0;
        int chunkIndex = 0;

        while (start < content.length()) {
            int end = Math.min(start + CHUNK_SIZE, content.length());

            // Try to break at a sentence or paragraph boundary
            if (end < content.length()) {
                int lastPeriod = content.lastIndexOf(". ", end);
                int lastNewline = content.lastIndexOf("\n", end);
                int breakPoint = Math.max(lastPeriod, lastNewline);

                if (breakPoint > start + CHUNK_SIZE / 2) {
                    end = breakPoint + 1;
                }
            }

            String chunk = content.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(new Document(chunk, Map.of(
                    "source", source,
                    "chunk", chunkIndex++
                )));
            }

            start = end - CHUNK_OVERLAP;
            if (start < 0) start = end;
        }

        return chunks;
    }
}
