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
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.aggregate.QuizAnswerDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.service.QuizAnswerService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.TutorException;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.CourseExecutionDto;
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
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentParticipant;
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
                                          TournamentDto tournamentDto) throws Exception {

        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);

        CreateTournamentData data = new CreateTournamentData();
        SagaWorkflow workflow = new SagaWorkflow(data, unitOfWorkService, functionalityName, unitOfWork);

        SyncStep checkInputStep = new SyncStep(() -> {
            checkInput(userId, topicsId, tournamentDto);
            if (tournamentDto.getState() != null && tournamentDto.getState().equals(AggregateState.IN_SAGA.toString())) {
                // throw new Exception("Error: Tournament aggregate is currently being modified in another saga."); TODO fix this
            }
            tournamentDto.setState(AggregateState.IN_SAGA.toString());
        });

        checkInputStep.registerCompensation(() -> {
            tournamentDto.setState(AggregateState.ACTIVE.toString());
        }, unitOfWork);

        SyncStep getCreatorStep = new SyncStep(() -> {
            // by making this call the invariants regarding the course execution and the role of the creator are guaranteed
            UserDto creatorDto = courseExecutionService.getStudentByExecutionIdAndUserId(executionId, userId, unitOfWork);
            creatorDto.setState(AggregateState.IN_SAGA.toString());
            data.setUserDto(creatorDto);
        }, new ArrayList<>(Arrays.asList(checkInputStep)));

        getCreatorStep.registerCompensation(() -> {
            UserDto creatorDto = data.getUserDto();
            creatorDto.setState(AggregateState.ACTIVE.toString());
            data.setUserDto(creatorDto);
        }, unitOfWork);

        SyncStep getCourseExecutionStep = new SyncStep(() -> {
            CourseExecutionDto courseExecutionDto = courseExecutionService.getCourseExecutionById(executionId, unitOfWork);
            courseExecutionDto.setState(AggregateState.IN_SAGA.toString());
            data.setCourseExecutionDto(courseExecutionDto);
        }, new ArrayList<>(Arrays.asList(checkInputStep)));

        getCourseExecutionStep.registerCompensation(() -> {
            CourseExecutionDto courseExecutionDto = data.getCourseExecutionDto();
            courseExecutionDto.setState(AggregateState.ACTIVE.toString());
            data.setCourseExecutionDto(courseExecutionDto);
        }, unitOfWork);

        SyncStep getTopicsStep = new SyncStep(() -> {
            //TODO change other steps that affect multiple aggregates to be like this
            topicsId.stream().forEach(topicId -> {
                TopicDto t = topicService.getTopicById(topicId, unitOfWork);
                t.setState(AggregateState.IN_SAGA.toString());
                data.addTopicDto(t);
            });

        }, new ArrayList<>(Arrays.asList(checkInputStep)));

        getTopicsStep.registerCompensation(() -> {
            Set<TopicDto> topicDtos = data.getTopicsDtos();
            topicDtos.stream().forEach(t -> t.setState(AggregateState.ACTIVE.toString()));
        }, unitOfWork);

        SyncStep generateQuizStep = new SyncStep(() -> {
            QuizDto quizDto = new QuizDto();
            quizDto.setAvailableDate(tournamentDto.getStartTime());
            quizDto.setConclusionDate(tournamentDto.getEndTime());
            quizDto.setResultsDate(tournamentDto.getEndTime());
            QuizDto quizResultDto = quizService.generateQuiz(executionId, quizDto, topicsId, tournamentDto.getNumberOfQuestions(), unitOfWork);
            quizDto.setState(AggregateState.IN_SAGA.toString());
            data.setQuizDto(quizResultDto);
        }, new ArrayList<>(Arrays.asList(checkInputStep, getTopicsStep)));

        generateQuizStep.registerCompensation(() -> {
            QuizDto quizDto = data.getQuizDto();
            Quiz quiz = (Quiz) unitOfWorkService.aggregateLoadAndRegisterRead(quizDto.getAggregateId(), unitOfWork);
            quiz.remove();
        }, unitOfWork);

    //        NUMBER_OF_QUESTIONS
    //            this.numberOfQuestions == Quiz(tournamentQuiz.id).quizQuestions.size
    //            Quiz(this.tournamentQuiz.id) DEPENDS ON this.numberOfQuestions
    //        QUIZ_TOPICS
    //            Quiz(this.tournamentQuiz.id) DEPENDS ON this.topics // the topics of the quiz questions are related to the tournament topics
    //        START_TIME_AVAILABLE_DATE
    //            this.startTime == Quiz(tournamentQuiz.id).availableDate
    //        END_TIME_CONCLUSION_DATE
    //            this.endTime == Quiz(tournamentQuiz.id).conclusionDate

        SyncStep createTournamentStep = new SyncStep(() -> {
            TournamentDto tournamentResultDto = tournamentService.createTournament(tournamentDto, data.getUserDto(), data.getCourseExecutionDto(), data.getTopicsDtos(), data.getQuizDto(), unitOfWork);
            data.setTournamentDto(tournamentResultDto);
        }, new ArrayList<>(Arrays.asList(checkInputStep, getCreatorStep, getCourseExecutionStep, getTopicsStep, generateQuizStep)));

        createTournamentStep.registerCompensation(() -> {
            Tournament tournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentDto.getAggregateId(), unitOfWork);
            tournament.remove();
            unitOfWork.registerChanged(tournament);
        }, unitOfWork);
        
        workflow.addStep(checkInputStep);
        workflow.addStep(getCreatorStep);
        workflow.addStep(getCourseExecutionStep);
        workflow.addStep(getTopicsStep);
        workflow.addStep(generateQuizStep);
        workflow.addStep(createTournamentStep);
        
        workflow.execute(unitOfWork);
        return data.getTournamentDto();
    }

    public void addParticipant(Integer tournamentAggregateId, Integer userAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);

        AddParticipantData data = new AddParticipantData();
        SagaWorkflow workflow = new SagaWorkflow(data, unitOfWorkService, functionalityName, unitOfWork);

        SyncStep getTournamentStep = new SyncStep(() -> {
            Tournament oldTournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentAggregateId, unitOfWork);
            TournamentDto tournamentDto = tournamentService
                    .getTournamentById(tournamentAggregateId, workflow.getUnitOfWork());
            data.setTournamentDto(tournamentDto);
            data.setTournament(oldTournament);
        });

        SyncStep getUserStep = new SyncStep(() -> {
            TournamentDto tournamentDto = data.getTournamentDto();
            UserDto userDto = courseExecutionService
                    .getStudentByExecutionIdAndUserId(
                            tournamentDto.getCourseExecution().getAggregateId(),
                            userAggregateId,
                            unitOfWork);
            data.setUserDto(userDto);
        }, new ArrayList<>(Arrays.asList(getTournamentStep)));

        SyncStep addParticipantStep = new SyncStep(() -> {
            
            UserDto userDto = data.getUserDto();
            TournamentParticipant participant = new TournamentParticipant(userDto);
            tournamentService.addParticipant(tournamentAggregateId, participant, userDto.getRole(), unitOfWork);
        }, new ArrayList<>(Arrays.asList(getUserStep)));

        addParticipantStep.registerCompensation(() -> {
            unitOfWork.registerChanged(data.getTournament());
        }, unitOfWork);

        workflow.addStep(getTournamentStep);
        workflow.addStep(getUserStep);
        workflow.addStep(addParticipantStep);

        workflow.execute(unitOfWork);
    }

    public void updateTournament(TournamentDto tournamentDto, Set<Integer> topicsAggregateIds) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        UpdateTournamentData data = new UpdateTournamentData();
        SagaWorkflow workflow = new SagaWorkflow(data, unitOfWorkService, functionalityName, unitOfWork);
    
        SyncStep getOriginalTournamentStep = new SyncStep(() -> {
            TournamentDto originalTournamentDto = tournamentService.getTournamentById(tournamentDto.getAggregateId(), unitOfWork);
            if (tournamentDto.getState() != null && tournamentDto.getState().equals(AggregateState.IN_SAGA.toString())) {
                // throw new Exception("Error: Tournament aggregate is currently being modified in another saga."); TODO fix this
            }
            originalTournamentDto.setState(AggregateState.IN_SAGA.toString());
            data.setOriginalTournamentDto(originalTournamentDto);
        });
    
        getOriginalTournamentStep.registerCompensation(() -> {
            TournamentDto originalTournamentDto = data.getOriginalTournamentDto();
            originalTournamentDto.setState(AggregateState.ACTIVE.toString());
            data.setOriginalTournamentDto(originalTournamentDto);
        }, unitOfWork);
    
        SyncStep getTopicsStep = new SyncStep(() -> {
            Set<TopicDto> topicDtos = topicsAggregateIds.stream()
                    .map(topicAggregateId -> topicService.getTopicById(topicAggregateId, unitOfWork))
                    .collect(Collectors.toSet());
            topicDtos.forEach(t -> t.setState(AggregateState.IN_SAGA.toString()));
            data.setTopicsDtos(topicDtos);
        });
    
        getTopicsStep.registerCompensation(() -> {
            Set<TopicDto> topicDtos = data.getTopicsDtos();
            topicDtos.forEach(t -> t.setState(AggregateState.ACTIVE.toString()));
            data.setTopicsDtos(topicDtos);
        }, unitOfWork);
    
        SyncStep getOldTournamentStep = new SyncStep(() -> {
            Tournament oldTournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentDto.getAggregateId(), unitOfWork);
            oldTournament.setState(AggregateState.IN_SAGA);
            data.setOldTournament(oldTournament);
        });
    
        getOldTournamentStep.registerCompensation(() -> {
            Tournament newTournament = tournamentFactory.createTournamentFromExisting(data.getOldTournament());
            unitOfWork.registerChanged(newTournament);
            tournamentDto.setState(AggregateState.ACTIVE.toString());
        }, unitOfWork);
    
        SyncStep updateTournamentStep = new SyncStep(() -> {
            TournamentDto newTournamentDto = tournamentService.updateTournament(tournamentDto, data.getTopicsDtos(), unitOfWork);
            newTournamentDto.setState(AggregateState.IN_SAGA.toString());
            data.setNewTournamentDto(newTournamentDto);
        }, new ArrayList<>(Arrays.asList(getTopicsStep)));
    
        updateTournamentStep.registerCompensation(() -> {
            QuizDto quizDto = new QuizDto();
            quizDto.setAggregateId(data.getNewTournamentDto().getQuiz().getAggregateId());
            quizDto.setAvailableDate(data.getNewTournamentDto().getStartTime());
            quizDto.setConclusionDate(data.getNewTournamentDto().getEndTime());
            quizDto.setResultsDate(data.getNewTournamentDto().getEndTime());
            quizDto.setState(AggregateState.ACTIVE.toString());
            data.setQuizDto(quizDto);
        }, unitOfWork);
    
        SyncStep updateQuizStep = new SyncStep(() -> {
            QuizDto quizDto = new QuizDto();
            quizDto.setAggregateId(data.getNewTournamentDto().getQuiz().getAggregateId());
            quizDto.setAvailableDate(data.getNewTournamentDto().getStartTime());
            quizDto.setConclusionDate(data.getNewTournamentDto().getEndTime());
            quizDto.setResultsDate(data.getNewTournamentDto().getEndTime());
            quizDto.setState(AggregateState.IN_SAGA.toString());
            data.setQuizDto(quizDto);
    
            Quiz oldQuiz = (Quiz) unitOfWorkService.aggregateLoadAndRegisterRead(quizDto.getAggregateId(), unitOfWork);
            oldQuiz.setState(AggregateState.IN_SAGA);
            data.setOldQuiz(oldQuiz);
    
            if (topicsAggregateIds != null || tournamentDto.getNumberOfQuestions() != null) {
                if (topicsAggregateIds == null) {
                    quizService.updateGeneratedQuiz(quizDto, data.getNewTournamentDto().getTopics().stream()
                            .filter(t -> t.getState().equals(AggregateState.ACTIVE.toString()))
                            .map(TopicDto::getAggregateId)
                            .collect(Collectors.toSet()), data.getNewTournamentDto().getNumberOfQuestions(), unitOfWork);
                } else {
                    quizService.updateGeneratedQuiz(quizDto, topicsAggregateIds, data.getNewTournamentDto().getNumberOfQuestions(), unitOfWork);
                }
            }
        }, new ArrayList<>(Arrays.asList(getOriginalTournamentStep, getOldTournamentStep, updateTournamentStep)));
    
        updateQuizStep.registerCompensation(() -> {
            Quiz newQuiz = quizFactory.createQuizFromExisting(data.getOldQuiz());
            unitOfWork.registerChanged(newQuiz);
            data.getQuizDto().setState(AggregateState.ACTIVE.toString());
        }, unitOfWork);
    
        workflow.addStep(getOriginalTournamentStep);
        workflow.addStep(getTopicsStep);
        workflow.addStep(getOldTournamentStep);
        workflow.addStep(updateTournamentStep);
        workflow.addStep(updateQuizStep);
    
        workflow.execute(unitOfWork);
    }

    public List<TournamentDto> getTournamentsForCourseExecution(Integer executionAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        GetTournamentsForCourseExecutionData data = new GetTournamentsForCourseExecutionData();
        SagaWorkflow workflow = new SagaWorkflow(data, unitOfWorkService, functionalityName, unitOfWork);
    
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
        SagaWorkflow workflow = new SagaWorkflow(data, unitOfWorkService, functionalityName, unitOfWork);
    
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
        SagaWorkflow workflow = new SagaWorkflow(data, unitOfWorkService, functionalityName, unitOfWork);
    
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
        SagaWorkflow workflow = new SagaWorkflow(data, unitOfWorkService, functionalityName, unitOfWork);
    
        SyncStep getOldTournamentStep = new SyncStep(() -> {
            Tournament oldTournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentAggregateId, unitOfWork);
            oldTournament.setState(AggregateState.IN_SAGA);
            data.setOldTournament(oldTournament);
        });
    
        getOldTournamentStep.registerCompensation(() -> {
            Tournament newTournament = tournamentFactory.createTournamentFromExisting(data.getOldTournament());
            unitOfWork.registerChanged(newTournament);
            data.getOldTournament().setState(AggregateState.ACTIVE);
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
        SagaWorkflow workflow = new SagaWorkflow(data, unitOfWorkService, functionalityName, unitOfWork);
    
        SyncStep getTournamentStep = new SyncStep(() -> {
            TournamentDto tournamentDto = tournamentService.getTournamentById(tournamentAggregateId, unitOfWork);
            tournamentDto.setState(AggregateState.IN_SAGA.toString());
            data.setTournamentDto(tournamentDto);
        });
    
        getTournamentStep.registerCompensation(() -> {
            TournamentDto tournamentDto = data.getTournamentDto();
            tournamentDto.setState(AggregateState.ACTIVE.toString());
            data.setTournamentDto(tournamentDto);
        }, unitOfWork);
    
        SyncStep startQuizStep = new SyncStep(() -> {
            QuizDto quizDto = quizService.startTournamentQuiz(userAggregateId, data.getTournamentDto().getQuiz().getAggregateId(), unitOfWork);
            quizDto.setState(AggregateState.IN_SAGA.toString());
            data.setQuizDto(quizDto);
        }, new ArrayList<>(Arrays.asList(getTournamentStep)));
    
        startQuizStep.registerCompensation(() -> {
            QuizDto quizDto = data.getQuizDto();
            quizDto.setState(AggregateState.ACTIVE.toString());
            data.setQuizDto(quizDto);
        }, unitOfWork);
    
        SyncStep startQuizAnswerStep = new SyncStep(() -> {
            QuizAnswerDto quizAnswerDto = quizAnswerService.startQuiz(data.getQuizDto().getAggregateId(), data.getTournamentDto().getCourseExecution().getAggregateId(), userAggregateId, unitOfWork);
            quizAnswerDto.setState(AggregateState.IN_SAGA.toString());
            data.setQuizAnswerDto(quizAnswerDto);
        }, new ArrayList<>(Arrays.asList(startQuizStep)));
    
        startQuizAnswerStep.registerCompensation(() -> {
            QuizAnswerDto quizAnswerDto = data.getQuizAnswerDto();
            quizAnswerDto.setState(AggregateState.ACTIVE.toString());
            data.setQuizAnswerDto(quizAnswerDto);
        }, unitOfWork);
    
        SyncStep getOldTournamentStep = new SyncStep(() -> {
            Tournament oldTournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentAggregateId, unitOfWork);
            oldTournament.setState(AggregateState.IN_SAGA);
            data.setOldTournament(oldTournament);
        });
    
        getOldTournamentStep.registerCompensation(() -> {
            Tournament newTournament = tournamentFactory.createTournamentFromExisting(data.getOldTournament());
            unitOfWork.registerChanged(newTournament);
            data.getOldTournament().setState(AggregateState.ACTIVE);
        }, unitOfWork);
    
        SyncStep solveQuizStep = new SyncStep(() -> {
            tournamentService.solveQuiz(tournamentAggregateId, userAggregateId, data.getQuizAnswerDto().getAggregateId(), unitOfWork);
        }, new ArrayList<>(Arrays.asList(startQuizAnswerStep, getOldTournamentStep)));
    
        workflow.addStep(getTournamentStep);
        workflow.addStep(startQuizStep);
        workflow.addStep(startQuizAnswerStep);
        workflow.addStep(getOldTournamentStep);
        workflow.addStep(solveQuizStep);
    
        workflow.execute(unitOfWork);
    
        return data.getQuizDto();
    }

    public void cancelTournament(Integer tournamentAggregateId) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        CancelTournamentData data = new CancelTournamentData();
        SagaWorkflow workflow = new SagaWorkflow(data, unitOfWorkService, functionalityName, unitOfWork);
    
        SyncStep getOldTournamentStep = new SyncStep(() -> {
            Tournament oldTournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentAggregateId, unitOfWork);
            oldTournament.setState(AggregateState.IN_SAGA);
            data.setOldTournament(oldTournament);
        });
    
        getOldTournamentStep.registerCompensation(() -> {
            Tournament newTournament = tournamentFactory.createTournamentFromExisting(data.getOldTournament());
            unitOfWork.registerChanged(newTournament);
            data.getOldTournament().setState(AggregateState.ACTIVE);
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
        SagaWorkflow workflow = new SagaWorkflow(data, unitOfWorkService, functionalityName, unitOfWork);
    
        SyncStep getOldTournamentStep = new SyncStep(() -> {
            Tournament oldTournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentAggregateId, unitOfWork);
            oldTournament.setState(AggregateState.IN_SAGA);
            data.setOldTournament(oldTournament);
        });
    
        getOldTournamentStep.registerCompensation(() -> {
            Tournament newTournament = tournamentFactory.createTournamentFromExisting(data.getOldTournament());
            unitOfWork.registerChanged(newTournament);
            data.getOldTournament().setState(AggregateState.ACTIVE);
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
        SagaWorkflow workflow = new SagaWorkflow(data, unitOfWorkService, functionalityName, unitOfWork);
    
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
