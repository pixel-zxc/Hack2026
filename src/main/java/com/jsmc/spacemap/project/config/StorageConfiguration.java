package com.jsmc.spacemap.project.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfiguration {
    private static final Logger log = LoggerFactory.getLogger(StorageConfiguration.class);

    @Bean
    public MinioClient minioClient(StorageProperties properties) {
        MinioClient client = MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .region(properties.getRegion())
                .build();
        ensureBucket(client, properties.getBucket());
        return client;
    }

    private void ensureBucket(MinioClient client, String bucket) {
        try {
            boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("Created storage bucket: {}", bucket);
            }
        } catch (Exception e) {
            log.warn("Could not ensure storage bucket '{}': {}", bucket, e.getMessage());
        }
    }
}
