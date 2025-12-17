package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.answer;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.service.AnswerService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class GetAnswerByIdFunctionalitySagas extends WorkflowFunctionality {
    private AnswerDto answerDto;
    private final AnswerService answerService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public GetAnswerByIdFunctionalitySagas(AnswerService answerService, SagaUnitOfWorkService unitOfWorkService, Integer answerAggregateId, SagaUnitOfWork unitOfWork) {
        this.answerService = answerService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(answerAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer answerAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getAnswerStep = new SagaSyncStep("getAnswerStep", () -> {
            AnswerDto answerDto = answerService.getAnswerById(answerAggregateId, unitOfWork);
            setAnswerDto(answerDto);
        });

        workflow.addStep(getAnswerStep);
    }

    public AnswerDto getAnswerDto() {
        return answerDto;
    }

    public void setAnswerDto(AnswerDto answerDto) {
        this.answerDto = answerDto;
    }
}
