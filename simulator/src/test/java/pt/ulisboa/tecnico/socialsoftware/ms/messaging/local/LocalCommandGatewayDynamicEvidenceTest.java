package pt.ulisboa.tecnico.socialsoftware.ms.messaging.local;

import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.MessagingObjectMapperProvider;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic.DynamicEvidenceContext;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic.DynamicEvidenceEvent;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic.DynamicEvidenceNoopRecorder;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic.DynamicEvidenceRecorder;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic.DynamicEvidenceRecorderHolder;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.unitOfWork.UnitOfWork;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LocalCommandGatewayDynamicEvidenceTest {

    @AfterEach
    void resetDynamicEvidenceState() {
        DynamicEvidenceRecorderHolder.setRecorder(new DynamicEvidenceNoopRecorder());
        DynamicEvidenceContext.clear();
        Thread.interrupted();
    }

    @Test
    void recordsCommandSentBeforeDispatchAndIncludesCurrentStepContext() {
        RecordingRecorder recorder = new RecordingRecorder();
        DynamicEvidenceRecorderHolder.setRecorder(recorder);

        ApplicationContext applicationContext = mock(ApplicationContext.class);
        LocalCommandService localCommandService = mock(LocalCommandService.class);
        when(localCommandService.send(any())).thenAnswer(invocation -> {
            assertThat(recorder.events).hasSize(1);
            DynamicEvidenceEvent event = recorder.events.getFirst();
            assertThat(event.getEventKind()).isEqualTo("COMMAND_SENT");
            assertThat(event.getFunctionalityName()).isEqualTo("checkout");
            assertThat(event.getStepName()).isEqualTo("reserveStock");
            assertThat(event.getUnitOfWorkVersion()).isEqualTo(88L);
            assertThat(event.getPayload()).containsEntry("commandType", DispatchCommand.class.getSimpleName());
            assertThat(event.getPayload()).containsEntry("serviceName", "inventory");
            assertThat(event.getPayload()).containsEntry("rootAggregateId", "55");
            return "dispatched";
        });

        LocalCommandGateway gateway = new LocalCommandGateway(
                applicationContext,
                RetryRegistry.ofDefaults(),
                localCommandService,
                new MessagingObjectMapperProvider(new com.fasterxml.jackson.databind.ObjectMapper()));

        DispatchCommand command = new DispatchCommand(new TestUnitOfWork(88L, "checkout"), "inventory", 55, "reserve-55");

        try (DynamicEvidenceContext.Scope scope = DynamicEvidenceContext.enterStep("checkout", "reserveStock", 88L)) {
            Object result = gateway.send(command);
            assertThat(result).isEqualTo("dispatched");
        }

        verify(localCommandService).send(command);
        assertThat(recorder.events).extracting(DynamicEvidenceEvent::getEventKind).containsExactly("COMMAND_SENT");
        Map<String, Object> fields = map(recorder.events.getFirst().getPayload().get("fields"));
        assertThat(fields).containsEntry("label", "reserve-55");
    }

    @Test
    void disabledRecorderSkipsCommandFieldExtractionAndStillDispatches() {
        DynamicEvidenceRecorderHolder.setRecorder(new DynamicEvidenceNoopRecorder());

        ApplicationContext applicationContext = mock(ApplicationContext.class);
        LocalCommandService localCommandService = mock(LocalCommandService.class);
        when(localCommandService.send(any())).thenReturn("dispatched");

        LocalCommandGateway gateway = new LocalCommandGateway(
                applicationContext,
                RetryRegistry.ofDefaults(),
                localCommandService,
                new MessagingObjectMapperProvider(new com.fasterxml.jackson.databind.ObjectMapper()));

        AtomicBoolean expensiveGetterInvoked = new AtomicBoolean(false);
        GuardedCommand command = new GuardedCommand(
                new TestUnitOfWork(89L, "checkout"),
                "inventory",
                56,
                expensiveGetterInvoked);

        Object result = gateway.send(command);

        assertThat(result).isEqualTo("dispatched");
        assertThat(expensiveGetterInvoked).isFalse();
        verify(localCommandService).send(command);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        assertThat(value).isInstanceOf(Map.class);
        return (Map<String, Object>) value;
    }

    private static final class RecordingRecorder implements DynamicEvidenceRecorder {
        private final List<DynamicEvidenceEvent> events = new CopyOnWriteArrayList<>();

        @Override
        public void record(DynamicEvidenceEvent event) {
            events.add(event);
        }

        @Override
        public void close() {
        }
    }

    private static final class TestUnitOfWork extends UnitOfWork {
        private TestUnitOfWork(Long version, String functionalityName) {
            super(version, functionalityName);
        }
    }

    private static final class DispatchCommand extends Command {
        private final String label;

        private DispatchCommand(UnitOfWork unitOfWork, String serviceName, Integer rootAggregateId, String label) {
            super(unitOfWork, serviceName, rootAggregateId);
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private static final class GuardedCommand extends Command {
        private final AtomicBoolean expensiveGetterInvoked;

        private GuardedCommand(UnitOfWork unitOfWork, String serviceName, Integer rootAggregateId,
                               AtomicBoolean expensiveGetterInvoked) {
            super(unitOfWork, serviceName, rootAggregateId);
            this.expensiveGetterInvoked = expensiveGetterInvoked;
        }

        public String getExpensiveValue() {
            expensiveGetterInvoked.set(true);
            throw new IllegalStateException("expensiveValue getter should not be invoked when dynamic evidence is disabled");
        }
    }
}
