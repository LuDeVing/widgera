package com.widgera.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.widgera.dto.*;
import com.widgera.entity.PromptHistory;
import com.widgera.entity.User;
import com.widgera.entity.UserImage;
import com.widgera.exception.ImageProcessingException;
import com.widgera.repository.PromptHistoryRepository;
import com.widgera.repository.UserImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromptService {

    private final GeminiService geminiService;
    private final S3Service s3Service;
    private final PromptHistoryRepository promptHistoryRepository;
    private final UserImageRepository userImageRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    @CacheEvict(value = "history", key = "#user.id")
    public PromptResponse processPrompt(PromptRequest request, User user) {
        log.info("Processing prompt for user: {}", user.getUsername());

        // Download image from S3 if provided (verify ownership via imageId)
        byte[] imageBytes = null;
        String imageS3Url = null;
        if (request.getImageId() != null) {
            UserImage userImage = userImageRepository.findByIdAndUser(request.getImageId(), user)
                    .orElseThrow(() -> new ImageProcessingException("Image not found or access denied"));
            imageBytes = s3Service.downloadImage(userImage.getS3Url());
            imageS3Url = userImage.getS3Url();
        }

        // Call Gemini API
        Map<String, Object> output = geminiService.generateStructuredOutput(
                request.getPrompt(),
                request.getFields(),
                imageBytes
        );

        // Save to history (store original S3 URL, not presigned)
        PromptHistory history = saveToHistory(request, output, user, imageS3Url);

        return PromptResponse.builder()
                .output(output)
                .prompt(request.getPrompt())
                .imageId(request.getImageId())
                .historyId(history.getId())
                .build();
    }

    private PromptHistory saveToHistory(PromptRequest request, Map<String, Object> output, User user, String imageS3Url) {
        try {
            String fieldStructure = objectMapper.writeValueAsString(request.getFields());
            String responseOutput = objectMapper.writeValueAsString(output);

            PromptHistory history = PromptHistory.builder()
                    .user(user)
                    .prompt(request.getPrompt())
                    .imageUrl(imageS3Url)
                    .fieldStructure(fieldStructure)
                    .responseOutput(responseOutput)
                    .build();

            return promptHistoryRepository.save(history);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize history data", e);
            throw new RuntimeException("Failed to save history", e);
        }
    }

    @Cacheable(value = "history", key = "#user.id")
    public List<HistoryResponse> getAllHistory(User user) {
        log.info("Cache MISS - Fetching all history from DB for user: {}", user.getUsername());

        List<PromptHistory> history = promptHistoryRepository.findByUserOrderByCreatedAtDesc(user);

        return history.stream()
                .map(this::mapToHistoryResponse)
                .collect(Collectors.toList());
    }

    private HistoryResponse mapToHistoryResponse(PromptHistory history) {
        try {
            List<FieldDefinition> fields = objectMapper.readValue(
                    history.getFieldStructure(),
                    new TypeReference<>() {
                    }
            );

            Map<String, Object> output = objectMapper.readValue(
                    history.getResponseOutput(),
                    new TypeReference<>() {
                    }
            );

            return HistoryResponse.builder()
                    .id(history.getId())
                    .prompt(history.getPrompt())
                    .imageUrl(history.getImageUrl())
                    .fields(fields)
                    .output(output)
                    .createdAt(history.getCreatedAt())
                    .build();
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize history data", e);
            throw new RuntimeException("Failed to read history", e);
        }
    }
}
