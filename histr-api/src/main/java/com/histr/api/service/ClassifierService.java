package com.histr.api.service;

import com.histr.api.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClassifierService {

    private final EmbeddingModel embeddingModel;
    private final CategoryRepository categoryRepository;

    public UUID classifyTransaction(String description, String recipient, double amount) {
        String text = (description + " " + (recipient != null ? recipient : "")).trim().replaceAll("\\s+", " ");
        float[] embedding = embeddingModel.embed(text);
        String vectorStr = toVectorString(embedding);
        String rawId = categoryRepository.findNearestId(vectorStr);
        log.debug("Classified '{}' -> category {}", text, rawId);
        return UUID.fromString(rawId);
    }

    public float[] embed(String text) {
        return embeddingModel.embed(text);
    }

    public String toVectorString(float[] embedding) {
        return "[" + IntStream.range(0, embedding.length)
                .mapToObj(i -> String.valueOf(embedding[i]))
                .collect(Collectors.joining(",")) + "]";
    }
}
