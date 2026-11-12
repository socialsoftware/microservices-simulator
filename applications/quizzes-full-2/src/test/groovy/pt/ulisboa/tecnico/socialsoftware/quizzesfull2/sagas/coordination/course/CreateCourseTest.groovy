package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.coordination.course

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.aggregate.CourseDto

@DataJpaTest
@Transactional
@Import(CreateCourseTest.LocalBeanConfiguration)
class CreateCourseTest extends QuizzesFull2SpockTest {

    def "createCourse: success"() {
        // Spec: plan.md §1 Course — CreateCourse(name, type)
        given: 'a course to create'
        def courseDto = new CourseDto()
        courseDto.setName(COURSE_NAME)
        courseDto.setType(COURSE_TYPE)

        when:
        def result = courseFunctionalities.createCourse(courseDto)

        then: 'orchestration outcome only — persistence is asserted in T2'
        result.aggregateId != null
        result.name == COURSE_NAME
        result.type == COURSE_TYPE
        sagaStateOf(result.aggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    // Semantic-lock acquisition: CreateCourseFunctionalitySagas has no setSemanticLock step —
    // Course is a DAG root and the create step brings the aggregate into existence
    // (sagas.md § Create Functionality Sagas), so there is no prior state to lock.

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
