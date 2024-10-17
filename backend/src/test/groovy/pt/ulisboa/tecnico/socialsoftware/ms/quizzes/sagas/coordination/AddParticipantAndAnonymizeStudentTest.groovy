package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.ms.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventService
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.QuizzesSpockTest
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.coordination.functionalities.CourseExecutionFunctionalities
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.coordination.functionalities.TournamentFunctionalities
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.TutorException
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.CourseExecutionDto
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionDto
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate.TopicDto
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentDto
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.events.handling.TournamentEventHandling
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserDto
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService


@DataJpaTest
class AddParticipantAndAnonymizeStudentTest extends QuizzesSpockTest {
    @Autowired
    private SagaUnitOfWorkService unitOfWorkService

    @Autowired
    private CourseExecutionFunctionalities courseExecutionFunctionalities
    @Autowired
    private TournamentFunctionalities tournamentFunctionalities

    @Autowired
    private EventService eventService;

    @Autowired
    private TournamentEventHandling tournamentEventHandling

    private CourseExecutionDto courseExecutionDto
    private UserDto userCreatorDto, userDto
    private TopicDto topicDto1, topicDto2, topicDto3
    private QuestionDto questionDto1, questionDto2, questionDto3
    private TournamentDto tournamentDto

    def setup() {
        given: 'a course execution'
        courseExecutionDto = createCourseExecution(COURSE_EXECUTION_NAME, COURSE_EXECUTION_TYPE, COURSE_EXECUTION_ACRONYM, COURSE_EXECUTION_ACADEMIC_TERM, TIME_4)

        and: 'a user to enroll in the course execution'
        userCreatorDto = createUser(USER_NAME_1, USER_USERNAME_1, STUDENT_ROLE)
        courseExecutionFunctionalities.addStudent(courseExecutionDto.getAggregateId(), userCreatorDto.getAggregateId())

        and: 'another user to enroll in the course execution'
        userDto = createUser(USER_NAME_2, USER_USERNAME_2, STUDENT_ROLE)
        courseExecutionFunctionalities.addStudent(courseExecutionDto.aggregateId, userDto.aggregateId)

        and: 'three topics'
        topicDto1 = createTopic(courseExecutionDto, TOPIC_NAME_1)
        topicDto2 = createTopic(courseExecutionDto, TOPIC_NAME_2)
        topicDto3 = createTopic(courseExecutionDto, TOPIC_NAME_3)

        and: 'three questions'
        questionDto1 = createQuestion(courseExecutionDto, new HashSet<>(Arrays.asList(topicDto1)), TITLE_1, CONTENT_1, OPTION_1, OPTION_2)
        questionDto2 = createQuestion(courseExecutionDto, new HashSet<>(Arrays.asList(topicDto2)), TITLE_2, CONTENT_2, OPTION_3, OPTION_4)
        questionDto3 = createQuestion(courseExecutionDto, new HashSet<>(Arrays.asList(topicDto3)), TITLE_3, CONTENT_3, OPTION_1, OPTION_3)

        and: 'a tournament where the first user is the creator'
        tournamentDto = createTournament(TIME_1, TIME_3, 2, userCreatorDto.getAggregateId(),  courseExecutionDto.getAggregateId(), [topicDto1.getAggregateId(),topicDto2.getAggregateId()])
    }

    def cleanup() {

    }


    // anonymize creator in course execution and add student in tournament

