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
class GetTripByIdTest extends TrainticketSpockTest {

    def "getTripById: success"() {
        // Spec: plan.md §6 Trip - GetTripById
        given: 'a trip exists'
        def routeAggregateId = createRoute()
        def trainTypeAggregateId = createTrainType()
        def tripAggregateId = createTrip(TRIP_NUMBER, routeAggregateId, trainTypeAggregateId,
                TRIP_START_TIME, TRIP_END_TIME)

        when:
        def result = tripFunctionalities.getTripById(tripAggregateId)

        then: 'the saga returns a coherent trip DTO'
        result.aggregateId == tripAggregateId
        result.tripNumber == TRIP_NUMBER
        result.routeAggregateId == routeAggregateId
        result.trainTypeAggregateId == trainTypeAggregateId
        result.startTime == TRIP_START_TIME
        result.endTime == TRIP_END_TIME
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
