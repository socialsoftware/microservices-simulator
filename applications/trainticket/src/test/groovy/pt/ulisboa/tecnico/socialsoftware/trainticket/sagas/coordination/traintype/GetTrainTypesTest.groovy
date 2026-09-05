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
class GetTrainTypesTest extends TrainticketSpockTest {

    def "getTrainTypes: success"() {
        // Spec: plan.md §2 TrainType - GetTrainTypes
        given: 'two train types exist'
        def firstAggregateId = createTrainType(TRAIN_TYPE_NAME, TRAIN_TYPE_ECONOMY_CLASS_SEATS,
                TRAIN_TYPE_FIRST_CLASS_SEATS, TRAIN_TYPE_AVERAGE_SPEED)
        def secondAggregateId = createTrainType(TRAIN_TYPE_NAME_TWO, TRAIN_TYPE_SEATS_ONE,
                TRAIN_TYPE_SEATS_ZERO, TRAIN_TYPE_AVERAGE_SPEED_ONE)

        when:
        def result = trainTypeFunctionalities.getTrainTypes()

        then: 'the saga returns a coherent DTO per train type'
        result.size() == 2
        result.collect { it.aggregateId }.toSet() == [firstAggregateId, secondAggregateId].toSet()
        result.collect { it.name }.toSet() == [TRAIN_TYPE_NAME, TRAIN_TYPE_NAME_TWO].toSet()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
