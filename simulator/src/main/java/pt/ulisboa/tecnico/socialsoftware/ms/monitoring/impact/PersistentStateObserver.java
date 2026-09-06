package pt.ulisboa.tecnico.socialsoftware.ms.monitoring.impact;

import jakarta.persistence.CascadeType;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.ManagedType;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.time.temporal.TemporalAccessor;
import java.util.*;

import static pt.ulisboa.tecnico.socialsoftware.ms.monitoring.impact.ImpactEvidence.*;

/** Read-only projection of managed persistent aggregate state. */
@Service
public class PersistentStateObserver {
    private static final Set<String> AGGREGATE_METADATA = Set.of(
            "id", "aggregateId", "version", "creationTs", "prev", "aggregateType");
    private final EntityManager entityManager;

    public PersistentStateObserver(EntityManager entityManager) { this.entityManager = entityManager; }

    @Transactional(readOnly = true)
    public SnapshotBatch snapshotAll() {
        List<CoverageGap> gaps = new ArrayList<>();
        Map<AggregateIdentity, Aggregate> latest = new LinkedHashMap<>();
        for (Aggregate aggregate : entityManager.createQuery("select aggregate from Aggregate aggregate", Aggregate.class)
                .getResultList()) {
            AggregateIdentity identity = identity(aggregate);
            if (identity.aggregateId() == null || identity.aggregateType() == null) {
                gaps.add(gap("SNAPSHOT", aggregate.getClass().getName(), "MISSING_AGGREGATE_IDENTITY",
                        "aggregate type and logical id are required"));
                continue;
            }
            Aggregate current = latest.get(identity);
            if (current == null || version(aggregate) > version(current)) latest.put(identity, aggregate);
        }
        List<AggregateSnapshot> snapshots = new ArrayList<>();
        Map<Integer, List<AggregateIdentity>> identityIndex = identityIndex(latest.values());
        latest.values().forEach(aggregate -> {
            Projection projection = project(aggregate, "SNAPSHOT", identityIndex);
            if (projection.snapshot() != null) snapshots.add(projection.snapshot());
            gaps.addAll(projection.gaps());
        });
        snapshots.sort(Comparator.comparing((AggregateSnapshot value) -> value.identity().aggregateType())
                .thenComparing(value -> value.identity().aggregateId()));
        return new SnapshotBatch(snapshots, gaps);
    }

    @Transactional(readOnly = true)
    public Projection snapshotLatest(Integer aggregateId, String stage) {
        if (aggregateId == null) return new Projection(null,
                List.of(gap(stage, null, "MISSING_AGGREGATE_IDENTITY", "logical id is required")));
        LatestResolution resolution = latestAggregate(aggregateId, null, stage);
        if (resolution.aggregate() == null) return new Projection(null, resolution.gaps());
        Projection projection = project(resolution.aggregate(), stage);
        return new Projection(projection.snapshot(), concat(resolution.gaps(), projection.gaps()));
    }

    /** Reloads one committed logical revision in an independent read transaction. */
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public Projection snapshotVersion(AggregateIdentity expectedIdentity, Long expectedVersion, String stage) {
        if (expectedIdentity == null || expectedIdentity.aggregateId() == null || expectedVersion == null) {
            return new Projection(null, List.of(gap(stage, String.valueOf(expectedIdentity),
                    "MISSING_AGGREGATE_REVISION", "typed aggregate identity and version are required")));
        }
        List<Aggregate> matches = entityManager.createQuery(
                        "select aggregate from Aggregate aggregate where aggregate.aggregateId = :aggregateId "
                                + "and aggregate.version = :version", Aggregate.class)
                .setParameter("aggregateId", expectedIdentity.aggregateId())
                .setParameter("version", expectedVersion)
                .getResultList().stream()
                .filter(value -> expectedIdentity.equals(identity(value)))
                .toList();
        if (matches.isEmpty()) return new Projection(null, List.of(gap(stage, expectedIdentity.toString(),
                "AGGREGATE_REVISION_NOT_FOUND", "committed aggregate revision is unavailable")));
        if (matches.size() > 1) return new Projection(null, List.of(gap(stage, expectedIdentity.toString(),
                "AMBIGUOUS_AGGREGATE_REVISION", "multiple rows match committed aggregate revision")));
        return project(matches.getFirst(), stage);
    }

