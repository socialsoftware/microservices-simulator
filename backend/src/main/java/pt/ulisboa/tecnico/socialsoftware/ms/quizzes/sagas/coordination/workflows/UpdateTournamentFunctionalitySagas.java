package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.Quiz;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.service.QuizService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.service.TopicService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.Tournament;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.SagaTopic;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.dtos.SagaQuizDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.dtos.SagaTopicDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.dtos.SagaTournamentDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.states.QuizSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.states.TopicSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.states.TournamentSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateTournamentFunctionalitySagas extends WorkflowFunctionality{
    private SagaTournamentDto originalTournamentDto;
    private HashSet<SagaTopicDto> topics = new HashSet<SagaTopicDto>();
    private Tournament tournament;
    private TournamentDto newTournamentDto;
    private QuizDto quizDto;
    private Quiz quiz;
    private final TournamentService tournamentService;
    private final TopicService topicService;
    private final QuizService quizService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public UpdateTournamentFunctionalitySagas(TournamentService tournamentService, TopicService topicService, QuizService quizService, SagaUnitOfWorkService unitOfWorkService, 
                                TournamentFactory tournamentFactory, QuizFactory quizFactory,
                                TournamentDto tournamentDto, Set<Integer> topicsAggregateIds, SagaUnitOfWork unitOfWork) {
        this.tournamentService = tournamentService;
        this.topicService = topicService;
        this.quizService = quizService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(tournamentDto, topicsAggregateIds, tournamentFactory, quizFactory, unitOfWork);
    }

    public void buildWorkflow(TournamentDto tournamentDto, Set<Integer> topicsAggregateIds, TournamentFactory tournamentFactory, QuizFactory quizFactory, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);
    
        SagaSyncStep getOriginalTournamentStep = new SagaSyncStep("getOriginalTournamentStep", () -> {
            unitOfWorkService.registerSagaState(tournamentDto.getAggregateId(), TournamentSagaState.IN_UPDATE_TOURNAMENT, unitOfWork);
            SagaTournamentDto originalTournamentDto = (SagaTournamentDto) tournamentService.getTournamentById(tournamentDto.getAggregateId(), unitOfWork);
            this.setOriginalTournamentDto(originalTournamentDto);
        });
    
        getOriginalTournamentStep.registerCompensation(() -> {
            unitOfWorkService.registerSagaState(originalTournamentDto.getAggregateId(), GenericSagaState.NOT_IN_SAGA, unitOfWork);
        }, unitOfWork);
    
        SagaSyncStep getTopicsStep = new SagaSyncStep("getTopicsStep", () -> {
            topicsAggregateIds.stream().forEach(topicId -> {
                unitOfWorkService.registerSagaState(topicId, TopicSagaState.READ_TOPIC, unitOfWork);
                SagaTopicDto topic = (SagaTopicDto) topicService.getTopicById(topicId, unitOfWork);
                this.addTopic(topic);
            });
        }, new ArrayList<>(Arrays.asList(getOriginalTournamentStep)));

        getTopicsStep.registerCompensation(() -> {
            this.getTopics().stream().forEach(t -> {
                SagaTopic topic = (SagaTopic) unitOfWorkService.aggregateLoadAndRegisterRead(t.getAggregateId(), unitOfWork);
                unitOfWorkService.registerSagaState(topic, GenericSagaState.NOT_IN_SAGA, unitOfWork);
            });
        }, unitOfWork);
    
        SagaSyncStep updateTournamentStep = new SagaSyncStep("updateTournamentStep", () -> {
            unitOfWorkService.registerSagaState(tournamentDto.getAggregateId(), TournamentSagaState.READ_UPDATED_TOPICS, new ArrayList<>(Arrays.asList(TournamentSagaState.IN_UPDATE_TOURNAMENT)), unitOfWork);
            SagaTournamentDto newTournamentDto = (SagaTournamentDto) tournamentService.updateTournament(tournamentDto, this.getTopicsDtos(), unitOfWork);
            this.setNewTournamentDto(newTournamentDto);
        }, new ArrayList<>(Arrays.asList(getTopicsStep)));
    
        updateTournamentStep.registerCompensation(() -> {
            if (newTournamentDto != null) {
                unitOfWorkService.registerSagaState(newTournamentDto.getAggregateId(), GenericSagaState.NOT_IN_SAGA, unitOfWork);
            }
        }, unitOfWork);
    
        SagaSyncStep updateQuizStep = new SagaSyncStep("updateQuizStep", () -> {
            QuizDto quizDto = new QuizDto();
            quizDto.setAggregateId(this.getNewTournamentDto().getQuiz().getAggregateId());
            quizDto.setAvailableDate(this.getNewTournamentDto().getStartTime());
            quizDto.setConclusionDate(this.getNewTournamentDto().getEndTime());
            quizDto.setResultsDate(this.getNewTournamentDto().getEndTime());    
            this.setQuizDto(quizDto);
            
            SagaQuizDto quiz = (SagaQuizDto) quizService.getQuizById(quizDto.getAggregateId(), unitOfWork);
            unitOfWorkService.registerSagaState(quiz.getAggregateId(), QuizSagaState.READ_QUIZ, unitOfWork);
    
            if (topicsAggregateIds != null || this.getNewTournamentDto().getNumberOfQuestions() != null) {
                if (topicsAggregateIds == null) {
                    quizService.updateGeneratedQuiz(quizDto, this.getTopics().stream()
                            .filter(t -> t.getSagaState().equals(GenericSagaState.NOT_IN_SAGA))
                            .map(SagaTopicDto::getAggregateId)
                            .collect(Collectors.toSet()), this.getNewTournamentDto().getNumberOfQuestions(), unitOfWork);
                } else {
                    quizService.updateGeneratedQuiz(quizDto, topicsAggregateIds, this.getNewTournamentDto().getNumberOfQuestions(), unitOfWork);
                }
            }
        }, new ArrayList<>(Arrays.asList(updateTournamentStep)));

        updateQuizStep.registerCompensation(() -> {
            unitOfWorkService.registerSagaState(quizDto.getAggregateId(), GenericSagaState.NOT_IN_SAGA, unitOfWork);
        }, unitOfWork);
    
        workflow.addStep(getOriginalTournamentStep);
        workflow.addStep(getTopicsStep);
        workflow.addStep(updateTournamentStep);
        workflow.addStep(updateQuizStep);
    }
    

    public SagaTournamentDto getOriginalTournamentDto() {
        return originalTournamentDto;
    }

    public void setOriginalTournamentDto(SagaTournamentDto originalTournamentDto) {
        this.originalTournamentDto = originalTournamentDto;
    }

    public HashSet<SagaTopicDto> getTopics() {
        return topics;
    }

    public HashSet<TopicDto> getTopicsDtos() {
        HashSet<TopicDto> topicDtos = topics.stream()
            .map(sagaTopicDto -> (TopicDto) sagaTopicDto) 
            .collect(Collectors.toCollection(HashSet::new));
        return topicDtos;
    }

    public void setTopics(HashSet<SagaTopicDto> topics) {
        this.topics = topics;
    }

    public void addTopic(SagaTopicDto topic) {
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