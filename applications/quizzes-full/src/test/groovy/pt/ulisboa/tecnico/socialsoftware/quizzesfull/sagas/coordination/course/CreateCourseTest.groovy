package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.coordination.course

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.CourseDto
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.CourseType
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class CreateCourseTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    def "createCourse: success"() {
        // Spec: plan.md §1 Course — CreateCourse; orchestration outcome only, persistence in CourseServiceTest.
        when:
        CourseDto result = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)

        then:
        result != null
        result.name == COURSE_NAME_1
        sagaStateOf(result.aggregateId) == GenericSagaState.NOT_IN_SAGA
    }
}
