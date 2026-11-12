package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.coordination.traintype

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class GetTrainTypeByIdTest extends TrainticketSpockTest {

    def "getTrainTypeById: success"() {
        // Spec: plan.md §2 TrainType - GetTrainTypeById
        given: 'a train type exists'
        def trainTypeAggregateId = createTrainType(TRAIN_TYPE_NAME, TRAIN_TYPE_ECONOMY_CLASS_SEATS,
                TRAIN_TYPE_FIRST_CLASS_SEATS, TRAIN_TYPE_AVERAGE_SPEED)

        when:
        def result = trainTypeFunctionalities.getTrainTypeById(trainTypeAggregateId)

        then: 'the saga returns a coherent train type DTO'
        result.aggregateId == trainTypeAggregateId
        result.name == TRAIN_TYPE_NAME
        result.economyClassSeats == TRAIN_TYPE_ECONOMY_CLASS_SEATS
        result.firstClassSeats == TRAIN_TYPE_FIRST_CLASS_SEATS
        result.averageSpeed == TRAIN_TYPE_AVERAGE_SPEED
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
