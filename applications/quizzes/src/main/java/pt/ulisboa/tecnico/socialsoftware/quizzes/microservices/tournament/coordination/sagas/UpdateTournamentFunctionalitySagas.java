package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.question.FindQuestionsByTopicIdsCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.quiz.UpdateGeneratedQuizCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.topic.GetTopicByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament.GetTournamentByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament.UpdateTournamentCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.Quiz;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.Tournament;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.sagas.states.TournamentSagaState;

import java.util.*;
import java.util.stream.Collectors;

public class UpdateTournamentFunctionalitySagas extends WorkflowFunctionality {
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;
    private TournamentDto originalTournamentDto;
    private HashSet<TopicDto> topics = new HashSet<TopicDto>();
    private Tournament tournament;
    private TournamentDto newTournamentDto;
    private QuizDto quizDto;
    private Quiz quiz;
    private List<QuestionDto> questionDtos;

    public UpdateTournamentFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, TournamentDto tournamentDto, Set<Integer> topicsAggregateIds, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(tournamentDto, topicsAggregateIds, unitOfWork);
    }

    public void buildWorkflow(TournamentDto tournamentDto, Set<Integer> topicsAggregateIds, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getOriginalTournamentStep = new SagaStep("getOriginalTournamentStep", () -> {
            List<SagaAggregate.SagaState> states = new ArrayList<>();
            states.add(TournamentSagaState.IN_UPDATE_TOURNAMENT);
            states.add(TournamentSagaState.IN_DELETE_TOURNAMENT);
            GetTournamentByIdCommand getTournamentByIdCommand = new GetTournamentByIdCommand(unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), tournamentDto.getAggregateId());
            getTournamentByIdCommand.setSemanticLock(TournamentSagaState.IN_UPDATE_TOURNAMENT);
            getTournamentByIdCommand.setForbiddenStates(states);
            TournamentDto originalTournamentDto = (TournamentDto) commandGateway.send(getTournamentByIdCommand);
            setOriginalTournamentDto(originalTournamentDto);
        });

        SagaStep getTopicsStep = new SagaStep("getTopicsStep", () -> {
            topicsAggregateIds.forEach(topicId -> {
                GetTopicByIdCommand getTopicByIdCommand = new GetTopicByIdCommand(unitOfWork, ServiceMapping.TOPIC.getServiceName(), topicId);
                TopicDto topic = (TopicDto) commandGateway.send(getTopicByIdCommand);
                this.addTopic(topic);
            });
        }, new ArrayList<>(Arrays.asList(getOriginalTournamentStep)));

        SagaStep updateTournamentStep = new SagaStep("updateTournamentStep", () -> {
            UpdateTournamentCommand updateTournamentCommand = new UpdateTournamentCommand(unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), tournamentDto, this.getTopicsDtos());
            this.newTournamentDto = (TournamentDto) commandGateway.send(updateTournamentCommand);
        }, new ArrayList<>(Arrays.asList(getTopicsStep)));

        updateTournamentStep.registerCompensation(() -> {
            UpdateTournamentCommand updateTournamentCommand = new UpdateTournamentCommand(unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), originalTournamentDto, originalTournamentDto.getTopics());
            commandGateway.send(updateTournamentCommand);
        }, unitOfWork);

        SagaStep findQuestionsByTopicIds = new SagaStep("findQuestionsByTopicIds", () -> {
            FindQuestionsByTopicIdsCommand findQuestionsByTopicIdsCommand = new FindQuestionsByTopicIdsCommand(unitOfWork, ServiceMapping.QUESTION.getServiceName(), new ArrayList<>(topicsAggregateIds));
            this.questionDtos = (List<QuestionDto>) commandGateway.send(findQuestionsByTopicIdsCommand);
        }, new ArrayList<>(Arrays.asList(updateTournamentStep)));

        SagaStep updateQuizStep = new SagaStep("updateQuizStep", () -> {
            QuizDto quizDto = new QuizDto();
            quizDto.setAggregateId(getNewTournamentDto().getQuiz().getAggregateId());
            quizDto.setAvailableDate(getNewTournamentDto().getStartTime());
            quizDto.setConclusionDate(getNewTournamentDto().getEndTime());
            quizDto.setResultsDate(getNewTournamentDto().getEndTime());
            setQuizDto(quizDto);

            if (topicsAggregateIds != null || getNewTournamentDto().getNumberOfQuestions() != null) {
                UpdateGeneratedQuizCommand updateGeneratedQuizCommand;
                updateGeneratedQuizCommand = new UpdateGeneratedQuizCommand(unitOfWork, ServiceMapping.QUIZ.getServiceName(), quizDto, Objects.requireNonNullElseGet(topicsAggregateIds, () -> getTopics().stream().map(TopicDto::getAggregateId).collect(Collectors.toSet())), this.newTournamentDto.getNumberOfQuestions(), this.questionDtos);
                commandGateway.send(updateGeneratedQuizCommand);
            }
        }, new ArrayList<>(Arrays.asList(updateTournamentStep, findQuestionsByTopicIds)));

        workflow.addStep(getOriginalTournamentStep);
        workflow.addStep(getTopicsStep);
        workflow.addStep(findQuestionsByTopicIds);
        workflow.addStep(updateTournamentStep);
        workflow.addStep(updateQuizStep);
    }

    public TournamentDto getOriginalTournamentDto() {
        return originalTournamentDto;
    }

    public void setOriginalTournamentDto(TournamentDto originalTournamentDto) {
        this.originalTournamentDto = originalTournamentDto;
    }

    public HashSet<TopicDto> getTopics() {
        return topics;
    }

    public void setTopics(HashSet<TopicDto> topics) {
        this.topics = topics;
    }

    public HashSet<TopicDto> getTopicsDtos() {
        HashSet<TopicDto> topicDtos = topics.stream().map(TopicDto -> (TopicDto) TopicDto).collect(Collectors.toCollection(HashSet::new));
        return topicDtos;
    }

    public void addTopic(TopicDto topic) {
        this.topics.add(topic);
    }

    public Tournament getTournament() {
        return tournament;
    }

    public void setTournament(Tournament tournament) {
        this.tournament = tournament;
    }

    public TournamentDto getNewTournamentDto() {
        return newTournamentDto;
    }

    public void setNewTournamentDto(TournamentDto newTournamentDto) {
        this.newTournamentDto = newTournamentDto;
    }

    public QuizDto getQuizDto() {
        return quizDto;
    }

    public void setQuizDto(QuizDto quizDto) {
        this.quizDto = quizDto;
    }

    public Quiz getQuiz() {
        return quiz;
    }

    public void setQuiz(Quiz quiz) {
        this.quiz = quiz;
    }
}