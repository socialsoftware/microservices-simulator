package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.coordination.station

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.StationDto

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class CreateStationTest extends TrainticketSpockTest {

    def "createStation: success"() {
        // Spec: plan.md §1 Station - CreateStation
        given: 'a station request'
        def stationDto = new StationDto(STATION_NAME, STATION_STAY_TIME)

        when:
        def result = stationFunctionalities.createStation(stationDto)

        then: 'the saga returns a coherent station DTO'
        result.aggregateId != null
        result.name == STATION_NAME
        result.stayTime == STATION_STAY_TIME

        and: 'the saga left no lock behind'
        sagaStateOf(result.aggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
