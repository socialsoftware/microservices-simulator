package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
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
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.SagaQuiz;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.SagaTopic;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.SagaTournament;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.states.TopicSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.states.TournamentSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateTournamentFunctionalitySagas extends WorkflowFunctionality{
    private TournamentDto originalTournamentDto;
    private HashSet<SagaTopic> topics = new HashSet<SagaTopic>();
    private Tournament oldTournament;
    private TournamentDto newTournamentDto;
    private QuizDto quizDto;
    private Quiz oldQuiz;

    

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
    
        SyncStep getOriginalTournamentStep = new SyncStep("getOriginalTournamentStep", () -> {
            TournamentDto originalTournamentDto = tournamentService.getTournamentById(tournamentDto.getAggregateId(), unitOfWork);
            SagaTournament tournament = (SagaTournament) unitOfWorkService.aggregateLoadAndRegisterRead(originalTournamentDto.getAggregateId(), unitOfWork);
            unitOfWorkService.registerSagaState(tournament, TournamentSagaState.UPDATE_TOURNAMENT_READ_TOURNAMENT, unitOfWork);
            this.setOriginalTournamentDto(originalTournamentDto);
            this.setOldTournament(tournament);
        });
    
        getOriginalTournamentStep.registerCompensation(() -> {
            TournamentDto originalTournamentDto = this.getOriginalTournamentDto();
            if (originalTournamentDto != null) {
                SagaTournament tournament = (SagaTournament) unitOfWorkService.aggregateLoadAndRegisterRead(originalTournamentDto.getAggregateId(), unitOfWork);
                unitOfWorkService.registerSagaState(tournament, GenericSagaState.NOT_IN_SAGA, unitOfWork);
            }
        }, unitOfWork);
    
        SyncStep getTopicsStep = new SyncStep("getTopicsStep", () -> {
            topicsAggregateIds.stream().forEach(topicId -> {
                TopicDto t = topicService.getTopicById(topicId, unitOfWork);
                SagaTopic topic = (SagaTopic) unitOfWorkService.aggregateLoadAndRegisterRead(t.getAggregateId(), unitOfWork);
                unitOfWorkService.registerSagaState(topic, TopicSagaState.UPDATE_TOURNAMENT_READ_TOPIC, unitOfWork);
                this.addTopic(topic);
            });
        }, new ArrayList<>(Arrays.asList(getOriginalTournamentStep)));

        getTopicsStep.registerCompensation(() -> {
            Set<SagaTopic> topics = this.getTopics();
            topics.stream().forEach(t -> {
                SagaTopic topic = (SagaTopic) unitOfWorkService.aggregateLoadAndRegisterRead(t.getAggregateId(), unitOfWork);
                unitOfWorkService.registerSagaState(topic, GenericSagaState.NOT_IN_SAGA, unitOfWork);
            });
        }, unitOfWork);
    
        SyncStep updateTournamentStep = new SyncStep("updateTournamentStep", () -> {
            TournamentDto newTournamentDto = tournamentService.updateTournament(tournamentDto, this.getTopicsDtos(), unitOfWork);
            SagaTournament newTournament = (SagaTournament) unitOfWorkService.aggregateLoadAndRegisterRead(newTournamentDto.getAggregateId(), unitOfWork);
            unitOfWorkService.registerSagaState(newTournament, TournamentSagaState.UPDATE_TOURNAMENT_READ_UPDATED_TOPICS, unitOfWork);
            this.setNewTournamentDto(newTournamentDto);
        }, new ArrayList<>(Arrays.asList(getTopicsStep)));
    
        updateTournamentStep.registerCompensation(() -> {
            Tournament oldTournament = tournamentFactory.createTournamentFromExisting(this.getOldTournament());
            unitOfWorkService.registerSagaState((SagaTournament) oldTournament, GenericSagaState.NOT_IN_SAGA, unitOfWork);
            unitOfWork.registerChanged(oldTournament);
        }, unitOfWork);
    
        SyncStep updateQuizStep = new SyncStep("updateQuizStep", () -> {
            QuizDto quizDto = new QuizDto();
            quizDto.setAggregateId(this.getNewTournamentDto().getQuiz().getAggregateId());
            quizDto.setAvailableDate(this.getNewTournamentDto().getStartTime());
            quizDto.setConclusionDate(this.getNewTournamentDto().getEndTime());
            quizDto.setResultsDate(this.getNewTournamentDto().getEndTime());    
            this.setQuizDto(quizDto);

            SagaQuiz oldQuiz = (SagaQuiz) unitOfWorkService.aggregateLoadAndRegisterRead(quizDto.getAggregateId(), unitOfWork);
            unitOfWorkService.registerSagaState(oldQuiz, GenericSagaState.IN_SAGA, unitOfWork);
            this.setOldQuiz(oldQuiz);
    
            if (topicsAggregateIds != null || this.getNewTournamentDto().getNumberOfQuestions() != null) {
                if (topicsAggregateIds == null) {
                    quizService.updateGeneratedQuiz(quizDto, this.getTopics().stream()
                            .filter(t -> t.getSagaState().equals(GenericSagaState.NOT_IN_SAGA))
                            .map(SagaTopic::getAggregateId)
                            .collect(Collectors.toSet()), this.getNewTournamentDto().getNumberOfQuestions(), unitOfWork);
                } else {
                    quizService.updateGeneratedQuiz(quizDto, topicsAggregateIds, this.getNewTournamentDto().getNumberOfQuestions(), unitOfWork);
                }
            }
        }, new ArrayList<>(Arrays.asList(updateTournamentStep)));
    
        updateQuizStep.registerCompensation(() -> {
            Quiz newQuiz = quizFactory.createQuizFromExisting(this.getOldQuiz());
            unitOfWorkService.registerSagaState((SagaQuiz) newQuiz, GenericSagaState.NOT_IN_SAGA, unitOfWork);
            unitOfWork.registerChanged(newQuiz);
        }, unitOfWork);
    
        workflow.addStep(getOriginalTournamentStep);
        workflow.addStep(getTopicsStep);
        workflow.addStep(updateTournamentStep);
        workflow.addStep(updateQuizStep);
    }

    @Override
    public void handleEvents() {

    }

    

    public TournamentDto getOriginalTournamentDto() {
        return originalTournamentDto;
    }

    public void setOriginalTournamentDto(TournamentDto originalTournamentDto) {
        this.originalTournamentDto = originalTournamentDto;
    }

    public HashSet<SagaTopic> getTopics() {
        return topics;
    }

    public HashSet<TopicDto> getTopicsDtos() {;
        HashSet<TopicDto> topicDtos = topics.stream()
                .map(TopicDto::new)
                .collect(Collectors.toCollection(HashSet::new));
        return topicDtos;
    }

    public void setTopics(HashSet<SagaTopic> topics) {
        this.topics = topics;
    }

    public void addTopic(SagaTopic topic) {
        this.topics.add(topic);
    }

    public Tournament getOldTournament() {
        return oldTournament;
    }

    public void setOldTournament(Tournament oldTournament) {
        this.oldTournament = oldTournament;
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

    public Quiz getOldQuiz() {
        return oldQuiz;
    }

    public void setOldQuiz(Quiz oldQuiz) {
        this.oldQuiz = oldQuiz;
    }
}