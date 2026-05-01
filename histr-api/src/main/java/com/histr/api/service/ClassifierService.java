package com.histr.api.service;

import com.histr.api.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClassifierService {

    private final EmbeddingModel embeddingModel;
    private final CategoryRepository categoryRepository;

    public record TransactionClassificationInput(String description, String recipient, double amount) {}

    public UUID classifyTransaction(String description, String recipient, double amount) {
        return classifyTransactions(List.of(
                new TransactionClassificationInput(description, recipient, amount)
        )).getFirst();
    }

    public List<UUID> classifyTransactions(List<TransactionClassificationInput> transactions) {
        if (transactions.isEmpty()) {
            return List.of();
        }

        List<String> texts = transactions.stream()
                .map(t -> classificationText(t.description(), t.recipient()))
                .toList();

        List<float[]> embeddings = embeddingModel.embed(texts);
        if (embeddings.size() != transactions.size()) {
            throw new IllegalStateException("Embedding response size did not match transaction batch size");
        }

        return IntStream.range(0, embeddings.size())
                .mapToObj(i -> {
                    String vectorStr = toVectorString(embeddings.get(i));
                    String rawId = categoryRepository.findNearestId(vectorStr);
                    log.debug("Classified '{}' -> category {}", texts.get(i), rawId);
                    return UUID.fromString(rawId);
                })
                .toList();
    }

    public float[] embed(String text) {
        return embeddingModel.embed(text);
    }

    public String toVectorString(float[] embedding) {
        return "[" + IntStream.range(0, embedding.length)
                .mapToObj(i -> String.valueOf(embedding[i]))
                .collect(Collectors.joining(",")) + "]";
    }

    private String classificationText(String description, String recipient) {
        return (description + " " + (recipient != null ? recipient : ""))
                .trim()
                .replaceAll("\\s+", " ");
    }
}
