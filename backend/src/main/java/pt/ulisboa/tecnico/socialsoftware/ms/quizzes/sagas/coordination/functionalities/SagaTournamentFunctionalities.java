package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalities;

import static pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage.TOURNAMENT_MISSING_END_TIME;
import static pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage.TOURNAMENT_MISSING_NUMBER_OF_QUESTIONS;
import static pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage.TOURNAMENT_MISSING_START_TIME;
import static pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage.TOURNAMENT_MISSING_TOPICS;
import static pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage.TOURNAMENT_MISSING_USER;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.aggregate.QuizAnswerDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.service.QuizAnswerService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.TutorException;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.service.CourseExecutionService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.Quiz;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.service.QuizService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.service.TopicService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.Tournament;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.service.TournamentFunctionalitiesInterface;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.service.UserService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.SagaQuiz;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.SagaQuizAnswer;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.SagaTopic;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.SagaTournament;
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
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

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
    
        UpdateTournamentData data = new UpdateTournamentData();
        SagaWorkflow workflow = new SagaWorkflow(data, unitOfWorkService, unitOfWork);
    
        SyncStep getOriginalTournamentStep = new SyncStep(() -> {
            TournamentDto originalTournamentDto = tournamentService.getTournamentById(tournamentDto.getAggregateId(), unitOfWork);
            SagaTournament tournament = (SagaTournament) unitOfWorkService.aggregateLoadAndRegisterRead(originalTournamentDto.getAggregateId(), unitOfWork);
            unitOfWorkService.registerSagaState(tournament, SagaState.UPDATE_TOURNAMENT_READ_TOURNAMENT, unitOfWork);
            data.setOriginalTournamentDto(originalTournamentDto);
            data.setOldTournament(tournament);
        });
    
        getOriginalTournamentStep.registerCompensation(() -> {
            TournamentDto originalTournamentDto = data.getOriginalTournamentDto();
            SagaTournament tournament = (SagaTournament) unitOfWorkService.aggregateLoadAndRegisterRead(originalTournamentDto.getAggregateId(), unitOfWork);
            unitOfWorkService.registerSagaState(tournament, SagaState.NOT_IN_SAGA, unitOfWork);
        }, unitOfWork);
    
        SyncStep getTopicsStep = new SyncStep(() -> {
            topicsAggregateIds.stream().forEach(topicId -> {
                TopicDto t = topicService.getTopicById(topicId, unitOfWork);
                SagaTopic topic = (SagaTopic) unitOfWorkService.aggregateLoadAndRegisterRead(t.getAggregateId(), unitOfWork);
                unitOfWorkService.registerSagaState(topic, SagaState.UPDATE_TOURNAMENT_READ_TOPIC, unitOfWork);
                data.addTopic(topic);
            });
        }, new ArrayList<>(Arrays.asList(getOriginalTournamentStep)));

        getTopicsStep.registerCompensation(() -> {
            Set<SagaTopic> topics = data.getTopics();
            topics.stream().forEach(t -> {
                SagaTopic topic = (SagaTopic) unitOfWorkService.aggregateLoadAndRegisterRead(t.getAggregateId(), unitOfWork);
                unitOfWorkService.registerSagaState(topic, SagaState.NOT_IN_SAGA, unitOfWork);
            });
        }, unitOfWork);
    
        SyncStep updateTournamentStep = new SyncStep(() -> {
            TournamentDto newTournamentDto = tournamentService.updateTournament(tournamentDto, data.getTopicsDtos(), unitOfWork);
            SagaTournament newTournament = (SagaTournament) unitOfWorkService.aggregateLoadAndRegisterRead(newTournamentDto.getAggregateId(), unitOfWork);
            unitOfWorkService.registerSagaState(newTournament, SagaState.UPDATE_TOURNAMENT_READ_UPDATED_TOPICS, unitOfWork);
            data.setNewTournamentDto(newTournamentDto);
        }, new ArrayList<>(Arrays.asList(getTopicsStep)));
    
        updateTournamentStep.registerCompensation(() -> {
            Tournament oldTournament = tournamentFactory.createTournamentFromExisting(data.getOldTournament());
            unitOfWorkService.registerSagaState((SagaTournament) oldTournament, SagaState.NOT_IN_SAGA, unitOfWork);
            unitOfWork.registerChanged(oldTournament);
        }, unitOfWork);
    
        SyncStep updateQuizStep = new SyncStep(() -> {
            QuizDto quizDto = new QuizDto();
            quizDto.setAggregateId(data.getNewTournamentDto().getQuiz().getAggregateId());
            quizDto.setAvailableDate(data.getNewTournamentDto().getStartTime());
            quizDto.setConclusionDate(data.getNewTournamentDto().getEndTime());
            quizDto.setResultsDate(data.getNewTournamentDto().getEndTime());    
            data.setQuizDto(quizDto);

            SagaQuiz oldQuiz = (SagaQuiz) unitOfWorkService.aggregateLoadAndRegisterRead(quizDto.getAggregateId(), unitOfWork);
            unitOfWorkService.registerSagaState(oldQuiz, SagaState.IN_SAGA, unitOfWork);
            data.setOldQuiz(oldQuiz);
    
            if (topicsAggregateIds != null || tournamentDto.getNumberOfQuestions() != null) {
                if (topicsAggregateIds == null) {
                    quizService.updateGeneratedQuiz(quizDto, data.getTopics().stream()
                            .filter(t -> t.getSagaState().equals(SagaState.NOT_IN_SAGA))
                            .map(SagaTopic::getAggregateId)
                            .collect(Collectors.toSet()), data.getNewTournamentDto().getNumberOfQuestions(), unitOfWork);
                } else {
                    quizService.updateGeneratedQuiz(quizDto, topicsAggregateIds, data.getNewTournamentDto().getNumberOfQuestions(), unitOfWork);
                }
            }
        }, new ArrayList<>(Arrays.asList(updateTournamentStep)));
    
        updateQuizStep.registerCompensation(() -> {
            Quiz newQuiz = quizFactory.createQuizFromExisting(data.getOldQuiz());
            unitOfWorkService.registerSagaState((SagaQuiz) newQuiz, SagaState.NOT_IN_SAGA, unitOfWork);
            unitOfWork.registerChanged(newQuiz);
        }, unitOfWork);
    
        workflow.addStep(getOriginalTournamentStep);
        workflow.addStep(getTopicsStep);
        workflow.addStep(updateTournamentStep);
        workflow.addStep(updateQuizStep);
    
        workflow.execute(unitOfWork);
    }

    public List<TournamentDto> getTournamentsForCourseExecution(Integer executionAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        GetTournamentsForCourseExecutionData data = new GetTournamentsForCourseExecutionData();
        SagaWorkflow workflow = new SagaWorkflow(data, unitOfWorkService, unitOfWork);
    
        SyncStep getTournamentsStep = new SyncStep(() -> {
            List<TournamentDto> tournaments = tournamentService.getTournamentsByCourseExecutionId(executionAggregateId, unitOfWork);
            data.setTournaments(tournaments);
        });
    
        workflow.addStep(getTournamentsStep);
        workflow.execute(unitOfWork);
    
        return data.getTournaments();
    }
    
    public List<TournamentDto> getOpenedTournamentsForCourseExecution(Integer executionAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        GetOpenedTournamentsForCourseExecutionData data = new GetOpenedTournamentsForCourseExecutionData();
        SagaWorkflow workflow = new SagaWorkflow(data, unitOfWorkService, unitOfWork);
    
        SyncStep getOpenedTournamentsStep = new SyncStep(() -> {
            List<TournamentDto> openedTournaments = tournamentService.getOpenedTournamentsForCourseExecution(executionAggregateId, unitOfWork);
            data.setOpenedTournaments(openedTournaments);
        });
    
        workflow.addStep(getOpenedTournamentsStep);
        workflow.execute(unitOfWork);
    
        return data.getOpenedTournaments();
    }
    
    public List<TournamentDto> getClosedTournamentsForCourseExecution(Integer executionAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        GetClosedTournamentsForCourseExecutionData data = new GetClosedTournamentsForCourseExecutionData();
        SagaWorkflow workflow = new SagaWorkflow(data, unitOfWorkService, unitOfWork);
    
        SyncStep getClosedTournamentsStep = new SyncStep(() -> {
            List<TournamentDto> closedTournaments = tournamentService.getClosedTournamentsForCourseExecution(executionAggregateId, unitOfWork);
            data.setClosedTournaments(closedTournaments);
        });
    
        workflow.addStep(getClosedTournamentsStep);
        workflow.execute(unitOfWork);
    
        return data.getClosedTournaments();
    }

    public void leaveTournament(Integer tournamentAggregateId, Integer userAggregateId) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        LeaveTournamentData data = new LeaveTournamentData();
        SagaWorkflow workflow = new SagaWorkflow(data, unitOfWorkService, unitOfWork);
    
        SyncStep getOldTournamentStep = new SyncStep(() -> {
            SagaTournament oldTournament = (SagaTournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentAggregateId, unitOfWork);
            unitOfWorkService.registerSagaState(oldTournament, SagaState.LEAVE_TOURNAMENT_READ_TOURANMENT, unitOfWork);
            data.setOldTournament(oldTournament);
        });
    
        getOldTournamentStep.registerCompensation(() -> {
            Tournament newTournament = tournamentFactory.createTournamentFromExisting(data.getOldTournament());
            unitOfWorkService.registerSagaState((SagaTournament) newTournament, SagaState.NOT_IN_SAGA, unitOfWork);
            unitOfWork.registerChanged(newTournament);
        }, unitOfWork);
    
        SyncStep leaveTournamentStep = new SyncStep(() -> {
            tournamentService.leaveTournament(tournamentAggregateId, userAggregateId, unitOfWork);
        }, new ArrayList<>(Arrays.asList(getOldTournamentStep)));
    
        workflow.addStep(getOldTournamentStep);
        workflow.addStep(leaveTournamentStep);
    
        workflow.execute(unitOfWork);
    }

    public QuizDto solveQuiz(Integer tournamentAggregateId, Integer userAggregateId) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        SolveQuizData data = new SolveQuizData();
        SagaWorkflow workflow = new SagaWorkflow(data, unitOfWorkService, unitOfWork);
    
        SyncStep getTournamentStep = new SyncStep(() -> {
            TournamentDto tournamentDto = tournamentService.getTournamentById(tournamentAggregateId, unitOfWork);
            SagaTournament tournament = (SagaTournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentAggregateId, unitOfWork);
            unitOfWorkService.registerSagaState(tournament, SagaState.SOLVE_QUIZ_READ_TOURNAMENT, unitOfWork);
            data.setOldTournament(tournament);
            data.setTournamentDto(tournamentDto);
        });
    
        getTournamentStep.registerCompensation(() -> {
            SagaTournament tournament = (SagaTournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentAggregateId, unitOfWork);
            unitOfWorkService.registerSagaState(tournament, SagaState.NOT_IN_SAGA, unitOfWork);
        }, unitOfWork);
    
        SyncStep startQuizStep = new SyncStep(() -> {
            QuizDto quizDto = quizService.startTournamentQuiz(userAggregateId, data.getTournamentDto().getQuiz().getAggregateId(), unitOfWork);
            SagaQuiz quiz = (SagaQuiz) unitOfWorkService.aggregateLoadAndRegisterRead(quizDto.getAggregateId(), unitOfWork);
            unitOfWorkService.registerSagaState(quiz, SagaState.SOLVE_QUIZ_STARTED_TOURNAMENT_QUIZ, unitOfWork);
            data.setQuizDto(quizDto);
        }, new ArrayList<>(Arrays.asList(getTournamentStep)));
    
        startQuizStep.registerCompensation(() -> {
            SagaQuiz quiz = (SagaQuiz) unitOfWorkService.aggregateLoadAndRegisterRead(data.getQuizDto().getAggregateId(), unitOfWork);
            unitOfWorkService.registerSagaState(quiz, SagaState.NOT_IN_SAGA, unitOfWork);
        }, unitOfWork);
    
        SyncStep startQuizAnswerStep = new SyncStep(() -> {
            QuizAnswerDto quizAnswerDto = quizAnswerService.startQuiz(data.getQuizDto().getAggregateId(), data.getTournamentDto().getCourseExecution().getAggregateId(), userAggregateId, unitOfWork);
            SagaQuizAnswer quizAnswer = (SagaQuizAnswer) unitOfWorkService.aggregateLoadAndRegisterRead(quizAnswerDto.getAggregateId(), unitOfWork);
            unitOfWorkService.registerSagaState(quizAnswer, SagaState.SOLVE_QUIZ_STARTED_QUIZ, unitOfWork);
            data.setQuizAnswerDto(quizAnswerDto);
        }, new ArrayList<>(Arrays.asList(startQuizStep)));
    
        startQuizAnswerStep.registerCompensation(() -> {
            QuizAnswerDto quizAnswerDto = data.getQuizAnswerDto();
            quizAnswerDto.setState(SagaState.NOT_IN_SAGA.toString());
        }, unitOfWork);
        
        SyncStep solveQuizStep = new SyncStep(() -> {
            tournamentService.solveQuiz(tournamentAggregateId, userAggregateId, data.getQuizAnswerDto().getAggregateId(), unitOfWork);
        });
        
        solveQuizStep.registerCompensation(() -> {
            Tournament newTournament = tournamentFactory.createTournamentFromExisting(data.getOldTournament());
            unitOfWorkService.registerSagaState((SagaTournament) newTournament, SagaState.NOT_IN_SAGA, unitOfWork);
            unitOfWork.registerChanged(newTournament);
        }, unitOfWork);

        workflow.addStep(getTournamentStep);
        workflow.addStep(startQuizStep);
        workflow.addStep(startQuizAnswerStep);
        workflow.addStep(solveQuizStep);
    
        workflow.execute(unitOfWork);
    
        return data.getQuizDto();
    }

    public void cancelTournament(Integer tournamentAggregateId) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        CancelTournamentData data = new CancelTournamentData();
        SagaWorkflow workflow = new SagaWorkflow(data, unitOfWorkService, unitOfWork);
    
        SyncStep getOldTournamentStep = new SyncStep(() -> {
            SagaTournament oldTournament = (SagaTournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentAggregateId, unitOfWork);
            unitOfWorkService.registerSagaState(oldTournament, SagaState.CANCEL_TOURNAMENT_READ_TOURNAMENT, unitOfWork);
            data.setOldTournament(oldTournament);
        });
    
        getOldTournamentStep.registerCompensation(() -> {
            Tournament newTournament = tournamentFactory.createTournamentFromExisting(data.getOldTournament());
            unitOfWorkService.registerSagaState((SagaTournament) newTournament, SagaState.NOT_IN_SAGA, unitOfWork);
            unitOfWork.registerChanged(newTournament);
        }, unitOfWork);
    
        SyncStep cancelTournamentStep = new SyncStep(() -> {
            tournamentService.cancelTournament(tournamentAggregateId, unitOfWork);
        }, new ArrayList<>(Arrays.asList(getOldTournamentStep)));
    
        workflow.addStep(getOldTournamentStep);
        workflow.addStep(cancelTournamentStep);
    
        workflow.execute(unitOfWork);
    }

    public void removeTournament(Integer tournamentAggregateId) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        RemoveTournamentData data = new RemoveTournamentData();
        SagaWorkflow workflow = new SagaWorkflow(data, unitOfWorkService, unitOfWork);
    
        SyncStep getOldTournamentStep = new SyncStep(() -> {
            SagaTournament oldTournament = (SagaTournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentAggregateId, unitOfWork);
            unitOfWorkService.registerSagaState(oldTournament, SagaState.REMOVE_TOURNAMENT_READ_TOURNAMENT, unitOfWork);
            data.setOldTournament(oldTournament);
        });
    
        getOldTournamentStep.registerCompensation(() -> {
            Tournament newTournament = tournamentFactory.createTournamentFromExisting(data.getOldTournament());
            unitOfWorkService.registerSagaState((SagaTournament) newTournament, SagaState.NOT_IN_SAGA, unitOfWork);
            unitOfWork.registerChanged(newTournament);
        }, unitOfWork);
    
        SyncStep removeTournamentStep = new SyncStep(() -> {
            tournamentService.removeTournament(tournamentAggregateId, unitOfWork);
        }, new ArrayList<>(Arrays.asList(getOldTournamentStep)));
    
        workflow.addStep(getOldTournamentStep);
        workflow.addStep(removeTournamentStep);
    
        workflow.execute(unitOfWork);
    }

    public TournamentDto findTournament(Integer tournamentAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        FindTournamentData data = new FindTournamentData();
        SagaWorkflow workflow = new SagaWorkflow(data, unitOfWorkService, unitOfWork);
    
        SyncStep findTournamentStep = new SyncStep(() -> {
            TournamentDto tournamentDto = tournamentService.getTournamentById(tournamentAggregateId, unitOfWork);
            data.setTournamentDto(tournamentDto);
        });
    
        workflow.addStep(findTournamentStep);
        workflow.execute(unitOfWork);
    
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
