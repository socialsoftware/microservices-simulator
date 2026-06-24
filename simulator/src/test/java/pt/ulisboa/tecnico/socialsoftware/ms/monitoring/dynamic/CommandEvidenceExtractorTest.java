package pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic;

import org.junit.jupiter.api.Test;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CommandEvidenceExtractorTest {

    @Test
    void capturesCommandMetadataSimpleFieldsNestedDtoIdsAndSkipsSensitiveAndUnitOfWorkFields() {
        DynamicEvidenceProperties properties = new DynamicEvidenceProperties();
        properties.setApplicationName("orders");
        properties.setMaxFieldDepth(2);
        properties.setMaxFieldValueLength(32);

        CommandEvidenceExtractor extractor = new CommandEvidenceExtractor(properties);
        SampleCommand command = new SampleCommand(
                new TestUnitOfWork(19L, "checkout"),
                "execution",
                42,
                "invoice-123",
                7,
                true,
                SampleStatus.ACTIVE,
                new SampleNestedDto(99, "student-1", "should-not-appear"),
                List.of("alpha", "beta"),
                Map.of("channel", "web"),
                "simple-description",
                "top-secret-token");

        DynamicEvidenceEvent event = extractor.buildCommandSentEvent(command, null);

        assertThat(event.getEventKind()).isEqualTo("COMMAND_SENT");
        assertThat(event.getFunctionalityName()).isEqualTo("checkout");
        assertThat(event.getFunctionalityInvocationId()).isEqualTo("checkout-19");
        assertThat(event.getStepName()).isNull();
        assertThat(event.getUnitOfWorkVersion()).isEqualTo(19L);

        Map<String, Object> payload = event.getPayload();
        assertThat(payload).containsEntry("commandType", "SampleCommand");
        assertThat(payload).containsEntry("commandFqn", SampleCommand.class.getName());
        assertThat(payload).containsEntry("serviceName", "execution");
        assertThat(payload).containsEntry("rootAggregateId", "42");

        Map<String, Object> fields = map(payload.get("fields"));
        assertThat(fields).containsEntry("title", "invoice-123");
        assertThat(fields).containsEntry("attempts", 7);
        assertThat(fields).containsEntry("active", true);
        assertThat(fields).containsEntry("status", "ACTIVE");
        assertThat(fields).containsEntry("description", "simple-description");
        assertThat(fields).containsEntry("tags", List.of("alpha", "beta"));
        assertThat(fields).containsEntry("metadata", Map.of("channel", "web"));
        assertThat(fields).doesNotContainKeys("unitOfWork", "password", "apiToken", "secretNote");

        Map<String, Object> nested = map(fields.get("studentDto"));
        assertThat(nested).containsEntry("aggregateId", "99");
        assertThat(nested).containsEntry("name", "student-1");
        assertThat(nested).doesNotContainKeys("secretToken");
    }

    @Test
    void truncatesLongValuesAndStopsAtMaxDepth() {
        DynamicEvidenceProperties properties = new DynamicEvidenceProperties();
        properties.setMaxFieldDepth(1);
        properties.setMaxFieldValueLength(8);

        CommandEvidenceExtractor extractor = new CommandEvidenceExtractor(properties);
        DepthCommand command = new DepthCommand(
                null,
                "execution",
                7,
                new DepthDto(21, "middle-layer", new DepthLeafDto(22, "very-deep-value")),
                "0123456789",
                List.of("1234567890", "short"));

        DynamicEvidenceEvent event = extractor.buildCommandSentEvent(command, null);
        Map<String, Object> fields = map(event.getPayload().get("fields"));

        assertThat(fields).containsEntry("description", "01234567");
        assertThat(fields.get("tags")).isEqualTo(List.of("12345678", "short"));

        Map<String, Object> outer = map(fields.get("outerDto"));
        assertThat(outer).containsEntry("aggregateId", "21");
        assertThat(outer.get("innerDto")).isInstanceOf(String.class);
        assertThat(outer.get("innerDto")).asString().isEqualTo("DepthLea");
    }

    @Test
    void usesCurrentStepFunctionalityClassIdentityWhenContextIsPresent() {
        DynamicEvidenceProperties properties = new DynamicEvidenceProperties();
        CommandEvidenceExtractor extractor = new CommandEvidenceExtractor(properties);
        SampleCommand command = new SampleCommand(
                new TestUnitOfWork(19L, "fallback"),
                "execution",
                42,
                "invoice-123",
                7,
                true,
                SampleStatus.ACTIVE,
                new SampleNestedDto(99, "student-1", "should-not-appear"),
                List.of(),
                Map.of(),
                "simple-description",
                "top-secret-token");
        DynamicEvidenceContext.StepContext context = new DynamicEvidenceContext.StepContext(
                "checkout",
                "example.CheckoutSaga",
                "CheckoutSaga",
                "checkout-88",
                "reserve",
                88L,
                System.currentTimeMillis(),
                System.nanoTime());

        DynamicEvidenceEvent event = extractor.buildCommandSentEvent(command, context);

        assertThat(event.getFunctionalityName()).isEqualTo("checkout");
        assertThat(event.getFunctionalityClassFqn()).isEqualTo("example.CheckoutSaga");
        assertThat(event.getFunctionalityClassSimpleName()).isEqualTo("CheckoutSaga");
        assertThat(event.getFunctionalityInvocationId()).isEqualTo("checkout-88");
        assertThat(event.getStepName()).isEqualTo("reserve");
        assertThat(event.getUnitOfWorkVersion()).isEqualTo(88L);
    }

    @Test
    void usesCurrentStepInputVariantIdWhenContextIsPresent() {
        DynamicEvidenceProperties properties = new DynamicEvidenceProperties();
        CommandEvidenceExtractor extractor = new CommandEvidenceExtractor(properties);
        SampleCommand command = new SampleCommand(
                new TestUnitOfWork(19L, "fallback"),
                "execution",
                42,
                "invoice-123",
                7,
                true,
                SampleStatus.ACTIVE,
                new SampleNestedDto(99, "student-1", "should-not-appear"),
                List.of(),
                Map.of(),
                "simple-description",
                "top-secret-token");
        DynamicEvidenceContext.StepContext context = new DynamicEvidenceContext.StepContext(
                "checkout",
                "example.CheckoutSaga",
                "CheckoutSaga",
                "input-1",
                "MATCHED",
                "TEST_FUNCTIONALITY_CLASS_STEP",
                List.of("input-1"),
                "checkout-88",
                "reserve",
                88L,
                System.currentTimeMillis(),
                System.nanoTime());

        DynamicEvidenceEvent event = extractor.buildCommandSentEvent(command, context);

        assertThat(event.getInputVariantId()).isEqualTo("input-1");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        assertThat(value).isInstanceOf(Map.class);
        return (Map<String, Object>) value;
    }

    private enum SampleStatus {
        ACTIVE,
        INACTIVE
    }

    private static final class TestUnitOfWork extends UnitOfWork {
        TestUnitOfWork(Long version, String functionalityName) {
            super(version, functionalityName);
        }
    }

    private static final class SampleCommand extends Command {
        private final String title;
        private final int attempts;
        private final boolean active;
        private final SampleStatus status;
        private final SampleNestedDto studentDto;
        private final List<String> tags;
        private final Map<String, Object> metadata;
        private final String description;
        private final String password;
        private final String apiToken;
        private final String secretNote;

        private SampleCommand(UnitOfWork unitOfWork, String serviceName, Integer rootAggregateId, String title,
                              int attempts, boolean active, SampleStatus status, SampleNestedDto studentDto,
                              List<String> tags, Map<String, Object> metadata, String description, String password) {
            super(unitOfWork, serviceName, rootAggregateId);
            this.title = title;
            this.attempts = attempts;
            this.active = active;
            this.status = status;
            this.studentDto = studentDto;
            this.tags = tags;
            this.metadata = metadata;
            this.description = description;
            this.password = password;
            this.apiToken = "token-123";
            this.secretNote = "hidden";
        }

        public String getTitle() {
            return title;
        }

        public int getAttempts() {
            return attempts;
        }

        public boolean isActive() {
            return active;
        }

        public SampleStatus getStatus() {
            return status;
        }

        public SampleNestedDto getStudentDto() {
            return studentDto;
        }

        public List<String> getTags() {
            return tags;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public String getDescription() {
            return description;
        }

        public String getPassword() {
            return password;
        }

        public String getApiToken() {
            return apiToken;
        }

        public String getSecretNote() {
            return secretNote;
        }
    }

    private static final class SampleNestedDto {
        private final Integer aggregateId;
        private final String name;
        private final String secretToken;

        private SampleNestedDto(Integer aggregateId, String name, String secretToken) {
            this.aggregateId = aggregateId;
            this.name = name;
            this.secretToken = secretToken;
        }

        public Integer getAggregateId() {
            return aggregateId;
        }

        public String getName() {
            return name;
        }

        public String getSecretToken() {
            return secretToken;
        }
    }

    private static final class DepthCommand extends Command {
        private final DepthDto outerDto;
        private final String description;
        private final List<String> tags;

        private DepthCommand(UnitOfWork unitOfWork, String serviceName, Integer rootAggregateId, DepthDto outerDto,
                             String description, List<String> tags) {
            super(unitOfWork, serviceName, rootAggregateId);
            this.outerDto = outerDto;
            this.description = description;
            this.tags = tags;
        }

        public DepthDto getOuterDto() {
            return outerDto;
        }

        public String getDescription() {
            return description;
        }

        public List<String> getTags() {
            return tags;
        }
    }

    private static final class DepthDto {
        private final Integer aggregateId;
        private final String label;
        private final DepthLeafDto innerDto;

        private DepthDto(Integer aggregateId, String label, DepthLeafDto innerDto) {
            this.aggregateId = aggregateId;
            this.label = label;
            this.innerDto = innerDto;
        }

        public Integer getAggregateId() {
            return aggregateId;
        }

        public String getLabel() {
            return label;
        }

        public DepthLeafDto getInnerDto() {
            return innerDto;
        }
    }

    private static final class DepthLeafDto {
        private final Integer aggregateId;
        private final String value;

        private DepthLeafDto(Integer aggregateId, String value) {
            this.aggregateId = aggregateId;
            this.value = value;
        }

        public Integer getAggregateId() {
            return aggregateId;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return "DepthLeafDto{" +
                    "aggregateId=" + aggregateId +
                    ", value='" + value + '\'' +
                    '}';
        }
    }
}
