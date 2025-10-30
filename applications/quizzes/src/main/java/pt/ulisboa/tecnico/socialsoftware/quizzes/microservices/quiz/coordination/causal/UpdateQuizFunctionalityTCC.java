package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.causal;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.quiz.UpdateQuizCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.Quiz;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizFactory;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizQuestion;

import java.util.Set;
import java.util.stream.Collectors;

public class UpdateQuizFunctionalityTCC extends WorkflowFunctionality {
    private Quiz oldQuiz;
    private QuizDto updatedQuizDto;
    private final CausalUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public UpdateQuizFunctionalityTCC(CausalUnitOfWorkService unitOfWorkService, QuizFactory quizFactory,
            QuizDto quizDto, CausalUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(quizDto, quizFactory, unitOfWork);
    }

    public void buildWorkflow(QuizDto quizDto, QuizFactory quizFactory, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep step = new SyncStep(() -> {
            Set<QuizQuestion> quizQuestions = quizDto.getQuestionDtos().stream().map(QuizQuestion::new).collect(Collectors.toSet());
            UpdateQuizCommand UpdateQuizCommand = new UpdateQuizCommand(unitOfWork, ServiceMapping.QUIZ.getServiceName(), quizDto, quizQuestions);
            this.updatedQuizDto = (QuizDto) commandGateway.send(UpdateQuizCommand);
        });

        workflow.addStep(step);
    }

    public Quiz getOldQuiz() {
        return oldQuiz;
    }

    public void setOldQuiz(Quiz oldQuiz) {
        this.oldQuiz = oldQuiz;
    }

    public QuizDto getUpdatedQuizDto() {
        return updatedQuizDto;
    }

    public void setUpdatedQuizDto(QuizDto updatedQuizDto) {
        this.updatedQuizDto = updatedQuizDto;
    }
}