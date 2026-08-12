package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.coordination.course

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest

@DataJpaTest
@Transactional
@Import(GetCourseByIdTest.LocalBeanConfiguration)
class GetCourseByIdTest extends QuizzesFull2SpockTest {

    def "getCourseById: success"() {
        // Spec: plan.md §1 Course — GetCourseById(courseAggregateId)
        given: 'a course exists'
        def courseAggregateId = createCourse()

        when:
        def result = courseFunctionalities.getCourseById(courseAggregateId)

        then: 'orchestration outcome only — persistence is asserted in T2'
        result.aggregateId == courseAggregateId
        result.name == COURSE_NAME
        result.type == COURSE_TYPE
        sagaStateOf(courseAggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
