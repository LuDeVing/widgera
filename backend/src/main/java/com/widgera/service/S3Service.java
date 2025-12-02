package com.widgera.service;

import com.widgera.entity.User;
import com.widgera.entity.UserImage;
import com.widgera.exception.ImageProcessingException;
import com.widgera.repository.UserImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final UserImageRepository userImageRepository;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String region;

    public record UploadResult(String imageUrl, boolean isDuplicate, UserImage userImage) {}

    public UploadResult uploadImage(MultipartFile file, User user) {
        try {
            byte[] fileBytes = file.getBytes();
            String imageHash = calculateHash(fileBytes);

            // Check for duplicate
            Optional<UserImage> existingImage = userImageRepository.findByUserAndImageHash(user, imageHash);
            if (existingImage.isPresent()) {
                log.info("Duplicate image detected for user: {}", user.getUsername());
                return new UploadResult(existingImage.get().getS3Url(), true, existingImage.get());
            }

            // Generate unique key
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String s3Key = String.format("images/%d/%s%s", user.getId(), UUID.randomUUID(), extension);

            // Upload to S3
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromBytes(fileBytes));

            // Get URL
            String s3Url = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, s3Key);

            // Save to database
            UserImage userImage = UserImage.builder()
                    .user(user)
                    .imageHash(imageHash)
                    .s3Key(s3Key)
                    .s3Url(s3Url)
                    .originalFilename(originalFilename)
                    .contentType(file.getContentType())
                    .fileSize(file.getSize())
                    .build();

            userImage = userImageRepository.save(userImage);
            log.info("Image uploaded successfully: {}", s3Url);

            return new UploadResult(s3Url, false, userImage);

        } catch (IOException e) {
            log.error("Failed to read file bytes", e);
            throw new ImageProcessingException("Failed to process image file", e);
        }
    }

    public byte[] downloadImage(String s3Url) {
        try {
            String s3Key = extractS3Key(s3Url);
            return s3Client.getObjectAsBytes(builder -> builder.bucket(bucketName).key(s3Key)).asByteArray();
        } catch (Exception e) {
            log.error("Failed to download image from S3: {}", s3Url, e);
            throw new ImageProcessingException("Failed to download image", e);
        }
    }

    private String calculateHash(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new ImageProcessingException("Failed to calculate image hash", e);
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".jpg";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    private String extractS3Key(String s3Url) {
        // Extract key from URL like https://bucket.s3.region.amazonaws.com/key
        String prefix = String.format("https://%s.s3.%s.amazonaws.com/", bucketName, region);
        if (s3Url.startsWith(prefix)) {
            return s3Url.substring(prefix.length());
        }
        throw new ImageProcessingException("Invalid S3 URL format");
    }

    /**
     * Generate a presigned URL for secure, time-limited access to an image.
     * The URL expires after the specified duration.
     */
    public String generatePresignedUrl(String s3Key, Duration expiration) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(expiration)
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    /**
     * Generate a presigned URL from the stored S3 URL with default 1-hour expiration.
     */
    public String generatePresignedUrlFromS3Url(String s3Url) {
        String s3Key = extractS3Key(s3Url);
        return generatePresignedUrl(s3Key, Duration.ofHours(1));
    }
}
