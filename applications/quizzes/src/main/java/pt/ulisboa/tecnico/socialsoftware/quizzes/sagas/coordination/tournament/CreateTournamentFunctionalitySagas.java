package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.tournament;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.courseExecution.GetCourseExecutionByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.courseExecution.GetStudentByExecutionIdAndUserIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.quiz.GenerateQuizCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.quiz.RemoveQuizCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.topic.GetTopicByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament.CreateTournamentCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.service.CourseExecutionService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.service.QuizService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.service.TopicService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.states.CourseExecutionSagaState;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.states.TopicSagaState;

import java.util.*;
import java.util.stream.Collectors;

public class CreateTournamentFunctionalitySagas extends WorkflowFunctionality {

    private CourseExecutionDto courseExecutionDto;
    private UserDto userDto;
    private HashSet<TopicDto> topicDtos = new HashSet<>();
    private QuizDto quizDto;
    private TournamentDto tournamentDto;
    private final TournamentService tournamentService;
    private final CourseExecutionService courseExecutionService;
    private final TopicService topicService;
    private final QuizService quizService;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway CommandGateway;

    public CreateTournamentFunctionalitySagas(TournamentService tournamentService,
            CourseExecutionService courseExecutionService, TopicService topicService, QuizService quizService,
            SagaUnitOfWorkService unitOfWorkService,
            Integer userId, Integer executionId, List<Integer> topicsId, TournamentDto tournamentDto,
            SagaUnitOfWork unitOfWork, CommandGateway CommandGateway) {
        this.tournamentService = tournamentService;
        this.courseExecutionService = courseExecutionService;
        this.topicService = topicService;
        this.quizService = quizService;
        this.unitOfWorkService = unitOfWorkService;
        this.CommandGateway = CommandGateway;
        this.buildWorkflow(userId, executionId, topicsId, tournamentDto, unitOfWork);
    }

