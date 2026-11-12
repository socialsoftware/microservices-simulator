package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.coordination.trip

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.TripDto

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class CreateTripTest extends TrainticketSpockTest {

    def "createTrip: success"() {
        // Spec: plan.md §6 Trip - CreateTrip
        given: 'a route and a train type exist'
        def routeAggregateId = createRoute()
        def trainTypeAggregateId = createTrainType()

        when:
        def result = tripFunctionalities.createTrip(
                new TripDto(TRIP_NUMBER, routeAggregateId, trainTypeAggregateId,
                        TRIP_START_TIME, TRIP_END_TIME))

        then: 'the saga returns a coherent trip DTO'
        result.aggregateId != null
        result.tripNumber == TRIP_NUMBER
        result.routeAggregateId == routeAggregateId
        result.trainTypeAggregateId == trainTypeAggregateId
        result.startTime == TRIP_START_TIME
        result.endTime == TRIP_END_TIME

        and: 'the saga left no lock behind'
        sagaStateOf(result.aggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    def "createTrip: ROUTE_AND_TRAIN_TYPE_EXIST - an unknown route aborts the saga"() {
        // Spec: plan.md §6 Trip - CreateTrip; rule ROUTE_AND_TRAIN_TYPE_EXIST (P4a, enforced by the fetch)
        given:
        def trainTypeAggregateId = createTrainType()

        when:
        tripFunctionalities.createTrip(
                new TripDto(TRIP_NUMBER, NONEXISTENT_AGGREGATE_ID, trainTypeAggregateId,
                        TRIP_START_TIME, TRIP_END_TIME))

        then:
        thrown(SimulatorException)
    }

    def "createTrip: ROUTE_AND_TRAIN_TYPE_EXIST - a deleted route aborts the saga"() {
        // Spec: plan.md §6 Trip - CreateTrip; rule ROUTE_AND_TRAIN_TYPE_EXIST (P4a, enforced by the fetch)
        given:
        def routeAggregateId = createRoute()
        def trainTypeAggregateId = createTrainType()
        routeFunctionalities.deleteRoute(routeAggregateId)

        when:
        tripFunctionalities.createTrip(
                new TripDto(TRIP_NUMBER, routeAggregateId, trainTypeAggregateId,
                        TRIP_START_TIME, TRIP_END_TIME))

        then:
        thrown(SimulatorException)
    }

    def "createTrip: ROUTE_AND_TRAIN_TYPE_EXIST - an unknown train type aborts the saga"() {
        // Spec: plan.md §6 Trip - CreateTrip; rule ROUTE_AND_TRAIN_TYPE_EXIST (P4a, enforced by the fetch)
        given:
        def routeAggregateId = createRoute()

        when:
        tripFunctionalities.createTrip(
                new TripDto(TRIP_NUMBER, routeAggregateId, NONEXISTENT_AGGREGATE_ID,
                        TRIP_START_TIME, TRIP_END_TIME))

        then:
        thrown(SimulatorException)
    }

    def "createTrip: ROUTE_AND_TRAIN_TYPE_EXIST - a deleted train type aborts the saga"() {
        // Spec: plan.md §6 Trip - CreateTrip; rule ROUTE_AND_TRAIN_TYPE_EXIST (P4a, enforced by the fetch)
        given:
        def routeAggregateId = createRoute()
        def trainTypeAggregateId = createTrainType()
        trainTypeFunctionalities.deleteTrainType(trainTypeAggregateId)

        when:
        tripFunctionalities.createTrip(
                new TripDto(TRIP_NUMBER, routeAggregateId, trainTypeAggregateId,
                        TRIP_START_TIME, TRIP_END_TIME))

        then:
        thrown(SimulatorException)
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
