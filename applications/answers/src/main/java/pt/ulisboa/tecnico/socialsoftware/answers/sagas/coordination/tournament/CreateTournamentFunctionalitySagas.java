package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.tournament;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentCreator;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.service.UserService;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.dtos.SagaUserDto;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.states.UserSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import java.util.ArrayList;
import java.util.Arrays;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentExecution;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.service.ExecutionService;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.dtos.SagaExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.states.ExecutionSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import java.util.ArrayList;
import java.util.Arrays;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentQuiz;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.service.QuizService;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.dtos.SagaQuizDto;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.states.QuizSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import java.util.ArrayList;
import java.util.Arrays;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentParticipant;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.service.UserService;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.dtos.SagaUserDto;
import java.util.Set;
import java.util.HashSet;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentTopic;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.service.TopicService;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.dtos.SagaTopicDto;
import java.util.Set;
import java.util.HashSet;

public class CreateTournamentFunctionalitySagas extends WorkflowFunctionality {
    private TournamentDto createdTournamentDto;
    private final TournamentService tournamentService;
    private final SagaUnitOfWorkService unitOfWorkService;
    private SagaUserDto userDto;
    private TournamentCreator creator;
    private final UserService userService;
    private SagaExecutionDto executionDto;
    private TournamentExecution execution;
    private final ExecutionService executionService;
    private SagaQuizDto quizDto;
    private TournamentQuiz quiz;
    private final QuizService quizService;
    private final TopicService topicService;


    public CreateTournamentFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, TournamentService tournamentService, UserService userService, ExecutionService executionService, QuizService quizService, TopicService topicService, TournamentDto tournamentDto) {
        this.tournamentService = tournamentService;
        this.unitOfWorkService = unitOfWorkService;
        this.userService = userService;
        this.executionService = executionService;
        this.quizService = quizService;
        this.topicService = topicService;
        this.buildWorkflow(tournamentDto, unitOfWork);
    }

    public void buildWorkflow(TournamentDto tournamentDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getUserStep = new SagaSyncStep("getUserStep", () -> {
            Integer creatorAggregateId = tournamentDto.getCreatorAggregateId();
            userDto = (SagaUserDto) userService.getUserById(creatorAggregateId, unitOfWork);
            unitOfWorkService.registerSagaState(userDto.getAggregateId(), UserSagaState.READ_USER, unitOfWork);
            TournamentCreator creator = new TournamentCreator(userDto);
            setCreator(creator);
        });

        getUserStep.registerCompensation(() -> {
            unitOfWorkService.registerSagaState(userDto.getAggregateId(), GenericSagaState.NOT_IN_SAGA, unitOfWork);
        }, unitOfWork);

        SagaSyncStep getExecutionStep = new SagaSyncStep("getExecutionStep", () -> {
            Integer executionAggregateId = tournamentDto.getExecutionAggregateId();
            executionDto = (SagaExecutionDto) executionService.getExecutionById(executionAggregateId, unitOfWork);
            unitOfWorkService.registerSagaState(executionDto.getAggregateId(), ExecutionSagaState.READ_EXECUTION, unitOfWork);
            TournamentExecution execution = new TournamentExecution(executionDto);
            setExecution(execution);
        });

        getExecutionStep.registerCompensation(() -> {
            unitOfWorkService.registerSagaState(executionDto.getAggregateId(), GenericSagaState.NOT_IN_SAGA, unitOfWork);
        }, unitOfWork);

        SagaSyncStep getQuizStep = new SagaSyncStep("getQuizStep", () -> {
            Integer quizAggregateId = tournamentDto.getQuizAggregateId();
            quizDto = (SagaQuizDto) quizService.getQuizById(quizAggregateId, unitOfWork);
            unitOfWorkService.registerSagaState(quizDto.getAggregateId(), QuizSagaState.READ_QUIZ, unitOfWork);
            TournamentQuiz quiz = new TournamentQuiz(quizDto);
            setQuiz(quiz);
        });

        getQuizStep.registerCompensation(() -> {
            unitOfWorkService.registerSagaState(quizDto.getAggregateId(), GenericSagaState.NOT_IN_SAGA, unitOfWork);
        }, unitOfWork);

        SagaSyncStep createTournamentStep = new SagaSyncStep("createTournamentStep", () -> {
            Set<TournamentParticipant> participants = null;
            if (tournamentDto.getParticipantsAggregateIds() != null) {
                participants = new HashSet<>();
                for (Integer userAggregateId : tournamentDto.getParticipantsAggregateIds()) {
                    SagaUserDto userDto = (SagaUserDto) userService.getUserById(userAggregateId, unitOfWork);
                    participants.add(new TournamentParticipant(userDto));
                }
            }
            Set<TournamentTopic> topics = null;
            if (tournamentDto.getTopicsAggregateIds() != null) {
                topics = new HashSet<>();
                for (Integer topicAggregateId : tournamentDto.getTopicsAggregateIds()) {
                    SagaTopicDto topicDto = (SagaTopicDto) topicService.getTopicById(topicAggregateId, unitOfWork);
                    topics.add(new TournamentTopic(topicDto));
                }
            }
            TournamentDto createdTournamentDto = tournamentService.createTournament(getCreator(), getExecution(), getQuiz(), tournamentDto, participants, topics, unitOfWork);
            setCreatedTournamentDto(createdTournamentDto);
        }, new ArrayList<>(Arrays.asList(getUserStep, getExecutionStep, getQuizStep)));

        workflow.addStep(getUserStep);
        workflow.addStep(getExecutionStep);
        workflow.addStep(getQuizStep);
        workflow.addStep(createTournamentStep);

    }

    public TournamentCreator getCreator() {
        return creator;
    }

    public void setCreator(TournamentCreator creator) {
        this.creator = creator;
    }

    public TournamentExecution getExecution() {
        return execution;
    }

    public void setExecution(TournamentExecution execution) {
        this.execution = execution;
    }

    public TournamentQuiz getQuiz() {
        return quiz;
    }

    public void setQuiz(TournamentQuiz quiz) {
        this.quiz = quiz;
    }

    public TournamentDto getCreatedTournamentDto() {
        return createdTournamentDto;
    }

    public void setCreatedTournamentDto(TournamentDto createdTournamentDto) {
        this.createdTournamentDto = createdTournamentDto;
    }
}
