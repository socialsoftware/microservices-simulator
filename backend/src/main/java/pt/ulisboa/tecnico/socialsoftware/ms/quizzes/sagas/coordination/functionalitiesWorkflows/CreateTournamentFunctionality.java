package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalitiesWorkflows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.service.CourseExecutionService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.Quiz;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.service.QuizService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.service.TopicService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.Tournament;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.SagaCourseExecution;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.SagaTopic;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class CreateTournamentFunctionality extends WorkflowFunctionality {
    private CourseExecutionDto courseExecutionDto;
    private UserDto userDto;
    private HashSet<TopicDto> topicDtos = new HashSet<TopicDto>();
    private QuizDto quizDto;
    private TournamentDto tournamentDto;

    private SagaWorkflow workflow;

    private final TournamentService tournamentService;
    private final CourseExecutionService courseExecutionService;
    private final TopicService topicService;
    private final QuizService quizService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public CreateTournamentFunctionality(TournamentService tournamentService, CourseExecutionService courseExecutionService, TopicService topicService, QuizService quizService, SagaUnitOfWorkService unitOfWorkService, 
                                Integer userId, Integer executionId, List<Integer> topicsId, TournamentDto tournamentDto, 
                                SagaUnitOfWork unitOfWork) {
        this.tournamentService = tournamentService;
        this.courseExecutionService = courseExecutionService;
        this.topicService = topicService;
        this.quizService = quizService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(userId, executionId, topicsId, tournamentDto, unitOfWork);
    }

    public void buildWorkflow(Integer userId, Integer executionId, List<Integer> topicsId,
                                          TournamentDto tournamentDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep getCreatorStep = new SyncStep("getCreatorStep", () -> {
            // by making this call the invariants regarding the course execution and the role of the creator are guaranteed
            UserDto creatorDto = courseExecutionService.getStudentByExecutionIdAndUserId(executionId, userId, unitOfWork);
            SagaCourseExecution courseExecution = (SagaCourseExecution) unitOfWorkService.aggregateLoadAndRegisterRead(executionId, unitOfWork);
            unitOfWorkService.registerSagaState(courseExecution, SagaState.CREATE_TOURNAMENT_READ_CREATOR, unitOfWork);
            this.setUserDto(creatorDto);
        });

        getCreatorStep.registerCompensation(() -> {
            SagaCourseExecution courseExecution = (SagaCourseExecution) unitOfWorkService.aggregateLoadAndRegisterRead(executionId, unitOfWork);
            unitOfWorkService.registerSagaState(courseExecution, SagaState.NOT_IN_SAGA, unitOfWork);
        }, unitOfWork);

        SyncStep getCourseExecutionStep = new SyncStep("getCourseExecutionStep", () -> {
            CourseExecutionDto courseExecutionDto = courseExecutionService.getCourseExecutionById(executionId, unitOfWork);
            SagaCourseExecution courseExecution = (SagaCourseExecution) unitOfWorkService.aggregateLoadAndRegisterRead(courseExecutionDto.getAggregateId(), unitOfWork);
            unitOfWorkService.registerSagaState(courseExecution, SagaState.CREATE_TOURNAMENT_READ_COURSE, unitOfWork);
            this.setCourseExecutionDto(courseExecutionDto);
        });

        getCourseExecutionStep.registerCompensation(() -> {
            CourseExecutionDto courseExecutionDto = this.getCourseExecutionDto();
            SagaCourseExecution courseExecution = (SagaCourseExecution) unitOfWorkService.aggregateLoadAndRegisterRead(courseExecutionDto.getAggregateId(), unitOfWork);
            unitOfWorkService.registerSagaState(courseExecution, SagaState.NOT_IN_SAGA, unitOfWork);
        }, unitOfWork);

        SyncStep getTopicsStep = new SyncStep("getTopicsStep", () -> {
            //TODO change other steps that affect multiple aggregates to be like this
            topicsId.stream().forEach(topicId -> {
                TopicDto t = topicService.getTopicById(topicId, unitOfWork);
                SagaTopic topic = (SagaTopic) unitOfWorkService.aggregateLoadAndRegisterRead(t.getAggregateId(), unitOfWork);
                unitOfWorkService.registerSagaState(topic, SagaState.CREATE_TOURNAMENT_READ_TOPIC, unitOfWork);
                this.addTopicDto(t);
            });
        });

        getTopicsStep.registerCompensation(() -> {
            Set<TopicDto> topicDtos = this.getTopicsDtos();
            topicDtos.stream().forEach(t -> {
                SagaTopic topic = (SagaTopic) unitOfWorkService.aggregateLoadAndRegisterRead(t.getAggregateId(), unitOfWork);
                unitOfWorkService.registerSagaState(topic, SagaState.NOT_IN_SAGA, unitOfWork);
            });
        }, unitOfWork);

        SyncStep generateQuizStep = new SyncStep("generateQuizStep", () -> {
            QuizDto quizDto = new QuizDto();
            quizDto.setAvailableDate(tournamentDto.getStartTime());
            quizDto.setConclusionDate(tournamentDto.getEndTime());
            quizDto.setResultsDate(tournamentDto.getEndTime());
            QuizDto quizResultDto = quizService.generateQuiz(executionId, quizDto, topicsId, tournamentDto.getNumberOfQuestions(), unitOfWork);
            this.setQuizDto(quizResultDto);
        }, new ArrayList<>(Arrays.asList(getTopicsStep)));

        generateQuizStep.registerCompensation(() -> {
            QuizDto quizDto = this.getQuizDto();
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

        SyncStep createTournamentStep = new SyncStep("createTournamentStep", () -> {
            TournamentDto tournamentResultDto = tournamentService.createTournament(tournamentDto, this.getUserDto(), this.getCourseExecutionDto(), this.getTopicsDtos(), this.getQuizDto(), unitOfWork);
            this.setTournamentDto(tournamentResultDto);
        }, new ArrayList<>(Arrays.asList(getCreatorStep, getCourseExecutionStep, getTopicsStep, generateQuizStep)));

        createTournamentStep.registerCompensation(() -> {
            Tournament tournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentDto.getAggregateId(), unitOfWork);
            tournament.remove();
            unitOfWork.registerChanged(tournament);
        }, unitOfWork);

        this.workflow.addStep(getCreatorStep);
        this.workflow.addStep(getCourseExecutionStep);
        this.workflow.addStep(getTopicsStep);
        this.workflow.addStep(generateQuizStep);
        this.workflow.addStep(createTournamentStep);
    }

    @Override
    public void handleEvents() {

    }

    public void executeWorkflow(SagaUnitOfWork unitOfWork) {
        workflow.execute(unitOfWork);
    }

    public void executeStepByName(String stepName, SagaUnitOfWork unitOfWork) {
        workflow.executeStepByName(stepName, unitOfWork);
    }

    public void executeUntilStep(String stepName, SagaUnitOfWork unitOfWork) {
        workflow.executeUntilStep(stepName, unitOfWork);
    }

    public void resumeWorkflow(SagaUnitOfWork unitOfWork) {
        workflow.resume(unitOfWork);
    }

    public void setCourseExecutionDto(CourseExecutionDto courseExecutionDto) {
        this.courseExecutionDto = courseExecutionDto;
    }

    public CourseExecutionDto getCourseExecutionDto() {
        return courseExecutionDto;
    }

    public void setUserDto(UserDto userDto) {
        this.userDto = userDto;
    }

    public UserDto getUserDto() {
        return userDto;
    }

    public HashSet<TopicDto> getTopicsDtos() {
        return topicDtos;
    }

    public void setTopicsDtos(HashSet<TopicDto> topicDtos) {
        this.topicDtos = topicDtos;
    }

    public void addTopicDto(TopicDto topicDto) {
        this.topicDtos.add(topicDto);
    }

    public QuizDto getQuizDto() {
        return quizDto;
    }

    public void setQuizDto(QuizDto quizDto) {
        this.quizDto = quizDto;
    }

    public TournamentDto getTournamentDto() {
        return this.tournamentDto;
    }

    public void setTournamentDto(TournamentDto tournamentDto) {
        this.tournamentDto = tournamentDto;
    }

}