package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.execution

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.quizzes.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzes.QuizzesSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesException
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.functionalities.ExecutionFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto

@DataJpaTest
class RemoveCourseExecutionTest extends QuizzesSpockTest {
    @Autowired
    private ExecutionFunctionalities courseExecutionFunctionalities

    private CourseExecutionDto courseExecutionDto
    private TopicDto topicDto

    def setup() {
        courseExecutionDto = createCourseExecution(COURSE_EXECUTION_NAME, COURSE_EXECUTION_TYPE, COURSE_EXECUTION_ACRONYM, COURSE_EXECUTION_ACADEMIC_TERM, TIME_4)
        topicDto = createTopic(courseExecutionDto, TOPIC_NAME_1)
    }

    def cleanup() {}

    def "can remove the sole course execution for a course when it has no content"() {
        when:
        courseExecutionFunctionalities.removeCourseExecution(courseExecutionDto.getAggregateId())

        then:
        noExceptionThrown()
    }

    def "cannot remove the sole course execution for a course when it has content"() {
        given: "the course has a question"
        createQuestion(courseExecutionDto, [topicDto], TITLE_1, CONTENT_1, OPTION_1, OPTION_2)

        when:
        courseExecutionFunctionalities.removeCourseExecution(courseExecutionDto.getAggregateId())

        then: 'CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT invariant is violated'
        thrown(QuizzesException)
    }

    def "can remove a non-last course execution for a course even when the course has content"() {
        given: "a second execution tied to the same course (same name+type, different acronym)"
        def courseExecutionDto2 = createCourseExecution(COURSE_EXECUTION_NAME, COURSE_EXECUTION_TYPE, ACRONYM_1, COURSE_EXECUTION_ACADEMIC_TERM, TIME_4)

        and: "the course has a question"
        createQuestion(courseExecutionDto, [topicDto], TITLE_1, CONTENT_1, OPTION_1, OPTION_2)

        when: "removing the first execution (not the last)"
        courseExecutionFunctionalities.removeCourseExecution(courseExecutionDto.getAggregateId())

        then: "no exception — content guard does not apply to non-last executions"
        noExceptionThrown()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
