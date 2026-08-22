package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.route

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketException
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.RouteDto
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.RouteStationDto
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.sagas.SagaRoute

import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketErrorMessage.ROUTE_DISTANCES_MONOTONIC
import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketErrorMessage.ROUTE_ENDPOINTS_MATCH_STATION_LIST
import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketErrorMessage.ROUTE_FIRST_DISTANCE_IS_ZERO
import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketErrorMessage.ROUTE_HAS_AT_LEAST_TWO_STATIONS
import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketErrorMessage.ROUTE_SEQUENCE_CONTIGUOUS
import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketErrorMessage.ROUTE_STATIONS_DISTINCT

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class RouteIntraInvariantTest extends TrainticketSpockTest {

    private static RouteStationDto stop(Integer sequence, Integer stationAggregateId, String stationName,
                                        Integer distanceFromStart) {
        return new RouteStationDto(sequence, stationAggregateId, stationName, distanceFromStart)
    }

    private static SagaRoute routeWith(String startStationName, String endStationName, RouteStationDto... stops) {
        return new SagaRoute(1, new RouteDto(startStationName, endStationName, stops.toList().toSet()))
    }

    private static SagaRoute threeStopRoute() {
        return routeWith(ROUTE_START_STATION_NAME, ROUTE_END_STATION_NAME,
                stop(ROUTE_SEQUENCE_ZERO, ROUTE_STATION_AGGREGATE_ID_ONE, ROUTE_START_STATION_NAME, ROUTE_DISTANCE_ZERO),
                stop(ROUTE_SEQUENCE_ONE, ROUTE_STATION_AGGREGATE_ID_TWO, ROUTE_MIDDLE_STATION_NAME, ROUTE_DISTANCE_MIDDLE),
                stop(ROUTE_SEQUENCE_TWO, ROUTE_STATION_AGGREGATE_ID_THREE, ROUTE_END_STATION_NAME, ROUTE_DISTANCE_END))
    }

    def "create route"() {
        // Spec: plan.md §4 Route - field list (startStationName, endStationName, routeStations x N)
        when:
        def route = threeStopRoute()
        route.verifyInvariants()

        then:
        route.aggregateId == 1
        route.startStationName == ROUTE_START_STATION_NAME
        route.endStationName == ROUTE_END_STATION_NAME
        route.state == Aggregate.AggregateState.ACTIVE
        route.sagaState == GenericSagaState.NOT_IN_SAGA

        and: 'the station list carries the sequence, the station reference, the cached name and the distance'
        def ordered = route.routeStations.sort { it.sequence }
        ordered.size() == 3
        ordered*.sequence == [ROUTE_SEQUENCE_ZERO, ROUTE_SEQUENCE_ONE, ROUTE_SEQUENCE_TWO]
        ordered*.stationAggregateId == [ROUTE_STATION_AGGREGATE_ID_ONE, ROUTE_STATION_AGGREGATE_ID_TWO, ROUTE_STATION_AGGREGATE_ID_THREE]
        ordered*.stationName == [ROUTE_START_STATION_NAME, ROUTE_MIDDLE_STATION_NAME, ROUTE_END_STATION_NAME]
        ordered*.distanceFromStart == [ROUTE_DISTANCE_ZERO, ROUTE_DISTANCE_MIDDLE, ROUTE_DISTANCE_END]
    }

    def "route: ROUTE_HAS_AT_LEAST_TWO_STATIONS on-point - two stations"() {
        // Spec: plan.md §3.1 - ROUTE_HAS_AT_LEAST_TWO_STATIONS, last satisfying size
        given:
        def route = routeWith(ROUTE_START_STATION_NAME, ROUTE_END_STATION_NAME,
                stop(ROUTE_SEQUENCE_ZERO, ROUTE_STATION_AGGREGATE_ID_ONE, ROUTE_START_STATION_NAME, ROUTE_DISTANCE_ZERO),
                stop(ROUTE_SEQUENCE_ONE, ROUTE_STATION_AGGREGATE_ID_TWO, ROUTE_END_STATION_NAME, ROUTE_DISTANCE_END))

        when:
        route.verifyInvariants()

        then:
        notThrown(TrainticketException)
        route.routeStations.size() == 2
    }

    def "route: ROUTE_HAS_AT_LEAST_TWO_STATIONS off-point - one station"() {
        // Spec: plan.md §3.1 - ROUTE_HAS_AT_LEAST_TWO_STATIONS, first violating size
        given:
        def route = routeWith(ROUTE_START_STATION_NAME, ROUTE_START_STATION_NAME,
                stop(ROUTE_SEQUENCE_ZERO, ROUTE_STATION_AGGREGATE_ID_ONE, ROUTE_START_STATION_NAME, ROUTE_DISTANCE_ZERO))

        when:
        route.verifyInvariants()

        then:
        def ex = thrown(TrainticketException)
        ex.message == ROUTE_HAS_AT_LEAST_TWO_STATIONS
    }

    def "route: ROUTE_SEQUENCE_CONTIGUOUS violation - a sequence number is skipped"() {
        // Spec: plan.md §3.1 - ROUTE_SEQUENCE_CONTIGUOUS
        given:
        def route = routeWith(ROUTE_START_STATION_NAME, ROUTE_END_STATION_NAME,
                stop(ROUTE_SEQUENCE_ZERO, ROUTE_STATION_AGGREGATE_ID_ONE, ROUTE_START_STATION_NAME, ROUTE_DISTANCE_ZERO),
                stop(ROUTE_SEQUENCE_ONE, ROUTE_STATION_AGGREGATE_ID_TWO, ROUTE_MIDDLE_STATION_NAME, ROUTE_DISTANCE_MIDDLE),
                stop(ROUTE_SEQUENCE_THREE, ROUTE_STATION_AGGREGATE_ID_THREE, ROUTE_END_STATION_NAME, ROUTE_DISTANCE_END))

        when:
        route.verifyInvariants()

        then:
        def ex = thrown(TrainticketException)
        ex.message == ROUTE_SEQUENCE_CONTIGUOUS
    }

    def "route: ROUTE_SEQUENCE_CONTIGUOUS violation - a sequence number is repeated"() {
        // Spec: plan.md §3.1 - ROUTE_SEQUENCE_CONTIGUOUS, the multiset half of the predicate
        given:
        def route = routeWith(ROUTE_START_STATION_NAME, ROUTE_END_STATION_NAME,
                stop(ROUTE_SEQUENCE_ZERO, ROUTE_STATION_AGGREGATE_ID_ONE, ROUTE_START_STATION_NAME, ROUTE_DISTANCE_ZERO),
                stop(ROUTE_SEQUENCE_ONE, ROUTE_STATION_AGGREGATE_ID_TWO, ROUTE_MIDDLE_STATION_NAME, ROUTE_DISTANCE_MIDDLE),
                stop(ROUTE_SEQUENCE_ONE, ROUTE_STATION_AGGREGATE_ID_THREE, ROUTE_END_STATION_NAME, ROUTE_DISTANCE_END))

        when:
        route.verifyInvariants()

        then:
        def ex = thrown(TrainticketException)
        ex.message == ROUTE_SEQUENCE_CONTIGUOUS
    }

    def "route: ROUTE_FIRST_DISTANCE_IS_ZERO off-point - the origin sits one unit from the start"() {
        // Spec: plan.md §3.1 - ROUTE_FIRST_DISTANCE_IS_ZERO, first violating value
        given:
        def route = routeWith(ROUTE_START_STATION_NAME, ROUTE_END_STATION_NAME,
                stop(ROUTE_SEQUENCE_ZERO, ROUTE_STATION_AGGREGATE_ID_ONE, ROUTE_START_STATION_NAME, ROUTE_DISTANCE_ONE),
                stop(ROUTE_SEQUENCE_ONE, ROUTE_STATION_AGGREGATE_ID_TWO, ROUTE_END_STATION_NAME, ROUTE_DISTANCE_MIDDLE))

        when:
        route.verifyInvariants()

        then:
        def ex = thrown(TrainticketException)
        ex.message == ROUTE_FIRST_DISTANCE_IS_ZERO
    }

    def "route: ROUTE_DISTANCES_MONOTONIC on-point - the next station is one unit further"() {
        // Spec: plan.md §3.1 - ROUTE_DISTANCES_MONOTONIC, smallest satisfying increment
        given:
        def route = routeWith(ROUTE_START_STATION_NAME, ROUTE_END_STATION_NAME,
                stop(ROUTE_SEQUENCE_ZERO, ROUTE_STATION_AGGREGATE_ID_ONE, ROUTE_START_STATION_NAME, ROUTE_DISTANCE_ZERO),
                stop(ROUTE_SEQUENCE_ONE, ROUTE_STATION_AGGREGATE_ID_TWO, ROUTE_END_STATION_NAME, ROUTE_DISTANCE_ONE))

        when:
        route.verifyInvariants()

        then:
        notThrown(TrainticketException)
    }

    def "route: ROUTE_DISTANCES_MONOTONIC off-point - the next station is at the same distance"() {
        // Spec: plan.md §3.1 - ROUTE_DISTANCES_MONOTONIC, first violating value
        given:
        def route = routeWith(ROUTE_START_STATION_NAME, ROUTE_END_STATION_NAME,
                stop(ROUTE_SEQUENCE_ZERO, ROUTE_STATION_AGGREGATE_ID_ONE, ROUTE_START_STATION_NAME, ROUTE_DISTANCE_ZERO),
                stop(ROUTE_SEQUENCE_ONE, ROUTE_STATION_AGGREGATE_ID_TWO, ROUTE_END_STATION_NAME, ROUTE_DISTANCE_ZERO))

        when:
        route.verifyInvariants()

        then:
        def ex = thrown(TrainticketException)
        ex.message == ROUTE_DISTANCES_MONOTONIC
    }

    def "route: ROUTE_STATIONS_DISTINCT violation - the same station is visited twice"() {
        // Spec: plan.md §3.1 - ROUTE_STATIONS_DISTINCT
        given:
        def route = routeWith(ROUTE_START_STATION_NAME, ROUTE_START_STATION_NAME,
                stop(ROUTE_SEQUENCE_ZERO, ROUTE_STATION_AGGREGATE_ID_ONE, ROUTE_START_STATION_NAME, ROUTE_DISTANCE_ZERO),
                stop(ROUTE_SEQUENCE_ONE, ROUTE_STATION_AGGREGATE_ID_ONE, ROUTE_START_STATION_NAME, ROUTE_DISTANCE_MIDDLE))

        when:
        route.verifyInvariants()

        then:
        def ex = thrown(TrainticketException)
        ex.message == ROUTE_STATIONS_DISTINCT
    }

    def "route: ROUTE_ENDPOINTS_MATCH_STATION_LIST violation - startStationName is not the first station"() {
        // Spec: plan.md §3.1 - ROUTE_ENDPOINTS_MATCH_STATION_LIST
        given:
        def route = routeWith(ROUTE_OTHER_STATION_NAME, ROUTE_END_STATION_NAME,
                stop(ROUTE_SEQUENCE_ZERO, ROUTE_STATION_AGGREGATE_ID_ONE, ROUTE_START_STATION_NAME, ROUTE_DISTANCE_ZERO),
                stop(ROUTE_SEQUENCE_ONE, ROUTE_STATION_AGGREGATE_ID_TWO, ROUTE_END_STATION_NAME, ROUTE_DISTANCE_END))

        when:
        route.verifyInvariants()

        then:
        def ex = thrown(TrainticketException)
        ex.message == ROUTE_ENDPOINTS_MATCH_STATION_LIST
    }

    def "route: ROUTE_ENDPOINTS_MATCH_STATION_LIST violation - endStationName is not the last station"() {
        // Spec: plan.md §3.1 - ROUTE_ENDPOINTS_MATCH_STATION_LIST
        given:
        def route = routeWith(ROUTE_START_STATION_NAME, ROUTE_OTHER_STATION_NAME,
                stop(ROUTE_SEQUENCE_ZERO, ROUTE_STATION_AGGREGATE_ID_ONE, ROUTE_START_STATION_NAME, ROUTE_DISTANCE_ZERO),
                stop(ROUTE_SEQUENCE_ONE, ROUTE_STATION_AGGREGATE_ID_TWO, ROUTE_END_STATION_NAME, ROUTE_DISTANCE_END))

        when:
        route.verifyInvariants()

        then:
        def ex = thrown(TrainticketException)
        ex.message == ROUTE_ENDPOINTS_MATCH_STATION_LIST
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
