package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import java.io.File;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.impairment.ImpairmentService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.functionalities.ExecutionFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.OptionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.functionalities.QuestionFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.coordination.functionalities.TopicFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.functionalities.TournamentFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.coordination.functionalities.UserFunctionalities;

public class QuizzesTestFactory {

    public static final String ANONYMOUS = "ANONYMOUS";

    public static final LocalDateTime TIME_1 = DateHandler.now().plusMinutes(5);
    public static final LocalDateTime TIME_2 = DateHandler.now().plusMinutes(25);
    public static final LocalDateTime TIME_3 = DateHandler.now().plusHours(1).plusMinutes(5);
    public static final LocalDateTime TIME_4 = DateHandler.now().plusHours(1).plusMinutes(25);

    public static final Integer COURSE_EXECUTION_AGGREGATE_ID_1 = 1;
    public static final String COURSE_EXECUTION_NAME = "BLCM";
    public static final String COURSE_EXECUTION_TYPE = "TECNICO";
    public static final String COURSE_EXECUTION_ACRONYM = "TESTBLCM";
    public static final String COURSE_EXECUTION_ACADEMIC_TERM = "2022/2023";

    public static final Integer TOPIC_AGGREGATE_ID_1 = 4;
    public static final Integer TOPIC_AGGREGATE_ID_2 = 5;
    public static final Integer TOPIC_AGGREGATE_ID_3 = 6;
    public static final Integer USER_AGGREGATE_ID_1 = 7;
    public static final Integer USER_AGGREGATE_ID_2 = 8;
    public static final Integer USER_AGGREGATE_ID_3 = 9;
    public static final Integer TOURNAMENT_AGGREGATE_ID_1 = 10;
    public static final Integer QUIZ_AGGREGATE_ID_1 = 13;

    public static final String USER_NAME_1 = "USER_NAME_1";
    public static final String USER_NAME_2 = "USER_NAME_2";
    public static final String USER_NAME_3 = "USER_NAME_3";

    public static final String USER_USERNAME_1 = "USER_USERNAME_1";
    public static final String USER_USERNAME_2 = "USER_USERNAME_2";
    public static final String USER_USERNAME_3 = "USER_USERNAME_3";

    public static final String STUDENT_ROLE = "STUDENT";
    public static final String ACRONYM_1 = "ACRONYM_1";

    public static final String TOPIC_NAME_1 = "TOPIC_NAME_1";
    public static final String TOPIC_NAME_2 = "TOPIC_NAME_2";
    public static final String TOPIC_NAME_3 = "TOPIC_NAME_3";

    public static final String TITLE_1 = "Title One";
    public static final String TITLE_2 = "Title Two";
    public static final String TITLE_3 = "Title Three";
    public static final String CONTENT_1 = "Content One";
    public static final String CONTENT_2 = "Content Two";
    public static final String CONTENT_3 = "Content Three";
    public static final String OPTION_1 = "Option One";
    public static final String OPTION_2 = "Option Two";
    public static final String OPTION_3 = "Option Three";
    public static final String OPTION_4 = "Option Four";

    private final ExecutionFunctionalities courseExecutionFunctionalities;
    private final UserFunctionalities userFunctionalities;
    private final TopicFunctionalities topicFunctionalities;
    private final QuestionFunctionalities questionFunctionalities;
    private final TournamentFunctionalities tournamentFunctionalities;
    private final ImpairmentService impairmentService;
    private final SagaUnitOfWorkService sagaUnitOfWorkService;

    public QuizzesTestFactory(
            SagaUnitOfWorkService sagaUnitOfWorkService,
            ExecutionFunctionalities courseExecutionFunctionalities,
            UserFunctionalities userFunctionalities,
            TopicFunctionalities topicFunctionalities,
            QuestionFunctionalities questionFunctionalities,
            TournamentFunctionalities tournamentFunctionalities,
            ImpairmentService impairmentService) {

        this.sagaUnitOfWorkService = sagaUnitOfWorkService;
        this.courseExecutionFunctionalities = courseExecutionFunctionalities;
        this.userFunctionalities = userFunctionalities;
        this.topicFunctionalities = topicFunctionalities;
        this.questionFunctionalities = questionFunctionalities;
        this.tournamentFunctionalities = tournamentFunctionalities;
        this.impairmentService = impairmentService;
    }

