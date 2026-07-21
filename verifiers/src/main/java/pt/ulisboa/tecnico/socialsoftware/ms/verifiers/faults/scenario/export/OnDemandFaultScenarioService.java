package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.export;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
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
                    "MISSING_MANIFEST_PATH", "A v3 package manifest path is required");
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
        ValidatedPackage validated;
        try {
            validated = readValidatedPackage(manifestPath);
        } catch (RuntimeException exception) {
            return failure(OnDemandFaultScenarioResult.Status.REJECTED, request, null,
                    "INVALID_PACKAGE", rootMessage(exception));
        }

        ScenarioCatalogManifest manifest = validated.contents().manifest();
        RecoveryScheduleCap cap = new RecoveryScheduleCap(manifest.recoveryScheduleCap());
        if (request.assertedRecoveryScheduleCap() != null) {
            RecoveryScheduleCap asserted;
            try {
                asserted = RecoveryScheduleCap.parse(request.assertedRecoveryScheduleCap());
            } catch (IllegalArgumentException exception) {
                return failure(OnDemandFaultScenarioResult.Status.REJECTED, request, cap.value(),
                        "INVALID_ASSERTED_RECOVERY_CAP", exception.getMessage());
            }
            if (asserted.value() != cap.value()) {
                return failure(OnDemandFaultScenarioResult.Status.REJECTED, request, cap.value(),
                        "RECOVERY_CAP_MISMATCH", "Package recovery cap is " + cap.value()
                                + " but request asserted " + asserted.value());
            }
        }

        WorkloadPlan workload = validated.contents().workloadPlans().stream()
                .filter(candidate -> Objects.equals(candidate.deterministicId(), request.workloadPlanId()))
                .findFirst()
                .orElse(null);
        if (workload == null) {
            return failure(OnDemandFaultScenarioResult.Status.REJECTED, request, cap.value(),
                    "WORKLOAD_PLAN_NOT_FOUND", "No WorkloadPlan exists with id " + request.workloadPlanId());
        }

        WorkloadMaterializability materializability = EagerFaultScenarioGenerator.evaluateMaterializability(workload);
        if (!materializability.materializable()) {
            return failure(OnDemandFaultScenarioResult.Status.REJECTED, request, cap.value(),
                    "WORKLOAD_NOT_MATERIALIZABLE", String.join("; ", materializability.diagnostics()));
        }
        if (request.assignedVector() == null
                || request.assignedVector().length() != workload.faultSlots().size()) {
            return failure(OnDemandFaultScenarioResult.Status.REJECTED, request, cap.value(),
                    "INVALID_VECTOR_LENGTH", "Vector length must equal the WorkloadPlan fault-slot count "
                            + workload.faultSlots().size());
        }
        if (!request.assignedVector().matches("[01]*")) {
            return failure(OnDemandFaultScenarioResult.Status.REJECTED, request, cap.value(),
                    "NON_BINARY_VECTOR", "Vector must contain only binary digits");
        }

        RecoveryScheduleGenerationResult generated;
        try {
            generated = recoveryScheduleSource.generate(workload, request.assignedVector(), cap.value());
        } catch (RuntimeException exception) {
            return failure(OnDemandFaultScenarioResult.Status.INTEGRITY_FAILURE, request, cap.value(),
                    "GENERATION_FAILED", rootMessage(exception));
        }

        if (generated.recoveryScheduleCap() != cap.value()
                || generated.writtenScheduleCount() <= 0
                || generated.writtenScheduleCount() > cap.value()
                || generated.faultScenarios().size() != generated.writtenScheduleCount()
                || generated.uncappedScheduleCount().compareTo(BigInteger.valueOf(generated.writtenScheduleCount())) < 0) {
            return failure(OnDemandFaultScenarioResult.Status.INTEGRITY_FAILURE, request, cap.value(),
                    "INVALID_GENERATION_RESULT", "Generated recovery counts do not match the frozen package cap and records");
        }

        Map<String, FaultScenario> existingById = validated.contents().faultScenarios().stream()
                .collect(Collectors.toMap(
                        FaultScenario::deterministicId,
                        scenario -> scenario,
                        (left, right) -> left,
                        LinkedHashMap::new));
        List<FaultScenario> additions = new ArrayList<>();
        java.util.Set<String> generatedIdsSeen = new java.util.HashSet<>();
        FaultScenarioValidator validator = new FaultScenarioValidator();
        for (FaultScenario scenario : generated.faultScenarios()) {
            if (scenario == null
                    || !generatedIdsSeen.add(scenario.deterministicId())
                    || !Objects.equals(workload.deterministicId(), scenario.workloadPlanId())
                    || !Objects.equals(request.assignedVector(), scenario.assignedVector())) {
                return failure(OnDemandFaultScenarioResult.Status.INTEGRITY_FAILURE, request, cap.value(),
                        "INVALID_GENERATED_FAULT_SCENARIO", "Generated records must be unique and match the requested workload/vector");
            }
            FaultScenario existing = existingById.get(scenario.deterministicId());
            if (existing != null) {
                if (!canonicalScenarioBytes(existing).equals(canonicalScenarioBytes(scenario))) {
                    return failure(OnDemandFaultScenarioResult.Status.INTEGRITY_FAILURE, request, cap.value(),
                            "FAULT_SCENARIO_ID_COLLISION", "FaultScenario id " + scenario.deterministicId()
                                    + " has different semantic content");
                }
                continue;
            }
            FaultScenarioValidator.ValidationResult scenarioValidation = validator.validate(scenario, workload);
            if (!scenarioValidation.valid()) {
                return failure(OnDemandFaultScenarioResult.Status.INTEGRITY_FAILURE, request, cap.value(),
                        "INVALID_GENERATED_FAULT_SCENARIO", scenarioValidation.diagnostics().toString());
            }
            additions.add(scenario);
        }

        String vectorKey = request.workloadPlanId() + "\u0000" + request.assignedVector();
        ScenarioSpaceAccountingReport.ComputedVectorRecoverySpace existingVector = validated.accounting()
                .faultScenarioCatalogSpace().perComputedVectorRecoverySpace().stream()
                .filter(row -> vectorKey.equals(row.workloadPlanId() + "\u0000" + row.assignedVector()))
                .findFirst()
                .orElse(null);
        List<String> generatedIds = generated.faultScenarios().stream()
                .map(FaultScenario::deterministicId)
                .sorted()
                .toList();
        if (additions.isEmpty()) {
            if (existingVector == null) {
                return failure(OnDemandFaultScenarioResult.Status.INTEGRITY_FAILURE, request, cap.value(),
                        "MISSING_VECTOR_ACCOUNTING", "Existing FaultScenarios have no computed-vector accounting row");
            }
            List<String> existingIds = validated.contents().faultScenarios().stream()
                    .filter(scenario -> Objects.equals(request.workloadPlanId(), scenario.workloadPlanId()))
                    .filter(scenario -> Objects.equals(request.assignedVector(), scenario.assignedVector()))
                    .map(FaultScenario::deterministicId)
                    .sorted()
                    .toList();
            if (!exact(existingVector.uncappedUniqueScheduleCount(), "uncappedUniqueScheduleCount")
                    .equals(generated.uncappedScheduleCount())
                    || !exact(existingVector.writtenScheduleCount(), "writtenScheduleCount")
                    .equals(BigInteger.valueOf(generated.writtenScheduleCount()))
                    || !existingIds.equals(generatedIds)) {
                return failure(OnDemandFaultScenarioResult.Status.INTEGRITY_FAILURE, request, cap.value(),
                        "DETERMINISTIC_VECTOR_MISMATCH",
                        "Persisted vector counts or FaultScenario ids differ from fresh deterministic generation");
            }
            return success(OnDemandFaultScenarioResult.Status.DEDUPLICATED, request, cap.value(), generated,
                    0, generatedIds);
        }
        if (existingVector != null) {
            return failure(OnDemandFaultScenarioResult.Status.INTEGRITY_FAILURE, request, cap.value(),
                    "PARTIAL_VECTOR_REVISION", "A computed vector row exists without its complete deterministic scenario set");
        }

        List<FaultScenario> mergedScenarios = new ArrayList<>(validated.contents().faultScenarios());
        mergedScenarios.addAll(additions);
        mergedScenarios.sort(FAULT_SCENARIO_ORDER);
        ComputedVectorRecovery computedVector = new ComputedVectorRecovery(
                workload.deterministicId(),
                request.assignedVector(),
                FaultScenarioVectorSource.ON_DEMAND_REQUEST,
                generated.uncappedScheduleCount(),
                generated.writtenScheduleCount());
        ScenarioSpaceAccountingReport revisedAccounting;
        try {
            revisedAccounting = validated.accounting().withOnDemandVector(computedVector, mergedScenarios.size());
        } catch (RuntimeException exception) {
            return failure(OnDemandFaultScenarioResult.Status.INTEGRITY_FAILURE, request, cap.value(),
                    "INVALID_ACCOUNTING_REVISION", rootMessage(exception));
        }

        byte[] faultScenarioBytes;
        byte[] accountingBytes;
        byte[] manifestBytes;
        ScenarioCatalogManifest revisedManifest;
        try {
            faultScenarioBytes = serializeFaultScenarios(mergedScenarios);
            accountingBytes = prettyBytes(revisedAccounting);
            revisedManifest = reviseManifest(manifest, revisedAccounting, mergedScenarios.size(),
                    faultScenarioBytes, accountingBytes);
            manifestBytes = prettyBytes(revisedManifest);
        } catch (RuntimeException exception) {
            return failure(OnDemandFaultScenarioResult.Status.INTEGRITY_FAILURE, request, cap.value(),
                    "SERIALIZATION_FAILED", rootMessage(exception));
        }

        Map<Path, byte[]> originals;
        try {
            originals = Map.of(
                    validated.contents().faultScenarioCatalogPath(),
                    Files.readAllBytes(validated.contents().faultScenarioCatalogPath()),
                    validated.contents().accountingPath(),
                    Files.readAllBytes(validated.contents().accountingPath()),
                    manifestPath,
                    Files.readAllBytes(manifestPath));
        } catch (IOException exception) {
            return failure(OnDemandFaultScenarioResult.Status.PERSISTENCE_FAILED, request, cap.value(),
                    "SNAPSHOT_FAILED", rootMessage(exception));
        }

        try {
            publishRevision(
                    manifestPath,
                    validated,
                    revisedManifest,
                    faultScenarioBytes,
                    accountingBytes,
                    manifestBytes,
                    originals);
            return success(OnDemandFaultScenarioResult.Status.PERSISTED, request, cap.value(), generated,
                    additions.size(), generatedIds);
        } catch (Exception exception) {
            return failure(OnDemandFaultScenarioResult.Status.PERSISTENCE_FAILED, request, cap.value(),
                    "PACKAGE_REVISION_FAILED", rootMessage(exception));
        }
    }

    private void publishRevision(Path manifestPath,
                                 ValidatedPackage originalPackage,
                                 ScenarioCatalogManifest revisedManifest,
                                 byte[] faultScenarioBytes,
                                 byte[] accountingBytes,
                                 byte[] manifestBytes,
                                 Map<Path, byte[]> originals) throws IOException {
        Path faultPath = originalPackage.contents().faultScenarioCatalogPath();
        Path accountingPath = originalPackage.contents().accountingPath();
        List<Path> temporaryFiles = new ArrayList<>();
        boolean promotionStarted = false;
        IOException publicationFailure = null;
        try {
            Path stagedFault = stage(faultPath, faultScenarioBytes, ".fault-scenario-");
            temporaryFiles.add(stagedFault);
            failureInjector.at(Boundary.FAULT_SCENARIO_STAGED);
            Path stagedAccounting = stage(accountingPath, accountingBytes, ".accounting-");
            temporaryFiles.add(stagedAccounting);
            failureInjector.at(Boundary.ACCOUNTING_STAGED);
            Path stagedManifest = stage(manifestPath, manifestBytes, ".manifest-");
            temporaryFiles.add(stagedManifest);
            failureInjector.at(Boundary.MANIFEST_STAGED);

            ScenarioCatalogManifest validationManifest = withStagedPaths(
                    revisedManifest, stagedFault, stagedAccounting);
            Path validationManifestPath = stage(manifestPath, prettyBytes(validationManifest), ".validation-manifest-");
            temporaryFiles.add(validationManifestPath);
            readValidatedPackage(validationManifestPath);

            promotionStarted = true;
            promote(stagedFault, faultPath);
            failureInjector.at(Boundary.FAULT_SCENARIO_PROMOTED);
            promote(stagedAccounting, accountingPath);
            failureInjector.at(Boundary.ACCOUNTING_PROMOTED);
            promote(stagedManifest, manifestPath);
            failureInjector.at(Boundary.MANIFEST_PROMOTED);
            readValidatedPackage(manifestPath);
        } catch (Exception exception) {
            if (promotionStarted) {
                try {
                    restoreOriginals(manifestPath, faultPath, accountingPath, originals);
                } catch (Exception rollbackException) {
                    exception.addSuppressed(rollbackException);
                }
            }
            publicationFailure = exception instanceof IOException ioException
                    ? ioException
                    : new IOException("Failed to publish package revision", exception);
        }

        IOException cleanupFailure = cleanupTemporaryFiles(temporaryFiles);
        if (publicationFailure != null) {
            if (cleanupFailure != null) {
                publicationFailure.addSuppressed(cleanupFailure);
            }
            throw publicationFailure;
        }
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

    private void restoreOriginals(Path manifestPath,
                                  Path faultPath,
                                  Path accountingPath,
                                  Map<Path, byte[]> originals) throws IOException {
        restore(faultPath, originals.get(faultPath));
        restore(accountingPath, originals.get(accountingPath));
        restore(manifestPath, originals.get(manifestPath));
    }

    private void restore(Path target, byte[] bytes) throws IOException {
        Path staged = stage(target, bytes, ".rollback-");
        try {
            promote(staged, target);
        } finally {
            Files.deleteIfExists(staged);
        }
    }

    private ScenarioCatalogManifest withStagedPaths(ScenarioCatalogManifest manifest,
                                                     Path stagedFault,
                                                     Path stagedAccounting) {
        return new ScenarioCatalogManifest(
                manifest.schemaVersion(),
                manifest.generatedAt(),
                manifest.effectiveConfig(),
                manifest.generationSource(),
                manifest.materializabilityPolicy(),
                manifest.recoveryScheduleCap(),
                manifest.faultScenarioVectorSource(),
                manifest.workloadMaterializability(),
                manifest.counts(),
                manifest.warnings(),
                manifest.workloadCatalog(),
                withPath(manifest.faultScenarioCatalog(), stagedFault),
                withPath(manifest.scenarioSpaceAccounting(), stagedAccounting),
                manifest.rejectedInputsDiagnostic(),
                manifest.inputVariantsBySourceMode(),
                manifest.inputVariantsAcceptedBySourceMode(),
                manifest.inputVariantsRejectedBySourceModeReason());
    }

    private ScenarioCatalogManifest.ArtifactMetadata withPath(ScenarioCatalogManifest.ArtifactMetadata artifact,
                                                               Path path) {
        return new ScenarioCatalogManifest.ArtifactMetadata(
                artifact.artifactKind(), artifact.schemaVersion(), path.toString(), artifact.recordCount(), artifact.sha256());
    }

    private ScenarioCatalogManifest reviseManifest(ScenarioCatalogManifest manifest,
                                                    ScenarioSpaceAccountingReport accounting,
                                                    int faultScenarioCount,
                                                    byte[] faultScenarioBytes,
                                                    byte[] accountingBytes) {
        ScenarioSpaceAccountingReport.FaultScenarioCatalogSpace faultSpace = accounting.faultScenarioCatalogSpace();
        LinkedHashMap<String, String> counts = new LinkedHashMap<>(manifest.counts());
        counts.put("computedEagerVectors", faultSpace.computedEagerVectorCount());
        counts.put("computedOnDemandVectors", faultSpace.computedOnDemandVectorCount());
        counts.put("computedVectors", faultSpace.computedVectorCount());
        counts.put("computedVectorUncappedScheduleSum", faultSpace.exactComputedVectorUncappedScheduleSum());
        counts.put("computedVectorWrittenScheduleSum", faultSpace.exactComputedVectorWrittenScheduleSum());
        counts.put("faultScenariosExported", Integer.toString(faultScenarioCount));
        return new ScenarioCatalogManifest(
                manifest.schemaVersion(),
                manifest.generatedAt(),
                manifest.effectiveConfig(),
                STATIC_AND_ON_DEMAND_GENERATION_SOURCE,
                manifest.materializabilityPolicy(),
                manifest.recoveryScheduleCap(),
                EAGER_AND_ON_DEMAND_VECTOR_SOURCE,
                manifest.workloadMaterializability(),
                counts,
                manifest.warnings(),
                manifest.workloadCatalog(),
                withRevision(manifest.faultScenarioCatalog(), faultScenarioCount, faultScenarioBytes),
                withRevision(manifest.scenarioSpaceAccounting(), 1, accountingBytes),
                manifest.rejectedInputsDiagnostic(),
                manifest.inputVariantsBySourceMode(),
                manifest.inputVariantsAcceptedBySourceMode(),
                manifest.inputVariantsRejectedBySourceModeReason());
    }

    private ScenarioCatalogManifest.ArtifactMetadata withRevision(ScenarioCatalogManifest.ArtifactMetadata artifact,
                                                                   int recordCount,
                                                                   byte[] bytes) {
        return new ScenarioCatalogManifest.ArtifactMetadata(
                artifact.artifactKind(),
                artifact.schemaVersion(),
                artifact.path(),
                Integer.toString(recordCount),
                ScenarioCatalogJsonlWriter.sha256(bytes));
    }

    private byte[] serializeFaultScenarios(List<FaultScenario> scenarios) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            for (FaultScenario scenario : scenarios) {
                output.write(objectMapper.writeValueAsBytes(scenario));
                output.write('\n');
            }
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalArgumentException("Failed to serialize FaultScenario catalog", exception);
        }
    }

    private byte[] prettyBytes(Object value) {
        try {
            ObjectWriter writer = objectMapper.writerWithDefaultPrettyPrinter();
            return (writer.writeValueAsString(value) + "\n").getBytes(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Failed to serialize package artifact", exception);
        }
    }

    private String canonicalScenarioBytes(FaultScenario scenario) {
        try {
            return objectMapper.writeValueAsString(scenario);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Failed to compare FaultScenario semantic content", exception);
        }
    }

    private ValidatedPackage readValidatedPackage(Path manifestPath) {
        Path normalizedManifest = manifestPath.toAbsolutePath().normalize();
        ManifestBoundary boundary = validateManifestBoundary(normalizedManifest);
        ScenarioCatalogPackageReader.PackageContents contents = new ScenarioCatalogPackageReader().read(normalizedManifest);
        List<ArtifactPath> artifacts = List.of(
                new ArtifactPath(contents.manifest().workloadCatalog(), contents.workloadCatalogPath()),
                new ArtifactPath(contents.manifest().faultScenarioCatalog(), contents.faultScenarioCatalogPath()),
                new ArtifactPath(contents.manifest().scenarioSpaceAccounting(), contents.accountingPath()),
                new ArtifactPath(contents.manifest().rejectedInputsDiagnostic(), contents.rejectedInputsPath()));
        if (!artifacts.stream().map(artifact -> artifact.path().toAbsolutePath().normalize()).toList()
                .equals(boundary.artifactPaths())) {
            throw new IllegalArgumentException("Linked package artifact paths changed during package read");
        }
        Path packageRoot = boundary.packageRoot();
        for (ArtifactPath artifact : artifacts) {
            Path normalized = artifact.path().toAbsolutePath().normalize();
            if (!normalized.startsWith(packageRoot)) {
                throw new IllegalArgumentException("Linked package artifact escapes the package directory: " + normalized);
            }
            rejectSymlinkSegments(normalized);
        }

        ScenarioSpaceAccountingReport accounting;
        try {
            accounting = objectMapper.treeToValue(contents.accounting(), ScenarioSpaceAccountingReport.class);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Malformed scenario-space accounting", exception);
        }
        validateConsistency(contents, accounting);
        return new ValidatedPackage(contents, accounting);
    }

    private ManifestBoundary validateManifestBoundary(Path manifestPath) {
        Path packageRoot = manifestPath.getParent();
        if (packageRoot == null || !Files.isRegularFile(manifestPath, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException("Scenario package manifest does not exist: " + manifestPath);
        }
        rejectSymlinkSegments(manifestPath);

        ScenarioCatalogManifest manifest;
        try {
            manifest = objectMapper.readValue(Files.readAllBytes(manifestPath), ScenarioCatalogManifest.class);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Malformed scenario package manifest", exception);
        }
        if (!ScenarioCatalogManifest.SCHEMA_VERSION.equals(manifest.schemaVersion())) {
            throw new IllegalArgumentException("Unsupported scenario package manifest schema");
        }

        List<ScenarioCatalogManifest.ArtifactMetadata> artifacts = List.of(
                requireArtifactMetadata(manifest.workloadCatalog(), "WORKLOAD_CATALOG"),
                requireArtifactMetadata(manifest.faultScenarioCatalog(), "FAULT_SCENARIO_CATALOG"),
                requireArtifactMetadata(manifest.scenarioSpaceAccounting(), "SCENARIO_SPACE_ACCOUNTING"),
                requireArtifactMetadata(manifest.rejectedInputsDiagnostic(), "REJECTED_INPUT_DIAGNOSTIC"));
        List<Path> artifactPaths = new ArrayList<>();
        for (ScenarioCatalogManifest.ArtifactMetadata artifact : artifacts) {
            Path configured;
            try {
                configured = Path.of(artifact.path());
            } catch (RuntimeException exception) {
                throw new IllegalArgumentException("Manifest artifact path is invalid for "
                        + artifact.artifactKind());
            }
            Path resolved = (configured.isAbsolute() ? configured : packageRoot.resolve(configured))
                    .toAbsolutePath()
                    .normalize();
            if (!resolved.startsWith(packageRoot)) {
                throw new IllegalArgumentException("Linked package artifact escapes the package directory: "
                        + artifact.artifactKind());
            }
            rejectSymlinkSegments(resolved);
            artifactPaths.add(resolved);
        }
        return new ManifestBoundary(packageRoot, List.copyOf(artifactPaths));
    }

    private ScenarioCatalogManifest.ArtifactMetadata requireArtifactMetadata(
            ScenarioCatalogManifest.ArtifactMetadata artifact,
            String kind) {
        if (artifact == null || artifact.path() == null) {
            throw new IllegalArgumentException("Manifest is missing linked artifact metadata for " + kind);
        }
        return artifact;
    }

    private void validateConsistency(ScenarioCatalogPackageReader.PackageContents contents,
                                     ScenarioSpaceAccountingReport accounting) {
        ScenarioCatalogManifest manifest = contents.manifest();
        Map<String, WorkloadPlan> workloadsById = contents.workloadPlans().stream()
                .collect(Collectors.toMap(WorkloadPlan::deterministicId, plan -> plan, (left, right) -> left, LinkedHashMap::new));
        Map<String, WorkloadMaterializability> declaredMaterializability = manifest.workloadMaterializability().stream()
                .collect(Collectors.toMap(WorkloadMaterializability::workloadPlanId, value -> value,
                        (left, right) -> { throw new IllegalArgumentException("Duplicate materializability row"); }, LinkedHashMap::new));
        if (!declaredMaterializability.keySet().equals(workloadsById.keySet())) {
            throw new IllegalArgumentException("Manifest materializability rows do not match WorkloadPlans");
        }
        Map<String, WorkloadMaterializability> actualMaterializabilityByWorkload = new LinkedHashMap<>();
        long materializableWorkloadCount = 0;
        for (WorkloadPlan workload : contents.workloadPlans()) {
            WorkloadMaterializability actual = EagerFaultScenarioGenerator.evaluateMaterializability(workload);
            actualMaterializabilityByWorkload.put(workload.deterministicId(), actual);
            if (!actual.equals(declaredMaterializability.get(workload.deterministicId()))) {
                throw new IllegalArgumentException("Manifest materializability diagnostic mismatch for "
                        + workload.deterministicId());
            }
            if (actual.materializable()) {
                materializableWorkloadCount++;
            }
        }
        long nonMaterializableWorkloadCount = contents.workloadPlans().size() - materializableWorkloadCount;

        Map<String, List<String>> actualScenarioIdsByVector = new LinkedHashMap<>();
        contents.faultScenarios().forEach(scenario -> actualScenarioIdsByVector
                .computeIfAbsent(scenario.workloadPlanId() + "\u0000" + scenario.assignedVector(),
                        ignored -> new ArrayList<>())
                .add(scenario.deterministicId()));
        actualScenarioIdsByVector.values().forEach(ids -> ids.sort(String::compareTo));
        Map<String, FaultScenarioVectorSource> sourcesByVector = new LinkedHashMap<>();
        BigInteger uncappedSum = BigInteger.ZERO;
        BigInteger writtenSum = BigInteger.ZERO;
        int eagerCount = 0;
        int onDemandCount = 0;
        for (ScenarioSpaceAccountingReport.ComputedVectorRecoverySpace row
                : accounting.faultScenarioCatalogSpace().perComputedVectorRecoverySpace()) {
            WorkloadPlan workload = workloadsById.get(row.workloadPlanId());
            if (workload == null || row.assignedVector() == null
                    || row.assignedVector().length() != workload.faultSlots().size()
                    || !row.assignedVector().matches("[01]*")) {
                throw new IllegalArgumentException("Invalid computed-vector accounting reference");
            }
            if (!actualMaterializabilityByWorkload.get(row.workloadPlanId()).materializable()) {
                throw new IllegalArgumentException("Computed vectors require a materializable WorkloadPlan: "
                        + row.workloadPlanId());
            }
            FaultScenarioVectorSource source;
            try {
                source = FaultScenarioVectorSource.valueOf(row.vectorSource());
            } catch (RuntimeException exception) {
                throw new IllegalArgumentException("Invalid computed-vector source " + row.vectorSource(), exception);
            }
            String key = row.workloadPlanId() + "\u0000" + row.assignedVector();
            if (sourcesByVector.putIfAbsent(key, source) != null) {
                throw new IllegalArgumentException("Duplicate computed-vector accounting row " + key);
            }
            BigInteger uncapped = exact(row.uncappedUniqueScheduleCount(), "uncappedUniqueScheduleCount");
            BigInteger written = exact(row.writtenScheduleCount(), "writtenScheduleCount");
            List<String> actualIds = actualScenarioIdsByVector.getOrDefault(key, List.of());
            if (written.signum() <= 0 || written.compareTo(BigInteger.valueOf(manifest.recoveryScheduleCap())) > 0
                    || uncapped.compareTo(written) < 0
                    || !written.equals(BigInteger.valueOf(actualIds.size()))) {
                throw new IllegalArgumentException("Computed-vector counts do not match FaultScenario records for " + key);
            }
            RecoveryScheduleGenerationResult expected;
            try {
                expected = RecoveryScheduleGenerator.generate(
                        workload, row.assignedVector(), manifest.recoveryScheduleCap());
            } catch (RuntimeException exception) {
                throw new IllegalArgumentException("Failed to deterministically reconcile computed vector " + key,
                        exception);
            }
            List<String> expectedIds = expected.faultScenarios().stream()
                    .map(FaultScenario::deterministicId)
                    .sorted()
                    .toList();
            if (!uncapped.equals(expected.uncappedScheduleCount())
                    || !written.equals(BigInteger.valueOf(expected.writtenScheduleCount()))
                    || !actualIds.equals(expectedIds)) {
                throw new IllegalArgumentException(
                        "Computed-vector exact metadata differs from fresh deterministic generation for " + key);
            }
            uncappedSum = uncappedSum.add(uncapped);
            writtenSum = writtenSum.add(written);
            if (source == FaultScenarioVectorSource.ON_DEMAND_REQUEST) {
                onDemandCount++;
            } else {
                eagerCount++;
            }
        }
        if (!sourcesByVector.keySet().equals(actualScenarioIdsByVector.keySet())) {
            throw new IllegalArgumentException("FaultScenario records and computed-vector accounting differ");
        }

        LinkedHashMap<String, FaultScenarioVectorSource> expectedEager = new LinkedHashMap<>();
        for (WorkloadPlan workload : contents.workloadPlans()) {
            if (!declaredMaterializability.get(workload.deterministicId()).materializable()) {
                continue;
            }
            String allZero = "0".repeat(workload.faultSlots().size());
            expectedEager.put(workload.deterministicId() + "\u0000" + allZero, FaultScenarioVectorSource.EAGER_ALL_ZERO);
            for (int index = 0; index < workload.faultSlots().size(); index++) {
                char[] vector = allZero.toCharArray();
                vector[index] = '1';
                expectedEager.put(workload.deterministicId() + "\u0000" + new String(vector),
                        FaultScenarioVectorSource.EAGER_SINGLE_POINT);
            }
        }
        Map<String, FaultScenarioVectorSource> actualEager = sourcesByVector.entrySet().stream()
                .filter(entry -> entry.getValue() != FaultScenarioVectorSource.ON_DEMAND_REQUEST)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (left, right) -> left, LinkedHashMap::new));
        if (!expectedEager.equals(actualEager)) {
            throw new IllegalArgumentException("Eager vector coverage is invalid");
        }

        ScenarioSpaceAccountingReport.FaultScenarioCatalogSpace faultSpace = accounting.faultScenarioCatalogSpace();
        requireExact(faultSpace.faultScenariosWritten(), contents.faultScenarios().size(), "faultScenariosWritten");
        requireExact(faultSpace.computedEagerVectorCount(), eagerCount, "computedEagerVectorCount");
        requireExact(faultSpace.computedOnDemandVectorCount(), onDemandCount, "computedOnDemandVectorCount");
        requireExact(faultSpace.computedVectorCount(), sourcesByVector.size(), "computedVectorCount");
        if (!exact(faultSpace.exactComputedVectorUncappedScheduleSum(), "uncapped sum").equals(uncappedSum)
                || !exact(faultSpace.exactComputedVectorWrittenScheduleSum(), "written sum").equals(writtenSum)
                || !"EXACT_SUM_OVER_COMPUTED_VECTORS_ONLY".equals(faultSpace.exactComputedSumsScope())
                || !"NOT_COMPUTED".equals(faultSpace.allVectorRecoveryTotalStatus())) {
            throw new IllegalArgumentException("FaultScenario accounting aggregate mismatch");
        }

        Map<String, ScenarioSpaceAccountingReport.WorkloadVectorSpace> workloadRows = accounting.workloadCatalogSpace()
                .perWorkloadVectorSpace().stream()
                .collect(Collectors.toMap(ScenarioSpaceAccountingReport.WorkloadVectorSpace::workloadPlanId, row -> row,
                        (left, right) -> { throw new IllegalArgumentException("Duplicate workload accounting row"); }, LinkedHashMap::new));
        if (!workloadRows.keySet().equals(workloadsById.keySet())) {
            throw new IllegalArgumentException("Workload accounting rows do not match WorkloadPlans");
        }
        for (WorkloadPlan workload : contents.workloadPlans()) {
            ScenarioSpaceAccountingReport.WorkloadVectorSpace row = workloadRows.get(workload.deterministicId());
            WorkloadMaterializability materializability = declaredMaterializability.get(workload.deterministicId());
            long workloadEager = sourcesByVector.entrySet().stream()
                    .filter(entry -> entry.getKey().startsWith(workload.deterministicId() + "\u0000"))
                    .filter(entry -> entry.getValue() != FaultScenarioVectorSource.ON_DEMAND_REQUEST)
                    .count();
            long workloadOnDemand = sourcesByVector.entrySet().stream()
                    .filter(entry -> entry.getKey().startsWith(workload.deterministicId() + "\u0000"))
                    .filter(entry -> entry.getValue() == FaultScenarioVectorSource.ON_DEMAND_REQUEST)
                    .count();
            if (!exact(row.faultSlotCount(), "faultSlotCount").equals(BigInteger.valueOf(workload.faultSlots().size()))
                    || !exact(row.possibleBinaryVectors(), "possibleBinaryVectors")
                    .equals(BigInteger.TWO.pow(workload.faultSlots().size()))
                    || row.executorMaterializable() != materializability.materializable()
                    || !row.materializabilityDiagnostics().equals(materializability.diagnostics())) {
                throw new IllegalArgumentException("Workload accounting mismatch for " + workload.deterministicId());
            }
            requireExact(row.eagerVectorCount(), workloadEager, "eagerVectorCount");
            requireExact(row.onDemandVectorCount(), workloadOnDemand, "onDemandVectorCount");
        }
        requireExact(accounting.workloadCatalogSpace().workloadPlansWritten(), contents.workloadPlans().size(),
                "workloadPlansWritten");
        requireExact(accounting.workloadCatalogSpace().materializableWorkloadPlans(), materializableWorkloadCount,
                "materializableWorkloadPlans");
        requireExact(accounting.workloadCatalogSpace().nonMaterializableWorkloadPlans(), nonMaterializableWorkloadCount,
                "nonMaterializableWorkloadPlans");

        requireManifestCount(manifest, "workloadsExported", contents.workloadPlans().size());
        requireManifestCount(manifest, "materializableWorkloadPlans", materializableWorkloadCount);
        requireManifestCount(manifest, "nonMaterializableWorkloadPlans", nonMaterializableWorkloadCount);
        requireManifestCount(manifest, "computedEagerVectors", eagerCount);
        requireManifestCount(manifest, "computedOnDemandVectors", onDemandCount);
        requireManifestCount(manifest, "computedVectors", sourcesByVector.size());
        requireManifestCount(manifest, "computedVectorUncappedScheduleSum", uncappedSum);
        requireManifestCount(manifest, "computedVectorWrittenScheduleSum", writtenSum);
        requireManifestCount(manifest, "faultScenariosExported", contents.faultScenarios().size());
        requireManifestCount(manifest, "rejectedInputsExported", contents.rejectedInputDiagnostics().size());
        String expectedSource = onDemandCount == 0 ? EAGER_VECTOR_SOURCE : EAGER_AND_ON_DEMAND_VECTOR_SOURCE;
        String expectedGenerationSource = onDemandCount == 0
                ? STATIC_GENERATION_SOURCE
                : STATIC_AND_ON_DEMAND_GENERATION_SOURCE;
        if (!expectedSource.equals(manifest.faultScenarioVectorSource())
                || !expectedGenerationSource.equals(manifest.generationSource())) {
            throw new IllegalArgumentException("Manifest generation-source metadata mismatch");
        }
    }

    private void requireManifestCount(ScenarioCatalogManifest manifest, String key, long expected) {
        requireManifestCount(manifest, key, BigInteger.valueOf(expected));
    }

    private void requireManifestCount(ScenarioCatalogManifest manifest, String key, BigInteger expected) {
        String value = manifest.counts().get(key);
        if (value == null || !exact(value, "manifest count " + key).equals(expected)) {
            throw new IllegalArgumentException("Manifest count mismatch for " + key);
        }
    }

    private void requireExact(String value, long expected, String label) {
        if (!exact(value, label).equals(BigInteger.valueOf(expected))) {
            throw new IllegalArgumentException(label + " mismatch");
        }
    }

    private BigInteger exact(String value, String label) {
        try {
            BigInteger parsed = new BigInteger(value);
            if (parsed.signum() < 0) {
                throw new NumberFormatException("negative");
            }
            return parsed;
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(label + " must be a non-negative exact decimal", exception);
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
        ACCOUNTING_STAGED,
        MANIFEST_STAGED,
        FAULT_SCENARIO_PROMOTED,
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

    private record ArtifactPath(ScenarioCatalogManifest.ArtifactMetadata metadata, Path path) {
    }

    private record ManifestBoundary(Path packageRoot, List<Path> artifactPaths) {
    }

    private record ValidatedPackage(ScenarioCatalogPackageReader.PackageContents contents,
                                    ScenarioSpaceAccountingReport accounting) {
    }
}
