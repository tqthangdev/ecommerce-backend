package com.dev.ecommerce.storage;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    StoredFile upload(MultipartFile file, String folder);

    void delete(String key);

    String getUrl(String key);
}