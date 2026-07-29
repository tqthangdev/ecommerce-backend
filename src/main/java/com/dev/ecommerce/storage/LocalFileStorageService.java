package com.dev.ecommerce.storage;

import com.dev.ecommerce.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.storage", name = "provider", havingValue = "local")
public class LocalFileStorageService implements FileStorageService {

    private final StorageProperties properties;

    @Override
    public StoredFile upload(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("File is empty", HttpStatus.BAD_REQUEST);
        }

        var local = properties.getLocal();
        Path baseDir = Paths.get(local.getBasePath()).toAbsolutePath().normalize();
        Path folderDir = StringUtils.hasText(folder)
                ? baseDir.resolve(folder).normalize()
                : baseDir;

        try {
            Files.createDirectories(folderDir);
            String original = StringUtils.cleanPath(
                    file.getOriginalFilename() == null ? "file" : file.getOriginalFilename()
            );
            String key = (StringUtils.hasText(folder) ? folder + "/" : "")
                    + UUID.randomUUID() + "-" + original;
            Path target = baseDir.resolve(key).normalize();

            if (!target.startsWith(baseDir)) {
                throw new BusinessException("Invalid file path", HttpStatus.BAD_REQUEST);
            }

            try (var is = file.getInputStream()) {
                Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
            }

            String url = (local.getPublicUrl().endsWith("/")
                    ? local.getPublicUrl().substring(0, local.getPublicUrl().length() - 1)
                    : local.getPublicUrl())
                    + "/" + key;
            return new StoredFile(url, key, file.getSize(), file.getContentType());
        } catch (IOException e) {
            throw new BusinessException(
                    "Failed to write file: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @Override
    public void delete(String key) {
        if (!StringUtils.hasText(key)) return;
        try {
            Path baseDir = Paths.get(properties.getLocal().getBasePath())
                    .toAbsolutePath().normalize();
            Path target = baseDir.resolve(key).normalize();
            if (!target.startsWith(baseDir)) return;
            Files.deleteIfExists(target);
        } catch (IOException e) {
            log.warn("Failed to delete local file '{}': {}", key, e.getMessage());
        }
    }

    @Override
    public String getUrl(String key) {
        if (!StringUtils.hasText(key)) return null;
        var local = properties.getLocal();
        String base = local.getPublicUrl().endsWith("/")
                ? local.getPublicUrl().substring(0, local.getPublicUrl().length() - 1)
                : local.getPublicUrl();
        return base + "/" + key;
    }
}