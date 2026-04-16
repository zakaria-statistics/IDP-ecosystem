package com.beebay.rag.config;

import org.springframework.ai.chroma.ChromaApi;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.transformers.TransformersEmbeddingModel;
import org.springframework.ai.vectorstore.ChromaVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class VectorStoreConfig {

    @Value("${spring.ai.vectorstore.chroma.url:http://localhost:8000}")
    private String chromaUrl;

    @Value("${spring.ai.vectorstore.chroma.collection-name:rag-documents}")
    private String collectionName;

    @Bean
    public EmbeddingModel embeddingModel() {
        // Uses sentence-transformers/all-MiniLM-L6-v2 by default
        return new TransformersEmbeddingModel();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public ChromaApi chromaApi(RestTemplate restTemplate) {
        return new ChromaApi(chromaUrl, restTemplate);
    }

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel, ChromaApi chromaApi) {
        return new ChromaVectorStore(embeddingModel, chromaApi, collectionName, true);
    }
}
