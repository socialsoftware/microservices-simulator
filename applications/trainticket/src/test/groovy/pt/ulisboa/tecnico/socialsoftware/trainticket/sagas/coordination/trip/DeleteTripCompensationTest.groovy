package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.coordination.trip

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
class DeleteTripCompensationTest extends TrainticketSpockTest {

    def tripAggregateId

    def setup() {
        loadBehaviorScripts()
        tripAggregateId = createTrip()
    }

    def cleanup() {
        impairmentService.cleanUpCounter()
        impairmentService.cleanDirectory()
    }

    def "deleteTrip: fault on deleteTripStep compensates the lock acquired by getTripStep"() {
        // Spec: plan.md §6 Trip - DeleteTrip; compensate transition
        when:
        tripFunctionalities.deleteTrip(tripAggregateId)

        then: 'the injected fault surfaces to the caller'
        thrown(SimulatorException)

        and: 'compensation released the semantic lock back to NOT_IN_SAGA'
        sagaStateOf(tripAggregateId) == GenericSagaState.NOT_IN_SAGA

        and: 'the delete never ran: read-back shows the pre-saga state'
        def reread = tripFunctionalities.getTripById(tripAggregateId)
        reread.tripNumber == TRIP_NUMBER
        reread.isActive()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
