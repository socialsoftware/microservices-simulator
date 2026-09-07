package pt.ulisboa.tecnico.socialsoftware.ms.monitoring.impact;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.sagaread.ReadObservationContext;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.sagaread.ReadResponseEvidence;

import java.util.ArrayList;
import java.util.List;

public final class ImpactEvidenceObserverHolder {
    private static final Logger logger = LoggerFactory.getLogger(ImpactEvidenceObserverHolder.class);
    private static final ImpactEvidenceObserver NOOP = new ImpactEvidenceObserver() {
        @Override public boolean isEnabled() { return false; }
        @Override public void committedWrite(ImpactEvidence.AggregateSnapshot aggregate, ImpactEvidence.Writer writer) { }
        @Override public void eventDelivery(ImpactEvidence.EventDelivery delivery) { }
        @Override public void coverageGap(ImpactEvidence.CoverageGap gap) { }
    };
    private static volatile ImpactEvidenceObserver observer = NOOP;
    private static ImpactEvidenceObserver scopedObserver;
    private static Scope activeScope;

    private ImpactEvidenceObserverHolder() {
    }

    public static ImpactEvidenceObserver current() {
        return observer;
    }

    public static boolean isEnabled() {
        return enabled(observer);
    }

    public static boolean isReadObservationEnabled(ImpactEvidenceObserver target) {
        try (ReadObservationContext.Scope ignored = ReadObservationContext.exclude("OBSERVER_CALLBACK")) {
            return target.isReadObservationEnabled();
        } catch (RuntimeException failure) {
            retainReadFailure(target, "READ_OBSERVER_ENABLEMENT_FAILED", failure);
            return false;
        }
    }

    /** Read failures have their own channel and never enter ImpactV2's retained gaps. */
    public static void readResponse(ImpactEvidenceObserver target, ReadResponseEvidence.Observation observation) {
        try (ReadObservationContext.Scope ignored = ReadObservationContext.exclude("OBSERVER_CALLBACK")) {
            target.readResponse(observation);
        } catch (RuntimeException failure) {
            retainReadFailure(target, "READ_OBSERVER_CALLBACK_FAILED", failure);
        }
    }

    public static synchronized void retainReadFailure(ImpactEvidenceObserver target, String reason,
                                                      RuntimeException failure) {
        logger.warn("Read observation failed; preserving application outcome ({})", reason, failure);
        if (activeScope != null && activeScope.installed == target) {
            activeScope.retainRead(new ImpactEvidence.CoverageGap("READ_OBSERVATION", null, reason,
                    failure.getClass().getName()));
        }
    }

    private static boolean enabled(ImpactEvidenceObserver current) {
        try (ReadObservationContext.Scope ignored = ReadObservationContext.exclude("OBSERVER_CALLBACK")) {
            return current.isEnabled();
        } catch (RuntimeException failure) {
            logger.warn("Impact observer failed while checking enablement; disabling observation for this call", failure);
            retain(current, failureGap("OBSERVER_ENABLEMENT", "isEnabled",
                    "OBSERVER_ENABLEMENT_FAILED", failure));
            return false;
        }
    }

    public static synchronized Scope install(ImpactEvidenceObserver temporary) {
        if (temporary == null) throw new IllegalArgumentException("Impact evidence observer cannot be null");
        if (scopedObserver != null) throw new IllegalStateException("An impact evidence observer is already active");
        ImpactEvidenceObserver previous = observer;
        observer = temporary;
        scopedObserver = temporary;
        activeScope = new Scope(temporary, previous);
        return activeScope;
    }

    public static void committedWrite(ImpactEvidence.AggregateSnapshot aggregate, ImpactEvidence.Writer writer) {
        invoke(current -> current.committedWrite(aggregate, writer), "committed write");
    }

    public static void committedWrite(ImpactEvidenceObserver target,
                                      ImpactEvidence.AggregateSnapshot aggregate,
                                      ImpactEvidence.Writer writer) {
        invoke(target, current -> current.committedWrite(aggregate, writer), "committed write");
    }

    public static void eventDelivery(ImpactEvidence.EventDelivery delivery) {
        invoke(current -> current.eventDelivery(delivery), "event delivery");
    }

    public static void coverageGap(ImpactEvidence.CoverageGap gap) {
        invoke(current -> current.coverageGap(gap), "coverage gap");
    }

    public static void coverageGap(ImpactEvidenceObserver target, ImpactEvidence.CoverageGap gap) {
        invoke(target, current -> current.coverageGap(gap), "coverage gap");
    }

    private static void invoke(java.util.function.Consumer<ImpactEvidenceObserver> call, String fact) {
        invoke(observer, call, fact);
    }

    private static void invoke(ImpactEvidenceObserver current,
                               java.util.function.Consumer<ImpactEvidenceObserver> call,
                               String fact) {
        if (!enabled(current)) return;
        try (ReadObservationContext.Scope ignored = ReadObservationContext.exclude("OBSERVER_CALLBACK")) {
            call.accept(current);
        } catch (RuntimeException failure) {
            logger.warn("Impact observer failed while recording {}; swallowing to preserve application outcome", fact,
                    failure);
            retain(current, failureGap("OBSERVER_CALLBACK", fact,
                    "OBSERVER_CALLBACK_FAILED", failure));
        }
    }

    private static ImpactEvidence.CoverageGap failureGap(String stage, String subject, String reason,
                                                          RuntimeException failure) {
        return new ImpactEvidence.CoverageGap(stage, subject, reason,
                failure.getClass().getName() + ": " + failure.getMessage());
    }

    private static synchronized void retain(ImpactEvidenceObserver failedObserver,
                                            ImpactEvidence.CoverageGap gap) {
        if (activeScope != null && activeScope.installed == failedObserver) activeScope.retain(gap);
    }

    private static synchronized void restore(ImpactEvidenceObserver installed, ImpactEvidenceObserver previous) {
        if (scopedObserver != installed) return;
        scopedObserver = null;
        activeScope = null;
        if (observer == installed) observer = previous;
    }

    public static final class Scope implements AutoCloseable {
        private final ImpactEvidenceObserver installed;
        private final ImpactEvidenceObserver previous;
        private final List<ImpactEvidence.CoverageGap> failures = new ArrayList<>();
        private final List<ImpactEvidence.CoverageGap> readFailures = new ArrayList<>();
        private boolean closed;

        private Scope(ImpactEvidenceObserver installed, ImpactEvidenceObserver previous) {
            this.installed = installed;
            this.previous = previous;
        }

        @Override
        public void close() {
            if (!closed) {
                closed = true;
                restore(installed, previous);
            }
        }

        /** Returns and clears failures retained independently of the observer callback. */
        public synchronized List<ImpactEvidence.CoverageGap> drainFailures() {
            List<ImpactEvidence.CoverageGap> result = List.copyOf(failures);
            failures.clear();
            return result;
        }

        public synchronized List<ImpactEvidence.CoverageGap> drainReadFailures() {
            List<ImpactEvidence.CoverageGap> result = List.copyOf(readFailures);
            readFailures.clear();
            return result;
        }

        private synchronized void retainRead(ImpactEvidence.CoverageGap gap) {
            readFailures.add(gap);
        }

        private synchronized void retain(ImpactEvidence.CoverageGap gap) {
            failures.add(gap);
        }
    }
}