    public AggregateIdentity identityOf(Aggregate aggregate) {
        return aggregate == null ? null : identity((Aggregate) Hibernate.unproxy(aggregate));
    }

    @Transactional(readOnly = true)
    public EligibilityObservation observeEligibility(Integer aggregateId, Event event, String stage) {
        return observeEligibility(null, aggregateId, event, stage);
    }

    @Transactional(readOnly = true)
    public EligibilityObservation observeEligibility(AggregateIdentity expectedIdentity, Event event, String stage) {
        return observeEligibility(expectedIdentity,
                expectedIdentity == null ? null : expectedIdentity.aggregateId(), event, stage);
    }

    private EligibilityObservation observeEligibility(AggregateIdentity expectedIdentity, Integer aggregateId,
                                                       Event event, String stage) {
        LatestResolution resolution = latestAggregate(aggregateId, expectedIdentity, stage);
        Aggregate latest = resolution.aggregate();
        if (latest == null) return new EligibilityObservation(new Projection(null, resolution.gaps()), false);
        Projection projection = project(latest, stage);
        boolean eligible = latest.getState() == Aggregate.AggregateState.ACTIVE
                && latest.getEventSubscriptions().stream().anyMatch(subscription -> subscription.subscribesEvent(event));
        return new EligibilityObservation(new Projection(projection.snapshot(),
                concat(resolution.gaps(), projection.gaps())), eligible);
    }

    public Projection project(Aggregate aggregate, String stage) {
        return project(aggregate, stage, targetIdentityIndex(aggregate));
    }

    private Projection project(Aggregate aggregate, String stage,
                               Map<Integer, List<AggregateIdentity>> identityIndex) {
        List<CoverageGap> gaps = new ArrayList<>();
        if (aggregate == null) return new Projection(null, List.of(gap(stage, null,
                "MISSING_AGGREGATE", "aggregate is required")));
        aggregate = (Aggregate) Hibernate.unproxy(aggregate);
        AggregateIdentity identity = identity(aggregate);
        if (identity.aggregateType() == null || identity.aggregateId() == null) {
            return new Projection(null, List.of(gap(stage, aggregate.getClass().getName(),
                    "MISSING_AGGREGATE_IDENTITY", "aggregate type and logical id are required")));
        }
        Map<String, Object> data = normalizeManaged(aggregate, aggregate, new IdentityHashMap<>(), gaps,
                stage, aggregate.getClass().getSimpleName(), 0);
        List<Dependency> dependencies = dependencies(aggregate, identity, identityIndex, gaps, stage);
        FrameworkMetadata metadata = null;
        try {
            metadata = frameworkMetadata(aggregate);
        } catch (RuntimeException failure) {
            gaps.add(gap(stage, identity.toString(), "FRAMEWORK_METADATA_UNAVAILABLE", details(failure)));
        }
        return new Projection(new AggregateSnapshot(identity, aggregate.getVersion(),
                aggregate.getState() == null ? null : aggregate.getState().name(), aggregate.getClass().getName(),
                metadata, data, dependencies), gaps);
    }

