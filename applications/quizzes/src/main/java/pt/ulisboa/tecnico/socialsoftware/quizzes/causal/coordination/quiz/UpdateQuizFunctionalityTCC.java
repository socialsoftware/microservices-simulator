package pt.ulisboa.tecnico.socialsoftware.quizzes.causal.coordination.quiz;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.Quiz;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizFactory;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizQuestion;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.service.QuizService;

import java.util.Set;
import java.util.stream.Collectors;

public class UpdateQuizFunctionalityTCC extends WorkflowFunctionality {
    private Quiz oldQuiz;
    private QuizDto updatedQuizDto;
    private final QuizService quizService;
    private final CausalUnitOfWorkService unitOfWorkService;

    public UpdateQuizFunctionalityTCC(QuizService quizService, CausalUnitOfWorkService unitOfWorkService, QuizFactory quizFactory, 
                        QuizDto quizDto, CausalUnitOfWork unitOfWork) {
        this.quizService = quizService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(quizDto, quizFactory, unitOfWork);
    }

    public void buildWorkflow(QuizDto quizDto, QuizFactory quizFactory, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep step = new SyncStep(() -> {
            Set<QuizQuestion> quizQuestions = quizDto.getQuestionDtos().stream().map(QuizQuestion::new).collect(Collectors.toSet());
            this.updatedQuizDto = quizService.updateQuiz(quizDto, quizQuestions, unitOfWork);
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