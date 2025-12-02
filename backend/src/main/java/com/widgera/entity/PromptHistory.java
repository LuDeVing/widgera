package com.widgera.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "prompt_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String prompt;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "field_structure", columnDefinition = "TEXT")
    private String fieldStructure;  // JSON string of field definitions

    @Column(name = "response_output", columnDefinition = "TEXT")
    private String responseOutput;  // JSON string of LLM response

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
