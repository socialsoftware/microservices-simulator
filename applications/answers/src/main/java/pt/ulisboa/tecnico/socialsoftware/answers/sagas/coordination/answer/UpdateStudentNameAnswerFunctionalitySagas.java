package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.answer;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.service.AnswerService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import java.util.ArrayList;
import java.util.Arrays;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.states.AnswerSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.dtos.SagaAnswerDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.publish.UpdateStudentNameEvent;

public class UpdateStudentNameAnswerFunctionalitySagas extends WorkflowFunctionality {
    private AnswerService answerService;
    private SagaUnitOfWorkService sagaUnitOfWorkService;
    private Integer aggregateId;
    private Integer publisherAggregateId;
    private String updatedName;
    private Integer studentAggregateId;
    private Integer publisherAggregateVersion;
    private AnswerDto answer;
    private AnswerUserDto user;

    public UpdateStudentNameAnswerFunctionalitySagas(AnswerService answerService, SagaUnitOfWorkService sagaUnitOfWorkService, Integer aggregateId, Integer publisherAggregateId, String updatedName, Integer studentAggregateId, Integer publisherAggregateVersion, SagaUnitOfWork unitOfWork) {
        this.answerService = answerService;
        this.sagaUnitOfWorkService = sagaUnitOfWorkService;
        this.aggregateId = aggregateId;
        this.publisherAggregateId = publisherAggregateId;
        this.updatedName = updatedName;
        this.studentAggregateId = studentAggregateId;
        this.publisherAggregateVersion = publisherAggregateVersion;
        this.buildWorkflow(aggregateId, updatedName, answerId, unitOfWork);
    }

    public void buildWorkflow(Integer aggregateId, String updatedName, Integer answerId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, sagaUnitOfWorkService, unitOfWork);

        SagaSyncStep getAnswerStep = new SagaSyncStep("getAnswerStep", () -> {
            this.answer = answerService.getAnswerById(answerId, unitOfWork);
            sagaUnitOfWorkService.registerSagaState(answerId, AnswerSagaState.READ_ANSWER, unitOfWork);

        });

        getAnswerStep.registerCompensation(() -> {
            sagaUnitOfWorkService.registerSagaState(answerId, GenericSagaState.NOT_IN_SAGA, unitOfWork);

        }, unitOfWork);

        SagaSyncStep getUserStep = new SagaSyncStep("getUserStep", () -> {
            this.user = answer.getUser().stream().filter(p -> p.getUserAggregateId().equals(studentAggregateId)).findFirst().orElse(null);

        }, new ArrayList<>(Arrays.asList(getAnswerStep)));

        getUserStep.registerCompensation(() -> {
            sagaUnitOfWorkService.registerSagaState(studentAggregateId, GenericSagaState.NOT_IN_SAGA, unitOfWork);

        }, unitOfWork);

        SagaSyncStep updateNameStep = new SagaSyncStep("updateNameStep", () -> {
            answerService.updateStudentName(answerId, studentAggregateId, name, unitOfWork);

        }, new ArrayList<>(Arrays.asList(getAnswerStep, getUserStep)));

        this.workflow.addStep(getAnswerStep);
        this.workflow.addStep(getUserStep);
        this.workflow.addStep(updateNameStep);
    }

    public void setAnswer(AnswerDto answer) {
        this.answer = answer;
    }

    public AnswerDto getAnswer() {
        return answer;
    }

    public void setUser(AnswerUserDto user) {
        this.user = user;
    }

    public AnswerUserDto getUser() {
        return user;
    }
}