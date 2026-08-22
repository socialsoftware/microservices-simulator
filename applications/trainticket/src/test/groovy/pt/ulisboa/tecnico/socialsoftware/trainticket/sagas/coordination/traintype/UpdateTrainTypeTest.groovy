package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.coordination.traintype

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.aggregate.TrainTypeDto
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.aggregate.sagas.states.TrainTypeSagaState
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.coordination.sagas.UpdateTrainTypeFunctionalitySagas

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class UpdateTrainTypeTest extends TrainticketSpockTest {

    @Autowired
    CommandGateway commandGateway

    def "updateTrainType: success"() {
        // Spec: plan.md §2 TrainType - UpdateTrainType
        given: 'a train type exists'
        def trainTypeAggregateId = createTrainType(TRAIN_TYPE_NAME, TRAIN_TYPE_ECONOMY_CLASS_SEATS,
                TRAIN_TYPE_FIRST_CLASS_SEATS, TRAIN_TYPE_AVERAGE_SPEED)

        when:
        trainTypeFunctionalities.updateTrainType(trainTypeAggregateId,
                new TrainTypeDto(TRAIN_TYPE_NAME, TRAIN_TYPE_SEATS_ONE,
                        TRAIN_TYPE_SEATS_ZERO, TRAIN_TYPE_AVERAGE_SPEED_ONE))

        then: 'the traversal completes and releases the lock'
        sagaStateOf(trainTypeAggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    def "updateTrainType: getTrainTypeStep acquires IN_UPDATE_TRAIN_TYPE semantic lock"() {
        // Spec: plan.md §2 TrainType - UpdateTrainType; primary-aggregate lock acquisition
        given:
        def trainTypeAggregateId = createTrainType(TRAIN_TYPE_NAME, TRAIN_TYPE_ECONOMY_CLASS_SEATS,
                TRAIN_TYPE_FIRST_CLASS_SEATS, TRAIN_TYPE_AVERAGE_SPEED)
        def uow = unitOfWorkService.createUnitOfWork("updateTrainType")
        def func = new UpdateTrainTypeFunctionalitySagas(unitOfWorkService, trainTypeAggregateId,
                new TrainTypeDto(TRAIN_TYPE_NAME, TRAIN_TYPE_SEATS_ONE,
                        TRAIN_TYPE_SEATS_ZERO, TRAIN_TYPE_AVERAGE_SPEED_ONE),
                uow, commandGateway)
        func.executeUntilStep("getTrainTypeStep", uow)

        expect: 'acquire transition: NOT_IN_SAGA -> IN_UPDATE_TRAIN_TYPE'
        sagaStateOf(trainTypeAggregateId) == TrainTypeSagaState.IN_UPDATE_TRAIN_TYPE

        when:
        func.resumeWorkflow(uow)

        then: 'traversal completes back to NOT_IN_SAGA'
        noExceptionThrown()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
