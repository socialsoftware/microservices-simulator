package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.coordination.trip

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.TripDto

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class UpdateTripCompensationTest extends TrainticketSpockTest {

    def tripAggregateId

    def setup() {
        loadBehaviorScripts()
        tripAggregateId = createTrip()
    }

    def cleanup() {
        impairmentService.cleanUpCounter()
        impairmentService.cleanDirectory()
    }

    def "updateTrip: fault on updateTripStep compensates the lock acquired by getTripStep"() {
        // Spec: plan.md §6 Trip - UpdateTrip; compensate transition
        when:
        tripFunctionalities.updateTrip(tripAggregateId,
                new TripDto(TRIP_NUMBER, TRIP_ROUTE_AGGREGATE_ID, TRIP_TRAIN_TYPE_AGGREGATE_ID,
                        TRIP_START_TIME, TRIP_END_TIME_TWO))

        then: 'the injected fault surfaces to the caller'
        thrown(SimulatorException)

        and: 'compensation released the semantic lock back to NOT_IN_SAGA'
        sagaStateOf(tripAggregateId) == GenericSagaState.NOT_IN_SAGA

        and: 'the mutation never ran: read-back shows the pre-saga state'
        def reread = tripFunctionalities.getTripById(tripAggregateId)
        reread.startTime == TRIP_START_TIME
        reread.endTime == TRIP_END_TIME
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
