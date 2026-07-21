package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.export

import com.fasterxml.jackson.databind.ObjectMapper
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.EagerFaultScenarioGenerator
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.RecoveryScheduleCap
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.RecoveryScheduleGenerator
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ScenarioGeneratorConfig
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ScenarioIdGenerator
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.accounting.ScenarioSpaceAccountingReport
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.*
import spock.lang.Specification

import java.nio.channels.FileChannel
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class OnDemandFaultScenarioServiceSpec extends Specification {

    private final ObjectMapper mapper = new ObjectMapper()

    def 'valid multi-fault request persists one consistent bounded package revision before returning'() {
        given:
        def fixture = writePackage(Files.createTempDirectory('on-demand-valid'), 2)
        def before = snapshotMutable(fixture)
        def unchangedWorkload = Files.readAllBytes(fixture.workloadPath)
        def unchangedRejected = Files.readAllBytes(fixture.rejected)

        when:
        def result = new OnDemandFaultScenarioService().request(new OnDemandFaultScenarioRequest(
                fixture.manifest,
                fixture.workload.deterministicId(),
                '0011',
                '2'))

        then:
        result.status() == OnDemandFaultScenarioResult.Status.PERSISTED
        result.addedFaultScenarioCount() > 0
        result.writtenScheduleCount() <= 2
        result.faultScenarioIds().size() == result.writtenScheduleCount()

        and:
        def loaded = new ScenarioCatalogPackageReader().read(fixture.manifest)
        def requestedScenarios = loaded.faultScenarios().findAll { it.assignedVector() == '0011' }
        requestedScenarios*.deterministicId() == result.faultScenarioIds()
        requestedScenarios.every { scenario ->
            scenario.actions().findAll { it.kind() == FaultScenarioActionKind.COMPENSATION }*.sagaInstanceId().toSet() == ['a', 'b'] as Set
        }
        loaded.manifest().generationSource() == 'STATIC_ANALYSIS_AND_ON_DEMAND_REQUEST'
        loaded.manifest().faultScenarioVectorSource() == 'EAGER_ALL_ZERO_AND_SINGLE_POINT_AND_ON_DEMAND'
        loaded.manifest().counts().computedOnDemandVectors == '1'
        loaded.manifest().counts().faultScenariosExported == loaded.faultScenarios().size().toString()
        def accounting = mapper.readTree(Files.readString(fixture.accounting))
        def requestedRow = accounting.path('faultScenarioCatalogSpace').path('perComputedVectorRecoverySpace')
                .find { it.path('assignedVector').asText() == '0011' }
        requestedRow.path('vectorSource').asText() == 'ON_DEMAND_REQUEST'
        requestedRow.path('writtenScheduleCount').asInt() == result.writtenScheduleCount()

        and:
        !Arrays.equals(before.faultScenario, Files.readAllBytes(fixture.faultScenario))
        !Arrays.equals(before.accounting, Files.readAllBytes(fixture.accounting))
        !Arrays.equals(before.manifest, Files.readAllBytes(fixture.manifest))
        Arrays.equals(unchangedWorkload, Files.readAllBytes(fixture.workloadPath))
        Arrays.equals(unchangedRejected, Files.readAllBytes(fixture.rejected))
    }

    def 'invalid request matrix returns structured diagnostics and leaves all mutable artifacts byte unchanged'() {
        given:
        def fixture = writePackage(Files.createTempDirectory('on-demand-invalid'), 2)
        def before = snapshotMutable(fixture)

        when:
        def result = new OnDemandFaultScenarioService().request(new OnDemandFaultScenarioRequest(
                fixture.manifest,
                workloadId == 'VALID' ? fixture.workload.deterministicId() : workloadId,
                vector,
                assertedCap))

        then:
        result.status() == OnDemandFaultScenarioResult.Status.REJECTED
        result.diagnostics()*.code() == [expectedCode]
        sameMutableBytes(before, fixture)

        where:
        workloadId | vector | assertedCap || expectedCode
        'missing'  | '0011' | '2'         || 'WORKLOAD_PLAN_NOT_FOUND'
        'VALID'    | '00x1' | '2'         || 'NON_BINARY_VECTOR'
        'VALID'    | '001'  | '2'         || 'INVALID_VECTOR_LENGTH'
        'VALID'    | '0011' | '0'         || 'INVALID_ASSERTED_RECOVERY_CAP'
        'VALID'    | '0011' | '-1'        || 'INVALID_ASSERTED_RECOVERY_CAP'
        'VALID'    | '0011' | 'invalid'   || 'INVALID_ASSERTED_RECOVERY_CAP'
        'VALID'    | '0011' | '3'         || 'RECOVERY_CAP_MISMATCH'
    }

    def 'blocked input and malformed slot mapping are rejected before mutation'() {
        given: 'a package whose requested workload is not input-ready'
        def blockedWorkload = workload(false)
        def blocked = writePackage(Files.createTempDirectory('on-demand-blocked'), 2, blockedWorkload)
        def blockedBefore = snapshotMutable(blocked)

        when:
        def blockedResult = new OnDemandFaultScenarioService().request(new OnDemandFaultScenarioRequest(
                blocked.manifest, blockedWorkload.deterministicId(), '0011', null))

        then:
        blockedResult.status() == OnDemandFaultScenarioResult.Status.REJECTED
        blockedResult.diagnostics()*.code() == ['WORKLOAD_NOT_MATERIALIZABLE']
        sameMutableBytes(blockedBefore, blocked)

        when: 'a linked workload has a malformed slot occurrence mapping with an otherwise current checksum'
        def malformed = writePackage(Files.createTempDirectory('on-demand-malformed-slot'), 2)
        def workloadJson = mapper.readTree(Files.readAllLines(malformed.workloadPath).first())
        workloadJson.path('faultSlots').first().put('occurrenceId', 'wrong-occurrence')
        Files.writeString(malformed.workloadPath, mapper.writeValueAsString(workloadJson) + '\n')
        refreshArtifactHash(malformed.manifest, 'workloadCatalog', malformed.workloadPath)
        def malformedBefore = snapshotMutable(malformed)
        def malformedResult = new OnDemandFaultScenarioService().request(new OnDemandFaultScenarioRequest(
                malformed.manifest, malformed.workload.deterministicId(), '0011', null))

        then:
        malformedResult.status() == OnDemandFaultScenarioResult.Status.REJECTED
        malformedResult.diagnostics()*.code() == ['INVALID_PACKAGE']
        sameMutableBytes(malformedBefore, malformed)
    }

    def 'on-demand request rejects a checksum-current repeated participant runtime step workload without mutation'() {
        given:
        def fixture = writePackage(Files.createTempDirectory('on-demand-repeated-runtime-step'), 2)
        def workloadJson = mapper.readTree(Files.readAllLines(fixture.workloadPath).first())
        def firstStep = workloadJson.path('forwardSchedule').get(0)
        def repeatedStep = workloadJson.path('forwardSchedule').get(2)
        repeatedStep.put('stepId', firstStep.path('stepId').asText())
        repeatedStep.put('runtimeStepName', firstStep.path('runtimeStepName').asText())
        Files.writeString(fixture.workloadPath, mapper.writeValueAsString(workloadJson) + '\n')
        refreshArtifactHash(fixture.manifest, 'workloadCatalog', fixture.workloadPath)
        def before = snapshotMutable(fixture)

        when:
        def result = new OnDemandFaultScenarioService().request(new OnDemandFaultScenarioRequest(
                fixture.manifest, fixture.workload.deterministicId(), '0011', null))

        then:
        result.status() == OnDemandFaultScenarioResult.Status.REJECTED
        result.diagnostics()*.code() == ['INVALID_PACKAGE']
        result.diagnostics().first().message().contains('DUPLICATE_PARTICIPANT_RUNTIME_STEP_NAME')
        sameMutableBytes(before, fixture)
    }

    def 'package validation rejects a carried on-demand vector owned by a non-materializable workload'() {
        given:
        def readyWorkload = workload(true)
        def blockedWorkload = workload(false)
        assert !EagerFaultScenarioGenerator.evaluateMaterializability(blockedWorkload).materializable()
        def fixture = writePackage(
                Files.createTempDirectory('on-demand-carried-blocked-workload'),
                2,
                [readyWorkload, blockedWorkload])
        def loaded = new ScenarioCatalogPackageReader().read(fixture.manifest)
        def generatedForBlocked = RecoveryScheduleGenerator.generate(blockedWorkload, '0011', 2)
        def revisedScenarios = (loaded.faultScenarios() + generatedForBlocked.faultScenarios()).sort { left, right ->
            left.workloadPlanId() <=> right.workloadPlanId()
                    ?: left.assignedVector() <=> right.assignedVector()
                    ?: left.deterministicId() <=> right.deterministicId()
        }
        Files.writeString(fixture.faultScenario,
                revisedScenarios.collect { mapper.writeValueAsString(it) }.join('\n') + '\n')

        def accounting = mapper.readValue(
                Files.readString(fixture.accounting), ScenarioSpaceAccountingReport)
        def revisedAccounting = accounting.withOnDemandVector(new ComputedVectorRecovery(
                blockedWorkload.deterministicId(),
                '0011',
                FaultScenarioVectorSource.ON_DEMAND_REQUEST,
                generatedForBlocked.uncappedScheduleCount(),
                generatedForBlocked.writtenScheduleCount()), revisedScenarios.size())
        Files.writeString(fixture.accounting,
                mapper.writerWithDefaultPrettyPrinter().writeValueAsString(revisedAccounting) + '\n')

        def manifest = mapper.readTree(Files.readString(fixture.manifest))
        def faultSpace = revisedAccounting.faultScenarioCatalogSpace()
        manifest.put('generationSource', OnDemandFaultScenarioService.STATIC_AND_ON_DEMAND_GENERATION_SOURCE)
        manifest.put('faultScenarioVectorSource', OnDemandFaultScenarioService.EAGER_AND_ON_DEMAND_VECTOR_SOURCE)
        manifest.path('counts').put('computedOnDemandVectors', faultSpace.computedOnDemandVectorCount())
        manifest.path('counts').put('computedVectors', faultSpace.computedVectorCount())
        manifest.path('counts').put(
                'computedVectorUncappedScheduleSum', faultSpace.exactComputedVectorUncappedScheduleSum())
        manifest.path('counts').put(
                'computedVectorWrittenScheduleSum', faultSpace.exactComputedVectorWrittenScheduleSum())
        manifest.path('counts').put('faultScenariosExported', revisedScenarios.size().toString())
        manifest.path('faultScenarioCatalog').put('recordCount', revisedScenarios.size().toString())
        manifest.path('faultScenarioCatalog').put(
                'sha256', ScenarioCatalogJsonlWriter.sha256(Files.readAllBytes(fixture.faultScenario)))
        manifest.path('scenarioSpaceAccounting').put(
                'sha256', ScenarioCatalogJsonlWriter.sha256(Files.readAllBytes(fixture.accounting)))
        Files.writeString(fixture.manifest,
                mapper.writerWithDefaultPrettyPrinter().writeValueAsString(manifest) + '\n')
        def before = snapshotMutable(fixture)

        when:
        def result = new OnDemandFaultScenarioService().request(new OnDemandFaultScenarioRequest(
                fixture.manifest, readyWorkload.deterministicId(), '0011', null))

        then:
        result.status() == OnDemandFaultScenarioResult.Status.REJECTED
        result.diagnostics()*.code() == ['INVALID_PACKAGE']
        sameMutableBytes(before, fixture)
    }

    def 'on-demand request rejects checksum-mismatched package through the shared reader without mutation'() {
        given:
        def fixture = writePackage(Files.createTempDirectory('on-demand-shared-checksum'), 2)
        Files.write(fixture.faultScenario, '\n'.bytes, java.nio.file.StandardOpenOption.APPEND)
        def before = snapshotMutable(fixture)

        when:
        def result = new OnDemandFaultScenarioService().request(new OnDemandFaultScenarioRequest(
                fixture.manifest, fixture.workload.deterministicId(), '0011', null))

        then:
        result.status() == OnDemandFaultScenarioResult.Status.REJECTED
        result.diagnostics()*.code() == ['INVALID_PACKAGE']
        result.diagnostics().first().message().contains('FAULT_SCENARIO_CATALOG')
        result.diagnostics().first().message().contains('checksum mismatch')
        sameMutableBytes(before, fixture)
    }

    def 'package mutation rejects outside linked artifacts before reading their content'() {
        given:
        def fixture = writePackage(Files.createTempDirectory('on-demand-outside-path'), 2)
        def secret = 'SUPER_SECRET_OUTSIDE_TOKEN'
        def outside = Files.createTempFile('on-demand-outside-secret', '.txt')
        Files.writeString(outside, secret)
        def manifest = mapper.readTree(Files.readString(fixture.manifest))
        manifest.path('faultScenarioCatalog').put('path', outside.toString())
        Files.writeString(fixture.manifest, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(manifest) + '\n')
        def before = snapshotMutable(fixture)

        when:
        def result = new OnDemandFaultScenarioService().request(new OnDemandFaultScenarioRequest(
                fixture.manifest, fixture.workload.deterministicId(), '0011', null))

        then:
        result.status() == OnDemandFaultScenarioResult.Status.REJECTED
        result.diagnostics()*.code() == ['INVALID_PACKAGE']
        result.diagnostics()*.message().every { !it.contains(secret) }
        sameMutableBytes(before, fixture)
    }

    def 'package mutation rejects linked symlink traversal before reading target content'() {
        given:
        def fixture = writePackage(Files.createTempDirectory('on-demand-symlink'), 2)
        def secret = 'SUPER_SECRET_SYMLINK_TOKEN'
        def outside = Files.createTempFile('on-demand-symlink-secret', '.txt')
        Files.writeString(outside, secret)
        def linkedCatalog = fixture.faultScenario.parent.resolve('linked-fault-scenarios.jsonl')
        Files.createSymbolicLink(linkedCatalog, outside)
        def manifest = mapper.readTree(Files.readString(fixture.manifest))
        manifest.path('faultScenarioCatalog').put('path', linkedCatalog.toString())
        Files.writeString(fixture.manifest, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(manifest) + '\n')
        def before = snapshotMutable(fixture)

        when:
        def result = new OnDemandFaultScenarioService().request(new OnDemandFaultScenarioRequest(
                fixture.manifest, fixture.workload.deterministicId(), '0011', null))

        then:
        result.status() == OnDemandFaultScenarioResult.Status.REJECTED
        result.diagnostics()*.code() == ['INVALID_PACKAGE']
        result.diagnostics()*.message().every { !it.contains(secret) }
        sameMutableBytes(before, fixture)
    }

    def 'hash-consistent false workload aggregate counts are rejected before mutation'() {
        given:
        def fixture = writePackage(Files.createTempDirectory('on-demand-false-workload-counts'), 2)
        def accounting = mapper.readTree(Files.readString(fixture.accounting))
        accounting.path('workloadCatalogSpace').put('materializableWorkloadPlans', '999')
        accounting.path('workloadCatalogSpace').put('nonMaterializableWorkloadPlans', '888')
        Files.writeString(fixture.accounting,
                mapper.writerWithDefaultPrettyPrinter().writeValueAsString(accounting) + '\n')
        def manifest = mapper.readTree(Files.readString(fixture.manifest))
        manifest.path('counts').put('materializableWorkloadPlans', '999')
        manifest.path('counts').put('nonMaterializableWorkloadPlans', '888')
        manifest.path('scenarioSpaceAccounting').put(
                'sha256', ScenarioCatalogJsonlWriter.sha256(Files.readAllBytes(fixture.accounting)))
        Files.writeString(fixture.manifest,
                mapper.writerWithDefaultPrettyPrinter().writeValueAsString(manifest) + '\n')
        def before = snapshotMutable(fixture)

        when:
        def result = new OnDemandFaultScenarioService().request(new OnDemandFaultScenarioRequest(
                fixture.manifest, fixture.workload.deterministicId(), '1001', null))

        then:
        result.status() == OnDemandFaultScenarioResult.Status.REJECTED
        result.diagnostics()*.code() == ['INVALID_PACKAGE']
        sameMutableBytes(before, fixture)
    }

    def 'false manifest package counts are rejected before mutation'() {
        given:
        def fixture = writePackage(Files.createTempDirectory("on-demand-false-manifest-${countKey}"), 2)
        def manifest = mapper.readTree(Files.readString(fixture.manifest))
        manifest.path('counts').put(countKey, falseValue)
        Files.writeString(fixture.manifest,
                mapper.writerWithDefaultPrettyPrinter().writeValueAsString(manifest) + '\n')
        def before = snapshotMutable(fixture)

        when:
        def result = new OnDemandFaultScenarioService().request(new OnDemandFaultScenarioRequest(
                fixture.manifest, fixture.workload.deterministicId(), '1001', null))

        then:
        result.status() == OnDemandFaultScenarioResult.Status.REJECTED
        result.diagnostics()*.code() == ['INVALID_PACKAGE']
        sameMutableBytes(before, fixture)

        where:
        countKey                        | falseValue
        'materializableWorkloadPlans'   | '999'
        'nonMaterializableWorkloadPlans'| '888'
        'rejectedInputsExported'        | '777'
    }

    def 'repeat request deduplicates byte-equivalent scenarios and cap mismatch still wins before mutation'() {
        given:
        def fixture = writePackage(Files.createTempDirectory('on-demand-repeat'), 2)
        def service = new OnDemandFaultScenarioService()
        def request = new OnDemandFaultScenarioRequest(
                fixture.manifest, fixture.workload.deterministicId(), '0011', '2')
        assert service.request(request).status() == OnDemandFaultScenarioResult.Status.PERSISTED
        def persisted = snapshotMutable(fixture)

        when:
        def repeated = service.request(request)

        then:
        repeated.status() == OnDemandFaultScenarioResult.Status.DEDUPLICATED
        repeated.addedFaultScenarioCount() == 0
        sameMutableBytes(persisted, fixture)

        when:
        def mismatch = service.request(new OnDemandFaultScenarioRequest(
                fixture.manifest, fixture.workload.deterministicId(), '0011', '3'))

        then:
        mismatch.status() == OnDemandFaultScenarioResult.Status.REJECTED
        mismatch.diagnostics()*.code() == ['RECOVERY_CAP_MISMATCH']
        sameMutableBytes(persisted, fixture)
    }

    def 'package validation rejects false exact metadata for a carried #vectorSource vector'() {
        given:
        def fixture = writePackage(Files.createTempDirectory("on-demand-carried-${vectorSource}-count-mismatch"), 2)
        if (persistCarriedVector) {
            def carriedRequest = new OnDemandFaultScenarioRequest(
                    fixture.manifest, fixture.workload.deterministicId(), carriedVector, null)
            assert new OnDemandFaultScenarioService().request(carriedRequest).status() ==
                    OnDemandFaultScenarioResult.Status.PERSISTED
        }
        def accounting = mapper.readTree(Files.readString(fixture.accounting))
        def faultSpace = accounting.path('faultScenarioCatalogSpace')
        def row = faultSpace.path('perComputedVectorRecoverySpace')
                .find { it.path('assignedVector').asText() == carriedVector }
        def originalUncapped = new BigInteger(row.path('uncappedUniqueScheduleCount').asText())
        def falseUncapped = new BigInteger('999')
        row.put('uncappedUniqueScheduleCount', falseUncapped.toString())
        def revisedSum = new BigInteger(faultSpace.path('exactComputedVectorUncappedScheduleSum').asText())
                .subtract(originalUncapped)
                .add(falseUncapped)
        faultSpace.put('exactComputedVectorUncappedScheduleSum', revisedSum.toString())
        Files.writeString(fixture.accounting,
                mapper.writerWithDefaultPrettyPrinter().writeValueAsString(accounting) + '\n')
        def manifest = mapper.readTree(Files.readString(fixture.manifest))
        manifest.path('counts').put('computedVectorUncappedScheduleSum', revisedSum.toString())
        manifest.path('scenarioSpaceAccounting').put(
                'sha256', ScenarioCatalogJsonlWriter.sha256(Files.readAllBytes(fixture.accounting)))
        Files.writeString(fixture.manifest,
                mapper.writerWithDefaultPrettyPrinter().writeValueAsString(manifest) + '\n')
        def before = snapshotMutable(fixture)

        when:
        def result = new OnDemandFaultScenarioService().request(new OnDemandFaultScenarioRequest(
                fixture.manifest, fixture.workload.deterministicId(), requestedVector, null))

        then:
        result.status() == OnDemandFaultScenarioResult.Status.REJECTED
        result.diagnostics()*.code() == ['INVALID_PACKAGE']
        sameMutableBytes(before, fixture)

        where:
        vectorSource | carriedVector | persistCarriedVector | requestedVector
        'eager'      | '1000'        | false                | '0011'
        'on-demand'  | '0011'        | true                 | '0110'
    }

    def 'package validation rejects a non-deterministic carried scenario-id set without mutation'() {
        given:
        def fixture = writePackage(Files.createTempDirectory('on-demand-carried-id-set-mismatch'), 2)
        def carriedRequest = new OnDemandFaultScenarioRequest(
                fixture.manifest, fixture.workload.deterministicId(), '0011', null)
        assert new OnDemandFaultScenarioService().request(carriedRequest).status() ==
                OnDemandFaultScenarioResult.Status.PERSISTED
        def loaded = new ScenarioCatalogPackageReader().read(fixture.manifest)
        def carried = loaded.faultScenarios().findAll { it.assignedVector() == '0011' }
        def alternate = RecoveryScheduleGenerator.generate(fixture.workload, '0011', 3).faultScenarios()
                .find { candidate -> carried.every { it.deterministicId() != candidate.deterministicId() } }
        assert alternate != null
        def replacementId = carried.last().deterministicId()
        def revisedScenarios = loaded.faultScenarios().collect {
            it.deterministicId() == replacementId ? alternate : it
        }.sort { left, right ->
            left.workloadPlanId() <=> right.workloadPlanId()
                    ?: left.assignedVector() <=> right.assignedVector()
                    ?: left.deterministicId() <=> right.deterministicId()
        }
        Files.writeString(fixture.faultScenario,
                revisedScenarios.collect { mapper.writeValueAsString(it) }.join('\n') + '\n')
        updateArtifact(fixture.manifest, 'faultScenarioCatalog', fixture.faultScenario, revisedScenarios.size())
        def before = snapshotMutable(fixture)

        when:
        def result = new OnDemandFaultScenarioService().request(new OnDemandFaultScenarioRequest(
                fixture.manifest, fixture.workload.deterministicId(), '0110', null))

        then:
        result.status() == OnDemandFaultScenarioResult.Status.REJECTED
        result.diagnostics()*.code() == ['INVALID_PACKAGE']
        sameMutableBytes(before, fixture)
    }

    def 'duplicate ids in the existing package and same-id different-content generation fail without mutation'() {
        given: 'an existing duplicate semantic id'
        def duplicate = writePackage(Files.createTempDirectory('on-demand-duplicate'), 2)
        def lines = Files.readAllLines(duplicate.faultScenario)
        Files.writeString(duplicate.faultScenario, (lines + lines.first()).join('\n') + '\n')
        updateArtifact(duplicate.manifest, 'faultScenarioCatalog', duplicate.faultScenario, lines.size() + 1)
        def duplicateBefore = snapshotMutable(duplicate)

        when:
        def duplicateResult = new OnDemandFaultScenarioService().request(new OnDemandFaultScenarioRequest(
                duplicate.manifest, duplicate.workload.deterministicId(), '0011', null))

        then:
        duplicateResult.status() == OnDemandFaultScenarioResult.Status.REJECTED
        duplicateResult.diagnostics()*.code() == ['INVALID_PACKAGE']
        sameMutableBytes(duplicateBefore, duplicate)

        when: 'the generator presents different semantic content under an existing valid id'
        def collision = writePackage(Files.createTempDirectory('on-demand-collision'), 2)
        def existing = new ScenarioCatalogPackageReader().read(collision.manifest).faultScenarios()
                .find { it.assignedVector() == '1000' }
        def conflicting = new FaultScenario(existing.schemaVersion(), existing.deterministicId(),
                existing.workloadPlanId(), existing.assignedVector(), [])
        def source = { WorkloadPlan ignored, String ignoredVector, int cap ->
            new RecoveryScheduleGenerationResult([conflicting], BigInteger.ONE, 1, cap, [], null)
        } as OnDemandFaultScenarioService.RecoveryScheduleSource
        def service = new OnDemandFaultScenarioService(source,
                { ignored -> } as OnDemandFaultScenarioService.FailureInjector,
                new OnDemandFaultScenarioService.NioFileMover())
        def collisionBefore = snapshotMutable(collision)
        def collisionResult = service.request(new OnDemandFaultScenarioRequest(
                collision.manifest, collision.workload.deterministicId(), '1000', null))

        then:
        collisionResult.status() == OnDemandFaultScenarioResult.Status.INTEGRITY_FAILURE
        collisionResult.diagnostics()*.code() == ['FAULT_SCENARIO_ID_COLLISION']
        sameMutableBytes(collisionBefore, collision)
    }

    def 'every staging and promotion boundary failure restores all original mutable artifact bytes'() {
        given:
        def fixture = writePackage(Files.createTempDirectory("on-demand-failure-${boundary}"), 2)
        def before = snapshotMutable(fixture)
        def injector = { OnDemandFaultScenarioService.Boundary reached ->
            if (reached == boundary) {
                throw new IOException("injected ${boundary}".toString())
            }
        } as OnDemandFaultScenarioService.FailureInjector
        def service = new OnDemandFaultScenarioService(
                { WorkloadPlan plan, String vector, int cap -> RecoveryScheduleGenerator.generate(plan, vector, cap) }
                        as OnDemandFaultScenarioService.RecoveryScheduleSource,
                injector,
                new OnDemandFaultScenarioService.NioFileMover())

        when:
        def result = service.request(new OnDemandFaultScenarioRequest(
                fixture.manifest, fixture.workload.deterministicId(), '0011', null))

        then:
        result.status() == OnDemandFaultScenarioResult.Status.PERSISTENCE_FAILED
        result.diagnostics()*.code() == ['PACKAGE_REVISION_FAILED']
        sameMutableBytes(before, fixture)
        new ScenarioCatalogPackageReader().read(fixture.manifest).faultScenarios().every { it.assignedVector() != '0011' }

        where:
        boundary << OnDemandFaultScenarioService.Boundary.values()
    }

    def 'temporary-file cleanup failure cannot flip a committed validated revision to failure'() {
        given:
        def fixture = writePackage(Files.createTempDirectory('on-demand-cleanup-failure'), 2)
        def before = snapshotMutable(fixture)
        def service = new OnDemandFaultScenarioService(
                { WorkloadPlan plan, String vector, int cap -> RecoveryScheduleGenerator.generate(plan, vector, cap) }
                        as OnDemandFaultScenarioService.RecoveryScheduleSource,
                { ignored -> } as OnDemandFaultScenarioService.FailureInjector,
                new OnDemandFaultScenarioService.NioFileMover(),
                { ignored -> throw new IOException('injected cleanup failure') }
                        as OnDemandFaultScenarioService.TemporaryFileCleaner)

        when:
        def result = service.request(new OnDemandFaultScenarioRequest(
                fixture.manifest, fixture.workload.deterministicId(), '0011', null))

        then:
        result.status() == OnDemandFaultScenarioResult.Status.PERSISTED
        !sameMutableBytes(before, fixture)
        new ScenarioCatalogPackageReader().read(fixture.manifest).faultScenarios().any { it.assignedVector() == '0011' }
    }

    def 'atomic-move fallback publishes a valid revision and canonical bytes do not depend on request order'() {
        given: 'a mover that forces the tested non-atomic replacement fallback'
        def fallbackFixture = writePackage(Files.createTempDirectory('on-demand-fallback'), 2)
        def fallbackCalls = new AtomicInteger()
        def fallbackMover = [
                atomicMove  : { source, target ->
                    throw new AtomicMoveNotSupportedException(source.toString(), target.toString(), 'injected')
                },
                fallbackMove: { source, target ->
                    fallbackCalls.incrementAndGet()
                    Files.move(source, target, StandardCopyOption.REPLACE_EXISTING)
                }
        ] as OnDemandFaultScenarioService.FileMover
        def fallbackService = new OnDemandFaultScenarioService(
                { WorkloadPlan plan, String vector, int cap -> RecoveryScheduleGenerator.generate(plan, vector, cap) }
                        as OnDemandFaultScenarioService.RecoveryScheduleSource,
                { ignored -> } as OnDemandFaultScenarioService.FailureInjector,
                fallbackMover)

        when:
        def fallbackResult = fallbackService.request(new OnDemandFaultScenarioRequest(
                fallbackFixture.manifest, fallbackFixture.workload.deterministicId(), '0011', null))

        then:
        fallbackResult.status() == OnDemandFaultScenarioResult.Status.PERSISTED
        fallbackCalls.get() == 3
        new ScenarioCatalogPackageReader().read(fallbackFixture.manifest)

        when: 'two identical packages receive the same vectors in opposite order'
        def first = writePackage(Files.createTempDirectory('on-demand-order-a'), 2)
        def second = writePackage(Files.createTempDirectory('on-demand-order-b'), 2)
        def service = new OnDemandFaultScenarioService()
        ['1001', '0110'].each { vector ->
            assert service.request(new OnDemandFaultScenarioRequest(
                    first.manifest, first.workload.deterministicId(), vector, null)).successful()
        }
        ['0110', '1001'].each { vector ->
            assert service.request(new OnDemandFaultScenarioRequest(
                    second.manifest, second.workload.deterministicId(), vector, null)).successful()
        }

        then:
        Files.readAllBytes(first.faultScenario) == Files.readAllBytes(second.faultScenario)
        Files.readAllBytes(first.accounting) == Files.readAllBytes(second.accounting)
    }

    def 'separate JVM requests wait for the package OS lock and accumulate both revisions'() {
        given:
        def fixture = writePackage(Files.createTempDirectory('on-demand-cross-process'), 2)
        def lockPath = fixture.manifest.parent.resolve('.on-demand-fault-scenario.lock')
        def channel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE)
        def heldLock = channel.lock()
        def firstSignals = processSignals(fixture, 'first')
        def secondSignals = processSignals(fixture, 'second')

        when:
        def first = requestProcess(fixture, '1001', firstSignals)
        def second = requestProcess(fixture, '0110', secondSignals)

        then: 'both child JVMs signal immediately before the real FileChannel.lock call'
        awaitFile(firstSignals.ready)
        awaitFile(secondSignals.ready)

        and: 'the parent still owns that exact lock inode, so neither child acquired or completed'
        heldLock.valid
        !Files.exists(firstSignals.acquired)
        !Files.exists(secondSignals.acquired)
        !Files.exists(firstSignals.completed)
        !Files.exists(secondSignals.completed)

        when:
        heldLock.release()
        channel.close()
        assert first.waitFor(20, TimeUnit.SECONDS)
        assert second.waitFor(20, TimeUnit.SECONDS)

        then:
        first.exitValue() == 0
        second.exitValue() == 0
        Files.exists(firstSignals.acquired)
        Files.exists(secondSignals.acquired)
        Files.exists(firstSignals.completed)
        Files.exists(secondSignals.completed)
        mapper.readTree(Files.readString(firstSignals.result)).path('status').asText() == 'PERSISTED'
        mapper.readTree(Files.readString(secondSignals.result)).path('status').asText() == 'PERSISTED'

        and: 'the second lock owner re-read the first published revision'
        def loaded = new ScenarioCatalogPackageReader().read(fixture.manifest)
        loaded.faultScenarios()*.assignedVector().containsAll(['1001', '0110'])
        def accounting = mapper.readTree(Files.readString(fixture.accounting))
        def requestedRows = accounting.path('faultScenarioCatalogSpace').path('perComputedVectorRecoverySpace')
                .findAll { it.path('assignedVector').asText() in ['1001', '0110'] }
        requestedRows.size() == 2
        requestedRows*.path('assignedVector')*.asText().toSet() == ['1001', '0110'] as Set
        loaded.manifest().counts().computedOnDemandVectors == '2'
        loaded.manifest().counts().faultScenariosExported == loaded.faultScenarios().size().toString()
        Files.isRegularFile(lockPath, java.nio.file.LinkOption.NOFOLLOW_LINKS)
        !Files.readString(fixture.manifest).contains('.on-demand-fault-scenario.lock')

        cleanup:
        if (heldLock.valid) {
            heldLock.release()
        }
        channel.close()
        if (first.alive) {
            first.destroyForcibly()
        }
        if (second.alive) {
            second.destroyForcibly()
        }
    }

    def 'injected package lock open failure is classified narrowly and a later writer proceeds'() {
        given:
        def fixture = writePackage(Files.createTempDirectory('on-demand-lock-open-failure'), 2)
        def before = snapshotMutable(fixture)
        def provider = { ignored -> throw new IOException('injected lock open failure') }
                as OnDemandFaultScenarioService.PackageLockProvider

        when:
        def result = new OnDemandFaultScenarioService(provider).request(new OnDemandFaultScenarioRequest(
                fixture.manifest, fixture.workload.deterministicId(), '1001', null))

        then:
        result.status() == OnDemandFaultScenarioResult.Status.PERSISTENCE_FAILED
        result.diagnostics()*.code() == ['PACKAGE_LOCK_FAILED']
        result.diagnostics().first().message() == 'injected lock open failure'
        sameMutableBytes(before, fixture)
        new OnDemandFaultScenarioService().request(new OnDemandFaultScenarioRequest(
                fixture.manifest, fixture.workload.deterministicId(), '1001', null)).status() ==
                OnDemandFaultScenarioResult.Status.PERSISTED
    }

    def 'injected package lock acquisition failure closes the opened handle and a later writer proceeds'() {
        given:
        def fixture = writePackage(Files.createTempDirectory('on-demand-lock-acquire-failure'), 2)
        def before = snapshotMutable(fixture)
        def closes = new AtomicInteger()
        def provider = { ignored ->
            [
                    acquire: { throw new IOException('injected lock acquisition failure') },
                    close  : { closes.incrementAndGet() }
            ] as OnDemandFaultScenarioService.PackageLockHandle
        } as OnDemandFaultScenarioService.PackageLockProvider

        when:
        def result = new OnDemandFaultScenarioService(provider).request(new OnDemandFaultScenarioRequest(
                fixture.manifest, fixture.workload.deterministicId(), '1001', null))

        then:
        result.status() == OnDemandFaultScenarioResult.Status.PERSISTENCE_FAILED
        result.diagnostics()*.code() == ['PACKAGE_LOCK_FAILED']
        result.diagnostics().first().message() == 'injected lock acquisition failure'
        closes.get() == 1
        sameMutableBytes(before, fixture)
        new OnDemandFaultScenarioService().request(new OnDemandFaultScenarioRequest(
                fixture.manifest, fixture.workload.deterministicId(), '1001', null)).status() ==
                OnDemandFaultScenarioResult.Status.PERSISTED
    }

    def 'request body failure after successful acquisition is not relabeled as a lock failure'() {
        given:
        def fixture = writePackage(Files.createTempDirectory('on-demand-lock-body-failure'), 2)
        def before = snapshotMutable(fixture)
        def service = new OnDemandFaultScenarioService(
                { ignoredPlan, ignoredVector, ignoredCap -> null }
                        as OnDemandFaultScenarioService.RecoveryScheduleSource,
                { ignored -> } as OnDemandFaultScenarioService.FailureInjector,
                new OnDemandFaultScenarioService.NioFileMover(),
                { path -> Files.deleteIfExists(path) } as OnDemandFaultScenarioService.TemporaryFileCleaner,
                new OnDemandFaultScenarioService.NioPackageLockProvider())

        when:
        def result = service.request(new OnDemandFaultScenarioRequest(
                fixture.manifest, fixture.workload.deterministicId(), '1001', null))

        then:
        result.status() == OnDemandFaultScenarioResult.Status.INTEGRITY_FAILURE
        result.diagnostics()*.code() == ['REQUEST_PROCESSING_FAILED']
        result.diagnostics().first().message().contains('null')
        sameMutableBytes(before, fixture)
        canAcquirePackageLock(fixture)
        new OnDemandFaultScenarioService().request(new OnDemandFaultScenarioRequest(
                fixture.manifest, fixture.workload.deterministicId(), '1001', null)).status() ==
                OnDemandFaultScenarioResult.Status.PERSISTED
    }

    def 'escaping request body error still releases the acquired package lock'() {
        given:
        def fixture = writePackage(Files.createTempDirectory('on-demand-lock-escaping-body-failure'), 2)
        def service = new OnDemandFaultScenarioService(
                { ignoredPlan, ignoredVector, ignoredCap -> throw new AssertionError('injected escaping body error') }
                        as OnDemandFaultScenarioService.RecoveryScheduleSource,
                { ignored -> } as OnDemandFaultScenarioService.FailureInjector,
                new OnDemandFaultScenarioService.NioFileMover())

        when:
        service.request(new OnDemandFaultScenarioRequest(
                fixture.manifest, fixture.workload.deterministicId(), '1001', null))

        then:
        def error = thrown(AssertionError)
        error.message == 'injected escaping body error'
        canAcquirePackageLock(fixture)
        new OnDemandFaultScenarioService().request(new OnDemandFaultScenarioRequest(
                fixture.manifest, fixture.workload.deterministicId(), '1001', null)).status() ==
                OnDemandFaultScenarioResult.Status.PERSISTED
    }

    def 'lock close failure cannot replace a committed validated result and later writers proceed'() {
        given:
        def fixture = writePackage(Files.createTempDirectory('on-demand-lock-close-failure'), 2)
        def nioProvider = new OnDemandFaultScenarioService.NioPackageLockProvider()
        def provider = { lockPath ->
            def delegate = nioProvider.open(lockPath)
            [
                    acquire: { delegate.acquire() },
                    close  : {
                        delegate.close()
                        throw new IOException('injected lock close failure')
                    }
            ] as OnDemandFaultScenarioService.PackageLockHandle
        } as OnDemandFaultScenarioService.PackageLockProvider

        when:
        def result = new OnDemandFaultScenarioService(provider).request(new OnDemandFaultScenarioRequest(
                fixture.manifest, fixture.workload.deterministicId(), '1001', null))

        then:
        result.status() == OnDemandFaultScenarioResult.Status.PERSISTED
        result.diagnostics().empty
        new ScenarioCatalogPackageReader().read(fixture.manifest).faultScenarios().any {
            it.assignedVector() == '1001'
        }
        canAcquirePackageLock(fixture)
        new OnDemandFaultScenarioService().request(new OnDemandFaultScenarioRequest(
                fixture.manifest, fixture.workload.deterministicId(), '0110', null)).status() ==
                OnDemandFaultScenarioResult.Status.PERSISTED
        new ScenarioCatalogPackageReader().read(fixture.manifest).faultScenarios()*.assignedVector()
                .containsAll(['1001', '0110'])
    }

    def 'unsupported package lock paths fail before semantic artifact mutation'() {
        given:
        def fixture = writePackage(Files.createTempDirectory("on-demand-lock-${shape}"), 2)
        def lockPath = fixture.manifest.parent.resolve('.on-demand-fault-scenario.lock')
        if (shape == 'symlink') {
            Files.createSymbolicLink(lockPath, Files.createTempFile('on-demand-lock-target', '.tmp'))
        } else {
            Files.createDirectory(lockPath)
        }
        def before = snapshotMutable(fixture)

        when:
        def result = new OnDemandFaultScenarioService().request(new OnDemandFaultScenarioRequest(
                fixture.manifest, fixture.workload.deterministicId(), '0011', null))

        then:
        result.status() == OnDemandFaultScenarioResult.Status.PERSISTENCE_FAILED
        result.diagnostics()*.code() == ['PACKAGE_LOCK_FAILED']
        sameMutableBytes(before, fixture)

        where:
        shape << ['symlink', 'directory']
    }

    def 'validation and publication failures release the package OS lock for a later request'() {
        given:
        def fixture = writePackage(Files.createTempDirectory('on-demand-lock-release'), 2)
        def failingService = new OnDemandFaultScenarioService(
                { WorkloadPlan plan, String vector, int cap -> RecoveryScheduleGenerator.generate(plan, vector, cap) }
                        as OnDemandFaultScenarioService.RecoveryScheduleSource,
                { boundary ->
                    if (boundary == OnDemandFaultScenarioService.Boundary.ACCOUNTING_PROMOTED) {
                        throw new IOException('injected publication failure')
                    }
                } as OnDemandFaultScenarioService.FailureInjector,
                new OnDemandFaultScenarioService.NioFileMover())

        expect: 'an early validation return releases the lock'
        new OnDemandFaultScenarioService().request(new OnDemandFaultScenarioRequest(
                fixture.manifest, fixture.workload.deterministicId(), 'bad', null)).status() ==
                OnDemandFaultScenarioResult.Status.REJECTED
        canAcquirePackageLock(fixture)

        and: 'a caught publication failure and rollback also release the lock'
        failingService.request(new OnDemandFaultScenarioRequest(
                fixture.manifest, fixture.workload.deterministicId(), '1001', null)).status() ==
                OnDemandFaultScenarioResult.Status.PERSISTENCE_FAILED
        canAcquirePackageLock(fixture)

        and: 'a later mutation can proceed'
        new OnDemandFaultScenarioService().request(new OnDemandFaultScenarioRequest(
                fixture.manifest, fixture.workload.deterministicId(), '0110', null)).status() ==
                OnDemandFaultScenarioResult.Status.PERSISTED
    }

    def 'different package directories can generate concurrently'() {
        given:
        def firstFixture = writePackage(Files.createTempDirectory('on-demand-independent-a'), 2)
        def secondFixture = writePackage(Files.createTempDirectory('on-demand-independent-b'), 2)
        def entered = new CountDownLatch(2)
        def release = new CountDownLatch(1)
        def source = { WorkloadPlan plan, String vector, int cap ->
            entered.countDown()
            assert release.await(10, TimeUnit.SECONDS)
            RecoveryScheduleGenerator.generate(plan, vector, cap)
        } as OnDemandFaultScenarioService.RecoveryScheduleSource
        def service = new OnDemandFaultScenarioService(source,
                { ignored -> } as OnDemandFaultScenarioService.FailureInjector,
                new OnDemandFaultScenarioService.NioFileMover())
        def executor = Executors.newFixedThreadPool(2)

        when:
        def first = executor.submit({ service.request(new OnDemandFaultScenarioRequest(
                firstFixture.manifest, firstFixture.workload.deterministicId(), '1001', null)) }
                as java.util.concurrent.Callable<OnDemandFaultScenarioResult>)
        def second = executor.submit({ service.request(new OnDemandFaultScenarioRequest(
                secondFixture.manifest, secondFixture.workload.deterministicId(), '0110', null)) }
                as java.util.concurrent.Callable<OnDemandFaultScenarioResult>)

        then:
        entered.await(10, TimeUnit.SECONDS)

        cleanup:
        release.countDown()
        assert first.get(20, TimeUnit.SECONDS).successful()
        assert second.get(20, TimeUnit.SECONDS).successful()
        executor.shutdownNow()
    }

    def 'package-local guard serializes concurrent requests in one process'() {
        given:
        def fixture = writePackage(Files.createTempDirectory('on-demand-concurrent'), 2)
        def active = new AtomicInteger()
        def maximum = new AtomicInteger()
        def source = { WorkloadPlan plan, String vector, int cap ->
            def current = active.incrementAndGet()
            maximum.accumulateAndGet(current, Math.&max)
            try {
                Thread.sleep(75)
                RecoveryScheduleGenerator.generate(plan, vector, cap)
            } finally {
                active.decrementAndGet()
            }
        } as OnDemandFaultScenarioService.RecoveryScheduleSource
        def firstService = new OnDemandFaultScenarioService(source,
                { ignored -> } as OnDemandFaultScenarioService.FailureInjector,
                new OnDemandFaultScenarioService.NioFileMover())
        def secondService = new OnDemandFaultScenarioService(source,
                { ignored -> } as OnDemandFaultScenarioService.FailureInjector,
                new OnDemandFaultScenarioService.NioFileMover())
        def executor = Executors.newFixedThreadPool(2)

        when:
        def results = [
                executor.submit({ firstService.request(new OnDemandFaultScenarioRequest(
                        fixture.manifest, fixture.workload.deterministicId(), '1001', null)) }
                        as java.util.concurrent.Callable<OnDemandFaultScenarioResult>),
                executor.submit({ secondService.request(new OnDemandFaultScenarioRequest(
                        fixture.manifest, fixture.workload.deterministicId(), '0110', null)) }
                        as java.util.concurrent.Callable<OnDemandFaultScenarioResult>)
        ]*.get(20, TimeUnit.SECONDS)
        executor.shutdownNow()

        then:
        results.every { it.status() == OnDemandFaultScenarioResult.Status.PERSISTED }
        maximum.get() == 1
        new ScenarioCatalogPackageReader().read(fixture.manifest).faultScenarios()*.assignedVector().containsAll(['1001', '0110'])
    }

    def 'package-local guard serializes accepted manifest aliases that share mutable artifacts'() {
        given:
        def fixture = writePackage(Files.createTempDirectory('on-demand-manifest-alias'), 2)
        def aliasManifest = fixture.manifest.parent.resolve('scenario-catalog-manifest-alias.json')
        Files.copy(fixture.manifest, aliasManifest)
        def enteredGeneration = new CountDownLatch(1)
        def active = new AtomicInteger()
        def maximum = new AtomicInteger()
        def source = { WorkloadPlan plan, String vector, int cap ->
            def current = active.incrementAndGet()
            maximum.accumulateAndGet(current, Math.&max)
            enteredGeneration.countDown()
            try {
                Thread.sleep(150)
                RecoveryScheduleGenerator.generate(plan, vector, cap)
            } finally {
                active.decrementAndGet()
            }
        } as OnDemandFaultScenarioService.RecoveryScheduleSource
        def firstService = new OnDemandFaultScenarioService(source,
                { ignored -> } as OnDemandFaultScenarioService.FailureInjector,
                new OnDemandFaultScenarioService.NioFileMover())
        def secondService = new OnDemandFaultScenarioService(source,
                { ignored -> } as OnDemandFaultScenarioService.FailureInjector,
                new OnDemandFaultScenarioService.NioFileMover())
        def executor = Executors.newFixedThreadPool(2)

        when:
        def first = executor.submit({ firstService.request(new OnDemandFaultScenarioRequest(
                fixture.manifest, fixture.workload.deterministicId(), '1001', null)) }
                as java.util.concurrent.Callable<OnDemandFaultScenarioResult>)
        assert enteredGeneration.await(10, TimeUnit.SECONDS)
        def second = executor.submit({ secondService.request(new OnDemandFaultScenarioRequest(
                aliasManifest, fixture.workload.deterministicId(), '0110', null)) }
                as java.util.concurrent.Callable<OnDemandFaultScenarioResult>)
        def firstResult = first.get(20, TimeUnit.SECONDS)
        def secondResult = second.get(20, TimeUnit.SECONDS)
        executor.shutdownNow()

        then:
        firstResult.status() == OnDemandFaultScenarioResult.Status.PERSISTED
        secondResult.status() == OnDemandFaultScenarioResult.Status.REJECTED
        secondResult.diagnostics()*.code() == ['INVALID_PACKAGE']
        maximum.get() == 1
        new ScenarioCatalogPackageReader().read(fixture.manifest).faultScenarios().any { it.assignedVector() == '1001' }
    }

    def 'operator CLI persists a request and emits structured JSON without invoking an executor'() {
        given:
        def fixture = writePackage(Files.createTempDirectory('on-demand-cli'), 2)
        def output = new ByteArrayOutputStream()

        when:
        def exitCode = FaultScenarioRequestCli.run([
                '--manifest-path', fixture.manifest.toString(),
                '--workload-plan-id', fixture.workload.deterministicId(),
                '--fault-vector', '0011',
                '--recovery-schedule-cap', '2'
        ] as String[], new PrintStream(output), new OnDemandFaultScenarioService())

        then:
        exitCode == 0
        mapper.readTree(output.toString()).path('status').asText() == 'PERSISTED'
        new ScenarioCatalogPackageReader().read(fixture.manifest).faultScenarios().any { it.assignedVector() == '0011' }
    }

    private static Process requestProcess(Map fixture, String vector, Map signals) {
        new ProcessBuilder(
                java.nio.file.Path.of(System.getProperty('java.home'), 'bin', 'java').toString(),
                '-cp', System.getProperty('java.class.path'),
                OnDemandFaultScenarioLockProcess.name,
                fixture.manifest.toString(),
                fixture.workload.deterministicId(),
                vector,
                signals.ready.toString(),
                signals.acquired.toString(),
                signals.completed.toString(),
                signals.result.toString())
                .redirectErrorStream(true)
                .start()
    }

    private static Map processSignals(Map fixture, String prefix) {
        [
                ready    : fixture.manifest.parent.resolve("${prefix}-ready"),
                acquired : fixture.manifest.parent.resolve("${prefix}-acquired"),
                completed: fixture.manifest.parent.resolve("${prefix}-completed"),
                result   : fixture.manifest.parent.resolve("${prefix}-result.json")
        ]
    }

    private static boolean awaitFile(java.nio.file.Path path) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20)
        while (!Files.exists(path) && System.nanoTime() < deadline) {
            Thread.sleep(10)
        }
        Files.exists(path)
    }

    private static boolean canAcquirePackageLock(Map fixture) {
        def channel = FileChannel.open(fixture.manifest.parent.resolve('.on-demand-fault-scenario.lock'),
                StandardOpenOption.CREATE, StandardOpenOption.WRITE)
        try {
            def lock = channel.tryLock()
            if (lock == null) {
                return false
            }
            lock.release()
            return true
        } finally {
            channel.close()
        }
    }

    private static Map writePackage(java.nio.file.Path directory, int cap, WorkloadPlan workload = workload()) {
        writePackage(directory, cap, [workload]) + [workload: workload]
    }

    private static Map writePackage(java.nio.file.Path directory, int cap, List<WorkloadPlan> workloads) {
        def generation = new WorkloadGenerationResult(
                WorkloadPlan.SCHEMA_VERSION,
                new ScenarioGeneratorConfig(),
                workloads,
                [],
                [workloadsGenerated: workloads.size()],
                [])
        def eager = EagerFaultScenarioGenerator.generate(generation, new RecoveryScheduleCap(cap))
        def paths = [
                workload     : directory.resolve('workload-catalog.jsonl'),
                faultScenario: directory.resolve('fault-scenario-catalog.jsonl'),
                manifest     : directory.resolve('scenario-catalog-manifest.json'),
                rejected     : directory.resolve('workload-catalog-rejected-inputs.jsonl'),
                accounting   : directory.resolve('scenario-space-accounting.json')
        ]
        new ScenarioCatalogJsonlWriter().write(
                eager,
                paths.workload,
                paths.faultScenario,
                paths.manifest,
                paths.rejected,
                paths.accounting,
                null,
                '2026-07-20T00:00:00Z')
        paths + [workloads: workloads, workloadPath: paths.workload]
    }

    private static Map<String, byte[]> snapshotMutable(Map fixture) {
        [
                faultScenario: Files.readAllBytes(fixture.faultScenario),
                accounting   : Files.readAllBytes(fixture.accounting),
                manifest     : Files.readAllBytes(fixture.manifest)
        ]
    }

    private static boolean sameMutableBytes(Map<String, byte[]> expected, Map fixture) {
        Arrays.equals(expected.faultScenario, Files.readAllBytes(fixture.faultScenario))
                && Arrays.equals(expected.accounting, Files.readAllBytes(fixture.accounting))
                && Arrays.equals(expected.manifest, Files.readAllBytes(fixture.manifest))
    }

    private void refreshArtifactHash(java.nio.file.Path manifestPath,
                                     String artifactField,
                                     java.nio.file.Path artifactPath) {
        def manifest = mapper.readTree(Files.readString(manifestPath))
        manifest.path(artifactField).put('sha256', ScenarioCatalogJsonlWriter.sha256(Files.readAllBytes(artifactPath)))
        Files.writeString(manifestPath, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(manifest) + '\n')
    }

    private void updateArtifact(java.nio.file.Path manifestPath,
                                String artifactField,
                                java.nio.file.Path artifactPath,
                                int recordCount) {
        def manifest = mapper.readTree(Files.readString(manifestPath))
        manifest.path(artifactField).put('recordCount', recordCount.toString())
        manifest.path(artifactField).put('sha256', ScenarioCatalogJsonlWriter.sha256(Files.readAllBytes(artifactPath)))
        Files.writeString(manifestPath, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(manifest) + '\n')
    }

    private static WorkloadPlan workload(boolean ready = true) {
        def definitions = [
                [owner: 'a', name: 'a1', checkpoint: true],
                [owner: 'b', name: 'b1', checkpoint: true],
                [owner: 'a', name: 'a2', checkpoint: false],
                [owner: 'b', name: 'b2', checkpoint: false]
        ]
        def inputs = ['a', 'b'].collect { owner -> readyInput(owner, ready) }
        def participants = ['a', 'b'].collect { owner ->
            new SagaInstance(owner, "example.${owner.toUpperCase()}Saga".toString(), "input-${owner}".toString(), [])
        }
        def schedule = []
        def slots = []
        def checkpoints = []
        definitions.eachWithIndex { definition, index ->
            def stepId = "example.${definition.owner.toUpperCase()}Saga::${definition.name}".toString()
            def occurrenceId = "forward-${index}".toString()
            schedule << new ScheduledStep(occurrenceId, definition.owner as String, stepId, index,
                    definition.name as String, [])
            slots << new ForwardFaultSlot("slot-${index}".toString(), index, occurrenceId,
                    definition.owner as String, stepId, definition.name as String, occurrenceId)
            if (definition.checkpoint) {
                checkpoints << new CompensationCheckpoint("checkpoint-${index}".toString(), checkpoints.size(),
                        definition.owner as String, occurrenceId, stepId, definition.name as String, occurrenceId,
                        CompensationEvidenceClass.EXPLICIT_COMPENSATION, [], [], [])
            }
        }
        def withoutId = new WorkloadPlan(
                WorkloadPlan.SCHEMA_VERSION,
                null,
                ScenarioKind.MULTI_SAGA,
                WorkloadExecutionShape.SAGA_LOCAL,
                participants,
                inputs,
                schedule,
                [],
                slots,
                checkpoints,
                [])
        new WorkloadPlan(
                withoutId.schemaVersion(),
                ScenarioIdGenerator.workloadPlanId(withoutId),
                withoutId.kind(),
                withoutId.executionShape(),
                withoutId.participants(),
                withoutId.acceptedInputs(),
                withoutId.forwardSchedule(),
                withoutId.conflictEvidence(),
                withoutId.faultSlots(),
                withoutId.compensationCheckpoints(),
                withoutId.warnings())
    }

    private static InputVariant readyInput(String owner, boolean ready) {
        def node = InputRecipeNode.builder('literal')
                .sourceText('1')
                .provenanceText('argument 0')
                .executorReady(true)
                .literalKind('integer')
                .value(1L)
                .targetTypeFqn(Long.name)
                .build()
        def argument = new InputRecipeArgument(0, Long.name, InputResolutionStatus.RESOLVED,
                true, [], 'argument 0', node)
        def recipe = ready ? new InputRecipe(InputRecipe.SCHEMA_VERSION, null, true, [], [argument]) : null
        new InputVariant(
                "input-${owner}".toString(),
                "example.${owner.toUpperCase()}Saga".toString(),
                'example.OnDemandSpec',
                'fixture',
                owner,
                InputResolutionStatus.RESOLVED,
                'source',
                'provenance',
                [],
                [:],
                [],
                recipe)
    }
}
