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
class GetStationByIdTest extends TrainticketSpockTest {

    def "getStationById: success"() {
        // Spec: plan.md §1 Station - GetStationById
        given: 'a station exists'
        def stationAggregateId = createStation(STATION_NAME, STATION_STAY_TIME)

        when:
        def result = stationFunctionalities.getStationById(stationAggregateId)

        then: 'the saga returns a coherent station DTO'
        result.aggregateId == stationAggregateId
        result.name == STATION_NAME
        result.stayTime == STATION_STAY_TIME
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
