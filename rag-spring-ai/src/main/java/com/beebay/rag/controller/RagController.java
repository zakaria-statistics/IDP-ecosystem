package com.beebay.rag.controller;

import com.beebay.rag.service.RagService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/rag")
@CrossOrigin(origins = "${app.cors.allowed-origins:*}")
public class RagController {

    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/query")
    public Map<String, Object> query(@RequestBody Map<String, String> request) {
        String question = request.get("question");
        String answer = ragService.query(question);
        return Map.of(
            "question", question,
            "answer", answer
        );
    }

    @PostMapping("/ingest")
    public Map<String, String> ingest(@RequestBody Map<String, String> request) {
        String content = request.get("content");
        ragService.ingest(content);
        return Map.of("status", "Document ingested successfully");
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return Map.of("status", "error", "message", "File is empty");
        }

        int chunks = ragService.ingestFile(file);
        return Map.of(
            "status", "success",
            "filename", file.getOriginalFilename(),
            "chunks", chunks,
            "message", "File ingested successfully into " + chunks + " chunks"
        );
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
