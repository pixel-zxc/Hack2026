package com.jsmc.spacemap.project.service;

import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.jsmc.spacemap.project.config.StorageProperties;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;

@Service
public class StorageService {
    private static final Logger log = LoggerFactory.getLogger(StorageService.class);

    private final MinioClient minioClient;
    private final StorageProperties properties;

    public StorageService(MinioClient minioClient, StorageProperties properties) {
        this.minioClient = minioClient;
        this.properties = properties;
    }

    public void putObject(String key, MultipartFile file, String contentType) {
        try (InputStream stream = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(key)
                    .stream(stream, file.getSize(), -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception e) {
            log.error("Failed to upload object {}: {}", key, e.getMessage());
            throw new IllegalStateException("Не удалось загрузить файл в хранилище", e);
        }
    }

    public InputStream getObject(String key) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(key)
                    .build());
        } catch (Exception e) {
            log.error("Failed to download object {}: {}", key, e.getMessage());
            throw new IllegalStateException("Не удалось получить файл из хранилища", e);
        }
    }

    public boolean objectExists(String key) {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(key)
                    .build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void deleteObject(String key) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(key)
                    .build());
        } catch (Exception e) {
            log.error("Failed to delete object {}: {}", key, e.getMessage());
            throw new IllegalStateException("Не удалось удалить файл из хранилища", e);
        }
    }

    public void deleteObjectQuietly(String key) {
        try {
            deleteObject(key);
        } catch (Exception e) {
            log.warn("Compensation delete failed for {}: {}", key, e.getMessage());
        }
    }
}
