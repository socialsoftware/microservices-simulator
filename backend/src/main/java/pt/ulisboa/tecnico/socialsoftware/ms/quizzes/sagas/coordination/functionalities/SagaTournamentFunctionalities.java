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

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.service.QuizAnswerService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.TutorException;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.service.CourseExecutionService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.service.QuizService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.service.TopicService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentParticipant;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.events.handling.TournamentEventHandling;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.service.TournamentFunctionalitiesInterface;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.service.UserService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalitiesWorkflows.AddParticipantFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalitiesWorkflows.CancelTournamentFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalitiesWorkflows.CreateTournamentFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalitiesWorkflows.FindParticipantFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalitiesWorkflows.FindTournamentFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalitiesWorkflows.GetClosedTournamentsForCourseExecutionFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalitiesWorkflows.GetOpenedTournamentsForCourseExecutionFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalitiesWorkflows.GetTournamentsForCourseExecutionFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalitiesWorkflows.LeaveTournamentFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalitiesWorkflows.RemoveTournamentFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalitiesWorkflows.SolveQuizFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalitiesWorkflows.UpdateTournamentFunctionality;
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
    @Autowired
    private TournamentEventHandling tournamentEventHandling;
    @Autowired
    private EventService eventService;

    public TournamentDto createTournament(Integer userId, Integer executionId, List<Integer> topicsId,
                                          TournamentDto tournamentDto) {

        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
        
        checkInput(userId, topicsId, tournamentDto);

        CreateTournamentFunctionality functionality = new CreateTournamentFunctionality(tournamentService, courseExecutionService, topicService, quizService, unitOfWorkService, 
                                                            userId, executionId, topicsId, tournamentDto, 
                                                            unitOfWork);

        functionality.executeWorkflow(unitOfWork);
        return functionality.getTournamentDto();
    }

    public void addParticipant(Integer tournamentAggregateId, Integer userAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);

        AddParticipantFunctionality functionality = new AddParticipantFunctionality(eventService, tournamentEventHandling, tournamentService, courseExecutionService, unitOfWorkService, tournamentAggregateId, userAggregateId, unitOfWork);

        functionality.executeWorkflow(unitOfWork);
    }

    public void updateTournament(TournamentDto tournamentDto, Set<Integer> topicsAggregateIds) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        UpdateTournamentFunctionality functionality = new UpdateTournamentFunctionality(tournamentService, topicService, quizService, unitOfWorkService, tournamentFactory, quizFactory, tournamentDto, topicsAggregateIds, unitOfWork);
        
        functionality.executeWorkflow(unitOfWork);
    }

    public List<TournamentDto> getTournamentsForCourseExecution(Integer executionAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        GetTournamentsForCourseExecutionFunctionality functionality = new GetTournamentsForCourseExecutionFunctionality(tournamentService, unitOfWorkService, executionAggregateId, unitOfWork);        
        
        functionality.executeWorkflow(unitOfWork);
        return functionality.getTournaments();
    }
    
    public List<TournamentDto> getOpenedTournamentsForCourseExecution(Integer executionAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        GetOpenedTournamentsForCourseExecutionFunctionality functionality = new GetOpenedTournamentsForCourseExecutionFunctionality(tournamentService, unitOfWorkService, executionAggregateId, unitOfWork);
        
        functionality.executeWorkflow(unitOfWork);
        return functionality.getOpenedTournaments();
    }
    
    public List<TournamentDto> getClosedTournamentsForCourseExecution(Integer executionAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        GetClosedTournamentsForCourseExecutionFunctionality functionality = new GetClosedTournamentsForCourseExecutionFunctionality(tournamentService, unitOfWorkService, executionAggregateId, unitOfWork);
        
        functionality.executeWorkflow(unitOfWork);
        return functionality.getClosedTournaments();
    }

    public void leaveTournament(Integer tournamentAggregateId, Integer userAggregateId) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        LeaveTournamentFunctionality functionality = new LeaveTournamentFunctionality(tournamentService, unitOfWorkService, tournamentFactory, tournamentAggregateId, userAggregateId, unitOfWork);
    
        functionality.executeWorkflow(unitOfWork);
    }

    public QuizDto solveQuiz(Integer tournamentAggregateId, Integer userAggregateId) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        SolveQuizFunctionality functionality = new SolveQuizFunctionality(tournamentService, quizService, quizAnswerService, unitOfWorkService, tournamentFactory, userAggregateId, userAggregateId, unitOfWork);
        
        functionality.executeWorkflow(unitOfWork);
        return functionality.getQuizDto();
    }

    public void cancelTournament(Integer tournamentAggregateId) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        CancelTournamentFunctionality functionality = new CancelTournamentFunctionality(tournamentService, unitOfWorkService, tournamentFactory, tournamentAggregateId, unitOfWork);

        functionality.executeWorkflow(unitOfWork);
    }

    public void removeTournament(Integer tournamentAggregateId) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        RemoveTournamentFunctionality functionality = new RemoveTournamentFunctionality(tournamentService, unitOfWorkService, tournamentFactory, tournamentAggregateId, unitOfWork);
    
        functionality.executeWorkflow(unitOfWork);
    }

    public TournamentDto findTournament(Integer tournamentAggregateId) throws TutorException {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        FindTournamentFunctionality functionality = new FindTournamentFunctionality(tournamentService, unitOfWorkService, tournamentAggregateId, unitOfWork);
        
        functionality.executeWorkflow(unitOfWork);
        /*
        change this 
        if (functionality.getTournamentDto() == null) {
            throw new TutorException(AGGREGATE_NOT_FOUND, tournamentAggregateId);
        }
        */
        return functionality.getTournamentDto();
    }

    public TournamentParticipant findParticipant(Integer tournamentAggregateId, Integer userAggregateId) throws TutorException {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        FindParticipantFunctionality functionality = new FindParticipantFunctionality(unitOfWorkService, tournamentAggregateId, userAggregateId, unitOfWork);
        
        functionality.executeWorkflow(unitOfWork);
        return functionality.getParticipant();
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
