package pt.ulisboa.tecnico.socialsoftware.ms.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.DomainFailure;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorDomainException;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;
import pt.ulisboa.tecnico.socialsoftware.ms.faults.FaultVectorFault;
import pt.ulisboa.tecnico.socialsoftware.ms.faults.FaultVectorInjectedFaultException;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.local.LocalCommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.local.LocalCommandService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CommandGatewayExceptionRestorationTest {

    @Test
    void restoresMarkedSimulatorDomainExceptionWithTemplateAndFormattedMessage() throws Exception {
        SimulatorDomainException original = new SimulatorDomainException("Aggregate %s is unavailable", "42");
        CommandResponse restoredResponse = roundTrip(CommandResponse.error("correlation", original, null));
        TestCommandGateway gateway = new TestCommandGateway();

        assertThat(restoredResponse.errorType()).isEqualTo(SimulatorDomainException.class.getName());
        assertThat(restoredResponse.errorTemplate()).isEqualTo("Aggregate %s is unavailable");
        assertThat(restoredResponse.errorMessage()).isEqualTo("Aggregate 42 is unavailable");
        assertThatThrownBy(() -> gateway.restore(restoredResponse))
                .isInstanceOfSatisfying(SimulatorDomainException.class, restored -> {
                    assertThat(restored).isInstanceOf(DomainFailure.class);
                    assertThat(restored.getErrorMessage()).isEqualTo("Aggregate %s is unavailable");
                    assertThat(restored.getMessage()).isEqualTo("Aggregate 42 is unavailable");
                });
    }

    @Test
    void restoresMarkedApplicationExceptionIdentityWithTemplateAndFormattedMessage() throws Exception {
        ApplicationDomainException original = new ApplicationDomainException("Quiz %s is invalid", "17");
        CommandResponse restoredResponse = roundTrip(CommandResponse.error("correlation", original, null));
        TestCommandGateway gateway = new TestCommandGateway();

        assertThatThrownBy(() -> gateway.restore(restoredResponse))
                .isInstanceOfSatisfying(ApplicationDomainException.class, restored -> {
                    assertThat(restored).isInstanceOf(DomainFailure.class);
                    assertThat(restored.getErrorMessage()).isEqualTo("Quiz %s is invalid");
                    assertThat(restored.getMessage()).isEqualTo("Quiz 17 is invalid");
                });
    }

    @Test
    void localSerializedCommandResponseRestoresMarkedApplicationException() throws Exception {
        ObjectMapper mapper = new MessagingObjectMapperProvider(new ObjectMapper()).newMapper();
        ApplicationDomainException original = new ApplicationDomainException("Quiz %s is invalid", "17");
        String responseJson = mapper.writeValueAsString(CommandResponse.error("correlation", original, null));
        ApplicationContext context = mock(ApplicationContext.class);
        LocalCommandService service = mock(LocalCommandService.class);
        when(service.sendJson(anyString())).thenReturn(responseJson);
        LocalCommandGateway gateway = new LocalCommandGateway(
                context, RetryRegistry.ofDefaults(), service, new MessagingObjectMapperProvider(new ObjectMapper()));
        ReflectionTestUtils.setField(gateway, "serializeMessages", true);

        assertThatThrownBy(() -> gateway.send(new Command(null, "quiz", 17)))
                .isInstanceOfSatisfying(ApplicationDomainException.class, restored -> {
                    assertThat(restored).isInstanceOf(DomainFailure.class);
                    assertThat(restored.getErrorMessage()).isEqualTo("Quiz %s is invalid");
                    assertThat(restored.getMessage()).isEqualTo("Quiz 17 is invalid");
                });
    }

    @Test
    void inheritedBaseFactoryIsIgnoredForRequestedSubtype() throws Exception {
        InheritedFactoryException original = new InheritedFactoryException("unmarked subtype failure");
        CommandResponse restoredResponse = roundTrip(CommandResponse.error("correlation", original, null));
        TestCommandGateway gateway = new TestCommandGateway();

        assertThatThrownBy(() -> gateway.restore(restoredResponse))
                .isExactlyInstanceOf(InheritedFactoryException.class)
                .isNotInstanceOf(DomainFailure.class)
                .hasMessage("unmarked subtype failure");
    }

    @Test
    void localSerializedCommandResponseRestoresExactUnmarkedAssignedFaultSubtype() throws Exception {
        ObjectMapper mapper = new MessagingObjectMapperProvider(new ObjectMapper()).newMapper();
        FaultVectorInjectedFaultException original = new FaultVectorInjectedFaultException(new FaultVectorFault(
                "execution-1", "scenario-1", "participant-1", "scheduled-step-1", 3,
                "example.Workflow", "Workflow", "reserve", 1));
        String responseJson = mapper.writeValueAsString(CommandResponse.error("correlation", original, null));
        ApplicationContext context = mock(ApplicationContext.class);
        LocalCommandService service = mock(LocalCommandService.class);
        when(service.sendJson(anyString())).thenReturn(responseJson);
        LocalCommandGateway gateway = new LocalCommandGateway(
                context, RetryRegistry.ofDefaults(), service, new MessagingObjectMapperProvider(new ObjectMapper()));
        ReflectionTestUtils.setField(gateway, "serializeMessages", true);

        assertThatThrownBy(() -> gateway.send(new Command(null, "quiz", 17)))
                .isExactlyInstanceOf(FaultVectorInjectedFaultException.class)
                .isNotInstanceOf(DomainFailure.class)
                .isInstanceOfSatisfying(FaultVectorInjectedFaultException.class, restored -> {
                    assertThat(restored.getErrorMessage()).isEqualTo(original.getErrorMessage());
                    assertThat(restored.getMessage()).isEqualTo(original.getMessage());
                });
    }

    @Test
    void retryExhaustionRemainsAnUnmarkedSimulatorException() {
        TestCommandGateway gateway = new TestCommandGateway();
        Command command = new Command(null, "quiz", 17);

        assertThatThrownBy(() -> gateway.fallbackSend(command, new IllegalStateException("connection refused")))
                .isExactlyInstanceOf(SimulatorException.class)
                .isNotInstanceOf(DomainFailure.class)
                .hasMessageContaining("Service 'quiz' unavailable after retries exhausted");
    }

    private static CommandResponse roundTrip(CommandResponse response) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(mapper.writeValueAsBytes(response), CommandResponse.class);
    }

    public static class InheritedFactoryException extends SimulatorException {
        public InheritedFactoryException(String message) {
            super(message);
        }
    }

    public static class ApplicationDomainException extends SimulatorException implements DomainFailure {
        private ApplicationDomainException(String template, String formattedMessage, boolean alreadyFormatted) {
            super(template, formattedMessage, alreadyFormatted);
        }

        public ApplicationDomainException(String template, String value) {
            super(template, value);
        }

        public static ApplicationDomainException fromRemote(String template, String formattedMessage) {
            return new ApplicationDomainException(template, formattedMessage, true);
        }
    }

    private static final class TestCommandGateway extends CommandGateway {
        private TestCommandGateway() {
            super(mock(ApplicationContext.class));
        }

        @Override
        public Object send(Command command) {
            throw new UnsupportedOperationException();
        }

        private void restore(CommandResponse response) {
            throwMatchingException(response.errorType(), response.errorMessage(), response.errorTemplate());
        }
    }
}
