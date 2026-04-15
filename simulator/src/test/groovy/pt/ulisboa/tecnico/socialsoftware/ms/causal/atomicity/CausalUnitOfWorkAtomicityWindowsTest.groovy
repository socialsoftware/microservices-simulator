package pt.ulisboa.tecnico.socialsoftware.ms.causal.atomicity


import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.causal.aggregate.CausalAggregate
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.causal.unitOfWork.CausalUnitOfWorkService
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.causal.unitOfWork.command.AbortCausalCommand
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.causal.unitOfWork.command.CommitCausalCommand
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.causal.unitOfWork.command.PrepareCausalCommand

class CausalUnitOfWorkAtomicityWindowsTest /*extends SpockTest*/ {

    def 'causal commit phase-2 failure must trigger abort commands to preserve atomicity'() {
        given:
        def service = new CausalUnitOfWorkService()
        def commandGateway = Mock(CommandGateway)
        setField(service, 'commandGateway', commandGateway)

        def aggregate1 = new TestCausalAggregate(11, 'ExecutionCausal')
        def aggregate2 = new TestCausalAggregate(12, 'ExecutionCausal')
        def committedAggregateIds = []

        when:
        service.commitAllObjects(100L, [aggregate1, aggregate2])

        then:
        thrown(RuntimeException)
        committedAggregateIds == []
        aggregate1.version == 100L
        aggregate2.version == 100L
        aggregate1.creationTs != null
        aggregate2.creationTs != null

        1 * commandGateway.send({ it instanceof PrepareCausalCommand && it.rootAggregateId == 11 }) >> null
        1 * commandGateway.send({ it instanceof PrepareCausalCommand && it.rootAggregateId == 12 }) >> null
        1 * commandGateway.send({ it instanceof CommitCausalCommand && it.rootAggregateId == 11 }) >> {
            committedAggregateIds << 11
            null
        }
        1 * commandGateway.send({ it instanceof CommitCausalCommand && it.rootAggregateId == 12 }) >> {
            throw new RuntimeException('commit phase-2 failure on second aggregate')
        }
        1 * commandGateway.send({ it instanceof AbortCausalCommand && it.rootAggregateId == 11 }) >> {
            committedAggregateIds.remove((Object) 11)
            null
        }
        1 * commandGateway.send({ it instanceof AbortCausalCommand && it.rootAggregateId == 12 }) >> null
    }

    private static void setField(Object target, String fieldName, Object value) {
        def field = target.class.getDeclaredField(fieldName)
        field.accessible = true
        field.set(target, value)
    }

    static class TestCausalAggregate extends Aggregate implements CausalAggregate {
        TestCausalAggregate(Integer aggregateId, String aggregateType) {
            super(aggregateId)
            setAggregateType(aggregateType)
        }

        @Override
        void verifyInvariants() {
            // No-op: these tests focus on command dispatch windows, not domain rules.
        }

        @Override
        Set<EventSubscription> getEventSubscriptions() {
            return [] as Set
        }

        @Override
        Aggregate mergeFields(Set<String> toCommitVersionChangedFields, Aggregate committedVersion, Set<String> committedVersionChangedFields) {
            return this
        }

        @Override
        Set<String[]> getIntentions() {
            return [] as Set
        }

        @Override
        Set<String> getMutableFields() {
            return [] as Set
        }
    }
}
