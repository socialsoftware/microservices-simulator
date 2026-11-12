package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.coordination.traintype

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class DeleteTrainTypeCompensationTest extends TrainticketSpockTest {

    def trainTypeAggregateId

    def setup() {
        loadBehaviorScripts()
        trainTypeAggregateId = createTrainType(TRAIN_TYPE_NAME, TRAIN_TYPE_ECONOMY_CLASS_SEATS,
                TRAIN_TYPE_FIRST_CLASS_SEATS, TRAIN_TYPE_AVERAGE_SPEED)
    }

    def cleanup() {
        impairmentService.cleanUpCounter()
        impairmentService.cleanDirectory()
    }

    def "deleteTrainType: fault on deleteTrainTypeStep compensates the lock acquired by getTrainTypeStep"() {
        // Spec: plan.md §2 TrainType - DeleteTrainType; compensate transition
        when:
        trainTypeFunctionalities.deleteTrainType(trainTypeAggregateId)

        then: 'the injected fault surfaces to the caller'
        thrown(SimulatorException)

        and: 'compensation released the semantic lock back to NOT_IN_SAGA'
        sagaStateOf(trainTypeAggregateId) == GenericSagaState.NOT_IN_SAGA

        and: 'the soft delete never ran: read-back still resolves the train type'
        def reread = trainTypeFunctionalities.getTrainTypeById(trainTypeAggregateId)
        reread.name == TRAIN_TYPE_NAME
        reread.economyClassSeats == TRAIN_TYPE_ECONOMY_CLASS_SEATS
        reread.firstClassSeats == TRAIN_TYPE_FIRST_CLASS_SEATS
        reread.averageSpeed == TRAIN_TYPE_AVERAGE_SPEED
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
