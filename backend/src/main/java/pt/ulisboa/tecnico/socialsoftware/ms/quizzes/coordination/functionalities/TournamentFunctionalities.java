package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.coordination.functionalities;

import static pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage.TOURNAMENT_MISSING_END_TIME;
import static pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage.TOURNAMENT_MISSING_NUMBER_OF_QUESTIONS;
import static pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage.TOURNAMENT_MISSING_START_TIME;
import static pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage.TOURNAMENT_MISSING_TOPICS;
import static pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage.TOURNAMENT_MISSING_USER;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.AddParticipantFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.CancelTournamentFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.CreateTournamentFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.FindParticipantFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.FindTournamentFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.GetClosedTournamentsForCourseExecutionFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.GetOpenedTournamentsForCourseExecutionFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.GetTournamentsForCourseExecutionFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.LeaveTournamentFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.RemoveTournamentFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.SolveQuizFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.UpdateTournamentFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.service.QuizAnswerService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.TutorException;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.service.CourseExecutionService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.service.QuizService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.service.TopicService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.events.handling.TournamentEventHandling;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.service.UserService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows.AddParticipantFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows.CancelTournamentFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows.CreateTournamentFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows.FindParticipantFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows.FindTournamentFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows.GetClosedTournamentsForCourseExecutionFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows.GetOpenedTournamentsForCourseExecutionFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows.GetTournamentsForCourseExecutionFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows.LeaveTournamentFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows.RemoveTournamentFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows.SolveQuizFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows.UpdateTournamentFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;

@Service
public class TournamentFunctionalities {
    @Autowired
    private TournamentService tournamentService;
    @Autowired
    private UserService userService;
    @Autowired
    private CourseExecutionService courseExecutionService;
    @Autowired
    private TopicService topicService;
    @Autowired
    private QuizService quizService;
    @Autowired
    private QuizAnswerService quizAnswerService;
    @Autowired(required = false)
    private SagaUnitOfWorkService sagaUnitOfWorkService;
    @Autowired(required = false)
    private CausalUnitOfWorkService causalUnitOfWorkService;
    @Autowired
    private TournamentFactory tournamentFactory;
    @Autowired
    private QuizFactory quizFactory;
    @Autowired
    private TournamentEventHandling tournamentEventHandling;
    @Autowired
    private EventService eventService;

    @Autowired
    private Environment env;

    private String workflowType;

    @PostConstruct
    public void init() {
        // Determine the workflow type based on active profiles
        String[] activeProfiles = env.getActiveProfiles();
        if (Arrays.asList(activeProfiles).contains("sagas")) {
            workflowType = "sagas";
        } else if (Arrays.asList(activeProfiles).contains("tcc")) {
            workflowType = "tcc";
        } else {
            workflowType = "unknown"; // Default or fallback value
        }
    }

    public TournamentDto createTournament(Integer userId, Integer executionId, List<Integer> topicsId, TournamentDto tournamentDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        if ("sagas".equals(workflowType)) {
            SagaUnitOfWork unitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
            checkInput(userId, topicsId, tournamentDto);

            CreateTournamentFunctionalitySagas functionality = new CreateTournamentFunctionalitySagas(
                    tournamentService, courseExecutionService, topicService, quizService, sagaUnitOfWorkService, 
                    userId, executionId, topicsId, tournamentDto, unitOfWork);

            functionality.executeWorkflow(unitOfWork);
            return functionality.getTournamentDto();
        } else {
            CausalUnitOfWork unitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
            checkInput(userId, topicsId, tournamentDto);

            CreateTournamentFunctionalityTCC functionality = new CreateTournamentFunctionalityTCC(
                    tournamentService, courseExecutionService, topicService, quizService, causalUnitOfWorkService, 
                    userId, executionId, topicsId, tournamentDto, unitOfWork);

            functionality.executeWorkflow(unitOfWork);
            return functionality.getTournamentDto();
        }
    }

    public void addParticipant(Integer tournamentAggregateId, Integer executionAggregateId, Integer userAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        if ("sagas".equals(workflowType)) {
            SagaUnitOfWork unitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
            AddParticipantFunctionalitySagas functionality = new AddParticipantFunctionalitySagas(
                    eventService, tournamentEventHandling, tournamentService, courseExecutionService, sagaUnitOfWorkService, 
                    tournamentAggregateId, executionAggregateId, userAggregateId, unitOfWork);

            functionality.executeWorkflow(unitOfWork);
        } else {
            CausalUnitOfWork unitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
            AddParticipantFunctionalityTCC functionality = new AddParticipantFunctionalityTCC(
                    eventService, tournamentEventHandling, tournamentService, courseExecutionService, causalUnitOfWorkService, 
                    tournamentAggregateId, userAggregateId, unitOfWork);

            functionality.executeWorkflow(unitOfWork);
        }
    }

