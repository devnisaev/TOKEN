package com.tokenrealty.document.storage;

import com.tokenrealty.web.exception.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

@Service
@Slf4j
public class IpfsStorageService {

    private static final String PINATA_PIN_URL = "https://api.pinata.cloud/pinning/pinFileToIPFS";

    private final RestClient restClient = RestClient.create();

    @Value("${tokenrealty.document.ipfs.mode:simulated}")
    private String mode;

    @Value("${tokenrealty.document.ipfs.pinata.jwt:}")
    private String pinataJwt;

    public String pin(byte[] content, String filename) {
        if (content == null || content.length == 0) {
            throw new ValidationException("File content is empty");
        }
        if ("pinata".equalsIgnoreCase(mode) && pinataJwt != null && !pinataJwt.isBlank()) {
            return pinViaPinata(content, filename);
        }
        return simulatedCid(content);
    }

    private String simulatedCid(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content);
            String hex = HexFormat.of().formatHex(hash);
            String cid = "bafy" + hex.substring(0, 46);
            log.debug("Simulated IPFS CID {} for {} bytes", cid, content.length);
            return cid;
        } catch (Exception ex) {
            throw new ValidationException("Failed to generate simulated CID");
        }
    }

    @SuppressWarnings("unchecked")
    private String pinViaPinata(byte[] content, String filename) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        }).filename(filename);

        Map<String, Object> response = restClient.post()
                .uri(PINATA_PIN_URL)
                .header("Authorization", "Bearer " + pinataJwt)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(builder.build())
                .retrieve()
                .body(Map.class);

        if (response == null || response.get("IpfsHash") == null) {
            throw new ValidationException("Pinata did not return IpfsHash");
        }
        return response.get("IpfsHash").toString();
    }
}