    public void buildWorkflow(Integer userId, Integer executionId, List<Integer> topicsId,
            TournamentDto tournamentDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getCourseExecutionStep = new SagaSyncStep("getCourseExecutionStep", () -> {
            // by making this call locks regarding the course execution are guaranteed
            // CourseExecutionDto courseExecutionDto = (CourseExecutionDto)
            // courseExecutionService.getCourseExecutionById(executionId, unitOfWork);
            // unitOfWorkService.registerSagaState(executionId,
            // CourseExecutionSagaState.READ_COURSE, unitOfWork);
            GetCourseExecutionByIdCommand getCourseExecutionByIdCommand = new GetCourseExecutionByIdCommand(unitOfWork,
                    ServiceMapping.COURSE_EXECUTION.getServiceName(), executionId);
            getCourseExecutionByIdCommand.setSemanticLock(CourseExecutionSagaState.READ_COURSE);
            CourseExecutionDto courseExecutionDto = (CourseExecutionDto) CommandGateway
                    .send(getCourseExecutionByIdCommand);
            this.setCourseExecutionDto(courseExecutionDto);
        });

        SagaSyncStep getCreatorStep = new SagaSyncStep("getCreatorStep", () -> {
            // by making this call locks regarding the role of the creator are guaranteed
            // by making this call the invariants regarding the course execution and the
            // role of the creator are guaranteed
//            UserDto creatorDto = courseExecutionService.getStudentByExecutionIdAndUserId(executionId, userId,
//                    unitOfWork);
            // unitOfWorkService.registerSagaState(userId, UserSagaState.READ_USER,
            // unitOfWork); // TODO calling another aggregate that is not courseExecution
             GetStudentByExecutionIdAndUserIdCommand getStudentByExecutionIdAndUserIdCommand = new GetStudentByExecutionIdAndUserIdCommand(unitOfWork, ServiceMapping.COURSE_EXECUTION.getServiceName(), executionId, userId);
            // getStudentByExecutionIdAndUserIdCommand.setSemanticLock(UserSagaState.READ_USER);
            UserDto creatorDto = (UserDto) CommandGateway.send(getStudentByExecutionIdAndUserIdCommand);
            this.setUserDto(creatorDto);
        }, new ArrayList<>(Arrays.asList(getCourseExecutionStep)));

        SagaSyncStep getTopicsStep = new SagaSyncStep("getTopicsStep", () -> { // TODO EACH TOPIC IN A SEPARATE STEP??
            topicsId.stream().forEach(topicId -> {
                // TopicDto topic = (TopicDto) topicService.getTopicById(topicId, unitOfWork);
                // unitOfWorkService.registerSagaState(topicId, TopicSagaState.READ_TOPIC,
                // unitOfWork);
                GetTopicByIdCommand getTopicByIdCommand = new GetTopicByIdCommand(unitOfWork,
                        ServiceMapping.TOPIC.getServiceName(), topicId);
                getTopicByIdCommand.setSemanticLock(TopicSagaState.READ_TOPIC);
                TopicDto topic = (TopicDto) CommandGateway.send(getTopicByIdCommand);
                this.addTopicDto(topic);
            });
        });

        SagaSyncStep generateQuizStep = new SagaSyncStep("generateQuizStep", () -> {
            QuizDto quizDto = new QuizDto();
            quizDto.setAvailableDate(tournamentDto.getStartTime());
            quizDto.setConclusionDate(tournamentDto.getEndTime());
            quizDto.setResultsDate(tournamentDto.getEndTime());
            // QuizDto quizResultDto = (QuizDto) quizService.generateQuiz(executionId,
            // quizDto, topicsId, tournamentDto.getNumberOfQuestions(), unitOfWork);
            GenerateQuizCommand generateQuizCommand = new GenerateQuizCommand(unitOfWork,
                    ServiceMapping.QUIZ.getServiceName(), executionId, quizDto, topicsId,
                    tournamentDto.getNumberOfQuestions());
            QuizDto quizResultDto = (QuizDto) CommandGateway.send(generateQuizCommand);
            this.setQuizDto(quizResultDto);
        }, new ArrayList<>(Arrays.asList(getTopicsStep)));

        generateQuizStep.registerCompensation(() -> {
            if (this.getQuizDto() != null) {
                // quizService.removeQuiz(this.getQuizDto().getAggregateId(), unitOfWork);
                RemoveQuizCommand removeQuizCommand = new RemoveQuizCommand(unitOfWork,
                        ServiceMapping.QUIZ.getServiceName(), this.getQuizDto().getAggregateId());
                CommandGateway.send(removeQuizCommand);
            }
        }, unitOfWork);

        // NUMBER_OF_QUESTIONS
        // this.numberOfQuestions == Quiz(tournamentQuiz.id).quizQuestions.size
        // Quiz(this.tournamentQuiz.id) DEPENDS ON this.numberOfQuestions
        // QUIZ_TOPICS
        // Quiz(this.tournamentQuiz.id) DEPENDS ON this.topics // the topics of the quiz
        // questions are related to the tournament topics
        // START_TIME_AVAILABLE_DATE
        // this.startTime == Quiz(tournamentQuiz.id).availableDate
        // END_TIME_CONCLUSION_DATE
        // this.endTime == Quiz(tournamentQuiz.id).conclusionDate

        SagaSyncStep createTournamentStep = new SagaSyncStep("createTournamentStep", () -> {
            // TournamentDto tournamentResultDto =
            // tournamentService.createTournament(tournamentDto, this.getUserDto(),
            // this.getCourseExecutionDto(), this.getTopicsDtos(), this.getQuizDto(),
            // unitOfWork);
            CreateTournamentCommand createTournamentCommand = new CreateTournamentCommand(unitOfWork,
                    ServiceMapping.TOURNAMENT.getServiceName(), tournamentDto, this.getUserDto(),
                    this.getCourseExecutionDto(), this.getTopicsDtos(), this.getQuizDto());
            TournamentDto tournamentResultDto = (TournamentDto) CommandGateway.send(createTournamentCommand);
            this.setTournamentDto(tournamentResultDto);
        }, new ArrayList<>(Arrays.asList(getCreatorStep, getCourseExecutionStep, getTopicsStep, generateQuizStep)));

        this.workflow.addStep(getCreatorStep);
        this.workflow.addStep(getCourseExecutionStep);
        this.workflow.addStep(getTopicsStep);
        this.workflow.addStep(generateQuizStep);
        this.workflow.addStep(createTournamentStep);
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

    public Set<TopicDto> getTopicsDtos() {
        return topicDtos.stream()
                .map(TopicDto -> (TopicDto) TopicDto)
                .collect(Collectors.toSet());
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