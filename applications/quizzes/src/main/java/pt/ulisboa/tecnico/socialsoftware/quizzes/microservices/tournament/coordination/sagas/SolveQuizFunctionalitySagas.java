package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.answer.RemoveQuizAnswerCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.answer.StartQuizCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.question.GetQuestionByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.quiz.StartTournamentQuizCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament.GetTournamentByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament.SolveQuizCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.QuizAnswerDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.sagas.states.QuizAnswerSagaState;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.sagas.states.QuizSagaState;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.Tournament;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.sagas.states.TournamentSagaState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SolveQuizFunctionalitySagas extends WorkflowFunctionality {
    private TournamentDto tournament;
    private QuizDto quizDto;
    private QuizAnswerDto quizAnswerDto;
    private Tournament oldTournament;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public SolveQuizFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                       Integer tournamentAggregateId, Integer userAggregateId, SagaUnitOfWork unitOfWork,
                                       CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(tournamentAggregateId, userAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer tournamentAggregateId,
                              Integer userAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getTournamentStep = new SagaSyncStep("getTournamentStep", () -> {
            GetTournamentByIdCommand getTournamentByIdCommand = new GetTournamentByIdCommand(unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), tournamentAggregateId);
            getTournamentByIdCommand.setSemanticLock(TournamentSagaState.READ_TOURNAMENT);
            TournamentDto tournament = (TournamentDto) commandGateway.send(getTournamentByIdCommand);
            this.setTournament(tournament);
        });

        SagaSyncStep startQuizStep = new SagaSyncStep("startQuizStep", () -> {
            StartTournamentQuizCommand startTournamentQuizCommand = new StartTournamentQuizCommand(unitOfWork, ServiceMapping.QUIZ.getServiceName(), userAggregateId, this.getTournamentDto().getQuiz().getAggregateId());
            startTournamentQuizCommand.setSemanticLock(QuizSagaState.STARTED_TOURNAMENT_QUIZ);
            QuizDto quiz = (QuizDto) commandGateway.send(startTournamentQuizCommand);
            this.setQuizDto(quiz);
        }, new ArrayList<>(Arrays.asList(getTournamentStep)));

        SagaSyncStep getQuestionById = new SagaSyncStep("getQuestionById", () -> { // TODO no loop for multiple sends inside a step
            List<QuestionDto> questionDtoList = new ArrayList<>();
            quizDto.getQuestionDtos().forEach(quizQuestion -> {
                GetQuestionByIdCommand getQuestionByIdCommand = new GetQuestionByIdCommand(unitOfWork, ServiceMapping.QUESTION.getServiceName(), quizQuestion.getAggregateId());
                QuestionDto questionDto = (QuestionDto) commandGateway.send(getQuestionByIdCommand);
                questionDto.getOptionDtos().forEach(o -> {
                    o.setCorrect(false); // by setting all to false frontend doesn't know which is correct
                });
                questionDtoList.add(questionDto);
            });
            quizDto.setQuestionDtos(questionDtoList);
        }, new ArrayList<>(Arrays.asList(startQuizStep)));

        SagaSyncStep startQuizAnswerStep = new SagaSyncStep("startQuizAnswerStep", () -> {
            StartQuizCommand startTournamentQuizCommand = new StartQuizCommand(unitOfWork, ServiceMapping.ANSWER.getServiceName(), this.getQuizDto().getAggregateId(), this.getTournamentDto().getCourseExecution().getAggregateId(), userAggregateId);
            startTournamentQuizCommand.setSemanticLock(QuizAnswerSagaState.STARTED_QUIZ);
            QuizAnswerDto quizAnswerDto = (QuizAnswerDto) commandGateway.send(startTournamentQuizCommand);
            this.setQuizAnswerDto(quizAnswerDto);
        }, new ArrayList<>(Arrays.asList(getQuestionById)));

        startQuizAnswerStep.registerCompensation(() -> {
            if (this.getQuizAnswerDto() != null) {
                RemoveQuizAnswerCommand removeQuizAnswerCommand = new RemoveQuizAnswerCommand(unitOfWork, ServiceMapping.ANSWER.getServiceName(), this.getQuizAnswerDto().getAggregateId());
                commandGateway.send(removeQuizAnswerCommand);
            }
        }, unitOfWork);

        SagaSyncStep solveQuizStep = new SagaSyncStep("solveQuizStep", () -> {
            SolveQuizCommand solveQuizCommand = new SolveQuizCommand(unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), tournamentAggregateId, userAggregateId, this.getQuizAnswerDto().getAggregateId());
            commandGateway.send(solveQuizCommand);
        }, new ArrayList<>(Arrays.asList(startQuizAnswerStep)));

        workflow.addStep(getTournamentStep);
        workflow.addStep(startQuizStep);
        workflow.addStep(getQuestionById);
        workflow.addStep(startQuizAnswerStep);
        workflow.addStep(solveQuizStep);
    }

    public TournamentDto getTournamentDto() {
        return tournament;
    }

    public void setTournament(TournamentDto tournament) {
        this.tournament = tournament;
    }

    public QuizDto getQuizDto() {
        return quizDto;
    }

    public void setQuizDto(QuizDto quizDto) {
        this.quizDto = quizDto;
    }

    public QuizAnswerDto getQuizAnswerDto() {
        return quizAnswerDto;
    }

    public void setQuizAnswerDto(QuizAnswerDto quizAnswerDto) {
        this.quizAnswerDto = quizAnswerDto;
    }

    public Tournament getOldTournament() {
        return oldTournament;
    }

    public void setOldTournament(Tournament oldTournament) {
        this.oldTournament = oldTournament;
    }
}