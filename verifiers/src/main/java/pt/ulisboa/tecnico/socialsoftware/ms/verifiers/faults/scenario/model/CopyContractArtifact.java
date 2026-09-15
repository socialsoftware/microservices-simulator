package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Source-derived, deliberately bounded constructor-copy contract package. */
public record CopyContractArtifact(
        String schema,
        Support support,
        List<String> limitations,
        List<Contract> contracts) {

    public static final String SCHEMA = "copy-contracts.v1";
    public static final String SUPPORT_DESCRIPTION =
            "Direct scalar getter-to-setter copies in one-argument entity constructors; source aggregateId supplies identity";
    public static final List<String> LIMITATIONS = List.of(
            "Computed expressions are unsupported",
            "Setter-based updates outside the supported constructor are unsupported",
            "Arbitrary cloning and remote transport are unsupported",
            "An empty contracts array means no supported contracts were inferred, not complete application coverage");

    public CopyContractArtifact {
        schema = SCHEMA;
        support = support == null ? new Support("bounded", SUPPORT_DESCRIPTION) : support;
        limitations = limitations == null ? LIMITATIONS : List.copyOf(limitations);
        contracts = contracts == null ? List.of() : contracts.stream()
                .sorted(Comparator.comparing(Contract::sourceType)
                        .thenComparing(Contract::targetType)
                        .thenComparing(Contract::targetKey))
                .toList();
    }

    public static CopyContractArtifact empty() {
        return new CopyContractArtifact(SCHEMA, null, LIMITATIONS, List.of());
    }

    public record Support(String status, String description) { }

    public record Contract(
            String sourceType,
            String targetType,
            String sourceKey,
            String targetKey,
            Map<String, String> fields,
            List<Proof> proof,
            List<SourceFile> sourceFiles) {
        public Contract {
            sourceKey = sourceKey == null || sourceKey.isBlank() ? "aggregateId" : sourceKey;
            Map<String, String> orderedFields = new LinkedHashMap<>();
            if (fields != null) fields.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> orderedFields.put(entry.getKey(), entry.getValue()));
            fields = Collections.unmodifiableMap(orderedFields);
            proof = proof == null ? List.of() : proof.stream()
                    .sorted(Comparator.comparingInt(Proof::line)
                            .thenComparing(Proof::sourceField)
                            .thenComparing(Proof::targetField))
                    .toList();
            List<SourceFile> unique = new ArrayList<>();
            if (sourceFiles != null) sourceFiles.stream()
                    .sorted(Comparator.comparing(SourceFile::path))
                    .distinct().forEach(unique::add);
            sourceFiles = List.copyOf(unique);
        }
    }

    public record Proof(String sourceField, String targetField, int line, String expression) { }

    public record SourceFile(String path, String sha256) { }
}
