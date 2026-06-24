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
        // Spec: Course.{name,type} = input; SagaState after commit == NOT_IN_SAGA.
        // Source: plan.md §2.1 Course / createCourse.
        when:
        CourseDto result = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)

        then:
        result != null
        result.name == COURSE_NAME_1
        result.type == COURSE_TYPE_TECNICO
        result.aggregateId != null
        sagaStateOf(result.aggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    def "createCourse: success with EXTERNAL type"() {
        // Spec: Course.{name,type} = input; SagaState after commit == NOT_IN_SAGA.
        // Source: plan.md §2.1 Course / createCourse.
        when:
        CourseDto result = createCourse(COURSE_NAME_2, COURSE_TYPE_EXTERNAL)

        then:
        result != null
        result.name == COURSE_NAME_2
        result.type == COURSE_TYPE_EXTERNAL
        sagaStateOf(result.aggregateId) == GenericSagaState.NOT_IN_SAGA
    }
}