    @Transactional(readOnly = true)
    public HorizonObservation observeAtHorizon(EventDelivery delivery) {
        if (delivery == null || delivery.eventId() == null || delivery.receiverAfter() == null) {
            return new HorizonObservation(delivery, List.of(gap("HORIZON_EVENT", null,
                    "EVENT_IDENTITY_UNAVAILABLE", "event and receiver identity are required")));
        }
        Event event = entityManager.find(Event.class, delivery.eventId());
        LatestResolution resolution = latestAggregate(delivery.receiverAfter().identity().aggregateId(),
                delivery.receiverAfter().identity(), "HORIZON_EVENT");
        Aggregate receiver = resolution.aggregate();
        if (event == null) return new HorizonObservation(copyAtHorizon(delivery, null, null), List.of(gap(
                "HORIZON_EVENT", String.valueOf(delivery.eventId()), "EVENT_NOT_FOUND", "exact event is unavailable")));
        if (receiver == null) return new HorizonObservation(copyAtHorizon(delivery, null, null), resolution.gaps());
        Projection projection = project(receiver, "HORIZON_EVENT");
        boolean eligible = receiver.getState() == Aggregate.AggregateState.ACTIVE
                && receiver.getEventSubscriptions().stream().anyMatch(subscription -> subscription.subscribesEvent(event));
        return new HorizonObservation(copyAtHorizon(delivery, projection.snapshot(), eligible),
                concat(resolution.gaps(), projection.gaps()));
    }

    private EventDelivery copyAtHorizon(EventDelivery value, AggregateSnapshot receiver, Boolean eligible) {
        return new EventDelivery(value.sequence(), value.eventId(), value.eventType(), value.publisherAggregateId(),
                value.publisherAggregateVersion(), value.receiverBefore(), value.receiverAfter(), value.eligibleBefore(),
                value.eligibleAfter(), receiver, eligible, value.writer());
    }

    private LatestResolution latestAggregate(Integer aggregateId, AggregateIdentity expectedIdentity, String stage) {
        if (aggregateId == null) return new LatestResolution(null, List.of(gap(stage, null,
                "MISSING_AGGREGATE_IDENTITY", "logical id is required")));
        List<Aggregate> candidates = entityManager.createQuery(
                        "select aggregate from Aggregate aggregate where aggregate.aggregateId = :aggregateId",
                        Aggregate.class).setParameter("aggregateId", aggregateId).getResultList();
        if (expectedIdentity != null) {
            candidates = candidates.stream().filter(value -> expectedIdentity.equals(identity(value))).toList();
        } else {
            Set<AggregateIdentity> identities = new LinkedHashSet<>();
            candidates.forEach(value -> identities.add(identity(value)));
            if (identities.size() > 1) return new LatestResolution(null, List.of(gap(stage,
                    String.valueOf(aggregateId), "AMBIGUOUS_AGGREGATE_IDENTITY",
                    "logical id resolves to multiple aggregate types: " + identities)));
        }
        Aggregate latest = candidates.stream().max(Comparator.comparingLong(this::version)).orElse(null);
        return latest == null ? new LatestResolution(null, List.of(gap(stage,
                expectedIdentity == null ? String.valueOf(aggregateId) : expectedIdentity.toString(),
                "AGGREGATE_NOT_FOUND", "latest persisted aggregate is unavailable")))
                : new LatestResolution(latest, List.of());
    }

    private List<Dependency> dependencies(Aggregate aggregate, AggregateIdentity identity,
                                          Map<Integer, List<AggregateIdentity>> identityIndex,
                                          List<CoverageGap> gaps, String stage) {
        List<Dependency> result = new ArrayList<>();
        try {
            Set<EventSubscription> subscriptions = aggregate.getEventSubscriptions();
            if (subscriptions == null) {
                gaps.add(gap(stage, identity.toString(), "MISSING_DEPENDENCY_DECLARATIONS",
                        "getEventSubscriptions returned null"));
            } else {
                subscriptions.stream().map(value -> dependency(identity, value, identityIndex, gaps, stage))
                        .sorted(Comparator.comparing(Dependency::eventType, Comparator.nullsFirst(String::compareTo))
                                .thenComparing(Dependency::declaredTargetAggregateId,
                                        Comparator.nullsFirst(Integer::compareTo))
                                .thenComparing(Dependency::subscriptionType))
                        .forEach(result::add);
            }
        } catch (RuntimeException failure) {
            gaps.add(gap(stage, identity.toString(), "DEPENDENCY_ENUMERATION_FAILED", details(failure)));
        }
        return List.copyOf(result);
    }

