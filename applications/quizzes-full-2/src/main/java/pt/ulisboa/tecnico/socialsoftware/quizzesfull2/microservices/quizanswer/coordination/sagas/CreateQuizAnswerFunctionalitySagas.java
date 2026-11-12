package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.execution.GetExecutionByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.question.GetQuestionByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.quiz.GetQuizByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.quizanswer.CreateQuizAnswerCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.user.GetUserByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.ExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate.OptionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.QuizQuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.aggregate.QuestionAnswerDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.aggregate.QuizAnswerDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.aggregate.UserDto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CreateQuizAnswerFunctionalitySagas extends WorkflowFunctionality {
    private QuizAnswerDto quizAnswerDto;
    private QuizDto quizDto;
    private UserDto userDto;
    private ExecutionDto executionDto;
    private final List<QuestionAnswerDto> questionAnswers = new ArrayList<>();

    public CreateQuizAnswerFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer quizAggregateId,
                                              Integer userAggregateId, Integer executionAggregateId,
                                              SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        buildWorkflow(unitOfWorkService, quizAggregateId, userAggregateId, executionAggregateId, unitOfWork,
                commandGateway);
    }

    public void buildWorkflow(SagaUnitOfWorkService unitOfWorkService, Integer quizAggregateId,
                              Integer userAggregateId, Integer executionAggregateId, SagaUnitOfWork unitOfWork,
                              CommandGateway commandGateway) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        // P3 COURSE_EXECUTION_SAME_QUIZ_COURSE_EXECUTION: supplies the quiz's own execution id, which
        // QuizAnswerService.createQuizAnswer() compares against the requested execution. Also seeds the
        // QuizAnswerQuiz snapshot and supplies the question list the QuestionAnswers are built from.
        SagaStep getQuizStep = new SagaStep("getQuizStep", () -> {
            GetQuizByIdCommand cmd = new GetQuizByIdCommand(
                    unitOfWork, ServiceMapping.QUIZ.getServiceName(), quizAggregateId);
            this.quizDto = (QuizDto) commandGateway.send(cmd);
        });

        // P4a prerequisite: seeds the QuizAnswerStudent snapshot with the student's cached name and version.
        SagaStep getUserStep = new SagaStep("getUserStep", () -> {
            GetUserByIdCommand cmd = new GetUserByIdCommand(
                    unitOfWork, ServiceMapping.USER.getServiceName(), userAggregateId);
            this.userDto = (UserDto) commandGateway.send(cmd);
        });

        // P4a prerequisite: seeds the QuizAnswerExecution snapshot with the execution's id and version.
        SagaStep getExecutionStep = new SagaStep("getExecutionStep", () -> {
            GetExecutionByIdCommand cmd = new GetExecutionByIdCommand(
                    unitOfWork, ServiceMapping.EXECUTION.getServiceName(), executionAggregateId);
            this.executionDto = (ExecutionDto) commandGateway.send(cmd);
        });

        // P4a support for the P1 rule ANSWER_MATCHES_CORRECT_OPTION: caches each question's correct
        // option key on its QuestionAnswer, so correctness is decided from local state at answer time.
        SagaStep getQuestionsStep = new SagaStep("getQuestionsStep", () -> {
            for (QuizQuestionDto quizQuestionDto : this.quizDto.getQuestions()) {
                GetQuestionByIdCommand cmd = new GetQuestionByIdCommand(unitOfWork,
                        ServiceMapping.QUESTION.getServiceName(), quizQuestionDto.getQuestionAggregateId());
                QuestionDto questionDto = (QuestionDto) commandGateway.send(cmd);
                QuestionAnswerDto questionAnswerDto = new QuestionAnswerDto();
                questionAnswerDto.setQuestionAggregateId(questionDto.getAggregateId());
                questionAnswerDto.setQuestionVersion(questionDto.getVersion());
                questionAnswerDto.setCorrectOptionKey(correctOptionKeyOf(questionDto));
                this.questionAnswers.add(questionAnswerDto);
            }
        }, new ArrayList<>(Arrays.asList(getQuizStep)));

        SagaStep createQuizAnswerStep = new SagaStep("createQuizAnswerStep", () -> {
            CreateQuizAnswerCommand cmd = new CreateQuizAnswerCommand(unitOfWork,
                    ServiceMapping.QUIZ_ANSWER.getServiceName(), this.quizDto, this.userDto, this.executionDto,
                    this.questionAnswers);
            this.quizAnswerDto = (QuizAnswerDto) commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(getQuizStep, getUserStep, getExecutionStep, getQuestionsStep)));

        this.workflow.addStep(getQuizStep);
        this.workflow.addStep(getUserStep);
        this.workflow.addStep(getExecutionStep);
        this.workflow.addStep(getQuestionsStep);
        this.workflow.addStep(createQuizAnswerStep);
    }

    // A question without a correct option leaves the key null; ANSWER_MATCHES_CORRECT_OPTION then holds
    // only for answers that pick no option key either, which is the intended reading of the predicate.
    private static Integer correctOptionKeyOf(QuestionDto questionDto) {
        return questionDto.getOptions().stream()
                .filter(option -> Boolean.TRUE.equals(option.isCorrect()))
                .map(OptionDto::getOptionKey)
                .findFirst()
                .orElse(null);
    }

    public QuizAnswerDto getQuizAnswerDto() {
        return quizAnswerDto;
    }
}
