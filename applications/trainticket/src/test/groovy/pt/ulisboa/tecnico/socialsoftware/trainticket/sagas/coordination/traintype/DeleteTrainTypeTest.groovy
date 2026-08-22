package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.coordination.traintype

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.aggregate.sagas.states.TrainTypeSagaState
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.coordination.sagas.DeleteTrainTypeFunctionalitySagas

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class DeleteTrainTypeTest extends TrainticketSpockTest {

    @Autowired
    CommandGateway commandGateway

    def "deleteTrainType: success"() {
        // Spec: plan.md §2 TrainType - DeleteTrainType (soft delete)
        given: 'a train type exists'
        def trainTypeAggregateId = createTrainType(TRAIN_TYPE_NAME, TRAIN_TYPE_ECONOMY_CLASS_SEATS,
                TRAIN_TYPE_FIRST_CLASS_SEATS, TRAIN_TYPE_AVERAGE_SPEED)

        when:
        trainTypeFunctionalities.deleteTrainType(trainTypeAggregateId)

        and: 'attempt to load the now-deleted aggregate'
        unitOfWorkService.aggregateLoadAndRegisterRead(trainTypeAggregateId,
                unitOfWorkService.createUnitOfWork("check"))

        then: 'DELETED aggregate is not loadable'
        thrown(SimulatorException)
    }

    def "deleteTrainType: getTrainTypeStep acquires IN_DELETE_TRAIN_TYPE semantic lock"() {
        // Spec: plan.md §2 TrainType - DeleteTrainType; primary-aggregate lock acquisition
        given:
        def trainTypeAggregateId = createTrainType(TRAIN_TYPE_NAME, TRAIN_TYPE_ECONOMY_CLASS_SEATS,
                TRAIN_TYPE_FIRST_CLASS_SEATS, TRAIN_TYPE_AVERAGE_SPEED)
        def uow = unitOfWorkService.createUnitOfWork("deleteTrainType")
        def func = new DeleteTrainTypeFunctionalitySagas(unitOfWorkService, trainTypeAggregateId,
                uow, commandGateway)
        func.executeUntilStep("getTrainTypeStep", uow)

        expect: 'acquire transition: NOT_IN_SAGA -> IN_DELETE_TRAIN_TYPE'
        sagaStateOf(trainTypeAggregateId) == TrainTypeSagaState.IN_DELETE_TRAIN_TYPE

        when:
        func.resumeWorkflow(uow)

        then: 'traversal completes'
        noExceptionThrown()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