    // TODO is this needed?
    public void loadBehaviorScripts() {
        var mavenBaseDir = System.getProperty("maven.basedir", new File(".").getAbsolutePath());
        var scriptDir = "groovy/" + this.getClass().getSimpleName();
        impairmentService.LoadDir(mavenBaseDir, scriptDir);
    }

    public CourseExecutionDto createCourseExecution(
            String name, String type, String acronym, String term, LocalDateTime endDate) {

        var courseExecutionDto = new CourseExecutionDto();
        courseExecutionDto.setName(name);
        courseExecutionDto.setType(type);
        courseExecutionDto.setAcronym(acronym);
        courseExecutionDto.setAcademicTerm(term);
        courseExecutionDto.setEndDate(DateHandler.toISOString(endDate));

        CourseExecutionDto createdCourseExecutionDto = courseExecutionFunctionalities
                .createCourseExecution(courseExecutionDto);
        return Objects.requireNonNull(createdCourseExecutionDto);
    }

    public UserDto createUser(String name, String username, String role) {
        var userDto = new UserDto();
        userDto.setName(name);
        userDto.setUsername(username);
        userDto.setRole(role);

        UserDto createdUserDto = userFunctionalities.createUser(userDto);
        Objects.requireNonNull(createdUserDto);

        userFunctionalities.activateUser(createdUserDto.getAggregateId());
        return createdUserDto;
    }

    public TopicDto createTopic(CourseExecutionDto courseExecutionDto, String name) {
        var topicDto = new TopicDto();
        topicDto.setName(name);

        TopicDto createdTopicDto = topicFunctionalities.createTopic(
                courseExecutionDto.getCourseAggregateId(), topicDto);
        return Objects.requireNonNull(createdTopicDto);
    }

    public QuestionDto createQuestion(
            CourseExecutionDto courseExecutionDto, List<TopicDto> topicDtos,
            String title, String content, String correctOption, String wrongOption) {

        var questionDto = new QuestionDto();
        questionDto.setTitle(title);
        questionDto.setContent(content);
        questionDto.setTopicDto(new HashSet<>(topicDtos));

        var optionDto1 = new OptionDto();
        optionDto1.setSequence(1);
        optionDto1.setCorrect(true);
        optionDto1.setContent(correctOption);

        var optionDto2 = new OptionDto();
        optionDto2.setSequence(2);
        optionDto2.setCorrect(false);
        optionDto2.setContent(wrongOption);

        questionDto.setOptionDtos(List.of(optionDto1, optionDto2));

        QuestionDto createdQuestionDto = questionFunctionalities.createQuestion(
                courseExecutionDto.getCourseAggregateId(), questionDto);
        return Objects.requireNonNull(createdQuestionDto);
    }

    public TournamentDto createTournament(LocalDateTime startTime, LocalDateTime endTime, Integer numberOfQuestions,
            Integer userCreatorId, Integer courseExecutionId, List<Integer> topicIds) {

        var tournamentDto = new TournamentDto();
        tournamentDto.setStartTime(DateHandler.toISOString(startTime));
        tournamentDto.setEndTime(DateHandler.toISOString(endTime));
        tournamentDto.setNumberOfQuestions(numberOfQuestions);

        TournamentDto createdTournamentDto = tournamentFunctionalities.createTournament(
                userCreatorId, courseExecutionId, topicIds, tournamentDto);
        return Objects.requireNonNull(createdTournamentDto);
    }

    public SagaState sagaStateOf(Integer aggregateId) {
        SagaUnitOfWork uow = sagaUnitOfWorkService.createUnitOfWork("TEST");
        Aggregate agg = sagaUnitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, uow);
        var sagaAgg = (SagaAggregate) Objects.requireNonNull(agg);
        return Objects.requireNonNull(sagaAgg.getSagaState());
    }
}