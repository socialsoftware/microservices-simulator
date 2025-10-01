package pt.ulisboa.tecnico.socialsoftware.quizzes.causal.coordination.quiz;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.quiz.GetAvailableQuizzesCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizDto;

import java.util.List;
import java.util.stream.Collectors;

public class GetAvailableQuizzesFunctionalityTCC extends WorkflowFunctionality {
    private List<QuizDto> availableQuizzes;
    private final CausalUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public GetAvailableQuizzesFunctionalityTCC(CausalUnitOfWorkService unitOfWorkService,
            Integer userAggregateId, Integer courseExecutionAggregateId, CausalUnitOfWork unitOfWork,
            CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(userAggregateId, courseExecutionAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer userAggregateId, Integer courseExecutionAggregateId,
            CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep step = new SyncStep(() -> {
            // this.availableQuizzes =
            // quizService.getAvailableQuizzes(courseExecutionAggregateId, unitOfWork);
            GetAvailableQuizzesCommand GetAvailableQuizzesCommand = new GetAvailableQuizzesCommand(unitOfWork,
                    ServiceMapping.QUIZ.getServiceName(), null, courseExecutionAggregateId);
            Object result = commandGateway.send(GetAvailableQuizzesCommand);
            List<?> list = (List<?>) result;
            this.availableQuizzes = list.stream().map(o -> (QuizDto) o).collect(Collectors.toList());
        });

        workflow.addStep(step);
    }

    public List<QuizDto> getAvailableQuizzes() {
        return availableQuizzes;
    }

    public void setAvailableQuizzes(List<QuizDto> availableQuizzes) {
        this.availableQuizzes = availableQuizzes;
    }
}