    def 'sequential anonymize creator and add student: event is processed before add student' () {
        given: 'anonymize creator'
        courseExecutionFunctionalities.anonymizeStudent(courseExecutionDto.aggregateId, userCreatorDto.aggregateId)
        and: 'tournament processes event to anonymize the creator'
        tournamentEventHandling.handleAnonymizeStudentEvents()

        when: 'a student is added to a tournament'
        tournamentFunctionalities.addParticipant(tournamentDto.getAggregateId(), courseExecutionDto.getAggregateId(), userDto.getAggregateId())

        then: 'fails because tournament is inactive'
        def error = thrown(TutorException)
        error.errorMessage == ErrorMessage.CANNOT_MODIFY_INACTIVE_AGGREGATE

        then: 'creator is anonymized'
        def courseExecutionDtoResult = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.getAggregateId())
        courseExecutionDtoResult.getStudents().find{it.aggregateId == userCreatorDto.aggregateId}.name == ANONYMOUS
        courseExecutionDtoResult.getStudents().find{it.aggregateId == userCreatorDto.aggregateId}.username == ANONYMOUS
        and: 'the tournament is inactive and the creator anonymized'
        def tournamentDtoResult = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        tournamentDtoResult.state == Aggregate.AggregateState.INACTIVE.toString()
        tournamentDtoResult.creator.name == ANONYMOUS
        tournamentDtoResult.creator.username == ANONYMOUS
        and: 'there are no participants'
        tournamentDtoResult.getParticipants().size() == 0
    }

    def 'sequential anonymize creator and add student: event is processed after add student' () {
        given: 'anonymize creator'
        courseExecutionFunctionalities.anonymizeStudent(courseExecutionDto.aggregateId, userCreatorDto.aggregateId)

        when: 'a student is added to a tournament'
        tournamentFunctionalities.addParticipant(tournamentDto.getAggregateId(), courseExecutionDto.getAggregateId(), userDto.getAggregateId())

        then: 'tournament processes event to anonymize the creator'
        tournamentEventHandling.handleAnonymizeStudentEvents()
        and: 'creator is anonymized'
        def courseExecutionDtoResult = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.getAggregateId())
        courseExecutionDtoResult.getStudents().find{it.aggregateId == userCreatorDto.aggregateId}.name == ANONYMOUS
        courseExecutionDtoResult.getStudents().find{it.aggregateId == userCreatorDto.aggregateId}.username == ANONYMOUS
        and: 'the tournament is inactive and the creator anonymized'
        def tournamentDtoResult = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        tournamentDtoResult.state == Aggregate.AggregateState.INACTIVE.toString()
        tournamentDtoResult.creator.name == ANONYMOUS
        tournamentDtoResult.creator.username == ANONYMOUS

        and: 'participant was added before event processing'
        tournamentDtoResult.getParticipants().size() == 1
    }

    def 'sequential anonymize creator and add creator: cant add anonymous user' () {
        given: 'anonymize creator'
        courseExecutionFunctionalities.anonymizeStudent(courseExecutionDto.getAggregateId(), userCreatorDto.getAggregateId())

        when: 'the creator is added as participant to a tournament'
        tournamentFunctionalities.addParticipant(tournamentDto.getAggregateId(), courseExecutionDto.getAggregateId(), userCreatorDto.getAggregateId())

        then: 'fails because tournament is deleted'
        def error = thrown(TutorException)
        error.errorMessage == ErrorMessage.USER_IS_ANONYMOUS
    }

    def 'concurrent anonymize creator and add student: anonymize finishes first' () {
        given: 'anonymize creator'
        courseExecutionFunctionalities.anonymizeStudent(courseExecutionDto.aggregateId, userCreatorDto.aggregateId)
        and: 'tournament processes event to anonymize the creator'
        tournamentEventHandling.handleAnonymizeStudentEvents()

        when: 'another student is concurrently added to a tournament where the creator was anonymized in the course execution' +
                'but not in the tournament'
        tournamentFunctionalities.addParticipant(tournamentDto.getAggregateId(), courseExecutionDto.getAggregateId(), userDto.getAggregateId())

        then: 'fails because tournament is inactive'
        def error = thrown(TutorException)
        error.errorMessage == ErrorMessage.CANNOT_MODIFY_INACTIVE_AGGREGATE

        then: 'creator is anonymized'
        def courseExecutionDtoResult = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.getAggregateId())
        courseExecutionDtoResult.getStudents().find{it.aggregateId == userCreatorDto.aggregateId}.name == ANONYMOUS
        courseExecutionDtoResult.getStudents().find{it.aggregateId == userCreatorDto.aggregateId}.username == ANONYMOUS
        and: 'the tournament is inactive and the creator anonymized'
        def tournamentDtoResult = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        tournamentDtoResult.state == Aggregate.AggregateState.INACTIVE.toString()
        tournamentDtoResult.creator.name == ANONYMOUS
        tournamentDtoResult.creator.username == ANONYMOUS
        and: 'there are no participants'
        tournamentDtoResult.getParticipants().size() == 0
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}