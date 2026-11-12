package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.coordination.traintype

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.aggregate.TrainTypeDto

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class UpdateTrainTypeCompensationTest extends TrainticketSpockTest {

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

    def "updateTrainType: fault on updateTrainTypeStep compensates the lock acquired by getTrainTypeStep"() {
        // Spec: plan.md §2 TrainType - UpdateTrainType; compensate transition
        when:
        trainTypeFunctionalities.updateTrainType(trainTypeAggregateId,
                new TrainTypeDto(TRAIN_TYPE_NAME, TRAIN_TYPE_SEATS_ONE,
                        TRAIN_TYPE_SEATS_ZERO, TRAIN_TYPE_AVERAGE_SPEED_ONE))

        then: 'the injected fault surfaces to the caller'
        thrown(SimulatorException)

        and: 'compensation released the semantic lock back to NOT_IN_SAGA'
        sagaStateOf(trainTypeAggregateId) == GenericSagaState.NOT_IN_SAGA

        and: 'the mutation never ran: read-back shows the pre-saga state'
        def reread = trainTypeFunctionalities.getTrainTypeById(trainTypeAggregateId)
        reread.economyClassSeats == TRAIN_TYPE_ECONOMY_CLASS_SEATS
        reread.firstClassSeats == TRAIN_TYPE_FIRST_CLASS_SEATS
        reread.averageSpeed == TRAIN_TYPE_AVERAGE_SPEED
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
