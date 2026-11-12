package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.coordination.traintype

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.aggregate.TrainTypeDto

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class CreateTrainTypeTest extends TrainticketSpockTest {

    def "createTrainType: success"() {
        // Spec: plan.md §2 TrainType - CreateTrainType
        given: 'a train type request'
        def trainTypeDto = new TrainTypeDto(TRAIN_TYPE_NAME, TRAIN_TYPE_ECONOMY_CLASS_SEATS,
                TRAIN_TYPE_FIRST_CLASS_SEATS, TRAIN_TYPE_AVERAGE_SPEED)

        when:
        def result = trainTypeFunctionalities.createTrainType(trainTypeDto)

        then: 'the saga returns a coherent train type DTO'
        result.aggregateId != null
        result.name == TRAIN_TYPE_NAME
        result.economyClassSeats == TRAIN_TYPE_ECONOMY_CLASS_SEATS
        result.firstClassSeats == TRAIN_TYPE_FIRST_CLASS_SEATS
        result.averageSpeed == TRAIN_TYPE_AVERAGE_SPEED

        and: 'the saga left no lock behind'
        sagaStateOf(result.aggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
