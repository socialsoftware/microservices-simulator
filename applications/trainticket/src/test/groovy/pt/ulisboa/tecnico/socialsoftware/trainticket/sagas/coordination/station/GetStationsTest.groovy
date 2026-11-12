package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.coordination.station

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class GetStationsTest extends TrainticketSpockTest {

    def "getStations: success"() {
        // Spec: plan.md §1 Station - GetStations
        given: 'two stations exist'
        def firstAggregateId = createStation(STATION_NAME, STATION_STAY_TIME)
        def secondAggregateId = createStation(STATION_NAME_TWO, STATION_STAY_TIME_TWO)

        when:
        def result = stationFunctionalities.getStations()

        then: 'the saga returns a coherent DTO per station'
        result.size() == 2
        result.collect { it.aggregateId }.toSet() == [firstAggregateId, secondAggregateId].toSet()
        result.collect { it.name }.toSet() == [STATION_NAME, STATION_NAME_TWO].toSet()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
