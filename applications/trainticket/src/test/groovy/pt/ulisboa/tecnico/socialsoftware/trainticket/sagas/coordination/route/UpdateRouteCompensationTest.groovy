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

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class UpdateRouteCompensationTest extends TrainticketSpockTest {

    def routeAggregateId
    def startStationAggregateId
    def endStationAggregateId

    def setup() {
        loadBehaviorScripts()
        startStationAggregateId = createStation(ROUTE_START_STATION_NAME, STATION_STAY_TIME)
        endStationAggregateId = createStation(ROUTE_END_STATION_NAME, STATION_STAY_TIME)
        routeAggregateId = createRoute(ROUTE_START_STATION_NAME, ROUTE_END_STATION_NAME,
                routeStationsOf([
                        [startStationAggregateId, ROUTE_START_STATION_NAME, ROUTE_DISTANCE_ZERO],
                        [endStationAggregateId, ROUTE_END_STATION_NAME, ROUTE_DISTANCE_END]
                ]))
    }

    def cleanup() {
        impairmentService.cleanUpCounter()
        impairmentService.cleanDirectory()
    }

    def "updateRoute: fault on updateRouteStep compensates the lock acquired by getRouteStep"() {
        // Spec: plan.md §4 Route - UpdateRoute; compensate transition
        given: 'a replacement list inserting a middle stop'
        def middleStationAggregateId = createStation(ROUTE_MIDDLE_STATION_NAME, STATION_STAY_TIME)
        def replacement = new RouteDto(ROUTE_START_STATION_NAME, ROUTE_END_STATION_NAME, routeStationsOf([
                [startStationAggregateId, ROUTE_START_STATION_NAME, ROUTE_DISTANCE_ZERO],
                [middleStationAggregateId, ROUTE_MIDDLE_STATION_NAME, ROUTE_DISTANCE_MIDDLE],
                [endStationAggregateId, ROUTE_END_STATION_NAME, ROUTE_DISTANCE_END]
        ]))

        when:
        routeFunctionalities.updateRoute(routeAggregateId, replacement)

        then: 'the injected fault surfaces to the caller'
        thrown(SimulatorException)

        and: 'compensation released the semantic lock back to NOT_IN_SAGA'
        sagaStateOf(routeAggregateId) == GenericSagaState.NOT_IN_SAGA

        and: 'the mutation never ran: read-back shows the pre-saga state'
        def reread = routeFunctionalities.getRouteById(routeAggregateId)
        reread.routeStations.size() == 2
        reread.routeStations.collect { it.stationAggregateId }.toSet() ==
                [startStationAggregateId, endStationAggregateId].toSet()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
