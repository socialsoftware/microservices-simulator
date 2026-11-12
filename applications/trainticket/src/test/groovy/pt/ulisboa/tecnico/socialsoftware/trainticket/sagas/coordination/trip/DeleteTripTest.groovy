package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.coordination.trip

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.sagas.states.TripSagaState
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.coordination.sagas.DeleteTripFunctionalitySagas

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class DeleteTripTest extends TrainticketSpockTest {

    @Autowired
    CommandGateway commandGateway

    // No happy-path case: deleteTrip makes its own aggregate unresolvable, so sagaStateOf throws.
    // See docs/concepts/testing.md § T4 - "Exception — a functionality whose success makes
    // its own aggregate unresolvable". The delete's effect is asserted in TripServiceTest (T2).

    def "deleteTrip: getTripStep acquires IN_DELETE_TRIP semantic lock"() {
        // Spec: plan.md §6 Trip - DeleteTrip; primary-aggregate lock acquisition
        given:
        def tripAggregateId = createTrip()
        def uow = unitOfWorkService.createUnitOfWork("deleteTrip")
        def func = new DeleteTripFunctionalitySagas(unitOfWorkService, tripAggregateId,
                uow, commandGateway)
        func.executeUntilStep("getTripStep", uow)

        expect: 'acquire transition: NOT_IN_SAGA -> IN_DELETE_TRIP'
        sagaStateOf(tripAggregateId) == TripSagaState.IN_DELETE_TRIP

        when:
        func.resumeWorkflow(uow)

        then: 'traversal completes'
        noExceptionThrown()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
