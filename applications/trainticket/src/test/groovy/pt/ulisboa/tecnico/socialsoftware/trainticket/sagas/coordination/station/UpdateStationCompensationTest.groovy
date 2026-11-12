package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.coordination.station

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.StationDto

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class UpdateStationCompensationTest extends TrainticketSpockTest {

    def stationAggregateId

    def setup() {
        loadBehaviorScripts()
        stationAggregateId = createStation(STATION_NAME, STATION_STAY_TIME)
    }

    def cleanup() {
        impairmentService.cleanUpCounter()
        impairmentService.cleanDirectory()
    }

    def "updateStation: fault on updateStationStep compensates the lock acquired by getStationStep"() {
        // Spec: plan.md §1 Station - UpdateStation; compensate transition
        when:
        stationFunctionalities.updateStation(stationAggregateId,
                new StationDto(STATION_NAME_TWO, STATION_STAY_TIME_TWO))

        then: 'the injected fault surfaces to the caller'
        thrown(SimulatorException)

        and: 'compensation released the semantic lock back to NOT_IN_SAGA'
        sagaStateOf(stationAggregateId) == GenericSagaState.NOT_IN_SAGA

        and: 'the mutation never ran: read-back shows the pre-saga state'
        def reread = stationFunctionalities.getStationById(stationAggregateId)
        reread.name == STATION_NAME
        reread.stayTime == STATION_STAY_TIME
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
