package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.traintype

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class TrainTypeServiceTest extends TrainticketSpockTest {

    def "getTrainTypeById: reads back the persisted train type through a fresh UnitOfWork"() {
        // Spec: plan.md §2 TrainType - GetTrainTypeById
        given:
        def trainTypeAggregateId = createTrainType(TRAIN_TYPE_NAME, TRAIN_TYPE_ECONOMY_CLASS_SEATS,
                TRAIN_TYPE_FIRST_CLASS_SEATS, TRAIN_TYPE_AVERAGE_SPEED)

        when:
        def result = trainTypeService.getTrainTypeById(trainTypeAggregateId,
                unitOfWorkService.createUnitOfWork("getTrainTypeById"))

        then:
        result.aggregateId == trainTypeAggregateId
        result.name == TRAIN_TYPE_NAME
        result.economyClassSeats == TRAIN_TYPE_ECONOMY_CLASS_SEATS
        result.firstClassSeats == TRAIN_TYPE_FIRST_CLASS_SEATS
        result.averageSpeed == TRAIN_TYPE_AVERAGE_SPEED
        result.isActive()
    }

    def "getTrainTypeById: unknown aggregate id"() {
        // Spec: plan.md §2 TrainType - GetTrainTypeById; Path A (aggregateLoadAndRegisterRead)
        when:
        trainTypeService.getTrainTypeById(NONEXISTENT_AGGREGATE_ID,
                unitOfWorkService.createUnitOfWork("getTrainTypeById"))

        then:
        thrown(SimulatorException)
    }

    def "getTrainTypes: returns every persisted train type"() {
        // Spec: plan.md §2 TrainType - GetTrainTypes
        given:
        def firstAggregateId = createTrainType(TRAIN_TYPE_NAME, TRAIN_TYPE_ECONOMY_CLASS_SEATS,
                TRAIN_TYPE_FIRST_CLASS_SEATS, TRAIN_TYPE_AVERAGE_SPEED)
        def secondAggregateId = createTrainType(TRAIN_TYPE_NAME_TWO, TRAIN_TYPE_SEATS_ONE,
                TRAIN_TYPE_SEATS_ZERO, TRAIN_TYPE_AVERAGE_SPEED_ONE)

        when:
        def result = trainTypeService.getTrainTypes(unitOfWorkService.createUnitOfWork("getTrainTypes"))

        then:
        result.size() == 2
        def first = result.find { it.aggregateId == firstAggregateId }
        def second = result.find { it.aggregateId == secondAggregateId }
        first.name == TRAIN_TYPE_NAME
        first.economyClassSeats == TRAIN_TYPE_ECONOMY_CLASS_SEATS
        first.firstClassSeats == TRAIN_TYPE_FIRST_CLASS_SEATS
        first.averageSpeed == TRAIN_TYPE_AVERAGE_SPEED
        second.name == TRAIN_TYPE_NAME_TWO
        second.economyClassSeats == TRAIN_TYPE_SEATS_ONE
        second.firstClassSeats == TRAIN_TYPE_SEATS_ZERO
        second.averageSpeed == TRAIN_TYPE_AVERAGE_SPEED_ONE
    }

    def "getTrainTypes: returns an empty list when no train type exists"() {
        // Spec: plan.md §2 TrainType - GetTrainTypes
        when:
        def result = trainTypeService.getTrainTypes(unitOfWorkService.createUnitOfWork("getTrainTypes"))

        then:
        result.isEmpty()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
