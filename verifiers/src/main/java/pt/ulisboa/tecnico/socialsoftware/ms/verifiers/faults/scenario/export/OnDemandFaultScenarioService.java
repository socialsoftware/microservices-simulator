package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.export;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.EagerFaultScenarioGenerator;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.FaultScenarioValidator;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.RecoveryScheduleCap;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.RecoveryScheduleGenerator;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.accounting.ScenarioSpaceAccountingReport;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ComputedVectorRecovery;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FaultScenario;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FaultScenarioVectorSource;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.RecoveryScheduleGenerationResult;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScenarioCatalogManifest;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.WorkloadMaterializability;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.WorkloadPlan;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class OnDemandFaultScenarioService {

    public static final String STATIC_GENERATION_SOURCE = "STATIC_ANALYSIS";
    public static final String STATIC_AND_ON_DEMAND_GENERATION_SOURCE = "STATIC_ANALYSIS_AND_ON_DEMAND_REQUEST";
    public static final String EAGER_VECTOR_SOURCE = "EAGER_ALL_ZERO_AND_SINGLE_POINT";
    public static final String EAGER_AND_ON_DEMAND_VECTOR_SOURCE = "EAGER_ALL_ZERO_AND_SINGLE_POINT_AND_ON_DEMAND";

    private static final Comparator<FaultScenario> FAULT_SCENARIO_ORDER = Comparator
            .comparing(FaultScenario::workloadPlanId, Comparator.nullsFirst(String::compareTo))
            .thenComparing(FaultScenario::assignedVector, Comparator.nullsFirst(String::compareTo))
            .thenComparing(FaultScenario::deterministicId, Comparator.nullsFirst(String::compareTo));
    static final String PACKAGE_LOCK_FILE_NAME = ".on-demand-fault-scenario.lock";

    private static final ConcurrentHashMap<Path, Object> PACKAGE_LOCKS = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;
    private final RecoveryScheduleSource recoveryScheduleSource;
    private final FailureInjector failureInjector;
    private final FileMover fileMover;
    private final TemporaryFileCleaner temporaryFileCleaner;
    private final PackageLockProvider packageLockProvider;

    public OnDemandFaultScenarioService() {
        this(RecoveryScheduleGenerator::generate, boundary -> { }, new NioFileMover(), Files::deleteIfExists,
                new NioPackageLockProvider());
    }

    OnDemandFaultScenarioService(PackageLockProvider packageLockProvider) {
        this(RecoveryScheduleGenerator::generate, boundary -> { }, new NioFileMover(), Files::deleteIfExists,
                packageLockProvider);
    }

    OnDemandFaultScenarioService(RecoveryScheduleSource recoveryScheduleSource,
                                 FailureInjector failureInjector,
                                 FileMover fileMover) {
        this(recoveryScheduleSource, failureInjector, fileMover, Files::deleteIfExists,
                new NioPackageLockProvider());
    }

    OnDemandFaultScenarioService(RecoveryScheduleSource recoveryScheduleSource,
                                 FailureInjector failureInjector,
                                 FileMover fileMover,
                                 TemporaryFileCleaner temporaryFileCleaner) {
        this(recoveryScheduleSource, failureInjector, fileMover, temporaryFileCleaner,
                new NioPackageLockProvider());
    }

    OnDemandFaultScenarioService(RecoveryScheduleSource recoveryScheduleSource,
                                 FailureInjector failureInjector,
                                 FileMover fileMover,
                                 TemporaryFileCleaner temporaryFileCleaner,
                                 PackageLockProvider packageLockProvider) {
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                .enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS);
        this.recoveryScheduleSource = Objects.requireNonNull(recoveryScheduleSource);
        this.failureInjector = Objects.requireNonNull(failureInjector);
        this.fileMover = Objects.requireNonNull(fileMover);
        this.temporaryFileCleaner = Objects.requireNonNull(temporaryFileCleaner);
        this.packageLockProvider = Objects.requireNonNull(packageLockProvider);
    }

    public OnDemandFaultScenarioResult request(OnDemandFaultScenarioRequest request) {
        if (request == null || request.manifestPath() == null) {
            return failure(OnDemandFaultScenarioResult.Status.REJECTED, request, null,
                    "MISSING_MANIFEST_PATH", "A current scenario package manifest path is required");
        }
        Path manifestPath = request.manifestPath().toAbsolutePath().normalize();
        Path packageIdentity;
        try {
            packageIdentity = resolvePackageIdentity(manifestPath);
        } catch (RuntimeException exception) {
            return failure(OnDemandFaultScenarioResult.Status.REJECTED, request, null,
                    "INVALID_PACKAGE", rootMessage(exception));
        }
        Object packageLock = PACKAGE_LOCKS.computeIfAbsent(packageIdentity, ignored -> new Object());
        synchronized (packageLock) {
            return requestWithPackageLock(request, manifestPath, packageIdentity);
        }
    }

    private Path resolvePackageIdentity(Path manifestPath) {
        Path packageRoot = manifestPath.getParent();
        if (packageRoot == null || !Files.isRegularFile(manifestPath, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException("Scenario package manifest does not exist: " + manifestPath);
        }
        rejectSymlinkSegments(manifestPath);
        try {
            return packageRoot.toRealPath();
        } catch (IOException exception) {
            throw new IllegalArgumentException("Failed to resolve scenario package directory", exception);
        }
    }

    private OnDemandFaultScenarioResult requestWithPackageLock(OnDemandFaultScenarioRequest request,
                                                                 Path manifestPath,
                                                                 Path packageIdentity) {
        PackageLockHandle lockHandle;
        try {
            lockHandle = packageLockProvider.open(packageIdentity.resolve(PACKAGE_LOCK_FILE_NAME));
        } catch (IOException | RuntimeException exception) {
            return packageLockFailure(request, exception);
        }
        try {
            lockHandle.acquire();
        } catch (IOException | RuntimeException exception) {
            closeIgnoringFailure(lockHandle);
            return packageLockFailure(request, exception);
        }

        try {
            try {
                return requestLocked(request, manifestPath);
            } catch (RuntimeException exception) {
                return failure(OnDemandFaultScenarioResult.Status.INTEGRITY_FAILURE, request, null,
                        "REQUEST_PROCESSING_FAILED", rootMessage(exception));
            }
        } finally {
            closeIgnoringFailure(lockHandle);
        }
    }

    private OnDemandFaultScenarioResult packageLockFailure(OnDemandFaultScenarioRequest request,
                                                            Exception exception) {
        return failure(OnDemandFaultScenarioResult.Status.PERSISTENCE_FAILED, request, null,
                "PACKAGE_LOCK_FAILED", rootMessage(exception));
    }

    private void closeIgnoringFailure(PackageLockHandle lockHandle) {
        try {
            lockHandle.close();
        } catch (IOException | RuntimeException ignored) {
            // The request result reflects package publication, not lock-resource cleanup.
        }
    }

    private OnDemandFaultScenarioResult requestLocked(OnDemandFaultScenarioRequest request, Path manifestPath) {
        if (!isCurrentManifest(manifestPath)) {
            return failure(OnDemandFaultScenarioResult.Status.REJECTED, request, null,
                    "INVALID_PACKAGE", "Only the current role-keyed scenario package is accepted");
        }
        return requestCurrentLocked(request, manifestPath);

    }

    private boolean isCurrentManifest(Path manifestPath) {
        try {
            JsonNode node = objectMapper.readTree(Files.readAllBytes(manifestPath));
            return node != null && node.has("formatVersion") && node.has("files") && !node.has("schemaVersion");
        } catch (IOException exception) {
            throw new IllegalArgumentException("Malformed scenario package manifest", exception);
        }
    }

    /** Current-package request path. All four mutable artifacts are staged and validated together. */
    private OnDemandFaultScenarioResult requestCurrentLocked(OnDemandFaultScenarioRequest request, Path manifestPath) {
        ScenarioCatalogPackageReader.ExecutablePackageContents raw;
        ScenarioCatalogPackageReader.PackageContents projected;
        try {
            ScenarioCatalogPackageReader reader = new ScenarioCatalogPackageReader();
            raw = reader.readCurrent(manifestPath);
            projected = reader.readCurrentForExecution(manifestPath);
        } catch (RuntimeException exception) {
            return failure(OnDemandFaultScenarioResult.Status.REJECTED, request, null,
                    "INVALID_PACKAGE", rootMessage(exception));
        }
        int defaultCap = raw.accounting().path("configuration").path("maxRecoverySchedulesPerVector").asInt(0);
        if (defaultCap <= 0) {
            return failure(OnDemandFaultScenarioResult.Status.INTEGRITY_FAILURE, request, null,
                    "MISSING_RECOVERY_CAP", "Current accounting.configuration.maxRecoverySchedulesPerVector must be positive");
        }
        int cap = defaultCap;
        if (request.assertedRecoveryScheduleCap() != null) {
            try {
                cap = RecoveryScheduleCap.parse(request.assertedRecoveryScheduleCap()).value();
            } catch (IllegalArgumentException exception) {
                return failure(OnDemandFaultScenarioResult.Status.REJECTED, request, defaultCap,
                        "INVALID_ASSERTED_RECOVERY_CAP", exception.getMessage());
            }
        }
        WorkloadPlan workload = projected.workloadPlans().stream()
                .filter(candidate -> Objects.equals(candidate.deterministicId(), request.workloadPlanId()))
                .findFirst().orElse(null);
        if (workload == null) {
            return failure(OnDemandFaultScenarioResult.Status.REJECTED, request, cap,
                    "WORKLOAD_PLAN_NOT_FOUND", "No WorkloadPlan exists with id " + request.workloadPlanId());
        }
        WorkloadMaterializability materializability = EagerFaultScenarioGenerator.evaluateMaterializability(workload);
        if (!materializability.materializable()) {
            return failure(OnDemandFaultScenarioResult.Status.REJECTED, request, cap,
                    "WORKLOAD_NOT_MATERIALIZABLE", String.join("; ", materializability.diagnostics()));
        }
        if (request.assignedVector() == null || request.assignedVector().length() != workload.faultSlots().size()) {
            return failure(OnDemandFaultScenarioResult.Status.REJECTED, request, cap,
                    "INVALID_VECTOR_LENGTH", "Vector length must equal the WorkloadPlan fault-slot count " + workload.faultSlots().size());
        }
        if (!request.assignedVector().matches("[01]*")) {
            return failure(OnDemandFaultScenarioResult.Status.REJECTED, request, cap,
                    "NON_BINARY_VECTOR", "Vector must contain only binary digits");
        }
        RecoveryScheduleGenerationResult generated;
        try {
            generated = recoveryScheduleSource.generate(workload, request.assignedVector(), cap);
        } catch (RuntimeException exception) {
            return failure(OnDemandFaultScenarioResult.Status.INTEGRITY_FAILURE, request, cap,
                    "GENERATION_FAILED", rootMessage(exception));
        }
        if (generated.recoveryScheduleCap() != cap || generated.writtenScheduleCount() <= 0
                || generated.writtenScheduleCount() > cap
                || generated.faultScenarios().size() != generated.writtenScheduleCount()
                || generated.uncappedScheduleCount().compareTo(BigInteger.valueOf(generated.writtenScheduleCount())) < 0) {
            return failure(OnDemandFaultScenarioResult.Status.INTEGRITY_FAILURE, request, cap,
                    "INVALID_GENERATION_RESULT", "Generated recovery counts do not match the requested cap");
        }
        Map<String, JsonNode> existingById = new LinkedHashMap<>();
        Map<JsonNode, List<JsonNode>> existingByExecutableContent = new LinkedHashMap<>();
        raw.faultScenarioRecords().stream()
                .sorted(Comparator.comparing(scenario -> scenario.path("id").asText()))
                .forEach(scenario -> {
                    existingById.put(scenario.path("id").asText(), scenario);
                    existingByExecutableContent.computeIfAbsent(executableContent(scenario), ignored -> new ArrayList<>())
                            .add(scenario);
                });
        List<JsonNode> additions = new ArrayList<>();
        List<String> generatedIds = new ArrayList<>();
        ExecutableArtifactWriter compactWriter = new ExecutableArtifactWriter();
        for (FaultScenario scenario : generated.faultScenarios()) {
            Map<String, Object> compact = compactWriter.currentFaultRecord(scenario, projected.workloadPlans());
            JsonNode node = objectMapper.valueToTree(compact);
            String id = node.path("id").asText();
            JsonNode sameId = existingById.get(id);
            if (sameId != null && !sameId.equals(node)) {
                return failure(OnDemandFaultScenarioResult.Status.INTEGRITY_FAILURE, request, cap,
                        "FAULT_SCENARIO_ID_COLLISION", "FaultScenario id " + id + " has different semantic content");
            }
            List<JsonNode> equivalent = existingByExecutableContent.get(executableContent(node));
            if (equivalent == null || equivalent.isEmpty()) {
                additions.add(node);
                existingById.put(id, node);
                existingByExecutableContent.computeIfAbsent(executableContent(node), ignored -> new ArrayList<>())
                        .add(node);
                generatedIds.add(id);
            } else {
                generatedIds.add(equivalent.getFirst().path("id").asText());
            }
        }
        generatedIds.sort(String::compareTo);
        String requestKey = request.workloadPlanId() + "\u0000" + request.assignedVector() + "\u0000" + cap;
        for (JsonNode prior : raw.requestRecords()) {
            String key = prior.path("workload").asText() + "\u0000" + prior.path("faultVector").asText()
                    + "\u0000" + prior.path("effectiveRecoveryScheduleCap").asText();
            if (requestKey.equals(key)) {
                List<String> priorIds = new ArrayList<>();
                prior.path("faultScenarioIds").forEach(value -> priorIds.add(value.asText()));
                priorIds.sort(String::compareTo);
                return success(OnDemandFaultScenarioResult.Status.DEDUPLICATED, request, cap, generated, 0, priorIds);
            }
        }
        List<JsonNode> mergedFaults = new ArrayList<>(raw.faultScenarioRecords());
        mergedFaults.addAll(additions);
        mergedFaults.sort(Comparator.comparing(node -> node.path("id").asText()));
        ObjectNode requestRecord = objectMapper.createObjectNode();
        requestRecord.put("workload", request.workloadPlanId());
        requestRecord.put("faultVector", request.assignedVector());
        requestRecord.put("effectiveRecoveryScheduleCap", cap);
        requestRecord.put("uncappedPossibleRecoverySchedules", generated.uncappedScheduleCount());
        ArrayNode requestIds = requestRecord.putArray("faultScenarioIds");
        generatedIds.forEach(requestIds::add);
        List<JsonNode> mergedRequests = new ArrayList<>(raw.requestRecords());
        mergedRequests.add(requestRecord);

        try {
            ObjectNode account = (ObjectNode) raw.accounting().deepCopy();
            ObjectNode current = account.with("faultScenarios").with("current");
            int priorVectors = current.path("vectorsComputed").asInt(0);
            BigInteger priorPossible = bigInteger(current.path("possibleForComputedVectors"));
            int priorWritten = current.path("written").asInt(raw.faultScenarioRecords().size());
            boolean vectorWasAbsent = additions.size() > 0 && raw.faultScenarioRecords().stream()
                    .noneMatch(node -> request.workloadPlanId().equals(node.path("workload").asText())
                            && request.assignedVector().equals(node.path("faultVector").asText()));
            if (vectorWasAbsent) {
                current.put("vectorsComputed", priorVectors + 1);
                current.put("possibleForComputedVectors", priorPossible.add(generated.uncappedScheduleCount()));
            }
            current.put("written", mergedFaults.size());
            account.with("requests").put("written", mergedRequests.size());
            byte[] faultBytes = jsonLinesBytes(mergedFaults);
            byte[] requestBytes = jsonLinesBytes(mergedRequests);
            byte[] accountBytes = compactBytes(account);
            byte[] manifestBytes = reviseCurrentManifest(manifestPath, faultBytes, requestBytes, accountBytes);
            publishCurrentRevision(manifestPath, raw, faultBytes, requestBytes, accountBytes, manifestBytes);
            return success(OnDemandFaultScenarioResult.Status.PERSISTED, request, cap, generated,
                    additions.size(), generatedIds);
        } catch (Exception exception) {
            return failure(OnDemandFaultScenarioResult.Status.PERSISTENCE_FAILED, request, cap,
                    "PACKAGE_REVISION_FAILED", rootMessage(exception));
        }
    }

    private ObjectNode executableContent(JsonNode scenario) {
        // The current package's workload owns setup, participants, slots, event origins and
        // recovery semantics. Its fault record adds the vector and exact ordered action refs.
        // Keep the persisted id out of this request-local equality check so historical ids survive.
        ObjectNode content = objectMapper.createObjectNode();
        content.set("workload", scenario.path("workload"));
        content.set("faultVector", scenario.path("faultVector"));
        content.set("actions", scenario.path("actions"));
        return content;
    }

    private byte[] reviseCurrentManifest(Path manifestPath,
                                         byte[] faultBytes,
                                         byte[] requestBytes,
                                         byte[] accountingBytes) throws IOException {
        ObjectNode manifest = (ObjectNode) objectMapper.readTree(Files.readAllBytes(manifestPath));
        ObjectNode files = (ObjectNode) manifest.path("files");
        files.with("faultScenarios").put("sha256", sha256(faultBytes));
        files.with("requests").put("sha256", sha256(requestBytes));
        files.with("accounting").put("sha256", sha256(accountingBytes));
        return compactBytes(manifest);
    }

    private void publishCurrentRevision(Path manifestPath,
                                        ScenarioCatalogPackageReader.ExecutablePackageContents original,
                                        byte[] faultBytes,
                                        byte[] requestBytes,
                                        byte[] accountingBytes,
                                        byte[] manifestBytes) throws IOException {
        Path faultPath = original.faultScenarioPath();
        Path requestPath = original.requestPath();
        Path accountingPath = original.accountingPath();
        List<Path> temporaryFiles = new ArrayList<>();
        Map<Path, byte[]> originals = new LinkedHashMap<>();
        for (Path path : List.of(faultPath, requestPath, accountingPath, manifestPath)) originals.put(path, Files.readAllBytes(path));
        boolean promotionStarted = false;
        IOException publicationFailure = null;
        try {
            Path stagedFault = stage(faultPath, faultBytes, ".current-fault-"); temporaryFiles.add(stagedFault);
            failureInjector.at(Boundary.FAULT_SCENARIO_STAGED);
            Path stagedRequest = stage(requestPath, requestBytes, ".current-request-"); temporaryFiles.add(stagedRequest);
            failureInjector.at(Boundary.REQUEST_STAGED);
            Path stagedAccounting = stage(accountingPath, accountingBytes, ".current-accounting-"); temporaryFiles.add(stagedAccounting);
            failureInjector.at(Boundary.ACCOUNTING_STAGED);
            Path stagedManifest = stage(manifestPath, manifestBytes, ".current-manifest-"); temporaryFiles.add(stagedManifest);
            failureInjector.at(Boundary.MANIFEST_STAGED);

            ObjectNode validation = (ObjectNode) objectMapper.readTree(manifestBytes);
            ObjectNode files = (ObjectNode) validation.path("files");
            Path root = manifestPath.getParent();
            files.with("faultScenarios").put("path", root.relativize(stagedFault).toString());
            files.with("requests").put("path", root.relativize(stagedRequest).toString());
            files.with("accounting").put("path", root.relativize(stagedAccounting).toString());
            Path validationManifest = stage(manifestPath, compactBytes(validation), ".current-validation-"); temporaryFiles.add(validationManifest);
            new ScenarioCatalogPackageReader().readCurrent(validationManifest);

            promotionStarted = true;
            promote(stagedFault, faultPath); failureInjector.at(Boundary.FAULT_SCENARIO_PROMOTED);
            promote(stagedRequest, requestPath); failureInjector.at(Boundary.REQUEST_PROMOTED);
            promote(stagedAccounting, accountingPath); failureInjector.at(Boundary.ACCOUNTING_PROMOTED);
            promote(stagedManifest, manifestPath); failureInjector.at(Boundary.MANIFEST_PROMOTED);
            new ScenarioCatalogPackageReader().readCurrent(manifestPath);
        } catch (Exception exception) {
            if (promotionStarted) {
                try { for (Map.Entry<Path, byte[]> entry : originals.entrySet()) restore(entry.getKey(), entry.getValue()); }
                catch (Exception rollbackException) { exception.addSuppressed(rollbackException); }
            }
            publicationFailure = exception instanceof IOException ioException ? ioException
                    : new IOException("Failed to publish current package revision", exception);
        }
        IOException cleanupFailure = cleanupTemporaryFiles(temporaryFiles);
        if (publicationFailure != null) {
            if (cleanupFailure != null) publicationFailure.addSuppressed(cleanupFailure);
            throw publicationFailure;
        }
    }

    private byte[] jsonLinesBytes(List<? extends JsonNode> records) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        for (JsonNode record : records) { output.write(objectMapper.writeValueAsBytes(record)); output.write('\n'); }
        return output.toByteArray();
    }

    private byte[] compactBytes(Object value) throws IOException {
        return objectMapper.writeValueAsBytes(value);
    }

    private String sha256(byte[] bytes) {
        try { return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (java.security.NoSuchAlgorithmException exception) { throw new IllegalStateException("SHA-256 is unavailable", exception); }
    }

    private BigInteger bigInteger(JsonNode node) {
        if (node == null || !node.isIntegralNumber()) return BigInteger.ZERO;
        return node.bigIntegerValue();
    }


    private IOException cleanupTemporaryFiles(List<Path> temporaryFiles) {
        IOException cleanupFailure = null;
        for (Path temporaryFile : temporaryFiles) {
            try {
                temporaryFileCleaner.deleteIfExists(temporaryFile);
            } catch (IOException exception) {
                if (cleanupFailure == null) {
                    cleanupFailure = exception;
                } else {
                    cleanupFailure.addSuppressed(exception);
                }
            }
        }
        return cleanupFailure;
    }

    private Path stage(Path target, byte[] bytes, String prefix) throws IOException {
        Path parent = target.getParent();
        if (parent == null) {
            parent = Path.of(".").toAbsolutePath().normalize();
        }
        Path staged = Files.createTempFile(parent, prefix, ".tmp");
        Files.write(staged, bytes);
        return staged;
    }

    private void promote(Path staged, Path target) throws IOException {
        try {
            fileMover.atomicMove(staged, target);
        } catch (AtomicMoveNotSupportedException exception) {
            fileMover.fallbackMove(staged, target);
        }
    }


    private void restore(Path target, byte[] bytes) throws IOException {
        Path staged = stage(target, bytes, ".rollback-");
        try {
            promote(staged, target);
        } finally {
            Files.deleteIfExists(staged);
        }
    }

    private void rejectSymlinkSegments(Path path) {
        Path absolute = path.toAbsolutePath().normalize();
        Path current = absolute.getRoot();
        for (Path segment : absolute) {
            current = current == null ? segment : current.resolve(segment);
            if (Files.exists(current, LinkOption.NOFOLLOW_LINKS) && Files.isSymbolicLink(current)) {
                throw new IllegalArgumentException("Scenario package paths must not traverse symbolic links: " + path);
            }
        }
    }

    private OnDemandFaultScenarioResult success(OnDemandFaultScenarioResult.Status status,
                                                 OnDemandFaultScenarioRequest request,
                                                 int cap,
                                                 RecoveryScheduleGenerationResult generated,
                                                 int additions,
                                                 List<String> ids) {
        return new OnDemandFaultScenarioResult(
                status,
                request.workloadPlanId(),
                request.assignedVector(),
                cap,
                generated.uncappedScheduleCount().toString(),
                generated.writtenScheduleCount(),
                additions,
                ids,
                List.of());
    }

    private OnDemandFaultScenarioResult failure(OnDemandFaultScenarioResult.Status status,
                                                 OnDemandFaultScenarioRequest request,
                                                 Integer cap,
                                                 String code,
                                                 String message) {
        return new OnDemandFaultScenarioResult(
                status,
                request == null ? null : request.workloadPlanId(),
                request == null ? null : request.assignedVector(),
                cap,
                null,
                0,
                0,
                List.of(),
                List.of(new OnDemandFaultScenarioResult.Diagnostic(code, message)));
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }

    @FunctionalInterface
    interface RecoveryScheduleSource {
        RecoveryScheduleGenerationResult generate(WorkloadPlan workload, String vector, int cap);
    }

    @FunctionalInterface
    interface FailureInjector {
        void at(Boundary boundary) throws IOException;
    }

    interface FileMover {
        void atomicMove(Path source, Path target) throws IOException;

        void fallbackMove(Path source, Path target) throws IOException;
    }

    @FunctionalInterface
    interface TemporaryFileCleaner {
        void deleteIfExists(Path path) throws IOException;
    }

    interface PackageLockProvider {
        PackageLockHandle open(Path lockPath) throws IOException;
    }

    interface PackageLockHandle extends AutoCloseable {
        void acquire() throws IOException;

        @Override
        void close() throws IOException;
    }

    @FunctionalInterface
    interface LockAcquisitionObserver {
        void beforeAcquire(Path lockPath) throws IOException;
    }

    enum Boundary {
        FAULT_SCENARIO_STAGED,
        REQUEST_STAGED,
        ACCOUNTING_STAGED,
        MANIFEST_STAGED,
        FAULT_SCENARIO_PROMOTED,
        REQUEST_PROMOTED,
        ACCOUNTING_PROMOTED,
        MANIFEST_PROMOTED
    }

    static final class NioPackageLockProvider implements PackageLockProvider {
        private final LockAcquisitionObserver observer;

        NioPackageLockProvider() {
            this(ignored -> { });
        }

        NioPackageLockProvider(LockAcquisitionObserver observer) {
            this.observer = Objects.requireNonNull(observer);
        }

        @Override
        public PackageLockHandle open(Path lockPath) throws IOException {
            if (Files.exists(lockPath, LinkOption.NOFOLLOW_LINKS)
                    && !Files.isRegularFile(lockPath, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("Package lock path is not a regular file: " + lockPath);
            }
            FileChannel channel = FileChannel.open(
                    lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS);
            return new NioPackageLockHandle(lockPath, channel, observer);
        }
    }

    private static final class NioPackageLockHandle implements PackageLockHandle {
        private final Path lockPath;
        private final FileChannel channel;
        private final LockAcquisitionObserver observer;
        private FileLock lock;

        private NioPackageLockHandle(Path lockPath,
                                     FileChannel channel,
                                     LockAcquisitionObserver observer) {
            this.lockPath = lockPath;
            this.channel = channel;
            this.observer = observer;
        }

        @Override
        public void acquire() throws IOException {
            observer.beforeAcquire(lockPath);
            lock = channel.lock();
            if (!Files.isRegularFile(lockPath, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("Package lock path is not a regular file: " + lockPath);
            }
        }

        @Override
        public void close() throws IOException {
            IOException failure = null;
            if (lock != null) {
                try {
                    lock.close();
                } catch (IOException exception) {
                    failure = exception;
                }
            }
            try {
                channel.close();
            } catch (IOException exception) {
                if (failure == null) {
                    failure = exception;
                } else {
                    failure.addSuppressed(exception);
                }
            }
            if (failure != null) {
                throw failure;
            }
        }
    }

    static final class NioFileMover implements FileMover {
        @Override
        public void atomicMove(Path source, Path target) throws IOException {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        }

        @Override
        public void fallbackMove(Path source, Path target) throws IOException {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

}
