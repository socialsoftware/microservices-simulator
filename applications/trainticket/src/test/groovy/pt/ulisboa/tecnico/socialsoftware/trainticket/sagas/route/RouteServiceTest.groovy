package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.route

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
class RouteServiceTest extends TrainticketSpockTest {

    def "getRouteById: reads back the persisted route through a fresh UnitOfWork"() {
        // Spec: plan.md §4 Route - GetRouteById
        given:
        def startStationAggregateId = createStation(ROUTE_START_STATION_NAME, STATION_STAY_TIME)
        def middleStationAggregateId = createStation(ROUTE_MIDDLE_STATION_NAME, STATION_STAY_TIME)
        def endStationAggregateId = createStation(ROUTE_END_STATION_NAME, STATION_STAY_TIME)
        def routeAggregateId = createRoute(ROUTE_START_STATION_NAME, ROUTE_END_STATION_NAME,
                routeStationsOf([
                        [startStationAggregateId, ROUTE_START_STATION_NAME, ROUTE_DISTANCE_ZERO],
                        [middleStationAggregateId, ROUTE_MIDDLE_STATION_NAME, ROUTE_DISTANCE_MIDDLE],
                        [endStationAggregateId, ROUTE_END_STATION_NAME, ROUTE_DISTANCE_END]
                ]))

        when:
        def result = routeService.getRouteById(routeAggregateId,
                unitOfWorkService.createUnitOfWork("getRouteById"))

        then:
        result.aggregateId == routeAggregateId
        result.startStationName == ROUTE_START_STATION_NAME
        result.endStationName == ROUTE_END_STATION_NAME
        result.isActive()

        and: 'the ordered station list is carried by the DTO'
        def ordered = result.routeStations.sort { it.sequence }
        ordered.collect { it.sequence } == [ROUTE_SEQUENCE_ZERO, ROUTE_SEQUENCE_ONE, ROUTE_SEQUENCE_TWO]
        ordered.collect { it.stationAggregateId } ==
                [startStationAggregateId, middleStationAggregateId, endStationAggregateId]
        ordered.collect { it.stationName } ==
                [ROUTE_START_STATION_NAME, ROUTE_MIDDLE_STATION_NAME, ROUTE_END_STATION_NAME]
        ordered.collect { it.distanceFromStart } ==
                [ROUTE_DISTANCE_ZERO, ROUTE_DISTANCE_MIDDLE, ROUTE_DISTANCE_END]
    }

    def "getRouteById: unknown aggregate id"() {
        // Spec: plan.md §4 Route - GetRouteById; Path A (aggregateLoadAndRegisterRead)
        when:
        routeService.getRouteById(NONEXISTENT_AGGREGATE_ID,
                unitOfWorkService.createUnitOfWork("getRouteById"))

        then:
        thrown(SimulatorException)
    }

    def "getRoutes: returns every persisted route"() {
        // Spec: plan.md §4 Route - GetRoutes
        given:
        def firstAggregateId = createRoute(ROUTE_START_STATION_NAME, ROUTE_END_STATION_NAME)
        def secondAggregateId = createRoute(ROUTE_MIDDLE_STATION_NAME, ROUTE_OTHER_STATION_NAME)

        when:
        def result = routeService.getRoutes(unitOfWorkService.createUnitOfWork("getRoutes"))

        then:
        result.size() == 2
        def first = result.find { it.aggregateId == firstAggregateId }
        def second = result.find { it.aggregateId == secondAggregateId }
        first.startStationName == ROUTE_START_STATION_NAME
        first.endStationName == ROUTE_END_STATION_NAME
        first.routeStations.size() == 2
        second.startStationName == ROUTE_MIDDLE_STATION_NAME
        second.endStationName == ROUTE_OTHER_STATION_NAME
        second.routeStations.size() == 2
    }

    def "getRoutes: returns an empty list when no route exists"() {
        // Spec: plan.md §4 Route - GetRoutes
        when:
        def result = routeService.getRoutes(unitOfWorkService.createUnitOfWork("getRoutes"))

        then:
        result.isEmpty()
    }

    def "getRoutesByStation: returns only the routes stopping at the station"() {
        // Spec: plan.md §4 Route - GetRoutesByStation
        given:
        def sharedStationAggregateId = createStation(ROUTE_START_STATION_NAME, STATION_STAY_TIME)
        def middleStationAggregateId = createStation(ROUTE_MIDDLE_STATION_NAME, STATION_STAY_TIME)
        def endStationAggregateId = createStation(ROUTE_END_STATION_NAME, STATION_STAY_TIME)
        def otherStationAggregateId = createStation(ROUTE_OTHER_STATION_NAME, STATION_STAY_TIME)
        def routeThroughMiddle = createRoute(ROUTE_START_STATION_NAME, ROUTE_END_STATION_NAME,
                routeStationsOf([
                        [sharedStationAggregateId, ROUTE_START_STATION_NAME, ROUTE_DISTANCE_ZERO],
                        [middleStationAggregateId, ROUTE_MIDDLE_STATION_NAME, ROUTE_DISTANCE_MIDDLE],
                        [endStationAggregateId, ROUTE_END_STATION_NAME, ROUTE_DISTANCE_END]
                ]))
        def routeAvoidingMiddle = createRoute(ROUTE_START_STATION_NAME, ROUTE_OTHER_STATION_NAME,
                routeStationsOf([
                        [sharedStationAggregateId, ROUTE_START_STATION_NAME, ROUTE_DISTANCE_ZERO],
                        [otherStationAggregateId, ROUTE_OTHER_STATION_NAME, ROUTE_DISTANCE_END]
                ]))

        when:
        def viaMiddle = routeService.getRoutesByStation(middleStationAggregateId,
                unitOfWorkService.createUnitOfWork("getRoutesByStation"))
        def viaShared = routeService.getRoutesByStation(sharedStationAggregateId,
                unitOfWorkService.createUnitOfWork("getRoutesByStation"))

        then:
        viaMiddle.collect { it.aggregateId } == [routeThroughMiddle]
        viaShared.collect { it.aggregateId }.toSet() == [routeThroughMiddle, routeAvoidingMiddle].toSet()
    }

    def "getRoutesByStation: returns an empty list when no route stops at the station"() {
        // Spec: plan.md §4 Route - GetRoutesByStation
        given:
        createRoute(ROUTE_START_STATION_NAME, ROUTE_END_STATION_NAME)
        def unusedStationAggregateId = createStation(ROUTE_OTHER_STATION_NAME, STATION_STAY_TIME)

        when:
        def result = routeService.getRoutesByStation(unusedStationAggregateId,
                unitOfWorkService.createUnitOfWork("getRoutesByStation"))

        then:
        result.isEmpty()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
