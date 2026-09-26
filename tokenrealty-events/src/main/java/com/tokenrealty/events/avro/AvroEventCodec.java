package com.tokenrealty.events.avro;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Opt-in Avro binary codec for settlement-flow payloads. JSON envelope remains default.
 */
public final class AvroEventCodec {

    private final Schema schema;

    public AvroEventCodec(Schema schema) {
        this.schema = schema;
    }

    public static AvroEventCodec fromClasspath(String resourcePath) throws IOException {
        try (InputStream in = AvroEventCodec.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("Avro schema not found: " + resourcePath);
            }
            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return new AvroEventCodec(new Schema.Parser().parse(json));
        }
    }

    public byte[] encode(Map<String, Object> payload) throws IOException {
        GenericRecord record = toRecord(schema, payload);
        var writer = new GenericDatumWriter<GenericRecord>(schema);
        var out = new ByteArrayOutputStream();
        var encoder = EncoderFactory.get().binaryEncoder(out, null);
        writer.write(record, encoder);
        encoder.flush();
        return out.toByteArray();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> decode(byte[] bytes) throws IOException {
        var reader = new GenericDatumReader<GenericRecord>(schema);
        var decoder = DecoderFactory.get().binaryDecoder(new ByteArrayInputStream(bytes), null);
        GenericRecord record = reader.read(null, decoder);
        return (Map<String, Object>) fromRecord(record);
    }

    public Schema schema() {
        return schema;
    }

    private static GenericRecord toRecord(Schema schema, Map<String, Object> payload) {
        GenericRecord record = new GenericData.Record(schema);
        for (Schema.Field field : schema.getFields()) {
            Object value = payload.get(field.name());
            if (value instanceof Map<?, ?> nested && field.schema().getType() == Schema.Type.RECORD) {
                record.put(field.name(), toRecord(field.schema(), (Map<String, Object>) nested));
            } else {
                record.put(field.name(), value);
            }
        }
        return record;
    }

    private static Object fromRecord(GenericRecord record) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (Schema.Field field : record.getSchema().getFields()) {
            Object value = record.get(field.name());
            if (value instanceof GenericRecord nested) {
                map.put(field.name(), fromRecord(nested));
            } else {
                map.put(field.name(), value == null ? null : value.toString());
            }
        }
        return map;
    }
}