    private Dependency dependency(AggregateIdentity owner, EventSubscription subscription,
                                  Map<Integer, List<AggregateIdentity>> identityIndex,
                                  List<CoverageGap> gaps, String stage) {
        Integer targetId = subscription.getSubscribedAggregateId();
        List<AggregateIdentity> targets = targetId == null ? List.of()
                : identityIndex.getOrDefault(targetId, List.of());
        AggregateIdentity target = targets.size() == 1 ? targets.get(0) : null;
        if (targets.isEmpty()) {
            gaps.add(gap(stage, owner.toString(), "DEPENDENCY_TARGET_NOT_FOUND",
                    "subscription target " + targetId + " has no persisted aggregate identity"));
        } else if (targets.size() > 1) {
            gaps.add(gap(stage, owner.toString(), "AMBIGUOUS_DEPENDENCY_TARGET",
                    "subscription target " + targetId + " resolves to " + targets));
        }
        return new Dependency(owner, target, targetId, subscription.getSubscribedVersion(),
                subscription.getEventType(), subscription.getClass().getName());
    }

    private Map<Integer, List<AggregateIdentity>> targetIdentityIndex(Aggregate aggregate) {
        if (aggregate == null) return Map.of();
        Set<EventSubscription> subscriptions = aggregate.getEventSubscriptions();
        if (subscriptions == null || subscriptions.isEmpty()) return Map.of();
        Set<Integer> targetIds = new TreeSet<>();
        subscriptions.stream().map(EventSubscription::getSubscribedAggregateId)
                .filter(Objects::nonNull).forEach(targetIds::add);
        if (targetIds.isEmpty()) return Map.of();
        List<Aggregate> targets = entityManager.createQuery(
                        "select aggregate from Aggregate aggregate where aggregate.aggregateId in :aggregateIds",
                        Aggregate.class).setParameter("aggregateIds", targetIds).getResultList();
        return identityIndex(targets);
    }

    private Map<Integer, List<AggregateIdentity>> identityIndex(Collection<Aggregate> aggregates) {
        Map<Integer, Set<AggregateIdentity>> index = new TreeMap<>();
        for (Aggregate aggregate : aggregates) {
            AggregateIdentity identity = identity(aggregate);
            if (identity.aggregateId() != null && identity.aggregateType() != null) {
                index.computeIfAbsent(identity.aggregateId(), ignored -> new TreeSet<>(
                                Comparator.comparing(AggregateIdentity::aggregateType)
                                        .thenComparing(AggregateIdentity::aggregateId)))
                        .add(identity);
            }
        }
        Map<Integer, List<AggregateIdentity>> result = new TreeMap<>();
        index.forEach((id, identities) -> result.put(id, List.copyOf(identities)));
        return Collections.unmodifiableMap(result);
    }

    private List<CoverageGap> concat(List<CoverageGap> first, List<CoverageGap> second) {
        List<CoverageGap> result = new ArrayList<>(first);
        result.addAll(second);
        return List.copyOf(result);
    }

    private Map<String, Object> normalizeManaged(Object value, Aggregate owner,
                                                  IdentityHashMap<Object, SeenObject> seen,
                                                  List<CoverageGap> gaps, String stage, String path, int depth) {
        Map<String, Object> result = new TreeMap<>();
        seen.put(value, new SeenObject(path, depth));
        ManagedType<?> managed;
        try {
            managed = managedType(value.getClass());
        } catch (RuntimeException failure) {
            gaps.add(gap(stage, path, "UNSUPPORTED_PERSISTENT_MAPPING", details(failure)));
            seen.remove(value);
            return Collections.unmodifiableMap(result);
        }
        managed.getAttributes().stream().sorted(Comparator.comparing(Attribute::getName)).forEach(attribute -> {
            if (excludedFrameworkAttribute(attribute)) return;
            String childPath = path + "." + attribute.getName();
            try {
                result.put(attribute.getName(), normalizeValue(read(attribute.getJavaMember(), value), owner, seen,
                        gaps, stage, childPath, depth, value, attribute));
            } catch (RuntimeException failure) {
                gaps.add(gap(stage, childPath, "PERSISTENT_ATTRIBUTE_UNSUPPORTED", details(failure)));
            }
        });
        seen.remove(value);
        return Collections.unmodifiableMap(result);
    }

