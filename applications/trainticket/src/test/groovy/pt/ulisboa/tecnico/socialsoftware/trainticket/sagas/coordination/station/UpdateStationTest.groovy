package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.coordination.station

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import org.springframework.beans.factory.annotation.Autowired
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.StationDto
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.sagas.states.StationSagaState
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.coordination.sagas.UpdateStationFunctionalitySagas

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class UpdateStationTest extends TrainticketSpockTest {

    @Autowired
    CommandGateway commandGateway

    def "updateStation: success"() {
        // Spec: plan.md §1 Station - UpdateStation
        given: 'a station exists'
        def stationAggregateId = createStation(STATION_NAME, STATION_STAY_TIME)

        when:
        stationFunctionalities.updateStation(stationAggregateId,
                new StationDto(STATION_NAME_TWO, STATION_STAY_TIME_TWO))

        then: 'the traversal completes and releases the lock'
        sagaStateOf(stationAggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    def "updateStation: getStationStep acquires IN_UPDATE_STATION semantic lock"() {
        // Spec: plan.md §1 Station - UpdateStation; primary-aggregate lock acquisition
        given:
        def stationAggregateId = createStation(STATION_NAME, STATION_STAY_TIME)
        def uow = unitOfWorkService.createUnitOfWork("updateStation")
        def func = new UpdateStationFunctionalitySagas(unitOfWorkService, stationAggregateId,
                new StationDto(STATION_NAME_TWO, STATION_STAY_TIME_TWO), uow, commandGateway)
        func.executeUntilStep("getStationStep", uow)

        expect: 'acquire transition: NOT_IN_SAGA -> IN_UPDATE_STATION'
        sagaStateOf(stationAggregateId) == StationSagaState.IN_UPDATE_STATION

        when:
        func.resumeWorkflow(uow)

        then: 'traversal completes back to NOT_IN_SAGA'
        noExceptionThrown()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
