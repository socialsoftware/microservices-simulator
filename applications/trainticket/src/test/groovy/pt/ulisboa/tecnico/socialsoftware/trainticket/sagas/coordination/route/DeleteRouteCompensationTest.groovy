package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.coordination.route

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class DeleteRouteCompensationTest extends TrainticketSpockTest {

    def routeAggregateId

    def setup() {
        loadBehaviorScripts()
        routeAggregateId = createRoute(ROUTE_START_STATION_NAME, ROUTE_END_STATION_NAME)
    }

    def cleanup() {
        impairmentService.cleanUpCounter()
        impairmentService.cleanDirectory()
    }

    def "deleteRoute: fault on deleteRouteStep compensates the lock acquired by getRouteStep"() {
        // Spec: plan.md §4 Route - DeleteRoute; compensate transition
        when:
        routeFunctionalities.deleteRoute(routeAggregateId)

        then: 'the injected fault surfaces to the caller'
        thrown(SimulatorException)

        and: 'compensation released the semantic lock back to NOT_IN_SAGA'
        sagaStateOf(routeAggregateId) == GenericSagaState.NOT_IN_SAGA

        and: 'the deletion never ran: the route is still readable'
        def reread = routeFunctionalities.getRouteById(routeAggregateId)
        reread.startStationName == ROUTE_START_STATION_NAME
        reread.isActive()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
