package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.coordination.route

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.sagas.states.RouteSagaState
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.coordination.sagas.DeleteRouteFunctionalitySagas

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class DeleteRouteTest extends TrainticketSpockTest {

    @Autowired
    CommandGateway commandGateway

    // No happy-path case: deleteRoute makes its own aggregate unresolvable, so sagaStateOf throws.
    // See docs/concepts/testing.md § T4 - "Exception — a functionality whose success makes
    // its own aggregate unresolvable". The delete's effect is asserted in RouteServiceTest (T2).

    def "deleteRoute: getRouteStep acquires IN_DELETE_ROUTE semantic lock"() {
        // Spec: plan.md §4 Route - DeleteRoute; primary-aggregate lock acquisition
        given:
        def routeAggregateId = createRoute(ROUTE_START_STATION_NAME, ROUTE_END_STATION_NAME)
        def uow = unitOfWorkService.createUnitOfWork("deleteRoute")
        def func = new DeleteRouteFunctionalitySagas(unitOfWorkService, routeAggregateId,
                uow, commandGateway)
        func.executeUntilStep("getRouteStep", uow)

        expect: 'acquire transition: NOT_IN_SAGA -> IN_DELETE_ROUTE'
        sagaStateOf(routeAggregateId) == RouteSagaState.IN_DELETE_ROUTE

        when:
        func.resumeWorkflow(uow)

        then: 'traversal completes'
        noExceptionThrown()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
