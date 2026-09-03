package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario;

import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputRecipeNode;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

/** The four relative date forms currently used by the Quizzes fixtures. */
public final class DateExpressionSupport {
    private static final Set<String> ALLOWED_OFFSETS = Set.of("PT5M", "PT25M", "PT1H5M", "PT1H25M");

    private DateExpressionSupport() {
    }

    public static Optional<String> normalize(InputRecipeNode node) {
        if (node != null && "relative_date_time".equals(node.kind())
                && "now".equals(node.anchor()) && ALLOWED_OFFSETS.contains(node.offset())) {
            return Optional.of(node.offset());
        }
        return parseOffset(node).filter(ALLOWED_OFFSETS::contains);
    }

    public static InputRecipeNode compactNode(InputRecipeNode source, String offset) {
        return InputRecipeNode.builder("relative_date_time")
                .sourceText(source == null ? null : source.sourceText())
                .provenanceText(source == null ? null : source.provenanceText())
                .executorReady(true)
                .expectedTypeFqn("java.time.LocalDateTime")
                .anchor("now")
                .offset(offset)
                .build();
    }

    public static boolean isSupported(InputRecipeNode node) {
        return normalize(node).isPresent();
    }

    public static boolean isAllowedRelativeDateTime(String anchor, String offset) {
        return "now".equals(anchor) && ALLOWED_OFFSETS.contains(offset);
    }

    public static Optional<LocalDateTime> materialize(InputRecipeNode node) {
        return normalize(node).map(offset -> DateHandler.now().plus(Duration.parse(offset)));
    }

    private static Optional<String> parseOffset(InputRecipeNode node) {
        if (node == null || !node.executorReady()) {
            return Optional.empty();
        }
        if ("call_result".equals(node.kind())) {
            if ("now".equals(node.methodName()) && node.callArguments().isEmpty()
                    && isDateHandler(node.receiverReference())) {
                return Optional.of("PT0S");
            }
            if (node.callArguments().size() != 1 || node.callArguments().get(0).recipe() == null) {
                return Optional.empty();
            }
            Object amount = node.callArguments().get(0).recipe().value();
            if (!(amount instanceof Number number) || !isIntegral(number)) {
                return Optional.empty();
            }
            Optional<String> receiver = parseOffset(node.receiver());
            if (receiver.isEmpty()) {
                return Optional.empty();
            }
            if ("plusHours".equals(node.methodName()) && "PT0S".equals(receiver.get())
                    && number.longValue() == 1L) {
                return Optional.of("PT1H");
            }
            if ("plusMinutes".equals(node.methodName())
                    && ("PT0S".equals(receiver.get()) || "PT1H".equals(receiver.get()))
                    && (number.longValue() == 5L || number.longValue() == 25L)) {
                return Optional.of("PT" + ("PT1H".equals(receiver.get()) ? "1H" : "")
                        + number.longValue() + "M");
            }
        }
        return Optional.empty();
    }

    private static boolean isIntegral(Number number) {
        return Double.isFinite(number.doubleValue())
                && number.doubleValue() == number.longValue();
    }

    private static boolean isDateHandler(String receiverReference) {
        return "DateHandler".equals(receiverReference)
                || "pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler".equals(receiverReference);
    }
}
