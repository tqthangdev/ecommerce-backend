package com.dev.ecommerce.storage;

import com.dev.ecommerce.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "app.storage", name = "provider", havingValue = "none", matchIfMissing = true)
public class NoopFileStorageService implements FileStorageService {

    @Override
    public StoredFile upload(MultipartFile file, String folder) {
        throw new BusinessException(
                "File storage is disabled (app.storage.provider=none)",
                HttpStatus.BAD_REQUEST
        );
    }

    @Override
    public void delete(String key) {
        log.debug("NoopFileStorageService.delete('{}') — no-op", key);
    }

    @Override
    public String getUrl(String key) {
        return null;
    }
}