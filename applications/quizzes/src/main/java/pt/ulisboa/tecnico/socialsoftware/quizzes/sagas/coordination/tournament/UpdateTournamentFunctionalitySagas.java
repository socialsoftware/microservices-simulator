package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.tournament;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.quiz.UpdateGeneratedQuizCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.topic.GetTopicByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament.GetTournamentByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament.UpdateTournamentCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.Quiz;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.service.QuizService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.service.TopicService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.Tournament;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.states.TournamentSagaState;

import java.util.*;
import java.util.stream.Collectors;

public class UpdateTournamentFunctionalitySagas extends WorkflowFunctionality{
    private TournamentDto originalTournamentDto;
    private HashSet<TopicDto> topics = new HashSet<TopicDto>();
    private Tournament tournament;
    private TournamentDto newTournamentDto;
    private QuizDto quizDto;
    private Quiz quiz;
    private final TournamentService tournamentService;
    private final TopicService topicService;
    private final QuizService quizService;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public UpdateTournamentFunctionalitySagas(TournamentService tournamentService, TopicService topicService, QuizService quizService, SagaUnitOfWorkService unitOfWorkService,
                                              TournamentDto tournamentDto, Set<Integer> topicsAggregateIds, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.tournamentService = tournamentService;
        this.topicService = topicService;
        this.quizService = quizService;
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(tournamentDto, topicsAggregateIds, unitOfWork);
    }

    public void buildWorkflow(TournamentDto tournamentDto, Set<Integer> topicsAggregateIds, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);
    
        SagaSyncStep getOriginalTournamentStep = new SagaSyncStep("getOriginalTournamentStep", () -> {
            List<SagaAggregate.SagaState> states = new ArrayList<>();
            states.add(TournamentSagaState.IN_UPDATE_TOURNAMENT);
            states.add(TournamentSagaState.IN_DELETE_TOURNAMENT);
            GetTournamentByIdCommand getTournamentByIdCommand = new GetTournamentByIdCommand(unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), tournamentDto.getAggregateId());
            getTournamentByIdCommand.setSemanticLock(TournamentSagaState.IN_UPDATE_TOURNAMENT);
            getTournamentByIdCommand.setForbiddenStates(states);
            TournamentDto originalTournamentDto = (TournamentDto) commandGateway.send(getTournamentByIdCommand);
//            unitOfWorkService.verifyAndRegisterSagaState(tournamentDto.getAggregateId(), TournamentSagaState.IN_UPDATE_TOURNAMENT, states, unitOfWork);
//            TournamentDto originalTournamentDto = (TournamentDto) tournamentService.getTournamentById(tournamentDto.getAggregateId(), unitOfWork);
            setOriginalTournamentDto(originalTournamentDto);
        });
    
        SagaSyncStep getTopicsStep = new SagaSyncStep("getTopicsStep", () -> {
            topicsAggregateIds.stream().forEach(topicId -> {
//                TopicDto topic = (TopicDto) topicService.getTopicById(topicId, unitOfWork);
                GetTopicByIdCommand getTopicByIdCommand = new GetTopicByIdCommand(unitOfWork, ServiceMapping.TOPIC.getServiceName(), topicId);
                TopicDto topic = (TopicDto) commandGateway.send(getTopicByIdCommand);
                this.addTopic(topic);
            });
        }, new ArrayList<>(Arrays.asList(getOriginalTournamentStep)));

        SagaSyncStep updateTournamentStep = new SagaSyncStep("updateTournamentStep", () -> {
//            this.newTournamentDto = tournamentService.updateTournament(tournamentDto, this.getTopicsDtos(), unitOfWork);
            UpdateTournamentCommand updateTournamentCommand = new UpdateTournamentCommand(unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), tournamentDto, this.getTopicsDtos());
            this.newTournamentDto = (TournamentDto) commandGateway.send(updateTournamentCommand);
        }, new ArrayList<>(Arrays.asList(getTopicsStep)));
    
        updateTournamentStep.registerCompensation(() -> {
//            tournamentService.updateTournament(originalTournamentDto, originalTournamentDto.getTopics(), unitOfWork);
            UpdateTournamentCommand updateTournamentCommand = new UpdateTournamentCommand(unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), originalTournamentDto, originalTournamentDto.getTopics());
            commandGateway.send(updateTournamentCommand);
        }, unitOfWork);

        SagaSyncStep updateQuizStep = new SagaSyncStep("updateQuizStep", () -> {
            QuizDto quizDto = new QuizDto();
            quizDto.setAggregateId(getNewTournamentDto().getQuiz().getAggregateId());
            quizDto.setAvailableDate(getNewTournamentDto().getStartTime());
            quizDto.setConclusionDate(getNewTournamentDto().getEndTime());
            quizDto.setResultsDate(getNewTournamentDto().getEndTime());
            setQuizDto(quizDto);
            
            if (topicsAggregateIds != null || getNewTournamentDto().getNumberOfQuestions() != null) {
                if (topicsAggregateIds == null) {
//                    quizService.updateGeneratedQuiz(quizDto, getTopics().stream()
//                            .map(TopicDto::getAggregateId)
//                            .collect(Collectors.toSet()), this.newTournamentDto.getNumberOfQuestions(), unitOfWork);
                    UpdateGeneratedQuizCommand updateGeneratedQuizCommand = new UpdateGeneratedQuizCommand(unitOfWork, ServiceMapping.QUIZ.getServiceName(), quizDto, getTopics().stream().map(TopicDto::getAggregateId).collect(Collectors.toSet()), this.newTournamentDto.getNumberOfQuestions());
                    commandGateway.send(updateGeneratedQuizCommand);
                } else {
//                    quizService.updateGeneratedQuiz(quizDto, topicsAggregateIds, this.newTournamentDto.getNumberOfQuestions(), unitOfWork);
                    UpdateGeneratedQuizCommand updateGeneratedQuizCommand = new UpdateGeneratedQuizCommand(unitOfWork, ServiceMapping.QUIZ.getServiceName(), quizDto, topicsAggregateIds, this.newTournamentDto.getNumberOfQuestions());
                    commandGateway.send(updateGeneratedQuizCommand);
                }
            }
        }, new ArrayList<>(Arrays.asList(updateTournamentStep)));
    
        workflow.addStep(getOriginalTournamentStep);
        workflow.addStep(getTopicsStep);
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

    public HashSet<TopicDto> getTopicsDtos() {
        HashSet<TopicDto> topicDtos = topics.stream()
            .map(TopicDto -> (TopicDto) TopicDto)
            .collect(Collectors.toCollection(HashSet::new));
        return topicDtos;
    }

    public void setTopics(HashSet<TopicDto> topics) {
        this.topics = topics;
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