package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.trip

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
class TripServiceTest extends TrainticketSpockTest {

    def "getTripById: reads back the persisted trip through a fresh UnitOfWork"() {
        // Spec: plan.md §6 Trip - GetTripById
        given:
        def routeAggregateId = createRoute()
        def trainTypeAggregateId = createTrainType()
        def tripAggregateId = createTrip(TRIP_NUMBER, routeAggregateId, trainTypeAggregateId,
                TRIP_START_TIME, TRIP_END_TIME)

        when:
        def result = tripService.getTripById(tripAggregateId,
                unitOfWorkService.createUnitOfWork("getTripById"))

        then:
        result.aggregateId == tripAggregateId
        result.tripNumber == TRIP_NUMBER
        result.routeAggregateId == routeAggregateId
        result.trainTypeAggregateId == trainTypeAggregateId
        result.startTime == TRIP_START_TIME
        result.endTime == TRIP_END_TIME
        result.isActive()
    }

    def "getTripById: unknown aggregate id"() {
        // Spec: plan.md §6 Trip - GetTripById; Path A (aggregateLoadAndRegisterRead)
        when:
        tripService.getTripById(NONEXISTENT_AGGREGATE_ID,
                unitOfWorkService.createUnitOfWork("getTripById"))

        then:
        thrown(SimulatorException)
    }

    def "getTrips: returns every persisted trip"() {
        // Spec: plan.md §6 Trip - GetTrips
        given:
        def firstAggregateId = createTrip(TRIP_NUMBER,
                createRoute(ROUTE_START_STATION_NAME, ROUTE_END_STATION_NAME),
                createTrainType(TRAIN_TYPE_NAME),
                TRIP_START_TIME, TRIP_END_TIME)
        def secondAggregateId = createTrip(TRIP_NUMBER_TWO,
                createRoute(ROUTE_MIDDLE_STATION_NAME, ROUTE_OTHER_STATION_NAME),
                createTrainType(TRAIN_TYPE_NAME_TWO),
                TRIP_START_TIME, TRIP_END_TIME_TWO)

        when:
        def result = tripService.getTrips(unitOfWorkService.createUnitOfWork("getTrips"))

        then:
        result.size() == 2
        def first = result.find { it.aggregateId == firstAggregateId }
        def second = result.find { it.aggregateId == secondAggregateId }
        first.tripNumber == TRIP_NUMBER
        first.startTime == TRIP_START_TIME
        first.endTime == TRIP_END_TIME
        second.tripNumber == TRIP_NUMBER_TWO
        second.startTime == TRIP_START_TIME
        second.endTime == TRIP_END_TIME_TWO
    }

    def "getTrips: no trip exists"() {
        // Spec: plan.md §6 Trip - GetTrips
        when:
        def result = tripService.getTrips(unitOfWorkService.createUnitOfWork("getTrips"))

        then:
        result.isEmpty()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
