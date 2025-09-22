package pt.ulisboa.tecnico.socialsoftware.quizzes.causal.coordination.tournament;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.causal.aggregates.CausalTopic;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.quiz.UpdateGeneratedQuizCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.topic.GetTopicByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament.UpdateTournamentCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.Quiz;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizFactory;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.service.QuizService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.service.TopicService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.Tournament;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentFactory;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.service.TournamentService;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class UpdateTournamentFunctionalityTCC extends WorkflowFunctionality {
    private TournamentDto originalTournamentDto;
    private HashSet<CausalTopic> topics = new HashSet<CausalTopic>();
    private Tournament oldTournament;
    private TournamentDto newTournamentDto;
    private QuizDto quizDto;
    private Quiz oldQuiz;
    private final TournamentService tournamentService;
    private final TopicService topicService;
    private final QuizService quizService;
    private final CausalUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public UpdateTournamentFunctionalityTCC(TournamentService tournamentService, TopicService topicService,
            QuizService quizService, CausalUnitOfWorkService unitOfWorkService,
            TournamentFactory tournamentFactory, QuizFactory quizFactory,
            TournamentDto tournamentDto, Set<Integer> topicsAggregateIds, CausalUnitOfWork unitOfWork,
            CommandGateway commandGateway) {
        this.tournamentService = tournamentService;
        this.topicService = topicService;
        this.quizService = quizService;
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(tournamentDto, topicsAggregateIds, tournamentFactory, quizFactory, unitOfWork);
    }

    public void buildWorkflow(TournamentDto tournamentDto, Set<Integer> topicsAggregateIds,
            TournamentFactory tournamentFactory, QuizFactory quizFactory, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep step = new SyncStep(() -> {
            // Set<TopicDto> topicDtos = topicsAggregateIds.stream()
            // .map(topicAggregateId -> topicService.getTopicById(topicAggregateId,
            // unitOfWork))
            // .collect(Collectors.toSet());
            Set<TopicDto> topicDtos = topicsAggregateIds.stream()
                    .map(topicAggregateId -> (TopicDto) commandGateway.send(new GetTopicByIdCommand(unitOfWork,
                            ServiceMapping.TOPIC.getServiceName(), topicAggregateId)))
                    .collect(Collectors.toSet());

            // TournamentDto newTournamentDto =
            // tournamentService.updateTournament(tournamentDto, topicDtos, unitOfWork);
            UpdateTournamentCommand UpdateTournamentCommand = new UpdateTournamentCommand(unitOfWork,
                    ServiceMapping.TOURNAMENT.getServiceName(), tournamentDto, topicDtos);
            TournamentDto newTournamentDto = (TournamentDto) commandGateway.send(UpdateTournamentCommand);

            QuizDto quizDto = new QuizDto();
            quizDto.setAggregateId(newTournamentDto.getQuiz().getAggregateId());
            quizDto.setAvailableDate(newTournamentDto.getStartTime());
            quizDto.setConclusionDate(newTournamentDto.getEndTime());
            quizDto.setResultsDate(newTournamentDto.getEndTime());

            // NUMBER_OF_QUESTIONS
            // this.numberOfQuestions == Quiz(tournamentQuiz.id).quizQuestions.size
            // Quiz(this.tournamentQuiz.id) DEPENDS ON this.numberOfQuestions
            // QUIZ_TOPICS
            // Quiz(this.tournamentQuiz.id) DEPENDS ON this.topics // the topics of the quiz
            // questions are related to the tournament topics
            // START_TIME_AVAILABLE_DATE
            // this.startTime == Quiz(tournamentQuiz.id).availableDate
            // END_TIME_CONCLUSION_DATE
            // this.endTime == Quiz(tournamentQuiz.id).conclusionDate

            /*
             * this if is required for the case of updating a quiz and not altering neither
             * the number of questions neither the topics
             */
            if (topicsAggregateIds != null || tournamentDto.getNumberOfQuestions() != null) {
                // quizService.updateGeneratedQuiz(quizDto, topicsAggregateIds,
                // newTournamentDto.getNumberOfQuestions(), unitOfWork);
                UpdateGeneratedQuizCommand UpdateGeneratedQuizCommand = new UpdateGeneratedQuizCommand(unitOfWork,
                        ServiceMapping.QUIZ.getServiceName(), quizDto, topicsAggregateIds,
                        newTournamentDto.getNumberOfQuestions());
                commandGateway.send(UpdateGeneratedQuizCommand);
            }
            // quizService.updateGeneratedQuiz(quizDto, topicsAggregateIds,
            // newTournamentDto.getNumberOfQuestions(), unitOfWork);
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
        ;
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