package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.trip

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketException
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.TripDto

import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketErrorMessage.DUPLICATE_TRIP_NUMBER

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

    def "createTrip: persisted and readable through a fresh UnitOfWork"() {
        // Spec: plan.md §6 Trip - CreateTrip postconditions
        given:
        def routeAggregateId = createRoute()
        def trainTypeAggregateId = createTrainType()

        when:
        def result = tripService.createTrip(
                new TripDto(TRIP_NUMBER, routeAggregateId, trainTypeAggregateId,
                        TRIP_START_TIME, TRIP_END_TIME),
                unitOfWorkService.createUnitOfWork("createTrip"))

        then: 'read back through a second, fresh UnitOfWork'
        def readBack = tripService.getTripById(result.aggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        readBack.tripNumber == TRIP_NUMBER
        readBack.routeAggregateId == routeAggregateId
        readBack.trainTypeAggregateId == trainTypeAggregateId
        readBack.startTime == TRIP_START_TIME
        readBack.endTime == TRIP_END_TIME
        readBack.isActive()
    }

    def "createTrip: DUPLICATE_TRIP_NUMBER violation"() {
        // Spec: plan.md §6 Trip - rule UNIQUE_TRIP_NUMBER (P3, own table, create only)
        given: 'a trip already holds the trip number'
        createTrip(TRIP_NUMBER, createRoute(), createTrainType(), TRIP_START_TIME, TRIP_END_TIME)

        when:
        tripService.createTrip(
                new TripDto(TRIP_NUMBER,
                        createRoute(ROUTE_MIDDLE_STATION_NAME, ROUTE_OTHER_STATION_NAME),
                        createTrainType(TRAIN_TYPE_NAME_TWO),
                        TRIP_START_TIME, TRIP_END_TIME_TWO),
                unitOfWorkService.createUnitOfWork("createTrip"))

        then:
        def ex = thrown(TrainticketException)
        ex.message == DUPLICATE_TRIP_NUMBER
    }

    def "createTrip: UNIQUE_TRIP_NUMBER constrains active trips only"() {
        // Spec: plan.md §6 Trip - rule UNIQUE_TRIP_NUMBER (P3, own table, create only)
        given: 'the trip holding the trip number has been deleted'
        def deletedAggregateId = createTrip(TRIP_NUMBER, createRoute(), createTrainType(),
                TRIP_START_TIME, TRIP_END_TIME)
        tripService.deleteTrip(deletedAggregateId, unitOfWorkService.createUnitOfWork("deleteTrip"))

        when:
        def result = tripService.createTrip(
                new TripDto(TRIP_NUMBER,
                        createRoute(ROUTE_MIDDLE_STATION_NAME, ROUTE_OTHER_STATION_NAME),
                        createTrainType(TRAIN_TYPE_NAME_TWO),
                        TRIP_START_TIME, TRIP_END_TIME_TWO),
                unitOfWorkService.createUnitOfWork("createTrip"))

        then: 'the freed trip number is accepted'
        notThrown(TrainticketException)
        tripService.getTripById(result.aggregateId,
                unitOfWorkService.createUnitOfWork("check")).tripNumber == TRIP_NUMBER
    }

    def "updateTrip: new times persisted and readable through a fresh UnitOfWork"() {
        // Spec: plan.md §6 Trip - UpdateTrip postconditions
        given:
        def tripAggregateId = createTrip()

        when:
        tripService.updateTrip(tripAggregateId,
                new TripDto(TRIP_NUMBER, TRIP_ROUTE_AGGREGATE_ID, TRIP_TRAIN_TYPE_AGGREGATE_ID,
                        TRIP_START_TIME, TRIP_END_TIME_TWO),
                unitOfWorkService.createUnitOfWork("updateTrip"))

        then: 'read back through a second, fresh UnitOfWork'
        def readBack = tripService.getTripById(tripAggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        readBack.endTime == TRIP_END_TIME_TWO
        readBack.startTime == TRIP_START_TIME
    }

    def "updateTrip: unknown aggregate id"() {
        // Spec: plan.md §6 Trip - UpdateTrip; Path A (aggregateLoadAndRegisterRead)
        when:
        tripService.updateTrip(NONEXISTENT_AGGREGATE_ID,
                new TripDto(TRIP_NUMBER, TRIP_ROUTE_AGGREGATE_ID, TRIP_TRAIN_TYPE_AGGREGATE_ID,
                        TRIP_START_TIME, TRIP_END_TIME_TWO),
                unitOfWorkService.createUnitOfWork("updateTrip"))

        then:
        thrown(SimulatorException)
    }

    def "deleteTrip: the deleted trip is no longer loadable"() {
        // Spec: plan.md §6 Trip - DeleteTrip (soft delete)
        given:
        def tripAggregateId = createTrip()

        when:
        tripService.deleteTrip(tripAggregateId, unitOfWorkService.createUnitOfWork("deleteTrip"))

        and: 'read back through a second, fresh UnitOfWork'
        tripService.getTripById(tripAggregateId, unitOfWorkService.createUnitOfWork("check"))

        then:
        thrown(SimulatorException)
    }

    def "deleteTrip: unknown aggregate id"() {
        // Spec: plan.md §6 Trip - DeleteTrip; Path A (aggregateLoadAndRegisterRead)
        when:
        tripService.deleteTrip(NONEXISTENT_AGGREGATE_ID,
                unitOfWorkService.createUnitOfWork("deleteTrip"))

        then:
        thrown(SimulatorException)
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
