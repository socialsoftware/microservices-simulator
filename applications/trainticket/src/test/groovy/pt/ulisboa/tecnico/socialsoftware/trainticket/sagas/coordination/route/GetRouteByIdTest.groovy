package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.coordination.route

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class GetRouteByIdTest extends TrainticketSpockTest {

    def "getRouteById: success"() {
        // Spec: plan.md §4 Route - GetRouteById
        given: 'a route exists'
        def routeAggregateId = createRoute(ROUTE_START_STATION_NAME, ROUTE_END_STATION_NAME)

        when:
        def result = routeFunctionalities.getRouteById(routeAggregateId)

        then: 'the saga returns a coherent route DTO'
        result.aggregateId == routeAggregateId
        result.startStationName == ROUTE_START_STATION_NAME
        result.endStationName == ROUTE_END_STATION_NAME
        result.routeStations.size() == 2
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
