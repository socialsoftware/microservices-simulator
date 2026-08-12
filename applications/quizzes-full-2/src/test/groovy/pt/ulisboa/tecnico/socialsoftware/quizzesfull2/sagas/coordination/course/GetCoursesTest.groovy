package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.coordination.course

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.aggregate.CourseType

@DataJpaTest
@Transactional
@Import(GetCoursesTest.LocalBeanConfiguration)
class GetCoursesTest extends QuizzesFull2SpockTest {

    def "getCourses: success"() {
        // Spec: plan.md §1 Course — GetCourses()
        given: 'two courses exist'
        def firstAggregateId = createCourse(COURSE_NAME, COURSE_TYPE)
        def secondAggregateId = createCourse("Distributed Systems", CourseType.EXTERNAL)

        when:
        def result = courseFunctionalities.getCourses()

        then: 'orchestration outcome only — persistence is asserted in T2'
        result.size() == 2
        result.collect { it.aggregateId } as Set == [firstAggregateId, secondAggregateId] as Set
        result.find { it.aggregateId == secondAggregateId }.name == "Distributed Systems"
        sagaStateOf(firstAggregateId) == GenericSagaState.NOT_IN_SAGA
        sagaStateOf(secondAggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
