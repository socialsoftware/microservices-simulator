package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.tournament;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.service.QuizService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

import java.util.ArrayList;
import java.util.Arrays;

public class RemoveTournamentFunctionalitySagas extends WorkflowFunctionality {
    private final TournamentService tournamentService;
    private final QuizService quizService;
    private final SagaUnitOfWorkService unitOfWorkService;

    private TournamentDto tournamentDto;

    public RemoveTournamentFunctionalitySagas(TournamentService tournamentService, QuizService quizService, SagaUnitOfWorkService unitOfWorkService,
                                Integer tournamentAggregateId, SagaUnitOfWork unitOfWork) {
        this.tournamentService = tournamentService;
        this.quizService = quizService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(tournamentAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer tournamentAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getTournamentStep = new SagaSyncStep("getTournamentStep", () -> {
            TournamentDto tournamentDto = tournamentService.getTournamentById(tournamentAggregateId, unitOfWork);
            setTournamentDto(tournamentDto);
        });

        SagaSyncStep removeQuizStep = new SagaSyncStep("removeQuizStep", () -> {
            quizService.removeQuiz(getTournamentDto().getQuiz().getAggregateId(), unitOfWork);
        }, new ArrayList<>(Arrays.asList(getTournamentStep)));
    
        SagaSyncStep removeTournamentStep = new SagaSyncStep("removeTournamentStep", () -> {
            tournamentService.removeTournament(tournamentAggregateId, unitOfWork);
        }, new ArrayList<>(Arrays.asList(removeQuizStep)));

        workflow.addStep(getTournamentStep);
        workflow.addStep(removeQuizStep);
        workflow.addStep(removeTournamentStep);
    }

    public TournamentDto getTournamentDto() {
        return tournamentDto;
    }

    public void setTournamentDto(TournamentDto tournamentDto) {
        this.tournamentDto = tournamentDto;
    }
}