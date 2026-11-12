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
class GetRoutesTest extends TrainticketSpockTest {

    def "getRoutes: success"() {
        // Spec: plan.md §4 Route - GetRoutes
        given: 'two routes exist'
        def firstAggregateId = createRoute(ROUTE_START_STATION_NAME, ROUTE_END_STATION_NAME)
        def secondAggregateId = createRoute(ROUTE_MIDDLE_STATION_NAME, ROUTE_OTHER_STATION_NAME)

        when:
        def result = routeFunctionalities.getRoutes()

        then: 'the saga returns a coherent DTO per route'
        result.size() == 2
        result.collect { it.aggregateId }.toSet() == [firstAggregateId, secondAggregateId].toSet()
        result.collect { it.startStationName }.toSet() ==
                [ROUTE_START_STATION_NAME, ROUTE_MIDDLE_STATION_NAME].toSet()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