    private boolean excludedFrameworkAttribute(Attribute<?, ?> attribute) {
        if (attribute.getJavaMember() instanceof java.lang.reflect.AnnotatedElement annotated
                && (annotated.isAnnotationPresent(Id.class) || annotated.isAnnotationPresent(EmbeddedId.class))) {
            return true;
        }
        Class<?> declaring = attribute.getJavaMember().getDeclaringClass();
        if (declaring == Aggregate.class && AGGREGATE_METADATA.contains(attribute.getName())) return true;
        return "sagaState".equals(attribute.getName())
                && SagaAggregate.class.isAssignableFrom(declaring)
                && SagaAggregate.SagaState.class.isAssignableFrom(attribute.getJavaType());
    }

    private Object normalizeValue(Object value, Aggregate owner, IdentityHashMap<Object, SeenObject> seen,
                                  List<CoverageGap> gaps, String stage, String path, int depth,
                                  Object source, Attribute<?, ?> sourceAttribute) {
        if (value == null || value instanceof String || value instanceof Number || value instanceof Boolean
                || value instanceof Character || value instanceof UUID) return value;
        if (value instanceof Enum<?> enumeration) return enumeration.name();
        if (value instanceof TemporalAccessor || value instanceof Date) return value.toString();
        if (value instanceof Aggregate aggregate) return aggregateReference(aggregate);
        if (value instanceof Map<?, ?>) {
            gaps.add(gap(stage, path, "UNSUPPORTED_PERSISTENT_MAPPING", "map-valued attribute"));
            return null;
        }
        if (value instanceof Collection<?> collection) {
            List<Object> normalized = new ArrayList<>();
            int index = 0;
            for (Object element : collection) normalized.add(normalizeValue(element, owner, seen, gaps, stage,
                    path + "[" + index++ + "]", depth, source, sourceAttribute));
            if (value instanceof Set<?>) normalized.sort(Comparator.comparing(this::stableText));
            return Collections.unmodifiableList(normalized);
        }
        if (seen.containsKey(value)) {
            if (value == owner) return aggregateReference(owner);
            SeenObject target = seen.get(value);
            if (isOwnedBackReference(source, sourceAttribute, value) && target.depth() < depth) {
                return ownedAncestorReference(depth - target.depth());
            }
            gaps.add(gap(stage, path, "PERSISTENT_MAPPING_CYCLE", "cycle refers to " + target.path()));
            return Map.of("cycleRef", target.path());
        }
        return normalizeManaged(value, owner, seen, gaps, stage, path, depth + 1);
    }

    private boolean isOwnedBackReference(Object child, Attribute<?, ?> backReference, Object parent) {
        if (child == null || backReference == null || parent == null) return false;
        String backReferenceName = backReference.getName();
        ManagedType<?> parentType;
        try {
            parentType = managedType(parent.getClass());
        } catch (RuntimeException unsupported) {
            return false;
        }
        return parentType.getAttributes().stream().anyMatch(attribute -> {
            OneToOne one = annotation(attribute.getJavaMember(), OneToOne.class);
            OneToMany many = annotation(attribute.getJavaMember(), OneToMany.class);
            boolean mappedBack = (one != null && backReferenceName.equals(one.mappedBy()) && owns(one.cascade()))
                    || (many != null && backReferenceName.equals(many.mappedBy()) && owns(many.cascade()));
            if (!mappedBack) return false;
            Object owned;
            try {
                owned = read(attribute.getJavaMember(), parent);
            } catch (RuntimeException unreadable) {
                return false;
            }
            return owned == child || owned instanceof Collection<?> collection
                    && collection.stream().anyMatch(element -> element == child);
        });
    }

