package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.courseExecution.GetCourseExecutionByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.courseExecution.GetStudentByExecutionIdAndUserIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.question.FindQuestionsByTopicIdsCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.quiz.GenerateQuizCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.quiz.RemoveQuizCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.topic.GetTopicByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament.CreateTournamentCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.sagas.states.CourseExecutionSagaState;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.sagas.states.TopicSagaState;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;

import java.util.*;
import java.util.stream.Collectors;

public class CreateTournamentFunctionalitySagas extends WorkflowFunctionality {
    private CourseExecutionDto courseExecutionDto;
    private UserDto userDto;
    private HashSet<TopicDto> topicDtos = new HashSet<>();
    private QuizDto quizDto;
    private TournamentDto tournamentDto;
    private List<QuestionDto> questionDtos;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public CreateTournamentFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                              Integer userId, Integer executionId, List<Integer> topicsId, TournamentDto tournamentDto,
                                              SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(userId, executionId, topicsId, tournamentDto, unitOfWork);
    }

    public void buildWorkflow(Integer userId, Integer executionId, List<Integer> topicsId,
            TournamentDto tournamentDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getCourseExecutionStep = new SagaStep("getCourseExecutionStep", () -> {
            // by making this call locks regarding the course execution are guaranteed
            GetCourseExecutionByIdCommand getCourseExecutionByIdCommand = new GetCourseExecutionByIdCommand(unitOfWork, ServiceMapping.COURSE_EXECUTION.getServiceName(), executionId);
            getCourseExecutionByIdCommand.setSemanticLock(CourseExecutionSagaState.READ_COURSE);
            CourseExecutionDto courseExecutionDto = (CourseExecutionDto) commandGateway.send(getCourseExecutionByIdCommand);
            this.setCourseExecutionDto(courseExecutionDto);
        });

        SagaStep getCreatorStep = new SagaStep("getCreatorStep", () -> {
            // by making this call locks regarding the role of the creator are guaranteed
            // by making this call the invariants regarding the course execution and the
            // role of the creator are guaranteed
            GetStudentByExecutionIdAndUserIdCommand getStudentByExecutionIdAndUserIdCommand = new GetStudentByExecutionIdAndUserIdCommand(unitOfWork, ServiceMapping.COURSE_EXECUTION.getServiceName(), executionId, userId);
            UserDto creatorDto = (UserDto) commandGateway.send(getStudentByExecutionIdAndUserIdCommand);
            this.setUserDto(creatorDto);
        }, new ArrayList<>(Arrays.asList(getCourseExecutionStep)));

        SagaStep getTopicsStep = new SagaStep("getTopicsStep", () -> { // TODO EACH TOPIC IN A SEPARATE STEP??
            topicsId.forEach(topicId -> {
                GetTopicByIdCommand getTopicByIdCommand = new GetTopicByIdCommand(unitOfWork, ServiceMapping.TOPIC.getServiceName(), topicId);
                getTopicByIdCommand.setSemanticLock(TopicSagaState.READ_TOPIC);
                TopicDto topic = (TopicDto) commandGateway.send(getTopicByIdCommand);
                this.addTopicDto(topic);
            });
        });

        SagaStep findQuestionsByTopicIdsStep = new SagaStep("findQuestionsByTopicIdsStep", () -> {
            FindQuestionsByTopicIdsCommand findQuestionsByTopicIdsCommand = new FindQuestionsByTopicIdsCommand(unitOfWork, ServiceMapping.QUESTION.getServiceName(), topicsId);
            this.questionDtos = (List<QuestionDto>) commandGateway.send(findQuestionsByTopicIdsCommand);
        }, new ArrayList<>(Arrays.asList(getTopicsStep)));

        SagaStep getCourseExecutionById = new SagaStep("getCourseExecutionById", () -> {
            GetCourseExecutionByIdCommand getCourseExecutionByIdCommand = new GetCourseExecutionByIdCommand(unitOfWork,  ServiceMapping.COURSE_EXECUTION.getServiceName(), executionId);
            this.courseExecutionDto = (CourseExecutionDto) commandGateway.send(getCourseExecutionByIdCommand);
        }, new ArrayList<>(Arrays.asList(findQuestionsByTopicIdsStep)));

        SagaStep generateQuizStep = new SagaStep("generateQuizStep", () -> {
            QuizDto quizDto = new QuizDto();
            quizDto.setAvailableDate(tournamentDto.getStartTime());
            quizDto.setConclusionDate(tournamentDto.getEndTime());
            quizDto.setResultsDate(tournamentDto.getEndTime());
            GenerateQuizCommand generateQuizCommand = new GenerateQuizCommand(unitOfWork, ServiceMapping.QUIZ.getServiceName(), courseExecutionDto, quizDto, questionDtos, tournamentDto.getNumberOfQuestions());
            QuizDto quizResultDto = (QuizDto) commandGateway.send(generateQuizCommand);
            this.setQuizDto(quizResultDto);
        }, new ArrayList<>(Arrays.asList(findQuestionsByTopicIdsStep, getCourseExecutionById)));

        generateQuizStep.registerCompensation(() -> {
            if (this.getQuizDto() != null) {
                RemoveQuizCommand removeQuizCommand = new RemoveQuizCommand(unitOfWork, ServiceMapping.QUIZ.getServiceName(), this.getQuizDto().getAggregateId());
                commandGateway.send(removeQuizCommand);
            }
        }, unitOfWork);

        SagaStep createTournamentStep = new SagaStep("createTournamentStep", () -> {
            CreateTournamentCommand createTournamentCommand = new CreateTournamentCommand(unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), tournamentDto, this.getUserDto(), this.getCourseExecutionDto(), this.getTopicsDtos(), this.getQuizDto());
            TournamentDto tournamentResultDto = (TournamentDto) commandGateway.send(createTournamentCommand);
            this.setTournamentDto(tournamentResultDto);
        }, new ArrayList<>(Arrays.asList(getCreatorStep, getCourseExecutionStep, getTopicsStep, generateQuizStep)));

        this.workflow.addStep(getCreatorStep);
        this.workflow.addStep(getCourseExecutionStep);
        this.workflow.addStep(getTopicsStep);
        this.workflow.addStep(findQuestionsByTopicIdsStep);
        this.workflow.addStep(getCourseExecutionById);
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