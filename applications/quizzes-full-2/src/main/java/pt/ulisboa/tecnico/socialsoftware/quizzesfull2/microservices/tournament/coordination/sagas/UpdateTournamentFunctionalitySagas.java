package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.messaging.SagaCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.question.GetQuestionsByCourseCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.quiz.GetQuizByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.quiz.UpdateQuizCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.topic.GetTopicByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.tournament.GetTournamentByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.tournament.UpdateTournamentCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.QuizQuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.sagas.states.QuizSagaState;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.TournamentTopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.sagas.states.TournamentSagaState;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UpdateTournamentFunctionalitySagas extends WorkflowFunctionality {
    private TournamentDto tournamentDto;
    private QuizDto quizDto;
    private final List<TournamentTopicDto> topics = new ArrayList<>();
    private final List<QuizQuestionDto> selectedQuestions = new ArrayList<>();

    public UpdateTournamentFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                              Integer tournamentAggregateId, LocalDateTime startTime,
                                              LocalDateTime endTime, Integer numberOfQuestions,
                                              List<Integer> topicAggregateIds, SagaUnitOfWork unitOfWork,
                                              CommandGateway commandGateway) {
        buildWorkflow(unitOfWorkService, tournamentAggregateId, startTime, endTime, numberOfQuestions,
                topicAggregateIds, unitOfWork, commandGateway);
    }

    public void buildWorkflow(SagaUnitOfWorkService unitOfWorkService, Integer tournamentAggregateId,
                              LocalDateTime startTime, LocalDateTime endTime, Integer numberOfQuestions,
                              List<Integer> topicAggregateIds, SagaUnitOfWork unitOfWork,
                              CommandGateway commandGateway) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        // P4a prerequisite: re-seeds each TournamentTopic snapshot for the new topic set.
        SagaStep getTopicsStep = new SagaStep("getTopicsStep", () -> {
            for (Integer topicAggregateId : topicAggregateIds) {
                GetTopicByIdCommand cmd = new GetTopicByIdCommand(
                        unitOfWork, ServiceMapping.TOPIC.getServiceName(), topicAggregateId);
                TopicDto topicDto = (TopicDto) commandGateway.send(cmd);
                this.topics.add(new TournamentTopicDto(topicDto.getAggregateId(), topicDto.getName(),
                        topicDto.getVersion(), topicDto.getCourseAggregateId()));
            }
        });

        SagaStep getTournamentStep = new SagaStep("getTournamentStep", () -> {
            GetTournamentByIdCommand readCmd = new GetTournamentByIdCommand(
                    unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), tournamentAggregateId);
            SagaCommand sagaCommand = new SagaCommand(readCmd);
            sagaCommand.setSemanticLock(TournamentSagaState.IN_UPDATE_TOURNAMENT);
            this.tournamentDto = (TournamentDto) commandGateway.send(sagaCommand);
        });

        // The generated quiz's title is not a parameter of UpdateTournament, so it is re-read rather
        // than re-derived - a quiz renamed since creation keeps its name across the update.
        SagaStep getQuizStep = new SagaStep("getQuizStep", () -> {
            GetQuizByIdCommand cmd = new GetQuizByIdCommand(
                    unitOfWork, ServiceMapping.QUIZ.getServiceName(),
                    this.tournamentDto.getQuizAggregateId());
            this.quizDto = (QuizDto) commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(getTournamentStep)));

        // Re-runs the create path's selection against the new topic set, so
        // NUMBER_OF_QUESTIONS / QUIZ_TOPICS still holds after the update instead of decaying to
        // whatever the quiz happened to be asking. Same containment predicate as the create saga.
        SagaStep selectQuestionsStep = new SagaStep("selectQuestionsStep", () -> {
            GetQuestionsByCourseCommand cmd = new GetQuestionsByCourseCommand(
                    unitOfWork, ServiceMapping.QUESTION.getServiceName(),
                    this.tournamentDto.getCourseAggregateId());
            @SuppressWarnings("unchecked")
            List<QuestionDto> questionsOfCourse = (List<QuestionDto>) commandGateway.send(cmd);
            List<Integer> coveredTopicIds = this.topics.stream()
                    .map(TournamentTopicDto::getTopicAggregateId)
                    .toList();
            questionsOfCourse.stream()
                    .filter(questionDto -> coveredTopicIds.containsAll(questionDto.getTopics().stream()
                            .map(questionTopicDto -> questionTopicDto.getTopicAggregateId())
                            .toList()))
                    .limit(numberOfQuestions)
                    .forEach(questionDto -> this.selectedQuestions.add(new QuizQuestionDto(
                            questionDto.getAggregateId(), questionDto.getVersion(), questionDto.getTitle(),
                            questionDto.getContent())));
        }, new ArrayList<>(Arrays.asList(getTopicsStep, getTournamentStep)));

        SagaStep updateTournamentStep = new SagaStep("updateTournamentStep", () -> {
            UpdateTournamentCommand cmd = new UpdateTournamentCommand(
                    unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), tournamentAggregateId,
                    startTime, endTime, numberOfQuestions, this.topics, this.selectedQuestions);
            commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(selectQuestionsStep, getQuizStep)));

        // P4b: START_TIME_AVAILABLE_DATE / END_TIME_CONCLUSION_DATE and
        // NUMBER_OF_QUESTIONS / QUIZ_TOPICS hold on the update path because the quiz is re-sent the
        // tournament's own new dates and the freshly selected question list. No enforcement code.
        // The quiz is a foreign aggregate here, so it is guarded with forbiddenStates rather than
        // locked (R4 decision table).
        SagaStep updateQuizStep = new SagaStep("updateQuizStep", () -> {
            UpdateQuizCommand cmd = new UpdateQuizCommand(
                    unitOfWork, ServiceMapping.QUIZ.getServiceName(),
                    this.tournamentDto.getQuizAggregateId(), this.quizDto.getTitle(), startTime, endTime,
                    endTime, this.selectedQuestions);
            List<SagaAggregate.SagaState> forbiddenStates = new ArrayList<>();
            forbiddenStates.add(QuizSagaState.IN_UPDATE_QUIZ);
            SagaCommand sagaCommand = new SagaCommand(cmd);
            sagaCommand.setForbiddenStates(forbiddenStates);
            commandGateway.send(sagaCommand);
        }, new ArrayList<>(Arrays.asList(updateTournamentStep)));

        this.workflow.addStep(getTopicsStep);
        this.workflow.addStep(getTournamentStep);
        this.workflow.addStep(getQuizStep);
        this.workflow.addStep(selectQuestionsStep);
        this.workflow.addStep(updateTournamentStep);
        this.workflow.addStep(updateQuizStep);
    }

    public TournamentDto getTournamentDto() {
        return tournamentDto;
    }
}
