package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.aggregate.QuizAnswerDto;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentParticipant;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.service.TournamentFunctionalitiesInterface;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.service.TopicService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.service.QuizAnswerService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.service.CourseExecutionService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.service.QuizService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.service.UserService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.TutorException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage.*;

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

    public TournamentDto createTournament(Integer userId, Integer executionId, List<Integer> topicsId,
                                          TournamentDto tournamentDto) throws Exception {
        //unit of work code
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        try {
            checkInput(userId, topicsId, tournamentDto);

            // by making this call the invariants regarding the course execution and the role of the creator are guaranteed
            UserDto creatorDto = courseExecutionService.getStudentByExecutionIdAndUserId(executionId, userId, unitOfWork);

            CourseExecutionDto courseExecutionDto = courseExecutionService.getCourseExecutionById(executionId, unitOfWork);

            Set<TopicDto> topicDtos = topicsId.stream()
                    .map(topicId -> topicService.getTopicById(topicId, unitOfWork))
                    .collect(Collectors.toSet());

            QuizDto quizDto = new QuizDto();
            quizDto.setAvailableDate(tournamentDto.getStartTime());
            quizDto.setConclusionDate(tournamentDto.getEndTime());
            quizDto.setResultsDate(tournamentDto.getEndTime());
            QuizDto quizResultDto = quizService.generateQuiz(executionId, quizDto, topicsId, tournamentDto.getNumberOfQuestions(), unitOfWork);

    //        NUMBER_OF_QUESTIONS
    //            this.numberOfQuestions == Quiz(tournamentQuiz.id).quizQuestions.size
    //            Quiz(this.tournamentQuiz.id) DEPENDS ON this.numberOfQuestions
    //        QUIZ_TOPICS
    //            Quiz(this.tournamentQuiz.id) DEPENDS ON this.topics // the topics of the quiz questions are related to the tournament topics
    //        START_TIME_AVAILABLE_DATE
    //            this.startTime == Quiz(tournamentQuiz.id).availableDate
    //        END_TIME_CONCLUSION_DATE
    //            this.endTime == Quiz(tournamentQuiz.id).conclusionDate

            if (tournamentDto.getState() == AggregateState.IN_SAGA.toString()) {
                // abortar ou esperar?
                throw new Exception("Error creating tournament: aggregate being changed in other saga");
            }

            //tournament.setState(AggregateState.IN_SAGA);

            TournamentDto tournamentResultDto = tournamentService.createTournament(tournamentDto, creatorDto, courseExecutionDto, topicDtos, quizResultDto, unitOfWork);

            //TODO
            //unitOfWork.registerCompensation(() -> tournamentService.removeTournament(tournamentResultDto.getAggregateId(), unitOfWork));

            unitOfWorkService.commit(unitOfWork);

            //tournamentResultDto.setState(AggregateState.ACTIVE);

            return tournamentResultDto;
        } catch (Exception ex) {
            unitOfWorkService.compensate(unitOfWork);
            throw new Exception("Error creating tournament", ex);
        }
    }

    public void addParticipant(Integer tournamentAggregateId, Integer userAggregateId) throws Exception {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        try {
            TournamentDto tournamentDto = tournamentService.getTournamentById(tournamentAggregateId, unitOfWork);
            // by making this call the invariants regarding the course execution and the role of the participant are guaranteed
            UserDto userDto = courseExecutionService.getStudentByExecutionIdAndUserId(tournamentDto.getCourseExecution().getAggregateId(), userAggregateId, unitOfWork);
            TournamentParticipant participant = new TournamentParticipant(userDto);
            tournamentService.addParticipant(tournamentAggregateId, participant, userDto.getRole(), unitOfWork);
            
            //TODO
            //unitOfWork.registerCompensation(() -> tournamentService.removeParticipant(tournamentAggregateId, userDto.getAggregateId(), unitOfWork));

            unitOfWorkService.commit(unitOfWork);
        } catch (Exception ex) {
            unitOfWorkService.compensate(unitOfWork);
            throw new Exception("Error adding participant to tournament", ex);
        }
    }

    public void updateTournament(TournamentDto tournamentDto, Set<Integer> topicsAggregateIds) throws Exception {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        try {
            //checkInput(topicsAggregateIds, tournamentDto);

            Set<TopicDto> topicDtos = topicsAggregateIds.stream()
                    .map(topicAggregateId -> topicService.getTopicById(topicAggregateId, unitOfWork))
                    .collect(Collectors.toSet());

            TournamentDto originalTournamentDto = tournamentService.getTournamentById(tournamentDto.getAggregateId(), unitOfWork);

            TournamentDto newTournamentDto = tournamentService.updateTournament(tournamentDto, topicDtos, unitOfWork);

            QuizDto quizDto = new QuizDto();
            quizDto.setAggregateId(newTournamentDto.getQuiz().getAggregateId());
            quizDto.setAvailableDate(newTournamentDto.getStartTime());
            quizDto.setConclusionDate(newTournamentDto.getEndTime());
            quizDto.setResultsDate(newTournamentDto.getEndTime());

    //        NUMBER_OF_QUESTIONS
    //		    this.numberOfQuestions == Quiz(tournamentQuiz.id).quizQuestions.size
    //		    Quiz(this.tournamentQuiz.id) DEPENDS ON this.numberOfQuestions
    //        QUIZ_TOPICS
    //            Quiz(this.tournamentQuiz.id) DEPENDS ON this.topics // the topics of the quiz questions are related to the tournament topics
    //        START_TIME_AVAILABLE_DATE
    //            this.startTime == Quiz(tournamentQuiz.id).availableDate
    //        END_TIME_CONCLUSION_DATE
    //            this.endTime == Quiz(tournamentQuiz.id).conclusionDate

            /* this if is required for the case of updating a quiz and not altering neither the number of questions neither the topics */
            if (topicsAggregateIds != null || tournamentDto.getNumberOfQuestions() != null) {
                if (topicsAggregateIds == null) {
                    quizService.updateGeneratedQuiz(quizDto, newTournamentDto.getTopics().stream().filter(t -> t.getState().equals(Aggregate.AggregateState.ACTIVE.toString())).map(TopicDto::getAggregateId).collect(Collectors.toSet()), newTournamentDto.getNumberOfQuestions(), unitOfWork);
                } else {
                    quizService.updateGeneratedQuiz(quizDto, topicsAggregateIds, newTournamentDto.getNumberOfQuestions(), unitOfWork);
                }
            }
            //quizService.updateGeneratedQuiz(quizDto, topicsAggregateIds, newTournamentDto.getNumberOfQuestions(), unitOfWork);

            //TODO
            unitOfWork.registerCompensation(() -> tournamentService.updateTournament(originalTournamentDto, originalTournamentDto.getTopics(), unitOfWork));    

            unitOfWorkService.commit(unitOfWork);
        } catch (Exception ex) {
            unitOfWorkService.compensate(unitOfWork);
            throw new Exception("Error updating tournament", ex);
        }
    }

    public List<TournamentDto> getTournamentsForCourseExecution(Integer executionAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        return tournamentService.getTournamentsByCourseExecutionId(executionAggregateId, unitOfWork);
    }

    public List<TournamentDto> getOpenedTournamentsForCourseExecution(Integer executionAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        return tournamentService.getOpenedTournamentsForCourseExecution(executionAggregateId, unitOfWork);
    }

    public List<TournamentDto> getClosedTournamentsForCourseExecution(Integer executionAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        return tournamentService.getClosedTournamentsForCourseExecution(executionAggregateId, unitOfWork);
    }

    public void leaveTournament(Integer tournamentAggregateId, Integer userAggregateId) throws Exception {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        try {
            tournamentService.leaveTournament(tournamentAggregateId, userAggregateId, unitOfWork);

            //TODO
            //unitOfWork.registerCompensation(() -> tournamentService.addParticipant(tournamentAggregateId, userAggregateId));    

            unitOfWorkService.commit(unitOfWork);
        } catch (Exception ex) {
            unitOfWorkService.compensate(unitOfWork);
            throw new Exception("Error leaving tournament", ex);
        }
    }

    public QuizDto solveQuiz(Integer tournamentAggregateId, Integer userAggregateId) throws Exception {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        try {
            TournamentDto tournamentDto = tournamentService.getTournamentById(tournamentAggregateId, unitOfWork);
            QuizDto quizDto = quizService.startTournamentQuiz(userAggregateId, tournamentDto.getQuiz().getAggregateId(), unitOfWork);
            QuizAnswerDto quizAnswerDto = quizAnswerService.startQuiz(tournamentDto.getQuiz().getAggregateId(), tournamentDto.getCourseExecution().getAggregateId(), userAggregateId, unitOfWork);
            tournamentService.solveQuiz(tournamentAggregateId, userAggregateId, quizAnswerDto.getAggregateId(), unitOfWork);

            //TODO
            //unitOfWork.registerCompensation(() -> tournamentService.unsolveQuiz(tournamentAggregateId, userAggregateId));    

            unitOfWorkService.commit(unitOfWork);
            return quizDto;
        } catch (Exception ex) {
            unitOfWorkService.compensate(unitOfWork);
            throw new Exception("Error solving tournament quiz", ex);
        }
    }

    public void cancelTournament(Integer tournamentAggregateId) throws Exception {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        try {
            tournamentService.cancelTournament(tournamentAggregateId, unitOfWork);

            //TODO
            //unitOfWork.registerCompensation(() -> tournamentService.uncancel(tournamentAggregateId, userAggregateId));

            unitOfWorkService.commit(unitOfWork);
        } catch (Exception ex) {
            unitOfWorkService.compensate(unitOfWork);
            throw new Exception("Error cancelling tournament", ex);
        }
    }

    public void removeTournament(Integer tournamentAggregateId) throws Exception {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());

        try {
            tournamentService.removeTournament(tournamentAggregateId, unitOfWork);

            //TODO
            //unitOfWork.registerCompensation(() -> tournamentService.addTournament(tournamentAggregateId, userAggregateId));
            
            unitOfWorkService.commit(unitOfWork);
        } catch (Exception ex) {
            unitOfWorkService.compensate(unitOfWork);
            throw new Exception("Error removing tournament", ex);
        }
    }

    public TournamentDto findTournament(Integer tournamentAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        return tournamentService.getTournamentById(tournamentAggregateId, unitOfWork);
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
