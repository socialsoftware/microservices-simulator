package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.coordination.user

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.aggregate.Role

@DataJpaTest
@Transactional
@Import(GetStudentsTest.LocalBeanConfiguration)
class GetStudentsTest extends QuizzesFull2SpockTest {

    def "getStudents: success"() {
        // Spec: plan.md §2 User — GetStudents()
        given: 'two students and one teacher exist'
        def firstStudentAggregateId = createUser(USER_NAME, USER_USERNAME, Role.STUDENT)
        def secondStudentAggregateId = createUser("Bob Jones", "bob", Role.STUDENT)
        def teacherAggregateId = createUser("Carol Reed", "carol", Role.TEACHER)

        when:
        def result = userFunctionalities.getStudents()

        then: 'orchestration outcome only — persistence is asserted in T2'
        result.size() == 2
        result.collect { it.aggregateId } as Set == [firstStudentAggregateId, secondStudentAggregateId] as Set
        result.find { it.aggregateId == secondStudentAggregateId }.name == "Bob Jones"
        sagaStateOf(firstStudentAggregateId) == GenericSagaState.NOT_IN_SAGA
        sagaStateOf(secondStudentAggregateId) == GenericSagaState.NOT_IN_SAGA
        sagaStateOf(teacherAggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
