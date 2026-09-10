package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.coordination.route

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class GetRoutesByStationTest extends TrainticketSpockTest {

    def "getRoutesByStation: success"() {
        // Spec: plan.md §4 Route - GetRoutesByStation
        given: 'two routes, only one of which stops at the middle station'
        def startStationAggregateId = createStation(ROUTE_START_STATION_NAME, STATION_STAY_TIME)
        def middleStationAggregateId = createStation(ROUTE_MIDDLE_STATION_NAME, STATION_STAY_TIME)
        def endStationAggregateId = createStation(ROUTE_END_STATION_NAME, STATION_STAY_TIME)
        def otherStationAggregateId = createStation(ROUTE_OTHER_STATION_NAME, STATION_STAY_TIME)
        def routeThroughMiddle = createRoute(ROUTE_START_STATION_NAME, ROUTE_END_STATION_NAME,
                routeStationsOf([
                        [startStationAggregateId, ROUTE_START_STATION_NAME, ROUTE_DISTANCE_ZERO],
                        [middleStationAggregateId, ROUTE_MIDDLE_STATION_NAME, ROUTE_DISTANCE_MIDDLE],
                        [endStationAggregateId, ROUTE_END_STATION_NAME, ROUTE_DISTANCE_END]
                ]))
        createRoute(ROUTE_START_STATION_NAME, ROUTE_OTHER_STATION_NAME,
                routeStationsOf([
                        [startStationAggregateId, ROUTE_START_STATION_NAME, ROUTE_DISTANCE_ZERO],
                        [otherStationAggregateId, ROUTE_OTHER_STATION_NAME, ROUTE_DISTANCE_END]
                ]))

        when:
        def result = routeFunctionalities.getRoutesByStation(middleStationAggregateId)

        then: 'the saga returns only the route stopping at that station'
        result.collect { it.aggregateId } == [routeThroughMiddle]
        result[0].startStationName == ROUTE_START_STATION_NAME
        result[0].endStationName == ROUTE_END_STATION_NAME
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
