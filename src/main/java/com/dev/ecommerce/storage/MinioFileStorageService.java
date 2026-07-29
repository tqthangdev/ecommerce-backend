package com.dev.ecommerce.storage;

import com.dev.ecommerce.exception.BusinessException;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.BucketExistsArgs;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.storage", name = "provider", havingValue = "minio")
public class MinioFileStorageService implements FileStorageService {

    private final StorageProperties properties;
    private MinioClient minioClient;

    @PostConstruct
    void init() {
        var minio = properties.getMinio();
        this.minioClient = MinioClient.builder()
                .endpoint(minio.getEndpoint())
                .credentials(minio.getAccessKey(), minio.getSecretKey())
                .build();

        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(minio.getBucket()).build()
            );
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(minio.getBucket()).build()
                );
                log.info("MinIO bucket '{}' created.", minio.getBucket());
            }
        } catch (Exception e) {
            log.warn("MinIO initialization failed for bucket '{}': {}",
                    minio.getBucket(), e.getMessage());
        }
    }

    @Override
    public StoredFile upload(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("File is empty", HttpStatus.BAD_REQUEST);
        }

        var minio = properties.getMinio();
        String original = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "file" : file.getOriginalFilename()
        );
        String key = (StringUtils.hasText(folder) ? folder + "/" : "")
                + UUID.randomUUID() + "-" + original;

        try (InputStream is = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minio.getBucket())
                            .object(key)
                            .stream(is, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
        } catch (Exception e) {
            throw new BusinessException(
                    "Failed to upload file to MinIO: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }

        String url = joinUrl(minio.getPublicUrl(), minio.getBucket(), key);
        return new StoredFile(url, key, file.getSize(), file.getContentType());
    }

    @Override
    public void delete(String key) {
        if (!StringUtils.hasText(key)) return;
        var minio = properties.getMinio();
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minio.getBucket())
                            .object(key)
                            .build()
            );
        } catch (Exception e) {
            log.warn("Failed to delete object '{}' from MinIO: {}", key, e.getMessage());
        }
    }

    @Override
    public String getUrl(String key) {
        if (!StringUtils.hasText(key)) return null;
        var minio = properties.getMinio();
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(minio.getBucket())
                            .object(key)
                            .method(Method.GET)
                            .expiry(7, TimeUnit.DAYS)
                            .build()
            );
        } catch (Exception e) {
            log.warn("Failed to build presigned URL for '{}': {}", key, e.getMessage());
            return joinUrl(minio.getPublicUrl(), minio.getBucket(), key);
        }
    }

    private static String joinUrl(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append('/');
            sb.append(parts[i]);
        }
        return sb.toString();
    }
}