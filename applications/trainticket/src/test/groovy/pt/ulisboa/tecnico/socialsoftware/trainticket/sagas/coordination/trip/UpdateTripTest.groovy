package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.coordination.trip

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.TripDto
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.sagas.states.TripSagaState
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.coordination.sagas.UpdateTripFunctionalitySagas

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class UpdateTripTest extends TrainticketSpockTest {

    @Autowired
    CommandGateway commandGateway

    def "updateTrip: success"() {
        // Spec: plan.md §6 Trip - UpdateTrip
        given: 'a trip exists'
        def tripAggregateId = createTrip()

        when:
        tripFunctionalities.updateTrip(tripAggregateId,
                new TripDto(TRIP_NUMBER, TRIP_ROUTE_AGGREGATE_ID, TRIP_TRAIN_TYPE_AGGREGATE_ID,
                        TRIP_START_TIME, TRIP_END_TIME_TWO))

        then: 'the traversal completes and releases the lock'
        sagaStateOf(tripAggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    def "updateTrip: getTripStep acquires IN_UPDATE_TRIP semantic lock"() {
        // Spec: plan.md §6 Trip - UpdateTrip; primary-aggregate lock acquisition
        given:
        def tripAggregateId = createTrip()
        def uow = unitOfWorkService.createUnitOfWork("updateTrip")
        def func = new UpdateTripFunctionalitySagas(unitOfWorkService, tripAggregateId,
                new TripDto(TRIP_NUMBER, TRIP_ROUTE_AGGREGATE_ID, TRIP_TRAIN_TYPE_AGGREGATE_ID,
                        TRIP_START_TIME, TRIP_END_TIME_TWO),
                uow, commandGateway)
        func.executeUntilStep("getTripStep", uow)

        expect: 'acquire transition: NOT_IN_SAGA -> IN_UPDATE_TRIP'
        sagaStateOf(tripAggregateId) == TripSagaState.IN_UPDATE_TRIP

        when:
        func.resumeWorkflow(uow)

        then: 'traversal completes back to NOT_IN_SAGA'
        noExceptionThrown()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
