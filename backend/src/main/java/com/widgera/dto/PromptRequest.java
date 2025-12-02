package com.widgera.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromptRequest {

    @NotBlank(message = "Prompt is required")
    private String prompt;

    @NotEmpty(message = "At least one field definition is required")
    @Valid
    private List<FieldDefinition> fields;

    private Long imageId;  // optional
}
