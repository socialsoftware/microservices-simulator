package pt.ulisboa.tecnico.socialsoftware.ms.quizzes

import org.springframework.beans.factory.annotation.Autowired
import pt.ulisboa.tecnico.socialsoftware.ms.SpockTest
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.coordination.functionalities.CourseExecutionFunctionalities
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.coordination.functionalities.QuestionFunctionalities
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.coordination.functionalities.TopicFunctionalities
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.coordination.functionalities.TournamentFunctionalities
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.coordination.functionalities.UserFunctionalities
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.CourseExecutionDto
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.OptionDto
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionDto
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate.TopicDto
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentDto
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserDto
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.utils.DateHandler

import java.time.LocalDateTime

class QuizzesSpockTest extends SpockTest {
    public static final String ANONYMOUS = "ANONYMOUS"

    public static final LocalDateTime TIME_1 = DateHandler.now().plusMinutes(5)
    public static final LocalDateTime TIME_2 = DateHandler.now().plusMinutes(25)
    public static final LocalDateTime TIME_3 = DateHandler.now().plusHours(1).plusMinutes(5)
    public static final LocalDateTime TIME_4 = DateHandler.now().plusHours(1).plusMinutes(25)

    public static final Integer COURSE_EXECUTION_AGGREGATE_ID_1 = 1
    public static final String COURSE_EXECUTION_NAME = 'BLCM'
    public static final String COURSE_EXECUTION_TYPE = 'TECNICO'
    public static final String COURSE_EXECUTION_ACRONYM = 'TESTBLCM'
    public static final String COURSE_EXECUTION_ACADEMIC_TERM = '2022/2023'

    public static final Integer TOPIC_AGGREGATE_ID_1 = 4
    public static final Integer TOPIC_AGGREGATE_ID_2 = 5
    public static final Integer TOPIC_AGGREGATE_ID_3 = 6
    public static final Integer USER_AGGREGATE_ID_1 = 7
    public static final Integer USER_AGGREGATE_ID_2 = 8
    public static final Integer USER_AGGREGATE_ID_3 = 9
    public static final Integer TOURNAMENT_AGGREGATE_ID_1 = 10
    public static final Integer QUIZ_AGGREGATE_ID_1 = 13

    public static final String USER_NAME_1 = "USER_NAME_1"
    public static final String USER_NAME_2 = "USER_NAME_2"
    public static final String USER_NAME_3 = "USER_NAME_3"

    public static final String USER_USERNAME_1 = "USER_USERNAME_1"
    public static final String USER_USERNAME_2 = "USER_USERNAME_2"
    public static final String USER_USERNAME_3 = "USER_USERNAME_3"

    public static final String STUDENT_ROLE = 'STUDENT'


    public static final String ACRONYM_1 = "ACRONYM_1"

    public static final String TOPIC_NAME_1 = "TOPIC_NAME_1"
    public static final String TOPIC_NAME_2 = "TOPIC_NAME_2"
    public static final String TOPIC_NAME_3 = "TOPIC_NAME_3"

    public static final String TITLE_1 = 'Title One'
    public static final String TITLE_2 = 'Title Two'
    public static final String TITLE_3 = 'Title Three'
    public static final String CONTENT_1 = 'Content One'
    public static final String CONTENT_2 = 'Content Two'
    public static final String CONTENT_3 = 'Content Three'
    public static final String OPTION_1 = "Option One"
    public static final String OPTION_2 = "Option Two"
    public static final String OPTION_3 = "Option Three"
    public static final String OPTION_4 = "Option Four"

    @Autowired
    private CourseExecutionFunctionalities courseExecutionFunctionalities
    @Autowired
    private UserFunctionalities userFunctionalities
    @Autowired
    private TopicFunctionalities topicFunctionalities
    @Autowired
    private QuestionFunctionalities questionFunctionalities
    @Autowired
    private TournamentFunctionalities tournamentFunctionalities

    def createCourseExecution(name, type, acronym, term, endDate) {
        def courseExecutionDto = new CourseExecutionDto()
        courseExecutionDto.setName(name)
        courseExecutionDto.setType(type)
        courseExecutionDto.setAcronym(acronym)
        courseExecutionDto.setAcademicTerm(term)
        courseExecutionDto.setEndDate(DateHandler.toISOString(endDate))
        courseExecutionDto = courseExecutionFunctionalities.createCourseExecution(courseExecutionDto)
        courseExecutionDto
    }

    def createUser(name, username, role) {
        def userDto = new UserDto()
        userDto.setName(name)
        userDto.setUsername(username)
        userDto.setRole(role)
        userDto = userFunctionalities.createUser(userDto)
        userFunctionalities.activateUser(userDto.getAggregateId())
        userDto
    }

    def createTopic(courseExecutionDto, name) {
        def topicDto = new TopicDto()
        topicDto.setName(name)
        topicDto = topicFunctionalities.createTopic(courseExecutionDto.getCourseAggregateId(), topicDto)
        topicDto
    }

    def createQuestion(courseExecutionDto, topicDtos, title, content, option1, option2) {
        def questionDto = new QuestionDto()
        questionDto.setTitle(title)
        questionDto.setContent(content)
        def set =  new HashSet<>(topicDtos);
        questionDto.setTopicDto(set)
        def optionDto1 = new OptionDto()
        optionDto1.setSequence(1)
        optionDto1.setCorrect(true)
        optionDto1.setContent(option1)
        def optionDto2 = new OptionDto()
        optionDto2.setSequence(2)
        optionDto2.setCorrect(false)
        optionDto2.setContent(option2)
        questionDto.setOptionDtos([optionDto1,optionDto2])
        questionDto = questionFunctionalities.createQuestion(courseExecutionDto.getCourseAggregateId(), questionDto)
        questionDto
    }

    def createTournament(startTime, endTime, numberOfQuestions, userCreatorId, courseExecutionId, topicIds) {
        def tournamentDto = new TournamentDto()
        tournamentDto.setStartTime(DateHandler.toISOString(startTime))
        tournamentDto.setEndTime(DateHandler.toISOString(endTime))
        tournamentDto.setNumberOfQuestions(numberOfQuestions)
        tournamentDto = tournamentFunctionalities.createTournament(userCreatorId, courseExecutionId, topicIds, tournamentDto)
        tournamentDto
    }
}