    private boolean owns(CascadeType[] cascade) {
        return Arrays.stream(cascade).anyMatch(value -> value == CascadeType.ALL || value == CascadeType.PERSIST);
    }

    private <A extends java.lang.annotation.Annotation> A annotation(Member member, Class<A> type) {
        return member instanceof java.lang.reflect.AnnotatedElement annotated
                ? annotated.getAnnotation(type) : null;
    }

    private Map<String, Object> ownedAncestorReference(int levels) {
        // A relative ancestor reference stays stable when an enclosing Set is traversed in another order.
        return Map.of("persistentReference", Map.of("kind", "OWNED_ANCESTOR", "levels", levels));
    }

    private Map<String, Object> aggregateReference(Aggregate aggregate) {
        Map<String, Object> ref = new TreeMap<>();
        ref.put("aggregateId", aggregate.getAggregateId());
        ref.put("aggregateType", aggregateType(aggregate));
        return Collections.unmodifiableMap(ref);
    }

    private FrameworkMetadata frameworkMetadata(Aggregate aggregate) {
        Aggregate predecessor = aggregate.getPrev();
        String lockType = null;
        String lockState = null;
        if (aggregate instanceof SagaAggregate sagaAggregate && sagaAggregate.getSagaState() != null) {
            lockType = sagaAggregate.getSagaState().getClass().getName();
            lockState = sagaAggregate.getSagaState().getStateName();
        }
        return new FrameworkMetadata(aggregate.getId(),
                aggregate.getCreationTs() == null ? null : aggregate.getCreationTs().toString(),
                predecessor == null ? null : identity(predecessor),
                predecessor == null ? null : predecessor.getVersion(), lockType, lockState);
    }

    private ManagedType<?> managedType(Class<?> runtimeType) {
        return entityManager.getMetamodel().getManagedTypes().stream()
                .filter(type -> type.getJavaType().isAssignableFrom(runtimeType))
                .max(Comparator.comparingInt(type -> hierarchyDepth(type.getJavaType())))
                .orElseThrow(() -> new IllegalArgumentException("not a JPA managed type: " + runtimeType.getName()));
    }

    private int hierarchyDepth(Class<?> type) {
        int depth = 0;
        for (Class<?> current = type; current != null; current = current.getSuperclass()) depth++;
        return depth;
    }

    private Object read(Member member, Object target) {
        try {
            if (member instanceof Field field) { field.setAccessible(true); return field.get(target); }
            if (member instanceof Method method) { method.setAccessible(true); return method.invoke(target); }
            throw new IllegalArgumentException("unsupported persistent member " + member);
        } catch (IllegalAccessException | InvocationTargetException failure) {
            throw new IllegalStateException("cannot read persistent member " + member.getName(), failure);
        }
    }

    private AggregateIdentity identity(Aggregate aggregate) {
        return new AggregateIdentity(aggregateType(aggregate), aggregate.getAggregateId());
    }
    private String aggregateType(Aggregate aggregate) {
        return aggregate.getAggregateType() == null ? aggregate.getClass().getSimpleName() : aggregate.getAggregateType();
    }
    private long version(Aggregate aggregate) { return aggregate.getVersion() == null ? Long.MIN_VALUE : aggregate.getVersion(); }
    private String stableText(Object value) { return String.valueOf(value); }
    private String details(Throwable failure) { return failure.getClass().getName() + ": " + failure.getMessage(); }
    private CoverageGap gap(String stage, String subject, String reason, String message) {
        return new CoverageGap(stage, subject, reason, message);
    }

    public record EligibilityObservation(Projection projection, boolean eligible) {
    }
    public record HorizonObservation(EventDelivery delivery, List<CoverageGap> gaps) {
        public HorizonObservation {
            gaps = gaps == null ? List.of() : List.copyOf(gaps);
        }
    }
    private record LatestResolution(Aggregate aggregate, List<CoverageGap> gaps) {
    }
    private record SeenObject(String path, int depth) {
    }
}
