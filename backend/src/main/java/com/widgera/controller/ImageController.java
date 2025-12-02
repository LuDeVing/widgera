package com.widgera.controller;

import com.widgera.dto.ImageUploadResponse;
import com.widgera.dto.PresignedUrlResponse;
import com.widgera.entity.User;
import com.widgera.repository.UserImageRepository;
import com.widgera.service.S3Service;
import com.widgera.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
@Slf4j
public class ImageController {

    private final S3Service s3Service;
    private final UserService userService;
    private final UserImageRepository userImageRepository;

    @PostMapping("/upload")
    public ResponseEntity<ImageUploadResponse> uploadImage(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        log.info("Image upload request from user: {}, filename: {}",
                userDetails.getUsername(), file.getOriginalFilename());

        User user = userService.getUserByUsername(userDetails.getUsername());
        S3Service.UploadResult result = s3Service.uploadImage(file, user);

        // presigned URL for immediate use
        String presignedUrl = s3Service.generatePresignedUrlFromS3Url(result.imageUrl());

        ImageUploadResponse response = ImageUploadResponse.builder()
                .imageId(result.userImage().getId())
                .imageUrl(presignedUrl)
                .filename(file.getOriginalFilename())
                .duplicate(result.isDuplicate())
                .message(result.isDuplicate()
                        ? "Image already exists, returning existing URL"
                        : "Image uploaded successfully")
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Get a presigned URL for secure access to an image.
     * Only the owner of the image can access it.
     */
    @GetMapping("/{imageId}/url")
    public ResponseEntity<PresignedUrlResponse> getPresignedUrl(
            @PathVariable Long imageId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = userService.getUserByUsername(userDetails.getUsername());

        return userImageRepository.findByIdAndUser(imageId, user)
                .map(image -> {
                    String presignedUrl = s3Service.generatePresignedUrlFromS3Url(image.getS3Url());
                    return ResponseEntity.ok(PresignedUrlResponse.builder()
                            .imageId(imageId)
                            .url(presignedUrl)
                            .expiresInSeconds(3600)
                            .build());
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all images for the authenticated user with presigned URLs.
     */
    @GetMapping
    public ResponseEntity<List<PresignedUrlResponse>> getUserImages(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = userService.getUserByUsername(userDetails.getUsername());

        List<PresignedUrlResponse> images = userImageRepository.findByUser(user).stream()
                .map(image -> PresignedUrlResponse.builder()
                        .imageId(image.getId())
                        .url(s3Service.generatePresignedUrlFromS3Url(image.getS3Url()))
                        .originalFilename(image.getOriginalFilename())
                        .expiresInSeconds(3600)
                        .build())
                .toList();

        return ResponseEntity.ok(images);
    }
}
