package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.trip

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketException
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.TripDto
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.sagas.SagaTrip

import java.time.LocalTime

import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketErrorMessage.TRIP_START_BEFORE_END

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class TripIntraInvariantTest extends TrainticketSpockTest {

    private static SagaTrip tripWith(LocalTime startTime, LocalTime endTime) {
        return new SagaTrip(1, new TripDto(TRIP_NUMBER, TRIP_ROUTE_AGGREGATE_ID,
                TRIP_TRAIN_TYPE_AGGREGATE_ID, startTime, endTime))
    }

    def "create trip"() {
        // Spec: plan.md §6 Trip - field list (tripNumber, routeAggregateId, trainTypeAggregateId, startTime, endTime)
        when:
        def trip = tripWith(TRIP_START_TIME, TRIP_END_TIME)
        trip.verifyInvariants()

        then:
        trip.aggregateId == 1
        trip.tripNumber == TRIP_NUMBER
        trip.routeAggregateId == TRIP_ROUTE_AGGREGATE_ID
        trip.trainTypeAggregateId == TRIP_TRAIN_TYPE_AGGREGATE_ID
        trip.startTime == TRIP_START_TIME
        trip.endTime == TRIP_END_TIME
        trip.state == Aggregate.AggregateState.ACTIVE
        trip.sagaState == GenericSagaState.NOT_IN_SAGA
    }

    def "trip: TRIP_START_BEFORE_END violation - the end time precedes the start time"() {
        // Spec: plan.md §3.1 - TRIP_START_BEFORE_END
        given:
        def trip = tripWith(TRIP_START_TIME, TRIP_END_TIME)
        trip.setEndTime(TRIP_END_TIME_BEFORE_START)

        when:
        trip.verifyInvariants()

        then:
        def ex = thrown(TrainticketException)
        ex.message == TRIP_START_BEFORE_END
    }

    def "trip: TRIP_START_BEFORE_END on-point - the end time is one tick after the start time"() {
        // Spec: plan.md §3.1 - TRIP_START_BEFORE_END, last satisfying value
        given:
        def trip = tripWith(TRIP_START_TIME, TRIP_END_TIME_ON_POINT)

        when:
        trip.verifyInvariants()

        then:
        notThrown(TrainticketException)
        trip.startTime == TRIP_START_TIME
        trip.endTime == TRIP_END_TIME_ON_POINT
    }

    def "trip: TRIP_START_BEFORE_END off-point - the end time equals the start time"() {
        // Spec: plan.md §3.1 - TRIP_START_BEFORE_END, first violating value
        given:
        def trip = tripWith(TRIP_START_TIME, TRIP_END_TIME_EQUAL_TO_START)

        when:
        trip.verifyInvariants()

        then:
        def ex = thrown(TrainticketException)
        ex.message == TRIP_START_BEFORE_END
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
