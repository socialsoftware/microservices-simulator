package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.causal;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.quiz.UpdateGeneratedQuizCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.topic.GetTopicByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament.UpdateTournamentCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.Quiz;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.causal.CausalTopic;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.Tournament;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class UpdateTournamentFunctionalityTCC extends WorkflowFunctionality {
    private TournamentDto originalTournamentDto;
    private HashSet<CausalTopic> topics = new HashSet<CausalTopic>();
    private Tournament oldTournament;
    private TournamentDto newTournamentDto;
    private QuizDto quizDto;
    private Quiz oldQuiz;
    private final CausalUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public UpdateTournamentFunctionalityTCC(CausalUnitOfWorkService unitOfWorkService,
                                            TournamentDto tournamentDto, Set<Integer> topicsAggregateIds, CausalUnitOfWork unitOfWork,
                                            CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(tournamentDto, topicsAggregateIds, unitOfWork);
    }

    public void buildWorkflow(TournamentDto tournamentDto, Set<Integer> topicsAggregateIds,
                              CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep step = new SyncStep(() -> {
            Set<TopicDto> topicDtos = topicsAggregateIds.stream()
                    .map(topicAggregateId -> (TopicDto) commandGateway.send(new GetTopicByIdCommand(unitOfWork, ServiceMapping.TOPIC.getServiceName(), topicAggregateId)))
                    .collect(Collectors.toSet());

            UpdateTournamentCommand UpdateTournamentCommand = new UpdateTournamentCommand(unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), tournamentDto, topicDtos);
            TournamentDto newTournamentDto = (TournamentDto) commandGateway.send(UpdateTournamentCommand);

            QuizDto quizDto = new QuizDto();
            quizDto.setAggregateId(newTournamentDto.getQuiz().getAggregateId());
            quizDto.setAvailableDate(newTournamentDto.getStartTime());
            quizDto.setConclusionDate(newTournamentDto.getEndTime());
            quizDto.setResultsDate(newTournamentDto.getEndTime());

            /*
             * this if is required for the case of updating a quiz and not altering neither
             * the number of questions neither the topics
             */
            UpdateGeneratedQuizCommand UpdateGeneratedQuizCommand = new UpdateGeneratedQuizCommand(unitOfWork, ServiceMapping.QUIZ.getServiceName(), quizDto, topicsAggregateIds, newTournamentDto.getNumberOfQuestions());
            commandGateway.send(UpdateGeneratedQuizCommand);
        });

        workflow.addStep(step);
    }

    public TournamentDto getOriginalTournamentDto() {
        return originalTournamentDto;
    }

    public void setOriginalTournamentDto(TournamentDto originalTournamentDto) {
        this.originalTournamentDto = originalTournamentDto;
    }

    public HashSet<CausalTopic> getTopics() {
        return topics;
    }

    public HashSet<TopicDto> getTopicsDtos() {
        HashSet<TopicDto> topicDtos = topics.stream()
                .map(TopicDto::new)
                .collect(Collectors.toCollection(HashSet::new));
        return topicDtos;
    }

    public void setTopics(HashSet<CausalTopic> topics) {
        this.topics = topics;
    }

    public void addTopic(CausalTopic topic) {
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