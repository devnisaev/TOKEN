package com.tokenrealty.document.storage;

import com.tokenrealty.web.exception.ValidationException;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

@Service
@Slf4j
public class MinioStorageService {

    @Value("${tokenrealty.document.minio.mode:simulated}")
    private String mode;

    @Value("${tokenrealty.document.minio.endpoint:http://localhost:9000}")
    private String endpoint;

    @Value("${tokenrealty.document.minio.bucket:tokenrealty-private}")
    private String bucket;

    @Value("${tokenrealty.document.minio.access-key:minio}")
    private String accessKey;

    @Value("${tokenrealty.document.minio.secret-key:minio123}")
    private String secretKey;

    public String store(byte[] content, String filename) {
        if (content == null || content.length == 0) {
            throw new ValidationException("File content is empty");
        }
        if ("minio".equalsIgnoreCase(mode)) {
            return storeViaMinio(content, filename);
        }
        return simulatedUrl(content, filename);
    }

    private String simulatedUrl(byte[] content, String filename) {
        String key = objectKey(content, filename);
        String url = "s3://" + bucket + "/" + key;
        log.debug("Simulated MinIO storageUrl {} for {} bytes", url, content.length);
        return url;
    }

    private String storeViaMinio(byte[] content, String filename) {
        String key = objectKey(content, filename);
        try {
            MinioClient client = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();
            client.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .stream(new ByteArrayInputStream(content), content.length, -1)
                    .build());
            String url = "s3://" + bucket + "/" + key;
            log.info("Stored private document at {}", url);
            return url;
        } catch (Exception ex) {
            throw new ValidationException("Failed to store document in MinIO: " + ex.getMessage());
        }
    }

    private static String objectKey(byte[] content, String filename) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String hash = HexFormat.of().formatHex(digest.digest(content)).substring(0, 16);
            String safeName = filename != null ? filename.replaceAll("[^a-zA-Z0-9._-]", "_") : "upload";
            return hash + "/" + UUID.randomUUID() + "-" + safeName;
        } catch (Exception ex) {
            throw new ValidationException("Failed to generate storage key");
        }
    }
}
