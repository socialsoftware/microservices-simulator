package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.causal;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Step;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.answer.StartQuizCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.courseExecution.GetStudentByExecutionIdAndUserIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.question.GetQuestionByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.quiz.GetQuizByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.quiz.StartTournamentQuizCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament.GetTournamentByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament.SolveQuizCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.QuizAnswerDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.Tournament;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;

import java.util.ArrayList;
import java.util.List;

public class SolveQuizFunctionalityTCC extends WorkflowFunctionality {
    private TournamentDto tournamentDto;
    private QuizDto quizDto;
    private UserDto userDto;
    private QuizAnswerDto quizAnswerDto;
    private Tournament oldTournament;
    private final CausalUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public SolveQuizFunctionalityTCC(CausalUnitOfWorkService unitOfWorkService,
                                     Integer tournamentAggregateId, Integer userAggregateId, CausalUnitOfWork unitOfWork,
                                     CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(tournamentAggregateId, userAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer tournamentAggregateId,
                              Integer userAggregateId, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        Step step = new Step(() -> {
            TournamentDto tournamentDto = (TournamentDto) commandGateway.send(new GetTournamentByIdCommand(unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), tournamentAggregateId));

            StartTournamentQuizCommand StartTournamentQuizCommand = new StartTournamentQuizCommand(unitOfWork, ServiceMapping.QUIZ.getServiceName(), userAggregateId, tournamentDto.getQuiz().getAggregateId());
            this.quizDto = (QuizDto) commandGateway.send(StartTournamentQuizCommand);

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

            GetQuizByIdCommand getQuizByIdCommand = new GetQuizByIdCommand(unitOfWork, ServiceMapping.QUIZ.getServiceName(), this.getQuizDto().getAggregateId());
            this.quizDto = (QuizDto) commandGateway.send(getQuizByIdCommand);

            GetStudentByExecutionIdAndUserIdCommand getStudentByExecutionIdAndUserIdCommand = new GetStudentByExecutionIdAndUserIdCommand(unitOfWork, ServiceMapping.COURSE_EXECUTION.getServiceName(), this.quizDto.getCourseExecutionAggregateId(), userAggregateId);
            this.userDto = (UserDto) commandGateway.send(getStudentByExecutionIdAndUserIdCommand);

            StartQuizCommand startQuizCommand = new StartQuizCommand(unitOfWork, ServiceMapping.ANSWER.getServiceName(), quizDto.getAggregateId(), this.getTournamentDto().getCourseExecution().getAggregateId(), this.quizDto, this.userDto);
            quizAnswerDto = (QuizAnswerDto) commandGateway.send(startQuizCommand);

            SolveQuizCommand SolveQuizCommand = new SolveQuizCommand(unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), tournamentAggregateId, userAggregateId, quizAnswerDto.getAggregateId());
            commandGateway.send(SolveQuizCommand);
        });

        workflow.addStep(step);
    }

    public TournamentDto getTournamentDto() {
        return tournamentDto;
    }

    public void setTournamentDto(TournamentDto tournamentDto) {
        this.tournamentDto = tournamentDto;
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