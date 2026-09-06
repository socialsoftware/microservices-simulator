package pt.ulisboa.tecnico.socialsoftware.ms.monitoring.impact;

import jakarta.persistence.CascadeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.Metamodel;
import org.junit.jupiter.api.Test;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PersistentStateObserverTest {
    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void normalizesManagedDataWithoutFrameworkMetadataAndPreservesCollectionMeaning() throws Exception {
        EntityManager entityManager = mock(EntityManager.class);
        Metamodel metamodel = mock(Metamodel.class);
        ManagedType aggregateType = managed(TestAggregate.class,
                "nullable", "applicationDate", "ordered", "unordered", "child", "state", "version", "creationTs");
        ManagedType childType = managed(OwnedChild.class, "id", "version", "owner");
        when(metamodel.getManagedTypes()).thenReturn(Set.of(aggregateType, childType));
        when(entityManager.getMetamodel()).thenReturn(metamodel);
        TestAggregate aggregate = new TestAggregate(7);
        aggregate.setId(12);
        aggregate.setVersion(91L);
        aggregate.setCreationTs(LocalDateTime.of(2026, 1, 1, 2, 3));
        aggregate.child.owner = aggregate;

        ImpactEvidence.Projection projection = new PersistentStateObserver(entityManager).project(aggregate, "TEST");

        assertThat(projection.gaps()).isEmpty();
        Map<String, Object> data = projection.snapshot().applicationData();
        assertThat(data).containsEntry("nullable", null)
                .containsEntry("applicationDate", "2026-02-03T04:05:06")
                .containsEntry("ordered", List.of("z", "a"))
                .containsEntry("unordered", List.of("a", "z"))
                .containsEntry("state", "ACTIVE");
        assertThat(data).doesNotContainKeys("creationTs", "version");
        assertThat(projection.snapshot().frameworkMetadata().rowId()).isEqualTo(12);
        assertThat(projection.snapshot().frameworkMetadata().creationTimestamp()).isEqualTo("2026-01-01T02:03");
        assertThat((Map<String, Object>) data.get("child"))
                .containsEntry("version", 17L)
                .doesNotContainKey("id");
        assertThat((Map<String, Object>) ((Map<String, Object>) data.get("child")).get("owner"))
                .containsEntry("aggregateId", 7).containsEntry("aggregateType", "TestAggregate");
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void normalizesOwnedBackReferencesWithoutSetOrderOrRevisionIdentityLeakingIntoEquality() throws Exception {
        EntityManager entityManager = mock(EntityManager.class);
        Metamodel metamodel = mock(Metamodel.class);
        ManagedType aggregateType = managed(TestAggregate.class, "ownedParents", "state");
        ManagedType parentType = managed(OwnedParent.class, "id", "label", "answer");
        ManagedType answerType = managed(OwnedAnswer.class, "id", "count", "parent");
        when(metamodel.getManagedTypes()).thenReturn(Set.of(aggregateType, parentType, answerType));
        when(entityManager.getMetamodel()).thenReturn(metamodel);
        TestAggregate firstRevision = new TestAggregate(7);
        firstRevision.ownedParents = new LinkedHashSet<>(List.of(
                new OwnedParent(101, "b", 2), new OwnedParent(100, "a", 1)));
        TestAggregate laterRevision = new TestAggregate(7);
        laterRevision.ownedParents = new LinkedHashSet<>(List.of(
                new OwnedParent(900, "a", 1), new OwnedParent(901, "b", 2)));

        ImpactEvidence.Projection first = new PersistentStateObserver(entityManager).project(firstRevision, "TEST");
        ImpactEvidence.Projection later = new PersistentStateObserver(entityManager).project(laterRevision, "TEST");

        assertThat(first.gaps()).isEmpty();
        assertThat(later.gaps()).isEmpty();
        assertThat(later.snapshot().applicationData()).isEqualTo(first.snapshot().applicationData());
        List<Map<String, Object>> parents = (List<Map<String, Object>>)
                first.snapshot().applicationData().get("ownedParents");
        Map<String, Object> answer = (Map<String, Object>) parents.getFirst().get("answer");
        assertThat(answer.get("parent")).isEqualTo(Map.of("persistentReference",
                Map.of("kind", "OWNED_ANCESTOR", "levels", 1)));

        laterRevision.ownedParents.stream().filter(parent -> "a".equals(parent.label)).findFirst().orElseThrow()
                .answer.count = 3;
        ImpactEvidence.Projection changed = new PersistentStateObserver(entityManager).project(laterRevision, "TEST");

        assertThat(changed.gaps()).isEmpty();
        assertThat(changed.snapshot().applicationData()).isNotEqualTo(first.snapshot().applicationData());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void retainsExplicitGapForCycleWithoutOwnedMapping() throws Exception {
        EntityManager entityManager = mock(EntityManager.class);
        Metamodel metamodel = mock(Metamodel.class);
        ManagedType aggregateType = managed(TestAggregate.class, "unsupported", "state");
        ManagedType nodeType = managed(UnsupportedNode.class, "id", "next");
        when(metamodel.getManagedTypes()).thenReturn(Set.of(aggregateType, nodeType));
        when(entityManager.getMetamodel()).thenReturn(metamodel);
        TestAggregate aggregate = new TestAggregate(7);
        aggregate.unsupported = new UnsupportedNode();
        aggregate.unsupported.next = aggregate.unsupported;

        ImpactEvidence.Projection projection = new PersistentStateObserver(entityManager).project(aggregate, "TEST");

        assertThat(projection.gaps()).extracting(ImpactEvidence.CoverageGap::reason)
                .containsExactly("PERSISTENT_MAPPING_CYCLE");
        Map<String, Object> node = (Map<String, Object>) projection.snapshot().applicationData().get("unsupported");
        assertThat(node.get("next")).isEqualTo(Map.of("cycleRef", "TestAggregate.unsupported"));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void eligibilityUsesLatestOwnerAndItsOverriddenExactEventPredicate() throws Exception {
        EntityManager entityManager = mock(EntityManager.class);
        Metamodel metamodel = mock(Metamodel.class);
        ManagedType aggregateType = managed(TestAggregate.class, "nullable", "applicationDate", "ordered",
                "unordered", "child", "state", "version", "creationTs");
        ManagedType childType = managed(OwnedChild.class, "id", "version", "owner");
        when(metamodel.getManagedTypes()).thenReturn(Set.of(aggregateType, childType));
        when(entityManager.getMetamodel()).thenReturn(metamodel);
        TypedQuery<Aggregate> query = mock(TypedQuery.class);
        when(entityManager.createQuery(
                "select aggregate from Aggregate aggregate where aggregate.aggregateId = :aggregateId",
                Aggregate.class)).thenReturn(query);
        when(query.setParameter("aggregateId", 7)).thenReturn(query);
        TestAggregate older = new TestAggregate(7);
        older.setVersion(1L);
        older.subscriptions = Set.of(new PayloadSubscription(true));
        TestAggregate latest = new TestAggregate(7);
        latest.setVersion(2L);
        latest.subscriptions = Set.of(new PayloadSubscription(false));
        when(query.getResultList()).thenReturn(List.of(older, latest));
        stubTargetAggregates(entityManager, List.of());
        ExactEvent event = new ExactEvent(9);
        event.setPublisherAggregateVersion(3L);

        PersistentStateObserver.EligibilityObservation observation =
                new PersistentStateObserver(entityManager).observeEligibility(7, event, "HORIZON_EVENT");

        assertThat(observation.projection().snapshot().version()).isEqualTo(2L);
        assertThat(observation.eligible()).isFalse();
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void idOnlyLookupRejectsMultipleLogicalAggregateTypes() throws Exception {
        EntityManager entityManager = mock(EntityManager.class);
        Metamodel metamodel = mock(Metamodel.class);
        ManagedType aggregateType = managed(TestAggregate.class, "state");
        when(metamodel.getManagedTypes()).thenReturn(Set.of(aggregateType));
        when(entityManager.getMetamodel()).thenReturn(metamodel);
        TypedQuery<Aggregate> query = mock(TypedQuery.class);
        when(entityManager.createQuery(
                "select aggregate from Aggregate aggregate where aggregate.aggregateId = :aggregateId",
                Aggregate.class)).thenReturn(query);
        when(query.setParameter("aggregateId", 7)).thenReturn(query);
        TestAggregate first = new TestAggregate(7);
        TestAggregate second = new TestAggregate(7);
        second.setAggregateType("OtherAggregate");
        when(query.getResultList()).thenReturn(List.of(first, second));

        ImpactEvidence.Projection projection =
                new PersistentStateObserver(entityManager).snapshotLatest(7, "BEFORE_EVENT");

        assertThat(projection.snapshot()).isNull();
        assertThat(projection.gaps()).extracting(ImpactEvidence.CoverageGap::reason)
                .containsExactly("AMBIGUOUS_AGGREGATE_IDENTITY");
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void dependencyTargetUsesPersistedLogicalIdentity() throws Exception {
        EntityManager entityManager = mock(EntityManager.class);
        Metamodel metamodel = mock(Metamodel.class);
        ManagedType aggregateType = managed(TestAggregate.class, "state");
        when(metamodel.getManagedTypes()).thenReturn(Set.of(aggregateType));
        when(entityManager.getMetamodel()).thenReturn(metamodel);
        TestAggregate owner = new TestAggregate(7);
        owner.subscriptions = Set.of(new PayloadSubscription(true));
        TestAggregate target = new TestAggregate(9);
        target.setAggregateType("TargetAggregate");
        stubTargetAggregates(entityManager, List.of(target));

        ImpactEvidence.Projection projection = new PersistentStateObserver(entityManager).project(owner, "SNAPSHOT");

        assertThat(projection.gaps()).isEmpty();
        assertThat(projection.snapshot().dependencies()).singleElement().satisfies(dependency -> {
            assertThat(dependency.declaredTargetAggregateId()).isEqualTo(9);
            assertThat(dependency.target()).isEqualTo(
                    new ImpactEvidence.AggregateIdentity("TargetAggregate", 9));
        });
        verify(entityManager, never()).createQuery(
                "select aggregate from Aggregate aggregate", Aggregate.class);
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void exactCommittedRevisionUsesTheReloadedDatabaseValue() throws Exception {
        EntityManager entityManager = mock(EntityManager.class);
        Metamodel metamodel = mock(Metamodel.class);
        ManagedType aggregateType = managed(TestAggregate.class, "applicationDate", "state");
        when(metamodel.getManagedTypes()).thenReturn(Set.of(aggregateType));
        when(entityManager.getMetamodel()).thenReturn(metamodel);
        TypedQuery<Aggregate> query = mock(TypedQuery.class);
        when(entityManager.createQuery(
                "select aggregate from Aggregate aggregate where aggregate.aggregateId = :aggregateId "
                        + "and aggregate.version = :version", Aggregate.class)).thenReturn(query);
        when(query.setParameter("aggregateId", 7)).thenReturn(query);
        when(query.setParameter("version", 12L)).thenReturn(query);
        TestAggregate persisted = new TestAggregate(7);
        persisted.setVersion(12L);
        persisted.applicationDate = LocalDateTime.of(2026, 9, 6, 1, 2, 3, 123_456_000);
        when(query.getResultList()).thenReturn(List.of(persisted));
        ImpactEvidence.AggregateIdentity identity = new ImpactEvidence.AggregateIdentity("TestAggregate", 7);

        ImpactEvidence.Projection projection =
                new PersistentStateObserver(entityManager).snapshotVersion(identity, 12L, "WRITE");

        assertThat(projection.gaps()).isEmpty();
        assertThat(projection.snapshot().applicationData().get("applicationDate"))
                .isEqualTo("2026-09-06T01:02:03.123456");
    }

    @SuppressWarnings("unchecked")
    private void stubTargetAggregates(EntityManager entityManager, List<Aggregate> aggregates) {
        TypedQuery<Aggregate> targets = mock(TypedQuery.class);
        when(entityManager.createQuery(
                "select aggregate from Aggregate aggregate where aggregate.aggregateId in :aggregateIds",
                Aggregate.class)).thenReturn(targets);
        when(targets.setParameter(org.mockito.ArgumentMatchers.eq("aggregateIds"),
                org.mockito.ArgumentMatchers.any())).thenReturn(targets);
        when(targets.getResultList()).thenReturn(aggregates);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private ManagedType managed(Class<?> type, String... fields) throws Exception {
        ManagedType managed = mock(ManagedType.class);
        when(managed.getJavaType()).thenReturn(type);
        Set<Attribute> attributes = new LinkedHashSet<>();
        for (String name : fields) {
            Field field = findField(type, name);
            Attribute attribute = mock(Attribute.class);
            when(attribute.getName()).thenReturn(name);
            when(attribute.getJavaMember()).thenReturn(field);
            when(attribute.getJavaType()).thenReturn(field.getType());
            attributes.add(attribute);
        }
        when(managed.getAttributes()).thenReturn(attributes);
        return managed;
    }

    private Field findField(Class<?> type, String name) throws Exception {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try { return current.getDeclaredField(name); } catch (NoSuchFieldException ignored) { }
        }
        throw new NoSuchFieldException(name);
    }

    private static final class OwnedChild {
        @Id private Integer id = 99;
        private Long version = 17L;
        private TestAggregate owner;
    }

    private static final class OwnedParent {
        @Id private Integer id;
        private String label;
        @OneToOne(cascade = CascadeType.ALL, mappedBy = "parent")
        private OwnedAnswer answer;

        private OwnedParent(Integer id, String label, int count) {
            this.id = id;
            this.label = label;
            this.answer = new OwnedAnswer(id + 1, count, this);
        }
    }

    private static final class OwnedAnswer {
        @Id private Integer id;
        private int count;
        @OneToOne private OwnedParent parent;

        private OwnedAnswer(Integer id, int count, OwnedParent parent) {
            this.id = id;
            this.count = count;
            this.parent = parent;
        }
    }

    private static final class UnsupportedNode {
        @Id private Integer id = 1;
        private UnsupportedNode next;
    }

    private static final class TestAggregate extends Aggregate {
        private String nullable;
        private LocalDateTime applicationDate = LocalDateTime.of(2026, 2, 3, 4, 5, 6);
        private List<String> ordered = List.of("z", "a");
        private Set<String> unordered = new LinkedHashSet<>(List.of("z", "a"));
        private OwnedChild child = new OwnedChild();
        private Set<OwnedParent> ownedParents = Set.of();
        private UnsupportedNode unsupported;
        private Set<EventSubscription> subscriptions = Set.of();

        private TestAggregate(Integer aggregateId) {
            super(aggregateId);
            setAggregateType("TestAggregate");
        }
        @Override public void verifyInvariants() { }
        @Override public Set<EventSubscription> getEventSubscriptions() { return subscriptions; }
    }

    private static final class ExactEvent extends Event {
        private ExactEvent(Integer publisherAggregateId) { super(publisherAggregateId); }
    }

    private static final class PayloadSubscription extends EventSubscription {
        private final boolean payloadAccepted;
        private PayloadSubscription(boolean payloadAccepted) {
            super(9, 0L, ExactEvent.class.getSimpleName());
            this.payloadAccepted = payloadAccepted;
        }
        @Override public boolean subscribesEvent(Event event) {
            return super.subscribesEvent(event) && payloadAccepted;
        }
    }
}
