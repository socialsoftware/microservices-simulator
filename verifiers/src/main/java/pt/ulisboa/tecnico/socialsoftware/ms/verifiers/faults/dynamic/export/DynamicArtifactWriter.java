package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.export;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model.DynamicEvidenceJoinResult;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.export.ScenarioCatalogPackageReader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Objects;

/** Finalizes normalized dynamic roles inside one current executable package. */
public final class DynamicArtifactWriter {
    public static final String OBSERVATION_ROLE = "dynamicObservations";
    public static final String ATTRIBUTION_ROLE = "dynamicAttributionLinks";
    public static final String OBSERVATION_FILE = "dynamic-observations.jsonl";
    public static final String ATTRIBUTION_FILE = "dynamic-attribution-links.jsonl";

    private final ObjectMapper mapper;
    private final FailureInjector failureInjector;

    public DynamicArtifactWriter() { this(new ObjectMapper(), ignored -> { }); }

    public DynamicArtifactWriter(ObjectMapper mapper) { this(mapper, ignored -> { }); }

    public DynamicArtifactWriter(ObjectMapper mapper, FailureInjector failureInjector) {
        this.mapper = Objects.requireNonNull(mapper).copy();
        this.failureInjector = Objects.requireNonNull(failureInjector);
    }

    public Result write(DynamicEvidenceJoinResult result, Path manifestPath) throws IOException {
        Objects.requireNonNull(result, "dynamic result");
        Path manifest = Objects.requireNonNull(manifestPath, "manifest path").toAbsolutePath().normalize();
        ScenarioCatalogPackageReader.ExecutablePackageContents current =
                new ScenarioCatalogPackageReader().readCurrent(manifest);
        Path root = manifest.getParent();
        if (root == null) throw new IllegalArgumentException("manifest path has no package directory");
        Path observations = root.resolve(OBSERVATION_FILE);
        Path attributions = root.resolve(ATTRIBUTION_FILE);
        List<Snapshot> originals = List.of(snapshot(observations), snapshot(attributions),
                snapshot(current.accountingPath()), snapshot(manifest));

        byte[] observationBytes = jsonLines(result.observations());
        byte[] attributionBytes = jsonLines(result.attributions());
        ObjectNode accounting = (ObjectNode) current.accounting().deepCopy();
        accounting.set("dynamicEvidence", mapper.valueToTree(result.dynamicAccounting()));
        byte[] accountingBytes = mapper.writeValueAsBytes(accounting);

        ObjectNode manifestNode = (ObjectNode) mapper.readTree(Files.readAllBytes(manifest));
        ObjectNode files = (ObjectNode) manifestNode.path("files");
        files.with("accounting").put("sha256", sha256(accountingBytes));
        if (observationBytes.length == 0) files.remove(OBSERVATION_ROLE);
        else putRole(files, OBSERVATION_ROLE, OBSERVATION_FILE, observationBytes);
        if (attributionBytes.length == 0) files.remove(ATTRIBUTION_ROLE);
        else putRole(files, ATTRIBUTION_ROLE, ATTRIBUTION_FILE, attributionBytes);
        byte[] manifestBytes = mapper.writeValueAsBytes(manifestNode);

        Path stagedObservations = null;
        Path stagedAttributions = null;
        Path stagedAccounting = null;
        Path stagedManifest = null;
        try {
            if (observationBytes.length > 0) stagedObservations = stage(observations, observationBytes);
            if (attributionBytes.length > 0) stagedAttributions = stage(attributions, attributionBytes);
            stagedAccounting = stage(current.accountingPath(), accountingBytes);
            stagedManifest = stage(manifest, manifestBytes);
            failureInjector.at(Boundary.AFTER_STAGING);
            if (stagedObservations != null) promote(stagedObservations, observations);
            failureInjector.at(Boundary.AFTER_OBSERVATIONS_PROMOTION);
            if (stagedAttributions != null) promote(stagedAttributions, attributions);
            failureInjector.at(Boundary.AFTER_ATTRIBUTIONS_PROMOTION);
            promote(stagedAccounting, current.accountingPath());
            failureInjector.at(Boundary.AFTER_ACCOUNTING_PROMOTION);
            promote(stagedManifest, manifest);
            failureInjector.at(Boundary.AFTER_MANIFEST_PROMOTION);
            if (observationBytes.length == 0) Files.deleteIfExists(observations);
            if (attributionBytes.length == 0) Files.deleteIfExists(attributions);
            new ScenarioCatalogPackageReader().readCurrent(manifest);
            failureInjector.at(Boundary.AFTER_FINAL_VALIDATION);
        } catch (Exception failure) {
            try {
                restore(originals);
                new ScenarioCatalogPackageReader().readCurrent(manifest);
            } catch (Exception restoreFailure) {
                failure.addSuppressed(restoreFailure);
            }
            if (failure instanceof IOException ioFailure) throw ioFailure;
            if (failure instanceof RuntimeException runtimeFailure) throw runtimeFailure;
            throw new IOException("dynamic package publication failed", failure);
        } finally {
            for (Path staged : new Path[]{stagedObservations, stagedAttributions, stagedAccounting, stagedManifest}) {
                if (staged != null) Files.deleteIfExists(staged);
            }
        }
        return new Result(manifest, observationBytes.length == 0 ? null : observations,
                attributionBytes.length == 0 ? null : attributions, current.accountingPath());
    }

    private byte[] jsonLines(List<?> records) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        for (Object record : records) {
            output.write(mapper.writeValueAsBytes(record));
            output.write('\n');
        }
        return output.toByteArray();
    }

    private void putRole(ObjectNode files, String role, String path, byte[] bytes) {
        ObjectNode value = files.putObject(role);
        value.put("path", path);
        value.put("sha256", sha256(bytes));
    }

    private Path stage(Path target, byte[] bytes) throws IOException {
        Files.createDirectories(target.getParent());
        Path staged = Files.createTempFile(target.getParent(), ".dynamic-", ".tmp");
        Files.write(staged, bytes);
        return staged;
    }

    private void promote(Path source, Path target) throws IOException {
        try { Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
        catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private Snapshot snapshot(Path path) throws IOException {
        return new Snapshot(path, Files.exists(path), Files.exists(path) ? Files.readAllBytes(path) : null);
    }

    private void restore(List<Snapshot> snapshots) throws IOException {
        IOException failure = null;
        for (Snapshot snapshot : snapshots) {
            try {
                if (!snapshot.existed()) Files.deleteIfExists(snapshot.path());
                else promote(stage(snapshot.path(), snapshot.bytes()), snapshot.path());
            } catch (IOException restoreFailure) {
                if (failure == null) failure = restoreFailure;
                else failure.addSuppressed(restoreFailure);
            }
        }
        if (failure != null) throw failure;
    }

    private String sha256(byte[] bytes) {
        try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException("SHA-256 unavailable", exception); }
    }

    public record Result(Path manifestPath, Path observationPath, Path attributionPath, Path accountingPath) { }
    private record Snapshot(Path path, boolean existed, byte[] bytes) { }

    public enum Boundary {
        AFTER_STAGING,
        AFTER_OBSERVATIONS_PROMOTION,
        AFTER_ATTRIBUTIONS_PROMOTION,
        AFTER_ACCOUNTING_PROMOTION,
        AFTER_MANIFEST_PROMOTION,
        AFTER_FINAL_VALIDATION
    }

    @FunctionalInterface
    public interface FailureInjector { void at(Boundary boundary) throws IOException; }
}
