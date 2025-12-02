package com.widgera.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptResponse {

    private Map<String, Object> output;
    private String prompt;
    private Long imageId;
    private Long historyId;
}
