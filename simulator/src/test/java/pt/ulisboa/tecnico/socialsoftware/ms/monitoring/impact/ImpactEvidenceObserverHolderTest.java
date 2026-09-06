package pt.ulisboa.tecnico.socialsoftware.ms.monitoring.impact;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ImpactEvidenceObserverHolderTest {
    @Test
    void sequentialAttemptScopesDoNotShareFacts() {
        RecordingObserver first = new RecordingObserver();
        RecordingObserver second = new RecordingObserver();
        ImpactEvidence.CoverageGap firstGap = new ImpactEvidence.CoverageGap(
                "ATTEMPT", "one", "TEST", "first");
        ImpactEvidence.CoverageGap secondGap = new ImpactEvidence.CoverageGap(
                "ATTEMPT", "two", "TEST", "second");

        try (ImpactEvidenceObserverHolder.Scope ignored = ImpactEvidenceObserverHolder.install(first)) {
            ImpactEvidenceObserverHolder.coverageGap(firstGap);
        }
        assertThat(ImpactEvidenceObserverHolder.isEnabled()).isFalse();
        try (ImpactEvidenceObserverHolder.Scope ignored = ImpactEvidenceObserverHolder.install(second)) {
            ImpactEvidenceObserverHolder.coverageGap(secondGap);
        }

        assertThat(first.gaps).containsExactly(firstGap);
        assertThat(second.gaps).containsExactly(secondGap);
        assertThat(ImpactEvidenceObserverHolder.isEnabled()).isFalse();
    }

    @Test
    void callbackAndEnablementFailuresAreRetainedOutsideTheFailingObserver() {
        ImpactEvidenceObserver failing = new ImpactEvidenceObserver() {
            private int enablementChecks;
            @Override public boolean isEnabled() {
                if (++enablementChecks == 2) throw new IllegalStateException("enablement failed");
                return true;
            }
            @Override public void committedWrite(ImpactEvidence.AggregateSnapshot aggregate,
                                                  ImpactEvidence.Writer writer) {
                throw new IllegalArgumentException("callback failed");
            }
            @Override public void eventDelivery(ImpactEvidence.EventDelivery delivery) { }
            @Override public void coverageGap(ImpactEvidence.CoverageGap gap) {
                throw new IllegalArgumentException("gap callback failed");
            }
        };

        ImpactEvidenceObserverHolder.Scope scope = ImpactEvidenceObserverHolder.install(failing);
        try (scope) {
            ImpactEvidenceObserverHolder.committedWrite(null, null);
            assertThat(ImpactEvidenceObserverHolder.isEnabled()).isFalse();
            ImpactEvidenceObserverHolder.coverageGap(new ImpactEvidence.CoverageGap(
                    "TEST", "subject", "SOURCE_GAP", "source"));
        }

        assertThat(scope.drainFailures()).extracting(ImpactEvidence.CoverageGap::reason)
                .containsExactly("OBSERVER_CALLBACK_FAILED", "OBSERVER_ENABLEMENT_FAILED",
                        "OBSERVER_CALLBACK_FAILED");
    }

    private static final class RecordingObserver implements ImpactEvidenceObserver {
        private final List<ImpactEvidence.CoverageGap> gaps = new ArrayList<>();
        @Override public void committedWrite(ImpactEvidence.AggregateSnapshot aggregate, ImpactEvidence.Writer writer) { }
        @Override public void eventDelivery(ImpactEvidence.EventDelivery delivery) { }
        @Override public void coverageGap(ImpactEvidence.CoverageGap gap) { gaps.add(gap); }
    }
}
