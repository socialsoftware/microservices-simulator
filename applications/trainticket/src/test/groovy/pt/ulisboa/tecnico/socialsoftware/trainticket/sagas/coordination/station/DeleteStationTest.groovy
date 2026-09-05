package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.coordination.station

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.sagas.states.StationSagaState
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.coordination.sagas.DeleteStationFunctionalitySagas

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class DeleteStationTest extends TrainticketSpockTest {

    @Autowired
    CommandGateway commandGateway

    // No happy-path case: deleteStation makes its own aggregate unresolvable, so sagaStateOf throws.
    // See docs/concepts/testing.md § T4 - "Exception — a functionality whose success makes
    // its own aggregate unresolvable". The delete's effect is asserted in StationServiceTest (T2).

    def "deleteStation: getStationStep acquires IN_DELETE_STATION semantic lock"() {
        // Spec: plan.md §1 Station - DeleteStation; primary-aggregate lock acquisition
        given:
        def stationAggregateId = createStation(STATION_NAME, STATION_STAY_TIME)
        def uow = unitOfWorkService.createUnitOfWork("deleteStation")
        def func = new DeleteStationFunctionalitySagas(unitOfWorkService, stationAggregateId,
                uow, commandGateway)
        func.executeUntilStep("getStationStep", uow)

        expect: 'acquire transition: NOT_IN_SAGA -> IN_DELETE_STATION'
        sagaStateOf(stationAggregateId) == StationSagaState.IN_DELETE_STATION

        when:
        func.resumeWorkflow(uow)

        then: 'traversal completes'
        noExceptionThrown()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
