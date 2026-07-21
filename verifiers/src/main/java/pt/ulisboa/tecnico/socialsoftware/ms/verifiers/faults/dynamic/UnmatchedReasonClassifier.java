package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic;

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model.DynamicEvidenceEvent;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model.UnmatchedReason;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FixtureOrigin;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputVariant;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.WorkloadPlan;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class UnmatchedReasonClassifier {
    private static final Set<String> FAILED_STATUSES = Set.of("FAILED", "TIMED_OUT", "NO_REPORT");

    public UnmatchedReason classify(WorkloadPlan workload,
                                    List<DynamicEvidenceEvent> relevantEvents,
                                    Set<String> selectedTestClassFqns,
                                    Map<String, String> testRunStatusByClassFqn) {
        if (workload == null) {
            return UnmatchedReason.UNCLASSIFIED;
        }
        if (hasFailedSourceClass(workload, testRunStatusByClassFqn)) {
            return UnmatchedReason.FAILED_TEST_CLASS;
        }
        if (hasNoSelectedSourceClass(workload, selectedTestClassFqns)) {
            return UnmatchedReason.NOT_SELECTED_TEST_CLASS;
        }
        if (hasHelperOwnerMismatch(workload, relevantEvents)) {
            return UnmatchedReason.HELPER_OWNER_MISMATCH;
        }
        return UnmatchedReason.UNCLASSIFIED;
    }

    private boolean hasFailedSourceClass(WorkloadPlan workload, Map<String, String> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return false;
        }
        return workload.acceptedInputs().stream()
                .map(InputVariant::sourceClassFqn)
                .filter(Objects::nonNull)
                .distinct()
                .map(statuses::get)
                .filter(Objects::nonNull)
                .anyMatch(FAILED_STATUSES::contains);
    }

    private boolean hasNoSelectedSourceClass(WorkloadPlan workload, Set<String> selectedTestClassFqns) {
        if (selectedTestClassFqns == null || selectedTestClassFqns.isEmpty()) {
            return false;
        }
        return workload.acceptedInputs().stream()
                .map(InputVariant::sourceClassFqn)
                .filter(Objects::nonNull)
                .noneMatch(selectedTestClassFqns::contains);
    }

    private boolean hasHelperOwnerMismatch(WorkloadPlan workload, List<DynamicEvidenceEvent> relevantEvents) {
        if (relevantEvents == null || relevantEvents.isEmpty()) {
            return false;
        }
        for (InputVariant input : workload.acceptedInputs()) {
            if (!isHelperFixture(input)) {
                continue;
            }
            for (DynamicEvidenceEvent event : relevantEvents) {
                if (event == null || event.testClassFqn() == null || event.testClassFqn().isBlank()) {
                    continue;
                }
                if (!Objects.equals(input.sourceClassFqn(), event.testClassFqn())) {
                    continue;
                }
                String runtimeMethod = event.testMethodName() == null || event.testMethodName().isBlank()
                        ? event.testDisplayName()
                        : event.testMethodName();
                if (runtimeMethod != null && !runtimeMethod.isBlank() && !Objects.equals(input.sourceMethodName(), runtimeMethod)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isHelperFixture(InputVariant input) {
        if (input == null) {
            return false;
        }
        if (input.fixtureOrigin() == FixtureOrigin.SETUP_HELPER || input.fixtureOrigin() == FixtureOrigin.INHERITED_HELPER) {
            return true;
        }
        String sourceMethodName = input.sourceMethodName();
        return input.callContextMethodName() != null
                && !Objects.equals(sourceMethodName, input.callContextMethodName())
                && ("setup".equals(input.callContextMethodName()) || "setupSpec".equals(input.callContextMethodName()));
    }
}
