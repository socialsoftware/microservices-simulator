package pt.ulisboa.tecnico.socialsoftware.ms.aggregate

import org.springframework.data.jpa.repository.JpaRepository
import pt.ulisboa.tecnico.socialsoftware.ms.SpockTest

class EventHandlerTest extends SpockTest {

    def "newest deleted aggregate version does not retain old event subscriptions"() {
        given: 'an active version with subscriptions followed by a deleted version'
        def repository = Mock(JpaRepository)
        def subscription = eventSubscription('TestEvent')
        def activeVersion = new TestAggregate(1, 5L, Aggregate.AggregateState.ACTIVE, [subscription] as Set)
        def deletedVersion = new TestAggregate(1, 6L, Aggregate.AggregateState.DELETED, Set.of())
        repository.findAll() >> [activeVersion, deletedVersion]
        def eventHandler = new TestEventHandler(repository)

        when: 'subscriptions are selected for the aggregate'
        def subscriptions = eventHandler.getEventSubscriptions(1, TestEvent)

        then: 'only the newest version determines whether it is an active subscriber'
        subscriptions.empty
    }

    def "newest active aggregate version supplies its event subscriptions"() {
        given: 'an old deleted version followed by an active version with subscriptions'
        def repository = Mock(JpaRepository)
        def subscription = eventSubscription('TestEvent')
        def deletedVersion = new TestAggregate(1, 5L, Aggregate.AggregateState.DELETED, Set.of())
        def activeVersion = new TestAggregate(1, 6L, Aggregate.AggregateState.ACTIVE, [subscription] as Set)
        repository.findAll() >> [deletedVersion, activeVersion]
        def eventHandler = new TestEventHandler(repository)

        when: 'subscriptions are selected for the aggregate'
        def subscriptions = eventHandler.getEventSubscriptions(1, TestEvent)

        then: 'subscriptions from newest active version are used'
        subscriptions == [subscription] as Set
    }

    private static EventSubscription eventSubscription(String eventType) {
        new EventSubscription(2, 0L, eventType) {}
    }

    private static class TestEvent extends Event {
    }

    private static class TestEventHandler extends EventHandler {
        TestEventHandler(JpaRepository<? extends Aggregate, Integer> aggregateRepository) {
            super(aggregateRepository)
        }

        @Override
        void handleEvent(Integer subscriberAggregateId, Event event) {
        }
    }

    private static class TestAggregate extends Aggregate {
        private final Set<EventSubscription> eventSubscriptions

        TestAggregate(Integer aggregateId, Long version, AggregateState state, Set<EventSubscription> eventSubscriptions) {
            this.aggregateId = aggregateId
            this.version = version
            this.state = state
            this.eventSubscriptions = eventSubscriptions
        }

        @Override
        void verifyInvariants() {
        }

        @Override
        Set<EventSubscription> getEventSubscriptions() {
            eventSubscriptions
        }
    }
}
