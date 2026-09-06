package pt.ulisboa.tecnico.socialsoftware.ms.monitoring.impact;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Immutable, application-independent facts consumed by potential-impact checks. */
public final class ImpactEvidence {
    private ImpactEvidence() {
    }

    public record AggregateIdentity(String aggregateType, Integer aggregateId) {
    }

    public record AggregateSnapshot(
            AggregateIdentity identity,
            Long version,
            String lifecycleState,
            String runtimeType,
            FrameworkMetadata frameworkMetadata,
            Map<String, Object> applicationData,
            List<Dependency> dependencies) {
        public AggregateSnapshot(AggregateIdentity identity, Long version, String lifecycleState,
                                 String runtimeType, Map<String, Object> applicationData,
                                 List<Dependency> dependencies) {
            this(identity, version, lifecycleState, runtimeType, null, applicationData, dependencies);
        }
        public AggregateSnapshot {
            applicationData = applicationData == null ? Map.of()
                    : java.util.Collections.unmodifiableMap(new TreeMap<>(applicationData));
            dependencies = dependencies == null ? List.of() : List.copyOf(dependencies);
        }
    }

    public record FrameworkMetadata(
            Integer rowId,
            String creationTimestamp,
            AggregateIdentity predecessorIdentity,
            Long predecessorVersion,
            String semanticLockType,
            String semanticLockState) {
    }

    public record Dependency(
            AggregateIdentity owner,
            AggregateIdentity target,
            Integer declaredTargetAggregateId,
            Long subscribedVersion,
            String eventType,
            String subscriptionType) {
    }

    public record Writer(
            String kind,
            String executionAttemptId,
            String workloadPlanId,
            String sagaInstanceId,
            String actionId,
            String phase,
            String functionalityName,
            String stepName,
            Integer eventId) {
        public static Writer unknown(String functionalityName, String stepName) {
            return new Writer("UNKNOWN", null, null, null, null, null,
                    functionalityName, stepName, null);
        }
    }

    public record CommittedWrite(long sequence, AggregateSnapshot aggregate, Writer writer) {
    }

    public record EventDelivery(
            long sequence,
            Integer eventId,
            String eventType,
            Integer publisherAggregateId,
            Long publisherAggregateVersion,
            AggregateSnapshot receiverBefore,
            AggregateSnapshot receiverAfter,
            boolean eligibleBefore,
            boolean eligibleAfter,
            AggregateSnapshot receiverFinal,
            Boolean eligibleAtHorizon,
            Writer writer) {
    }

    public record CoverageGap(String stage, String subject, String reason, String message) {
    }

    public record SnapshotBatch(List<AggregateSnapshot> aggregates, List<CoverageGap> gaps) {
        public SnapshotBatch {
            aggregates = aggregates == null ? List.of() : List.copyOf(aggregates);
            gaps = gaps == null ? List.of() : List.copyOf(gaps);
        }
    }

    public record Projection(AggregateSnapshot snapshot, List<CoverageGap> gaps) {
        public Projection {
            gaps = gaps == null ? List.of() : List.copyOf(gaps);
        }
    }
}
