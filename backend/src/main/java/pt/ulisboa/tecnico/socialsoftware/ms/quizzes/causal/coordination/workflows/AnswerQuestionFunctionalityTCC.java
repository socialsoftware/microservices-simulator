package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.workflows;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.aggregate.QuestionAnswerDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.aggregate.QuizAnswer;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.aggregate.QuizAnswerFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.service.QuizAnswerService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.service.QuestionService;

public class AnswerQuestionFunctionalityTCC extends WorkflowFunctionality {
    private QuestionDto questionDto;
    private QuizAnswer oldQuizAnswer;
    private final QuizAnswerService quizAnswerService;
    private final QuestionService questionService;
    private final CausalUnitOfWorkService unitOfWorkService;

    public AnswerQuestionFunctionalityTCC(QuizAnswerService quizAnswerService, QuestionService questionService, CausalUnitOfWorkService unitOfWorkService, QuizAnswerFactory quizAnswerFactory, 
                            Integer quizAggregateId, Integer userAggregateId, QuestionAnswerDto userQuestionAnswerDto, CausalUnitOfWork unitOfWork) {
        this.quizAnswerService = quizAnswerService;
        this.questionService = questionService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(quizAggregateId, userAggregateId, userQuestionAnswerDto, quizAnswerFactory, unitOfWork);
    }

    public void buildWorkflow(Integer quizAggregateId, Integer userAggregateId, QuestionAnswerDto userQuestionAnswerDto, QuizAnswerFactory quizAnswerFactory, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep step = new SyncStep(() -> {
            QuestionDto questionDto = questionService.getQuestionById(userQuestionAnswerDto.getQuestionAggregateId(), unitOfWork);
            quizAnswerService.answerQuestion(quizAggregateId, userAggregateId, userQuestionAnswerDto, questionDto, unitOfWork);
        });
    
        workflow.addStep(step);
    }
    

    public QuestionDto getQuestionDto() {
        return questionDto;
    }

    public void setQuestionDto(QuestionDto questionDto) {
        this.questionDto = questionDto;
    }

    public QuizAnswer getOldQuizAnswer() {
        return oldQuizAnswer;
    }

    public void setOldQuizAnswer(QuizAnswer oldQuizAnswer) {
        this.oldQuizAnswer = oldQuizAnswer;
    }
}