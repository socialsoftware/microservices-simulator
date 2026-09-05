package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.coordination.trip

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class GetTripsTest extends TrainticketSpockTest {

    def "getTrips: success"() {
        // Spec: plan.md §6 Trip - GetTrips
        given: 'two trips exist'
        def firstAggregateId = createTrip(TRIP_NUMBER,
                createRoute(ROUTE_START_STATION_NAME, ROUTE_END_STATION_NAME),
                createTrainType(TRAIN_TYPE_NAME),
                TRIP_START_TIME, TRIP_END_TIME)
        def secondAggregateId = createTrip(TRIP_NUMBER_TWO,
                createRoute(ROUTE_MIDDLE_STATION_NAME, ROUTE_OTHER_STATION_NAME),
                createTrainType(TRAIN_TYPE_NAME_TWO),
                TRIP_START_TIME, TRIP_END_TIME_TWO)

        when:
        def result = tripFunctionalities.getTrips()

        then: 'the saga returns a coherent DTO per trip'
        result.size() == 2
        result.collect { it.aggregateId }.toSet() == [firstAggregateId, secondAggregateId].toSet()
        result.collect { it.tripNumber }.toSet() == [TRIP_NUMBER, TRIP_NUMBER_TWO].toSet()
        result.collect { it.endTime }.toSet() == [TRIP_END_TIME, TRIP_END_TIME_TWO].toSet()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
