package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalities;

import static pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage.TOURNAMENT_MISSING_END_TIME;
import static pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage.TOURNAMENT_MISSING_NUMBER_OF_QUESTIONS;
import static pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage.TOURNAMENT_MISSING_START_TIME;
import static pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage.TOURNAMENT_MISSING_TOPICS;
import static pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage.TOURNAMENT_MISSING_USER;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.service.QuizAnswerService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.TutorException;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.service.CourseExecutionService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.service.QuizService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.service.TopicService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.service.TournamentFunctionalitiesInterface;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.service.UserService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data.AddParticipantData;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data.CancelTournamentData;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data.CreateTournamentData;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data.FindTournamentData;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data.GetClosedTournamentsForCourseExecutionData;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data.GetOpenedTournamentsForCourseExecutionData;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data.GetTournamentsForCourseExecutionData;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data.LeaveTournamentData;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data.RemoveTournamentData;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data.SolveQuizData;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data.UpdateTournamentData;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;

@Profile("sagas")
@Service
public class SagaTournamentFunctionalities implements TournamentFunctionalitiesInterface {
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
    @Autowired
    private SagaUnitOfWorkService unitOfWorkService;
    @Autowired
    private TournamentFactory tournamentFactory;
    @Autowired
    private QuizFactory quizFactory;

    public TournamentDto createTournament(Integer userId, Integer executionId, List<Integer> topicsId,
                                          TournamentDto tournamentDto) {

        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
        
        checkInput(userId, topicsId, tournamentDto);

        CreateTournamentData data = new CreateTournamentData(tournamentService, courseExecutionService, topicService, quizService, unitOfWorkService, 
                                                            userId, executionId, topicsId, tournamentDto, 
                                                            unitOfWork);

        data.executeWorkflow(unitOfWork);
        return data.getTournamentDto();
    }

    public void addParticipant(Integer tournamentAggregateId, Integer userAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);

        AddParticipantData data = new AddParticipantData(tournamentService, courseExecutionService, unitOfWorkService, tournamentAggregateId, userAggregateId, unitOfWork);

        data.executeWorkflow(unitOfWork);
    }

    public void updateTournament(TournamentDto tournamentDto, Set<Integer> topicsAggregateIds) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        UpdateTournamentData data = new UpdateTournamentData(tournamentService, topicService, quizService, unitOfWorkService, tournamentFactory, quizFactory, tournamentDto, topicsAggregateIds, unitOfWork);
        
        data.executeWorkflow(unitOfWork);
    }

    public List<TournamentDto> getTournamentsForCourseExecution(Integer executionAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        GetTournamentsForCourseExecutionData data = new GetTournamentsForCourseExecutionData(tournamentService, unitOfWorkService, executionAggregateId, unitOfWork);        
        
        data.executeWorkflow(unitOfWork);
        return data.getTournaments();
    }
    
    public List<TournamentDto> getOpenedTournamentsForCourseExecution(Integer executionAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        GetOpenedTournamentsForCourseExecutionData data = new GetOpenedTournamentsForCourseExecutionData(tournamentService, unitOfWorkService, executionAggregateId, unitOfWork);
        
        data.executeWorkflow(unitOfWork);
        return data.getOpenedTournaments();
    }
    
    public List<TournamentDto> getClosedTournamentsForCourseExecution(Integer executionAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        GetClosedTournamentsForCourseExecutionData data = new GetClosedTournamentsForCourseExecutionData(tournamentService, unitOfWorkService, executionAggregateId, unitOfWork);
        
        data.executeWorkflow(unitOfWork);
        return data.getClosedTournaments();
    }

    public void leaveTournament(Integer tournamentAggregateId, Integer userAggregateId) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        LeaveTournamentData data = new LeaveTournamentData(tournamentService, unitOfWorkService, tournamentFactory, tournamentAggregateId, userAggregateId, unitOfWork);
    
        data.executeWorkflow(unitOfWork);
    }

    public QuizDto solveQuiz(Integer tournamentAggregateId, Integer userAggregateId) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        SolveQuizData data = new SolveQuizData(tournamentService, quizService, quizAnswerService, unitOfWorkService, tournamentFactory, userAggregateId, userAggregateId, unitOfWork);
        
        data.executeWorkflow(unitOfWork);
        return data.getQuizDto();
    }

    public void cancelTournament(Integer tournamentAggregateId) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        CancelTournamentData data = new CancelTournamentData(tournamentService, unitOfWorkService, tournamentFactory, tournamentAggregateId, unitOfWork);

        data.executeWorkflow(unitOfWork);
    }

    public void removeTournament(Integer tournamentAggregateId) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        RemoveTournamentData data = new RemoveTournamentData(tournamentService, unitOfWorkService, tournamentFactory, tournamentAggregateId, unitOfWork);
    
        data.executeWorkflow(unitOfWork);
    }

    public TournamentDto findTournament(Integer tournamentAggregateId) throws TutorException {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        FindTournamentData data = new FindTournamentData(tournamentService, unitOfWorkService, tournamentAggregateId, unitOfWork);
        
        data.executeWorkflow(unitOfWork);
        return data.getTournamentDto();
    }

    /** FOR TESTING PURPOSES **/
    public void getTournamentAndUser(Integer tournamentAggregateId, Integer userAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        TournamentDto tournamentDto = tournamentService.getTournamentById(tournamentAggregateId, unitOfWork);
        UserDto userDto = userService.getUserById(userAggregateId, unitOfWork);
        return;
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
