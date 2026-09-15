package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;

/** Deterministic one-way projection of top-level persistent application attributes. */
final class SagaReadAttributeFingerprinter {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

    private SagaReadAttributeFingerprinter() { }

    static Map<String, String> fingerprint(Map<String, Object> applicationData) {
        if (applicationData == null || applicationData.isEmpty()) return Map.of();
        Map<String, String> result = new TreeMap<>();
        applicationData.forEach((name, value) -> result.put(name, fingerprintValue(value)));
        return result;
    }

    private static String fingerprintValue(Object value) {
        try {
            byte[] canonical = MAPPER.writeValueAsString(value).getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Application attribute cannot be fingerprinted", exception);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