    public void updateTournament(TournamentDto tournamentDto, Set<Integer> topicsAggregateIds) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        if ("sagas".equals(workflowType)) {
            SagaUnitOfWork unitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
            UpdateTournamentFunctionalitySagas functionality = new UpdateTournamentFunctionalitySagas(
                    tournamentService, topicService, quizService, sagaUnitOfWorkService, tournamentFactory, quizFactory, 
                    tournamentDto, topicsAggregateIds, unitOfWork);

            functionality.executeWorkflow(unitOfWork);
        } else {
            CausalUnitOfWork unitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
            UpdateTournamentFunctionalityTCC functionality = new UpdateTournamentFunctionalityTCC(
                    tournamentService, topicService, quizService, causalUnitOfWorkService, tournamentFactory, quizFactory, 
                    tournamentDto, topicsAggregateIds, unitOfWork);

            functionality.executeWorkflow(unitOfWork);
        }
    }

    public List<TournamentDto> getTournamentsForCourseExecution(Integer executionAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        if ("sagas".equals(workflowType)) {
            SagaUnitOfWork unitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
            GetTournamentsForCourseExecutionFunctionalitySagas functionality = new GetTournamentsForCourseExecutionFunctionalitySagas(
                    tournamentService, sagaUnitOfWorkService, executionAggregateId, unitOfWork);

            functionality.executeWorkflow(unitOfWork);
            return functionality.getTournaments();
        } else {
            CausalUnitOfWork unitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
            GetTournamentsForCourseExecutionFunctionalityTCC functionality = new GetTournamentsForCourseExecutionFunctionalityTCC(
                    tournamentService, causalUnitOfWorkService, executionAggregateId, unitOfWork);

            functionality.executeWorkflow(unitOfWork);
            return functionality.getTournaments();
        }
    }

    public List<TournamentDto> getOpenedTournamentsForCourseExecution(Integer executionAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        if ("sagas".equals(workflowType)) {
            SagaUnitOfWork unitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
            GetOpenedTournamentsForCourseExecutionFunctionalitySagas functionality = new GetOpenedTournamentsForCourseExecutionFunctionalitySagas(
                    tournamentService, sagaUnitOfWorkService, executionAggregateId, unitOfWork);

            functionality.executeWorkflow(unitOfWork);
            return functionality.getOpenedTournaments();
        } else {
            CausalUnitOfWork unitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
            GetOpenedTournamentsForCourseExecutionFunctionalityTCC functionality = new GetOpenedTournamentsForCourseExecutionFunctionalityTCC(
                    tournamentService, causalUnitOfWorkService, executionAggregateId, unitOfWork);

            functionality.executeWorkflow(unitOfWork);
            return functionality.getOpenedTournaments();
        }
    }

    public List<TournamentDto> getClosedTournamentsForCourseExecution(Integer executionAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        if ("sagas".equals(workflowType)) {
            SagaUnitOfWork unitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
            GetClosedTournamentsForCourseExecutionFunctionalitySagas functionality = new GetClosedTournamentsForCourseExecutionFunctionalitySagas(
                    tournamentService, sagaUnitOfWorkService, executionAggregateId, unitOfWork);

            functionality.executeWorkflow(unitOfWork);
            return functionality.getClosedTournaments();
        } else {
            CausalUnitOfWork unitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
            GetClosedTournamentsForCourseExecutionFunctionalityTCC functionality = new GetClosedTournamentsForCourseExecutionFunctionalityTCC(
                    tournamentService, causalUnitOfWorkService, executionAggregateId, unitOfWork);

            functionality.executeWorkflow(unitOfWork);
            return functionality.getClosedTournaments();
        }
    }

    public void leaveTournament(Integer tournamentAggregateId, Integer userAggregateId) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        if ("sagas".equals(workflowType)) {
            SagaUnitOfWork unitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
            LeaveTournamentFunctionalitySagas functionality = new LeaveTournamentFunctionalitySagas(
                    tournamentService, sagaUnitOfWorkService, tournamentFactory, tournamentAggregateId, userAggregateId, unitOfWork);

            functionality.executeWorkflow(unitOfWork);
        } else {
            CausalUnitOfWork unitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
            LeaveTournamentFunctionalityTCC functionality = new LeaveTournamentFunctionalityTCC(
                    tournamentService, causalUnitOfWorkService, tournamentFactory, tournamentAggregateId, userAggregateId, unitOfWork);

            functionality.executeWorkflow(unitOfWork);
        }
    }

    public QuizDto solveQuiz(Integer tournamentAggregateId, Integer userAggregateId) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        if ("sagas".equals(workflowType)) {
            SagaUnitOfWork unitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
            SolveQuizFunctionalitySagas functionality = new SolveQuizFunctionalitySagas(
                    tournamentService, quizService, quizAnswerService, sagaUnitOfWorkService, tournamentFactory, 
                    tournamentAggregateId, userAggregateId, unitOfWork);

            functionality.executeWorkflow(unitOfWork);
            return functionality.getQuizDto();
        } else {
            CausalUnitOfWork unitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
            SolveQuizFunctionalityTCC functionality = new SolveQuizFunctionalityTCC(
                    tournamentService, quizService, quizAnswerService, causalUnitOfWorkService, tournamentFactory, 
                    tournamentAggregateId, userAggregateId, unitOfWork);

            functionality.executeWorkflow(unitOfWork);
            return functionality.getQuizDto();
        }
    }

    public void cancelTournament(Integer tournamentAggregateId) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        if ("sagas".equals(workflowType)) {
            SagaUnitOfWork unitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
            CancelTournamentFunctionalitySagas functionality = new CancelTournamentFunctionalitySagas(
                    tournamentService, sagaUnitOfWorkService, tournamentFactory, tournamentAggregateId, unitOfWork);

            functionality.executeWorkflow(unitOfWork);
        } else {
            CausalUnitOfWork unitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
            CancelTournamentFunctionalityTCC functionality = new CancelTournamentFunctionalityTCC(
                    tournamentService, causalUnitOfWorkService, tournamentFactory, tournamentAggregateId, unitOfWork);

            functionality.executeWorkflow(unitOfWork);
        }
    }

    public void removeTournament(Integer tournamentAggregateId) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        if ("sagas".equals(workflowType)) {
            SagaUnitOfWork unitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
            RemoveTournamentFunctionalitySagas functionality = new RemoveTournamentFunctionalitySagas(
                    eventService, tournamentService, sagaUnitOfWorkService, tournamentFactory, tournamentAggregateId, unitOfWork);

            functionality.executeWorkflow(unitOfWork);
        } else {
            CausalUnitOfWork unitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
            RemoveTournamentFunctionalityTCC functionality = new RemoveTournamentFunctionalityTCC(
                    eventService, tournamentService, causalUnitOfWorkService, tournamentFactory, tournamentAggregateId, unitOfWork);

            functionality.executeWorkflow(unitOfWork);
        }
    }

    public TournamentDto findTournament(Integer tournamentAggregateId) throws TutorException {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        if ("sagas".equals(workflowType)) {
            SagaUnitOfWork unitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
        
            FindTournamentFunctionalitySagas functionality = new FindTournamentFunctionalitySagas(tournamentService, sagaUnitOfWorkService, tournamentAggregateId, unitOfWork);
            
            functionality.executeWorkflow(unitOfWork);
            return functionality.getTournamentDto();

        } else {
            CausalUnitOfWork unitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
            FindTournamentFunctionalityTCC functionality = new FindTournamentFunctionalityTCC(
                    tournamentService, causalUnitOfWorkService, tournamentAggregateId, unitOfWork);

            functionality.executeWorkflow(unitOfWork);
            return functionality.getTournamentDto();
        }

        /*
        change this 
        if (functionality.getTournamentDto() == null) {
            throw new TutorException(AGGREGATE_NOT_FOUND, tournamentAggregateId);
        }
        */
    }

    public UserDto findParticipant(Integer tournamentAggregateId, Integer userAggregateId) throws TutorException {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        if ("sagas".equals(workflowType)) {
            SagaUnitOfWork unitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
            FindParticipantFunctionalitySagas functionality = new FindParticipantFunctionalitySagas(
                    tournamentService, sagaUnitOfWorkService, tournamentAggregateId, userAggregateId, unitOfWork);

            functionality.executeWorkflow(unitOfWork);
            return functionality.getParticipant();
        } else {
            CausalUnitOfWork unitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
            FindParticipantFunctionalityTCC functionality = new FindParticipantFunctionalityTCC(
                    causalUnitOfWorkService, tournamentAggregateId, userAggregateId, unitOfWork);

            functionality.executeWorkflow(unitOfWork);
            return functionality.getParticipant();
        }
    }

    /** FOR TESTING PURPOSES **/
    public void getTournamentAndUser(Integer tournamentAggregateId, Integer userAggregateId) {
        if ("sagas".equals(workflowType)) {
            SagaUnitOfWork unitOfWork = sagaUnitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
            TournamentDto tournamentDto = tournamentService.getTournamentById(tournamentAggregateId, unitOfWork);
            UserDto userDto = userService.getUserById(userAggregateId, unitOfWork);
        } else {
            CausalUnitOfWork unitOfWork = causalUnitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
            TournamentDto tournamentDto = tournamentService.getTournamentById(tournamentAggregateId, unitOfWork);
            UserDto userDto = userService.getUserById(userAggregateId, unitOfWork);
        }
    }

    private void checkInput(Integer userId, List<Integer> topicsId, TournamentDto tournamentDto) {
        if (userId == null) {
            throw new TutorException(TOURNAMENT_MISSING_USER);
        }
        if (topicsId == null) {
            throw new TutorException(TOURNAMENT_MISSING_TOPICS);
        }
        if (tournamentDto.getStartTime() == null) {
            throw new TutorException(TOURNAMENT_MISSING_START_TIME);
        }
        if (tournamentDto.getEndTime() == null) {
            throw new TutorException(TOURNAMENT_MISSING_END_TIME);
        }
        if (tournamentDto.getNumberOfQuestions() == null) {
            throw new TutorException(TOURNAMENT_MISSING_NUMBER_OF_QUESTIONS);
        }
    }

}
