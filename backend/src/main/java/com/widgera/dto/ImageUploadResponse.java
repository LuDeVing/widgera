package com.widgera.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageUploadResponse {

    private Long imageId;
    private String imageUrl;
    private String filename;
    private boolean duplicate;
    private String message;
}
