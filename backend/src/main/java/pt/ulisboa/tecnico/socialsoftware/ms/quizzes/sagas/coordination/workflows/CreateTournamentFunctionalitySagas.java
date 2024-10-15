package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.service.CourseExecutionService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.service.QuizService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.service.TopicService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.dtos.SagaCourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.dtos.SagaTopicDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.states.CourseExecutionSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.states.TopicSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.states.UserSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class CreateTournamentFunctionalitySagas extends WorkflowFunctionality {
    
    private SagaCourseExecutionDto courseExecutionDto;
    private UserDto userDto;
    private HashSet<SagaTopicDto> topicDtos = new HashSet<SagaTopicDto>();
    private QuizDto quizDto;
    private TournamentDto tournamentDto;
    private final TournamentService tournamentService;
    private final CourseExecutionService courseExecutionService;
    private final TopicService topicService;
    private final QuizService quizService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public CreateTournamentFunctionalitySagas(TournamentService tournamentService, CourseExecutionService courseExecutionService, TopicService topicService, QuizService quizService, SagaUnitOfWorkService unitOfWorkService, 
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
        
        SagaSyncStep getCourseExecutionStep = new SagaSyncStep("getCourseExecutionStep", () -> {
            // by making this call locks regarding the course execution are guaranteed
            SagaCourseExecutionDto courseExecutionDto = (SagaCourseExecutionDto) courseExecutionService.getCourseExecutionById(executionId, unitOfWork);
            unitOfWorkService.registerSagaState(executionId, CourseExecutionSagaState.READ_COURSE, unitOfWork);
            this.setCourseExecutionDto(courseExecutionDto);
        });
    
        
        SagaSyncStep getCreatorStep = new SagaSyncStep("getCreatorStep", () -> {
            // by making this call locks regarding the role of the creator are guaranteed
            // by making this call the invariants regarding the course execution and the role of the creator are guaranteed
            UserDto creatorDto = courseExecutionService.getStudentByExecutionIdAndUserId(executionId, userId, unitOfWork);
            unitOfWorkService.registerSagaState(userId, UserSagaState.READ_USER, unitOfWork);
            this.setUserDto(creatorDto);
        }, new ArrayList<>(Arrays.asList(getCourseExecutionStep)));
        

        SagaSyncStep getTopicsStep = new SagaSyncStep("getTopicsStep", () -> {
            topicsId.stream().forEach(topicId -> {
                SagaTopicDto topic = (SagaTopicDto) topicService.getTopicById(topicId, unitOfWork);
                unitOfWorkService.registerSagaState(topicId, TopicSagaState.READ_TOPIC, unitOfWork);
                this.addTopicDto(topic);
            });
        });

        SagaSyncStep generateQuizStep = new SagaSyncStep("generateQuizStep", () -> {
            QuizDto quizDto = new QuizDto();
            quizDto.setAvailableDate(tournamentDto.getStartTime());
            quizDto.setConclusionDate(tournamentDto.getEndTime());
            quizDto.setResultsDate(tournamentDto.getEndTime());
            QuizDto quizResultDto = (QuizDto) quizService.generateQuiz(executionId, quizDto, topicsId, tournamentDto.getNumberOfQuestions(), unitOfWork);
            this.setQuizDto(quizResultDto);
        }, new ArrayList<>(Arrays.asList(getTopicsStep)));

        generateQuizStep.registerCompensation(() -> {
            if (this.getQuizDto() != null) {
                quizService.removeQuiz(this.getQuizDto().getAggregateId(), unitOfWork);
            }
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

        SagaSyncStep createTournamentStep = new SagaSyncStep("createTournamentStep", () -> {
            TournamentDto tournamentResultDto = tournamentService.createTournament(tournamentDto, this.getUserDto(), this.getCourseExecutionDto(), this.getTopicsDtos(), this.getQuizDto(), unitOfWork);
            this.setTournamentDto(tournamentResultDto);
        }, new ArrayList<>(Arrays.asList(getCreatorStep, getCourseExecutionStep, getTopicsStep, generateQuizStep)));

        this.workflow.addStep(getCreatorStep);
        this.workflow.addStep(getCourseExecutionStep);
        this.workflow.addStep(getTopicsStep);
        this.workflow.addStep(generateQuizStep);
        this.workflow.addStep(createTournamentStep);
    }
    

    public void setCourseExecutionDto(SagaCourseExecutionDto courseExecutionDto) {
        this.courseExecutionDto = courseExecutionDto;
    }

    public SagaCourseExecutionDto getCourseExecutionDto() {
        return courseExecutionDto;
    }

    public void setUserDto(UserDto userDto) {
        this.userDto = userDto;
    }

    public UserDto getUserDto() {
        return userDto;
    }

    public Set<TopicDto> getTopicsDtos() {
        return topicDtos.stream()
            .map(sagaTopicDto -> (TopicDto) sagaTopicDto) 
            .collect(Collectors.toSet());
    }

    public void setTopicsDtos(HashSet<SagaTopicDto> topicDtos) {
        this.topicDtos = topicDtos;
    }

    public void addTopicDto(SagaTopicDto topicDto) {
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