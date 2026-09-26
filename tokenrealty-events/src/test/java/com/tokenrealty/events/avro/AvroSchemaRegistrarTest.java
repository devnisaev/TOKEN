package com.tokenrealty.events.avro;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Avro schema registrar")
class AvroSchemaRegistrarTest {

    @Test
    void registerFromClasspathFailsWhenResourceMissing() {
        AvroSchemaRegistrar registrar = new AvroSchemaRegistrar("http://localhost:8092");

        assertThatThrownBy(() -> registrar.registerFromClasspath("missing", "/avro/does-not-exist.avsc"))
                .isInstanceOf(java.io.IOException.class)
                .hasMessageContaining("Avro schema not found");
    }
}
