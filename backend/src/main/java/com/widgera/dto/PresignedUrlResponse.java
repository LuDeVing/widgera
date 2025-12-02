package com.widgera.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresignedUrlResponse {

    private Long imageId;
    private String url;
    private String originalFilename;
    private int expiresInSeconds;
}
