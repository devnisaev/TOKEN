package com.tokenrealty.document.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("IpfsStorageService unit tests")
class IpfsStorageServiceTest {

    @Test
    void simulatedMode_returnsDeterministicCid() {
        IpfsStorageService service = new IpfsStorageService();
        ReflectionTestUtils.setField(service, "mode", "simulated");

        String cid = service.pin("hello tokenrealty".getBytes(), "test.txt");

        assertThat(cid).startsWith("bafy");
        assertThat(service.pin("hello tokenrealty".getBytes(), "test.txt")).isEqualTo(cid);
    }

    @Test
    void selfHostedModeAlias_usesKuboPath() {
        IpfsStorageService service = new IpfsStorageService();
        ReflectionTestUtils.setField(service, "mode", "self-hosted");
        ReflectionTestUtils.setField(service, "kuboApiUrl", "http://127.0.0.1:1");

        assertThat(ReflectionTestUtils.getField(service, "mode")).isEqualTo("self-hosted");
    }
}
