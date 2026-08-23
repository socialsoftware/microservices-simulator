package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.coordination.route

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.RouteDto
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.RouteStationDto

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class CreateRouteTest extends TrainticketSpockTest {

    def "createRoute: success"() {
        // Spec: plan.md §4 Route - CreateRoute
        given: 'three stations, requested without their names'
        def startStationAggregateId = createStation(ROUTE_START_STATION_NAME, STATION_STAY_TIME)
        def middleStationAggregateId = createStation(ROUTE_MIDDLE_STATION_NAME, STATION_STAY_TIME)
        def endStationAggregateId = createStation(ROUTE_END_STATION_NAME, STATION_STAY_TIME)
        def requested = [
                new RouteStationDto(ROUTE_SEQUENCE_ZERO, startStationAggregateId, null, ROUTE_DISTANCE_ZERO),
                new RouteStationDto(ROUTE_SEQUENCE_ONE, middleStationAggregateId, null, ROUTE_DISTANCE_MIDDLE),
                new RouteStationDto(ROUTE_SEQUENCE_TWO, endStationAggregateId, null, ROUTE_DISTANCE_END)
        ].toSet()

        when:
        def result = routeFunctionalities.createRoute(
                new RouteDto(ROUTE_START_STATION_NAME, ROUTE_END_STATION_NAME, requested))

        then: 'the saga returns a coherent route DTO'
        result.aggregateId != null
        result.startStationName == ROUTE_START_STATION_NAME
        result.endStationName == ROUTE_END_STATION_NAME

        and: 'the data-assembly step seeded each station name from the Station it fetched'
        result.routeStations.sort { it.sequence }.collect { it.stationName } ==
                [ROUTE_START_STATION_NAME, ROUTE_MIDDLE_STATION_NAME, ROUTE_END_STATION_NAME]

        and: 'the saga left no lock behind'
        sagaStateOf(result.aggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    def "createRoute: STATIONS_EXIST - an unknown station aborts the saga"() {
        // Spec: plan.md §4 Route - CreateRoute; rule STATIONS_EXIST (P4a, enforced by the fetch)
        given:
        def startStationAggregateId = createStation(ROUTE_START_STATION_NAME, STATION_STAY_TIME)
        def requested = routeStationsOf([
                [startStationAggregateId, ROUTE_START_STATION_NAME, ROUTE_DISTANCE_ZERO],
                [NONEXISTENT_AGGREGATE_ID, ROUTE_END_STATION_NAME, ROUTE_DISTANCE_END]
        ])

        when:
        routeFunctionalities.createRoute(
                new RouteDto(ROUTE_START_STATION_NAME, ROUTE_END_STATION_NAME, requested))

        then:
        thrown(SimulatorException)
    }

    def "createRoute: STATIONS_EXIST - a deleted station aborts the saga"() {
        // Spec: plan.md §4 Route - CreateRoute; rule STATIONS_EXIST (P4a, enforced by the fetch)
        given:
        def startStationAggregateId = createStation(ROUTE_START_STATION_NAME, STATION_STAY_TIME)
        def endStationAggregateId = createStation(ROUTE_END_STATION_NAME, STATION_STAY_TIME)
        stationFunctionalities.deleteStation(endStationAggregateId)
        def requested = routeStationsOf([
                [startStationAggregateId, ROUTE_START_STATION_NAME, ROUTE_DISTANCE_ZERO],
                [endStationAggregateId, ROUTE_END_STATION_NAME, ROUTE_DISTANCE_END]
        ])

        when:
        routeFunctionalities.createRoute(
                new RouteDto(ROUTE_START_STATION_NAME, ROUTE_END_STATION_NAME, requested))

        then:
        thrown(SimulatorException)
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
