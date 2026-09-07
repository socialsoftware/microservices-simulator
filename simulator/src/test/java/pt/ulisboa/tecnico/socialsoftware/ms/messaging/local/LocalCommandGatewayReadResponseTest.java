package pt.ulisboa.tecnico.socialsoftware.ms.messaging.local;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.MessagingObjectMapperProvider;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.impact.ImpactEvidence;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.impact.ImpactEvidenceObserver;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.impact.ImpactEvidenceObserverHolder;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.impact.ImpactWriterContext;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.sagaread.ReadObservationContext;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.sagaread.ReadResponseAdapter;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.sagaread.ReadResponseEvidence;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.messaging.SagaCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.messaging.SagaCommandHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

import java.util.ArrayList;
import java.util.List;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import javax.tools.ToolProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LocalCommandGatewayReadResponseTest {
    private static final ImpactEvidence.Writer READER = new ImpactEvidence.Writer(
            "SAGA", "attempt-1", "workload-1", "B", "read#0", "FORWARD", "FindParcel", "inspect", null);
    private static final ParcelAdapter ADAPTER = new ParcelAdapter();

    @Test
    void sourceOnlyDummyappFixtureCoversExplicitRevisionAndUnversionedNegative(@TempDir Path output) throws Exception {
        Path fixture = Path.of("../applications/dummyapp/src/main/java/com/example/dummyapp/diagnostics/ReadResponseFixture.java");
        var compiler = ToolProvider.getSystemJavaCompiler();
        assertThat(compiler).isNotNull();
        try (var files = compiler.getStandardFileManager(null, null, null)) {
            assertThat(compiler.getTask(null, files, null, List.of("-classpath",
                            System.getProperty("surefire.test.class.path", System.getProperty("java.class.path")),
                            "-d", output.toString()), null,
                    files.getJavaFileObjects(fixture.toFile())).call()).isTrue();
        }
        try (var loader = new URLClassLoader(new java.net.URL[]{output.toUri().toURL()}, getClass().getClassLoader())) {
            String prefix = "com.example.dummyapp.diagnostics.ReadResponseFixture$";
            Command command = (Command) loader.loadClass(prefix + "Inspect")
                    .getConstructor(UnitOfWork.class, Integer.class).newInstance(new TestUnitOfWork(999L, "FindParcel"), 999);
            Object revision = loader.loadClass(prefix + "Revision").getConstructor(Integer.class, Long.class)
                    .newInstance(41, 1L);
            Object unversioned = loader.loadClass(prefix + "Unversioned").getConstructor(Integer.class).newInstance(41);
            var positiveAdapter = (ReadResponseAdapter<?, ?>) loader.loadClass(prefix + "RevisionAdapter")
                    .getConstructor().newInstance();
            var negativeAdapter = (ReadResponseAdapter<?, ?>) loader.loadClass(prefix + "UnversionedAdapter")
                    .getConstructor().newInstance();
            // Route this fixture to the same small in-process service, without adding a runtime dummyapp.
            command.setServiceName("parcel");
            RecordingObserver observer = new RecordingObserver();
            try (var scope = ImpactEvidenceObserverHolder.install(observer);
                 var writer = ImpactWriterContext.enter(READER)) {
                gateway(false, ignored -> revision, List.of(positiveAdapter, negativeAdapter)).send(command);
                gateway(false, ignored -> unversioned, List.of(positiveAdapter, negativeAdapter)).send(command);
            }
            assertThat(observer.reads).extracting(ReadResponseEvidence.Observation::outcome)
                    .containsExactly(ReadResponseEvidence.Outcome.DELIVERED, ReadResponseEvidence.Outcome.DELIVERED_INVALID);
            assertThat(observer.reads.getFirst().identity()).isEqualTo(
                    new ImpactEvidence.AggregateIdentity("com.example.dummyapp.item.aggregate.Item", 41));
            assertThat(observer.reads.getFirst().version()).isEqualTo(1L);
            assertThat(observer.reads.get(1).reason()).isEqualTo("MISSING_RETURNED_REVISION");
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void observesTheReturnedOuterRevisionAndIdentityNotTheInternalSelectionOrRequestedId(boolean serialized) {
        Parcel olderDto = new Parcel(41, 1L);
        olderDto.setNested(new Parcel(42, 900L));
        LocalCommandGateway gateway = gateway(serialized, command -> {
            command.getUnitOfWork().setVersion(2L); // Different internal selection/UoW; never a read oracle.
            return olderDto;
        }, List.of(ADAPTER));
        RecordingObserver observer = new RecordingObserver();
        try (var scope = ImpactEvidenceObserverHolder.install(observer);
             var writer = ImpactWriterContext.enter(READER)) {
            Parcel actual = (Parcel) gateway.send(new SagaCommand(new InspectParcel(999)));
            assertThat(actual.getReferenceToken()).isEqualTo(41);
            assertThat(actual.getRevisionMarker()).isEqualTo(1L);
            assertThat(actual.getNested().getRevisionMarker()).isEqualTo(900L);
            if (serialized) assertThat(actual).isNotSameAs(olderDto);
            else assertThat(actual).isSameAs(olderDto);
        }
        assertThat(observer.reads).hasSize(1);
        var read = observer.reads.getFirst();
        assertThat(read.outcome()).isEqualTo(ReadResponseEvidence.Outcome.DELIVERED);
        assertThat(read.identity()).isEqualTo(new ImpactEvidence.AggregateIdentity("Parcel", 41));
        assertThat(read.version()).isEqualTo(1L);
        assertThat(read.reader()).isEqualTo(READER);
        assertThat(read.transportCommandType()).isEqualTo(SagaCommand.class.getName());
        assertThat(read.commandType()).isEqualTo(InspectParcel.class.getName());
        assertThat(read.contract().runtimeType()).isEqualTo("fixture.PersistentParcel");
        assertThat(read.serialized()).isEqualTo(serialized);
    }

    @Test
    void usesThePostDeserializationResultEvenWhenItsRevisionDiffersFromTheServiceObject() {
        ConvertedParcel serviceObject = new ConvertedParcel(41, 2L);
        ReadResponseAdapter<InspectParcel, ConvertedParcel> adapter = new Adapter<>(ConvertedParcel.class,
                ConvertedParcel::getReferenceToken, ConvertedParcel::getRevisionMarker);
        LocalCommandGateway gateway = gateway(true, ignored -> serviceObject, List.of(adapter));
        RecordingObserver observer = new RecordingObserver();
        try (var scope = ImpactEvidenceObserverHolder.install(observer);
             var writer = ImpactWriterContext.enter(READER)) {
            ConvertedParcel actual = (ConvertedParcel) gateway.send(new InspectParcel(41));
            assertThat(serviceObject.getRevisionMarker()).isEqualTo(2L);
            assertThat(actual.getRevisionMarker()).isEqualTo(1L);
            assertThat(observer.reads.getFirst().version()).isEqualTo(actual.getRevisionMarker());
        }
    }

    @Test
    void missingIdentityAndRevisionRemainExplicitWithoutFallingBackToRequestedIdOrUow() {
        List<Parcel> responses = List.of(new Parcel(null, 2L), new Parcel(41, null));
        AtomicInteger calls = new AtomicInteger();
        LocalCommandGateway gateway = gateway(false, ignored -> responses.get(calls.getAndIncrement()), List.of(ADAPTER));
        RecordingObserver observer = new RecordingObserver();
        try (var scope = ImpactEvidenceObserverHolder.install(observer);
             var writer = ImpactWriterContext.enter(READER)) {
            gateway.send(new InspectParcel(41));
            gateway.send(new InspectParcel(41));
        }
        assertThat(observer.reads).extracting(ReadResponseEvidence.Observation::reason)
                .containsExactly("MISSING_RETURNED_IDENTITY", "MISSING_RETURNED_REVISION");
        assertThat(observer.reads).allMatch(read -> read.outcome() == ReadResponseEvidence.Outcome.DELIVERED_INVALID);
        assertThat(observer.reads.getFirst().identity().aggregateId()).isNull();
        assertThat(observer.reads.get(1).version()).isNull();
    }

    @Test
    void exactCommandAndResponseClassesAreRequiredAndAmbiguousContractsAreNeverChosen() {
        RecordingObserver observer = new RecordingObserver();
        try (var scope = ImpactEvidenceObserverHolder.install(observer);
             var writer = ImpactWriterContext.enter(READER)) {
            gateway(false, ignored -> new Parcel(41, 1L), List.of(ADAPTER, new ParcelAdapter()))
                    .send(new InspectParcel(41));
            gateway(false, ignored -> new Parcel(41, 1L), List.of(ADAPTER))
                    .send(new DerivedInspectParcel(41));
            gateway(false, ignored -> new ConvertedParcel(41, 1L), List.of(ADAPTER))
                    .send(new InspectParcel(41));
            gateway(false, ignored -> null, List.of(ADAPTER)).send(new InspectParcel(41));
            gateway(false, ignored -> new Parcel(41, 1L), List.of(ADAPTER))
                    .send(new DerivedSagaCommand(new InspectParcel(41)));
        }
        assertThat(observer.reads).extracting(ReadResponseEvidence.Observation::reason).containsExactly(
                "AMBIGUOUS_READ_ADAPTER", "NO_READ_ADAPTER", "UNSUPPORTED_RESPONSE_TYPE",
                "UNSUPPORTED_RESPONSE_TYPE", "NO_READ_ADAPTER");
        assertThat(observer.reads).noneMatch(read -> read.outcome() == ReadResponseEvidence.Outcome.DELIVERED);
    }

    @Test
    void invalidAdapterMetadataCannotSupplyAPersistentType() {
        ParcelAdapter invalid = new ParcelAdapter() {
            @Override public String runtimeType() { return ""; }
        };
        RecordingObserver observer = new RecordingObserver();
        try (var scope = ImpactEvidenceObserverHolder.install(observer);
             var writer = ImpactWriterContext.enter(READER)) {
            gateway(false, ignored -> new Parcel(41, 1L), List.of(invalid)).send(new InspectParcel(41));
        }
        assertThat(observer.reads.getFirst().reason()).isEqualTo("INVALID_ADAPTER_CONTRACT");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void failedServiceAfterConstructingDtoDoesNotCreateADeliveryAndRetryRecordsOnlyTheSuccess(boolean serialized) {
        AtomicInteger calls = new AtomicInteger();
        LocalCommandGateway gateway = gateway(serialized, ignored -> {
            Parcel dto = new Parcel(41, 1L);
            if (calls.incrementAndGet() == 1) throw new IllegalStateException("after constructing DTO");
            return dto;
        }, List.of(ADAPTER));
        RecordingObserver observer = new RecordingObserver();
        try (var scope = ImpactEvidenceObserverHolder.install(observer);
             var writer = ImpactWriterContext.enter(READER)) {
            assertThatThrownBy(() -> gateway.send(new InspectParcel(41))).isInstanceOf(RuntimeException.class);
            assertThat(gateway.send(new InspectParcel(41))).isInstanceOf(Parcel.class);
        }
        assertThat(observer.reads).extracting(ReadResponseEvidence.Observation::outcome)
                .containsExactly(ReadResponseEvidence.Outcome.FAILED, ReadResponseEvidence.Outcome.DELIVERED);
    }

    @Test
    void semanticLockFailureAfterTheDomainHandlerBuildsADtoDoesNotCreateADelivery() {
        SagaUnitOfWorkService uowService = mock(SagaUnitOfWorkService.class);
        doThrow(new IllegalStateException("semantic lock failed"))
                .when(uowService).registerSagaState(eq(41), any(), any());
        SagaCommandHandler decorator = new SagaCommandHandler();
        ReflectionTestUtils.setField(decorator, "sagaUnitOfWorkService", uowService);
        AtomicInteger builtDtos = new AtomicInteger();
        TestHandler handler = new TestHandler(command -> {
            builtDtos.incrementAndGet();
            return new Parcel(41, 1L);
        });
        ReflectionTestUtils.setField(handler, "commandHandlerDecorator", decorator);
        LocalCommandGateway gateway = gateway(false, handler, List.of(ADAPTER));
        SagaCommand command = new SagaCommand(new InspectParcel(41));
        command.setUnitOfWork(new SagaUnitOfWork(2L, "FindParcel"));
        command.setSemanticLock(GenericSagaState.NOT_IN_SAGA);
        RecordingObserver observer = new RecordingObserver();
        try (var scope = ImpactEvidenceObserverHolder.install(observer);
             var writer = ImpactWriterContext.enter(READER)) {
            assertThatThrownBy(() -> gateway.send(command)).hasMessage("semantic lock failed");
        }
        assertThat(builtDtos).hasValue(1);
        assertThat(observer.reads).extracting(ReadResponseEvidence.Observation::outcome)
                .containsExactly(ReadResponseEvidence.Outcome.FAILED);
    }

    @Test
    void malformedJsonResponseDoesNotCreateADelivery() {
        ApplicationContext context = mock(ApplicationContext.class);
        LocalCommandService service = mock(LocalCommandService.class);
        when(service.sendJson(any())).thenReturn("{incomplete");
        LocalCommandGateway gateway = configured(true, context, service, List.of(ADAPTER));
        RecordingObserver observer = new RecordingObserver();
        try (var scope = ImpactEvidenceObserverHolder.install(observer);
             var writer = ImpactWriterContext.enter(READER)) {
            assertThatThrownBy(() -> gateway.send(new InspectParcel(41)))
                    .hasMessage("Failed to deserialize command response");
        }
        assertThat(observer.reads).extracting(ReadResponseEvidence.Observation::outcome)
                .containsExactly(ReadResponseEvidence.Outcome.FAILED);
    }

    @Test
    void observerSetupRecoveryAndEventCallsAreExcludedAndUnknownOrMismatchedAttributionCannotBecomeB() {
        RecordingObserver observer = new RecordingObserver();
        LocalCommandGateway gateway = gateway(false, ignored -> new Parcel(41, 1L), List.of(ADAPTER));
        try (var scope = ImpactEvidenceObserverHolder.install(observer)) {
            gateway.send(new InspectParcel(41));
            try (var writer = ImpactWriterContext.enter(READER)) {
                try (var excluded = ReadObservationContext.exclude("OBSERVER")) {
                    gateway.send(new InspectParcel(41));
                }
                try (var excluded = ReadObservationContext.exclude("SETUP")) {
                    gateway.send(new InspectParcel(41));
                }
                try (var excluded = ReadObservationContext.exclude("PROBE")) {
                    gateway.send(new InspectParcel(41));
                }
                gateway.send(new InspectParcel(41)); // suppression restored
            }
            for (ImpactEvidence.Writer excluded : List.of(writer("SAGA", "RECOVERY", "recover#0"),
                    writer("EVENT_CONSUMER", "EVENT", "event#0"), writer("SETUP", "SETUP", "setup#0"))) {
                try (var writer = ImpactWriterContext.enter(excluded)) { gateway.send(new InspectParcel(41)); }
            }
            try (var writer = ImpactWriterContext.enter(writer("SAGA", "FORWARD", null))) {
                gateway.send(new InspectParcel(41));
            }
            try (var writer = ImpactWriterContext.enter(ImpactEvidence.Writer.unknown("FindParcel", "inspect"))) {
                gateway.send(new InspectParcel(41));
            }
            ImpactWriterContext.Scope[] changed = new ImpactWriterContext.Scope[1];
            try (var writer = ImpactWriterContext.enter(READER)) {
                try {
                    gateway(false, ignored -> {
                        changed[0] = ImpactWriterContext.enter(writer("SAGA", "FORWARD", "another#0"));
                        return new Parcel(41, 1L);
                    }, List.of(ADAPTER)).send(new InspectParcel(41));
                } finally { if (changed[0] != null) changed[0].close(); }
            }
        }
        assertThat(observer.reads).extracting(ReadResponseEvidence.Observation::reason).containsExactly(
                "MISSING_READER_ATTRIBUTION", "OBSERVER", "SETUP", "PROBE", null,
                "READER_ROLE_EXCLUDED", "READER_ROLE_EXCLUDED", "READER_ROLE_EXCLUDED",
                "MISSING_READER_ATTRIBUTION", "MISSING_READER_ATTRIBUTION", "READER_ATTRIBUTION_CHANGED");
        assertThat(observer.reads.stream().filter(read -> read.outcome() == ReadResponseEvidence.Outcome.DELIVERED))
                .hasSize(1);
        assertThat(ImpactWriterContext.current()).isEmpty();
        assertThat(ReadObservationContext.excludedRole()).isNull();
    }

    @Test
    void failedEventRecoveryObserverAndSetupCallsAreExcludedWhileMissingReadersRemainInvalid() {
        LocalCommandGateway gateway = gateway(false, ignored -> {
            throw new IllegalStateException("application failure");
        }, List.of(ADAPTER));
        RecordingObserver observer = new RecordingObserver();
        try (var scope = ImpactEvidenceObserverHolder.install(observer)) {
            for (ImpactEvidence.Writer excluded : List.of(writer("SAGA", "RECOVERY", "recover#0"),
                    writer("EVENT_CONSUMER", "EVENT", "event#0"), writer("SETUP", "SETUP", "setup#0"))) {
                try (var writer = ImpactWriterContext.enter(excluded)) {
                    assertThatThrownBy(() -> gateway.send(new InspectParcel(41))).hasMessage("application failure");
                }
            }
            try (var writer = ImpactWriterContext.enter(READER);
                 var excluded = ReadObservationContext.exclude("OBSERVER")) {
                assertThatThrownBy(() -> gateway.send(new InspectParcel(41))).hasMessage("application failure");
            }
            assertThatThrownBy(() -> gateway.send(new InspectParcel(41))).hasMessage("application failure");
            try (var writer = ImpactWriterContext.enter(ImpactEvidence.Writer.unknown("FindParcel", "inspect"))) {
                assertThatThrownBy(() -> gateway.send(new InspectParcel(41))).hasMessage("application failure");
            }
        }
        assertThat(observer.reads).extracting(ReadResponseEvidence.Observation::outcome).containsExactly(
                ReadResponseEvidence.Outcome.EXCLUDED, ReadResponseEvidence.Outcome.EXCLUDED,
                ReadResponseEvidence.Outcome.EXCLUDED, ReadResponseEvidence.Outcome.EXCLUDED,
                ReadResponseEvidence.Outcome.FAILED_INVALID, ReadResponseEvidence.Outcome.FAILED_INVALID);
        assertThat(observer.reads).extracting(ReadResponseEvidence.Observation::reason).containsExactly(
                "READER_ROLE_EXCLUDED", "READER_ROLE_EXCLUDED", "READER_ROLE_EXCLUDED", "OBSERVER",
                "MISSING_READER_ATTRIBUTION", "MISSING_READER_ATTRIBUTION");
    }

    @Test
    void readCallbackAndAdapterFailuresAreContainedAndRetainedOutsideImpactV2() {
        Parcel response = new Parcel(41, 1L);
        RecordingObserver failing = new RecordingObserver() {
            @Override public void readResponse(ReadResponseEvidence.Observation observation) {
                throw new IllegalStateException("read callback failed");
            }
        };
        var scope = ImpactEvidenceObserverHolder.install(failing);
        try (scope; var writer = ImpactWriterContext.enter(READER)) {
            assertThat(gateway(false, ignored -> response, List.of(ADAPTER)).send(new InspectParcel(41)))
                    .isSameAs(response);
        }
        assertThat(scope.drainFailures()).isEmpty();
        assertThat(scope.drainReadFailures()).extracting(ImpactEvidence.CoverageGap::reason)
                .containsExactly("READ_OBSERVER_CALLBACK_FAILED");
        RecordingObserver observer = new RecordingObserver();
        ParcelAdapter broken = new ParcelAdapter() {
            @Override public Long version(Parcel result) { throw new IllegalArgumentException("bad adapter"); }
        };
        var adapterScope = ImpactEvidenceObserverHolder.install(observer);
        try (adapterScope; var writer = ImpactWriterContext.enter(READER)) {
            assertThat(gateway(false, ignored -> response, List.of(broken)).send(new InspectParcel(41)))
                    .isSameAs(response);
        }
        assertThat(observer.reads.getFirst().reason()).isEqualTo("READ_ADAPTER_FAILED");
        assertThat(adapterScope.drainReadFailures()).extracting(ImpactEvidence.CoverageGap::reason)
                .containsExactly("READ_ADAPTER_FAILED");
        assertThat(adapterScope.drainFailures()).isEmpty();
    }

    @Test
    void readEnablementFailureCannotChangeApplicationResultOrPolluteImpactV2() {
        RecordingObserver failing = new RecordingObserver() {
            @Override public boolean isReadObservationEnabled() { throw new IllegalStateException("enabled failed"); }
        };
        var scope = ImpactEvidenceObserverHolder.install(failing);
        try (scope; var writer = ImpactWriterContext.enter(READER)) {
            assertThat(gateway(false, ignored -> "result", List.of(ADAPTER)).send(new InspectParcel(41))).isEqualTo("result");
        }
        assertThat(scope.drainFailures()).isEmpty();
        assertThat(scope.drainReadFailures()).extracting(ImpactEvidence.CoverageGap::reason)
                .containsExactly("READ_OBSERVER_ENABLEMENT_FAILED");
    }

    @Test
    void observerCallbacksCannotRecursivelyCreateApplicationReadFacts() {
        AtomicInteger handlerCalls = new AtomicInteger();
        LocalCommandGateway gateway = gateway(false, ignored -> {
            handlerCalls.incrementAndGet();
            return new Parcel(41, 1L);
        }, List.of(ADAPTER));
        RecordingObserver observer = new RecordingObserver() {
            @Override public void readResponse(ReadResponseEvidence.Observation observation) {
                super.readResponse(observation);
                gateway.send(new InspectParcel(41));
            }
        };
        try (var scope = ImpactEvidenceObserverHolder.install(observer);
             var writer = ImpactWriterContext.enter(READER)) {
            gateway.send(new InspectParcel(41));
        }
        assertThat(handlerCalls).hasValue(2);
        assertThat(observer.reads).hasSize(1);
        assertThat(ReadObservationContext.excludedRole()).isNull();
    }

    @Test
    void everyExistingImpactObserverCallbackExcludesItsOwnGatewayReadsFromApplicationFacts() {
        AtomicInteger handlerCalls = new AtomicInteger();
        LocalCommandGateway gateway = gateway(false, ignored -> {
            handlerCalls.incrementAndGet();
            return new Parcel(41, 1L);
        }, List.of(ADAPTER));
        RecordingObserver observer = new RecordingObserver() {
            @Override public boolean isEnabled() { gateway.send(new InspectParcel(41)); return true; }
            @Override public void committedWrite(ImpactEvidence.AggregateSnapshot aggregate, ImpactEvidence.Writer writer) {
                gateway.send(new InspectParcel(41));
            }
            @Override public void eventDelivery(ImpactEvidence.EventDelivery delivery) {
                gateway.send(new InspectParcel(41));
            }
            @Override public void coverageGap(ImpactEvidence.CoverageGap gap) {
                gateway.send(new InspectParcel(41));
            }
        };
        try (var scope = ImpactEvidenceObserverHolder.install(observer);
             var writer = ImpactWriterContext.enter(READER)) {
            assertThat(ImpactEvidenceObserverHolder.isEnabled()).isTrue();
            ImpactEvidenceObserverHolder.committedWrite(null, READER);
            ImpactEvidenceObserverHolder.eventDelivery(null);
            ImpactEvidenceObserverHolder.coverageGap(new ImpactEvidence.CoverageGap("test", "subject", "reason", "message"));
            assertThat(scope.drainFailures()).isEmpty();
            assertThat(scope.drainReadFailures()).isEmpty();
        }
        assertThat(handlerCalls).hasValue(7); // enablement once, then enablement + callback for each fact
        assertThat(observer.reads).isEmpty();
        assertThat(ReadObservationContext.excludedRole()).isNull();
    }

    @Test
    void disabledByDefaultAndReadEnablementDoesNotEnableWriteObservation() {
        AtomicInteger enablementCalls = new AtomicInteger();
        RecordingObserver observer = new RecordingObserver() {
            @Override public boolean isEnabled() { return false; }
            @Override public boolean isReadObservationEnabled() { enablementCalls.incrementAndGet(); return true; }
        };
        LocalCommandGateway gateway = gateway(false, ignored -> new Parcel(41, 1L), List.of(ADAPTER));
        ReflectionTestUtils.setField(gateway, "sagaReadExposureEnabled", false);
        try (var scope = ImpactEvidenceObserverHolder.install(observer);
             var writer = ImpactWriterContext.enter(READER)) {
            assertThat(ImpactEvidenceObserverHolder.isEnabled()).isFalse();
            gateway.send(new InspectParcel(41));
            assertThat(observer.reads).isEmpty();
            assertThat(enablementCalls).hasValue(0);
            ReflectionTestUtils.setField(gateway, "sagaReadExposureEnabled", true);
            gateway.send(new InspectParcel(41));
            assertThat(observer.reads).hasSize(1);
            assertThat(ImpactEvidenceObserverHolder.isEnabled()).isFalse();
        }
    }

    private static ImpactEvidence.Writer writer(String kind, String phase, String action) {
        return new ImpactEvidence.Writer(kind, "attempt-1", "workload-1", "B", action, phase,
                "FindParcel", "inspect", null);
    }

    private static LocalCommandGateway gateway(boolean serialized, Function<Command, Object> handler,
                                                List<ReadResponseAdapter<?, ?>> adapters) {
        return gateway(serialized, new TestHandler(handler), adapters);
    }

    private static LocalCommandGateway gateway(boolean serialized, CommandHandler handler,
                                                List<ReadResponseAdapter<?, ?>> adapters) {
        ApplicationContext context = mock(ApplicationContext.class);
        when(context.getBean("parcelCommandHandler")).thenReturn(handler);
        LocalCommandService service = new LocalCommandService(context, new MessagingObjectMapperProvider(new ObjectMapper()));
        return configured(serialized, context, service, adapters);
    }

    private static LocalCommandGateway configured(boolean serialized, ApplicationContext context,
                                                   LocalCommandService service, List<ReadResponseAdapter<?, ?>> adapters) {
        LocalCommandGateway gateway = new LocalCommandGateway(context, RetryRegistry.ofDefaults(), service,
                new MessagingObjectMapperProvider(new ObjectMapper()));
        ReflectionTestUtils.setField(gateway, "serializeMessages", serialized);
        ReflectionTestUtils.setField(gateway, "sagaReadExposureEnabled", true);
        ReflectionTestUtils.setField(gateway, "readResponseAdapters", adapters);
        return gateway;
    }

    private static class RecordingObserver implements ImpactEvidenceObserver {
        private final List<ReadResponseEvidence.Observation> reads = new ArrayList<>();
        @Override public boolean isReadObservationEnabled() { return true; }
        @Override public void readResponse(ReadResponseEvidence.Observation observation) { reads.add(observation); }
        @Override public void committedWrite(ImpactEvidence.AggregateSnapshot aggregate, ImpactEvidence.Writer writer) { }
        @Override public void eventDelivery(ImpactEvidence.EventDelivery delivery) { }
        @Override public void coverageGap(ImpactEvidence.CoverageGap gap) { }
    }

    private static final class TestHandler extends CommandHandler {
        private final Function<Command, Object> operation;
        private TestHandler(Function<Command, Object> operation) { this.operation = operation; }
        @Override public String getAggregateTypeName() { return "Parcel"; }
        @Override public Object handleDomainCommand(Command command) { return operation.apply(command); }
    }

    public static class InspectParcel extends Command {
        public InspectParcel() { }
        InspectParcel(Integer requested) { super(new TestUnitOfWork(999L, "FindParcel"), "parcel", requested); }
    }
    public static class DerivedInspectParcel extends InspectParcel {
        DerivedInspectParcel(Integer requested) { super(requested); }
    }
    public static class DerivedSagaCommand extends SagaCommand {
        DerivedSagaCommand(Command command) { super(command); }
    }
    public static class TestUnitOfWork extends UnitOfWork {
        public TestUnitOfWork() { }
        TestUnitOfWork(Long version, String name) { super(version, name); }
    }

    /** Deliberately lacks conventional ID/version fields; nested revision is unrelated. */
    public static class Parcel {
        private Integer referenceToken;
        private Long revisionMarker;
        private Parcel nested;
        public Parcel() { }
        Parcel(Integer referenceToken, Long revisionMarker) {
            this.referenceToken = referenceToken;
            this.revisionMarker = revisionMarker;
        }
        public Integer getReferenceToken() { return referenceToken; }
        public void setReferenceToken(Integer value) { referenceToken = value; }
        public Long getRevisionMarker() { return revisionMarker; }
        public void setRevisionMarker(Long value) { revisionMarker = value; }
        public Parcel getNested() { return nested; }
        public void setNested(Parcel value) { nested = value; }
        @JsonIgnore public Integer getAggregateId() { throw new AssertionError("Do not infer getters"); }
        @JsonIgnore public Long getVersion() { throw new AssertionError("Do not infer getters"); }
    }

    /** A deterministic deserialization conversion proves the hook uses the consumer's result. */
    public static class ConvertedParcel extends Parcel {
        public ConvertedParcel() { }
        ConvertedParcel(Integer referenceToken, Long revisionMarker) { super(referenceToken, revisionMarker); }
        @Override public void setRevisionMarker(Long value) { super.setRevisionMarker(value - 1); }
    }

    private static class Adapter<R> implements ReadResponseAdapter<InspectParcel, R> {
        private final Class<R> resultType;
        private final Function<R, Integer> identity;
        private final Function<R, Long> revision;
        Adapter(Class<R> resultType, Function<R, Integer> identity, Function<R, Long> revision) {
            this.resultType = resultType;
            this.identity = identity;
            this.revision = revision;
        }
        @Override public Class<InspectParcel> commandType() { return InspectParcel.class; }
        @Override public Class<R> responseType() { return resultType; }
        @Override public String contractId() { return "fixture.parcel.outer"; }
        @Override public String contractVersion() { return "1"; }
        @Override public String aggregateType() { return "Parcel"; }
        @Override public String runtimeType() { return "fixture.PersistentParcel"; }
        @Override public Integer aggregateId(R response) { return identity.apply(response); }
        @Override public Long version(R response) { return revision.apply(response); }
    }

    private static class ParcelAdapter extends Adapter<Parcel> {
        ParcelAdapter() { super(Parcel.class, Parcel::getReferenceToken, Parcel::getRevisionMarker); }
    }
}
