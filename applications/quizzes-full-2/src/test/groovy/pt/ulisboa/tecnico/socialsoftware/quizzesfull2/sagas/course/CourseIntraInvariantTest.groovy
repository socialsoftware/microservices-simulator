package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.course

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.aggregate.sagas.SagaCourse

@DataJpaTest
@Transactional
@Import(CourseIntraInvariantTest.LocalBeanConfiguration)
class CourseIntraInvariantTest extends QuizzesFull2SpockTest {

    def "create course"() {
        // Spec: plan.md §1 Course — CreateCourse(name, type); fields name, type
        when:
        def course = new SagaCourse(COURSE_AGGREGATE_ID, COURSE_NAME, COURSE_TYPE)
        course.verifyInvariants()

        then:
        course.aggregateId == COURSE_AGGREGATE_ID
        course.name == COURSE_NAME
        course.type == COURSE_TYPE
        course.sagaState == GenericSagaState.NOT_IN_SAGA
    }

    // COURSE_NAME_FINAL and COURSE_TYPE_FINAL are Java `final` fields — the compiler enforces
    // them, so testing.md § T1 requires no violation case for either.

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
