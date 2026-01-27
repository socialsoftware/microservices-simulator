package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.answer;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.service.AnswerService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi.requestDtos.CreateAnswerRequestDto;

public class CreateAnswerFunctionalitySagas extends WorkflowFunctionality {
    private AnswerDto createdAnswerDto;
    private final AnswerService answerService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public CreateAnswerFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, AnswerService answerService, CreateAnswerRequestDto createRequest) {
        this.answerService = answerService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(createRequest, unitOfWork);
    }

    public void buildWorkflow(CreateAnswerRequestDto createRequest, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep createAnswerStep = new SagaSyncStep("createAnswerStep", () -> {
            AnswerDto createdAnswerDto = answerService.createAnswer(createRequest, unitOfWork);
            setCreatedAnswerDto(createdAnswerDto);
        });

        workflow.addStep(createAnswerStep);

    }

    public AnswerDto getCreatedAnswerDto() {
        return createdAnswerDto;
    }

    public void setCreatedAnswerDto(AnswerDto createdAnswerDto) {
        this.createdAnswerDto = createdAnswerDto;
    }
}
