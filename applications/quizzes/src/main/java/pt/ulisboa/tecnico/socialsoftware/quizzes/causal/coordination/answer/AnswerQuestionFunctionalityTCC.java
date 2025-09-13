package pt.ulisboa.tecnico.socialsoftware.quizzes.causal.coordination.answer;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.answer.AnswerQuestionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.question.GetQuestionByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.QuestionAnswerDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.QuizAnswer;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.QuizAnswerFactory;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.service.QuizAnswerService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.service.QuestionService;

public class AnswerQuestionFunctionalityTCC extends WorkflowFunctionality {
    private QuestionDto questionDto;
    private QuizAnswer oldQuizAnswer;
    private final QuizAnswerService quizAnswerService;
    private final QuestionService questionService;
    private final CausalUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public AnswerQuestionFunctionalityTCC(QuizAnswerService quizAnswerService, QuestionService questionService, CausalUnitOfWorkService unitOfWorkService, QuizAnswerFactory quizAnswerFactory,
                                          Integer quizAggregateId, Integer userAggregateId, QuestionAnswerDto userQuestionAnswerDto, CausalUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.quizAnswerService = quizAnswerService;
        this.questionService = questionService;
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(quizAggregateId, userAggregateId, userQuestionAnswerDto, quizAnswerFactory, unitOfWork);
    }

    public void buildWorkflow(Integer quizAggregateId, Integer userAggregateId, QuestionAnswerDto userQuestionAnswerDto, QuizAnswerFactory quizAnswerFactory, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep step = new SyncStep(() -> {
//            QuestionDto questionDto = questionService.getQuestionById(userQuestionAnswerDto.getQuestionAggregateId(), unitOfWork);
//            quizAnswerService.answerQuestion(quizAggregateId, userAggregateId, userQuestionAnswerDto, questionDto, unitOfWork);
            GetQuestionByIdCommand getQuestionByIdCommand = new GetQuestionByIdCommand(unitOfWork, ServiceMapping.QUESTION.getServiceName(), userQuestionAnswerDto.getQuestionAggregateId());
            QuestionDto questionDto = (QuestionDto) commandGateway.send(getQuestionByIdCommand);
            AnswerQuestionCommand answerQuestionCommand = new AnswerQuestionCommand(unitOfWork, ServiceMapping.ANSWER.getServiceName(), quizAggregateId, userAggregateId, userQuestionAnswerDto, questionDto);
            commandGateway.send(answerQuestionCommand);
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