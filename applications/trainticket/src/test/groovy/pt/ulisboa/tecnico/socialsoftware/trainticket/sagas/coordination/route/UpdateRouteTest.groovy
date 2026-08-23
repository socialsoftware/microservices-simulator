package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.coordination.route

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.RouteDto
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.sagas.states.RouteSagaState
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.coordination.sagas.UpdateRouteFunctionalitySagas

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class UpdateRouteTest extends TrainticketSpockTest {

    @Autowired
    CommandGateway commandGateway

    def routeAggregateId
    def startStationAggregateId
    def endStationAggregateId

    def setup() {
        startStationAggregateId = createStation(ROUTE_START_STATION_NAME, STATION_STAY_TIME)
        endStationAggregateId = createStation(ROUTE_END_STATION_NAME, STATION_STAY_TIME)
        routeAggregateId = createRoute(ROUTE_START_STATION_NAME, ROUTE_END_STATION_NAME,
                routeStationsOf([
                        [startStationAggregateId, ROUTE_START_STATION_NAME, ROUTE_DISTANCE_ZERO],
                        [endStationAggregateId, ROUTE_END_STATION_NAME, ROUTE_DISTANCE_END]
                ]))
    }

    private RouteDto replacementRoute() {
        def middleStationAggregateId = createStation(ROUTE_MIDDLE_STATION_NAME, STATION_STAY_TIME)
        return new RouteDto(ROUTE_START_STATION_NAME, ROUTE_END_STATION_NAME, routeStationsOf([
                [startStationAggregateId, ROUTE_START_STATION_NAME, ROUTE_DISTANCE_ZERO],
                [middleStationAggregateId, ROUTE_MIDDLE_STATION_NAME, ROUTE_DISTANCE_MIDDLE],
                [endStationAggregateId, ROUTE_END_STATION_NAME, ROUTE_DISTANCE_END]
        ]))
    }

    def "updateRoute: success"() {
        // Spec: plan.md §4 Route - UpdateRoute
        when:
        routeFunctionalities.updateRoute(routeAggregateId, replacementRoute())

        then: 'the traversal completes and releases the lock'
        sagaStateOf(routeAggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    def "updateRoute: getRouteStep acquires IN_UPDATE_ROUTE semantic lock"() {
        // Spec: plan.md §4 Route - UpdateRoute; primary-aggregate lock acquisition
        given:
        def uow = unitOfWorkService.createUnitOfWork("updateRoute")
        def func = new UpdateRouteFunctionalitySagas(unitOfWorkService, routeAggregateId,
                replacementRoute(), uow, commandGateway)
        func.executeUntilStep("getRouteStep", uow)

        expect: 'acquire transition: NOT_IN_SAGA -> IN_UPDATE_ROUTE'
        sagaStateOf(routeAggregateId) == RouteSagaState.IN_UPDATE_ROUTE

        when:
        func.resumeWorkflow(uow)

        then: 'traversal completes back to NOT_IN_SAGA'
        noExceptionThrown()
    }

    def "updateRoute: STATIONS_EXIST - an unknown station aborts the saga"() {
        // Spec: plan.md §4 Route - UpdateRoute; rule STATIONS_EXIST (P4a, enforced by the fetch)
        given:
        def replacement = new RouteDto(ROUTE_START_STATION_NAME, ROUTE_END_STATION_NAME, routeStationsOf([
                [startStationAggregateId, ROUTE_START_STATION_NAME, ROUTE_DISTANCE_ZERO],
                [NONEXISTENT_AGGREGATE_ID, ROUTE_MIDDLE_STATION_NAME, ROUTE_DISTANCE_MIDDLE],
                [endStationAggregateId, ROUTE_END_STATION_NAME, ROUTE_DISTANCE_END]
        ]))

        when:
        routeFunctionalities.updateRoute(routeAggregateId, replacement)

        then:
        thrown(SimulatorException)
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
