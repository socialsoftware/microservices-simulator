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
@Import(GetTeachersTest.LocalBeanConfiguration)
class GetTeachersTest extends QuizzesFull2SpockTest {

    def "getTeachers: success"() {
        // Spec: plan.md §2 User — GetTeachers()
        given: 'two teachers and one student exist'
        def firstTeacherAggregateId = createUser("Carol Reed", "carol", Role.TEACHER)
        def secondTeacherAggregateId = createUser("Dave Kent", "dave", Role.TEACHER)
        def studentAggregateId = createUser(USER_NAME, USER_USERNAME, Role.STUDENT)

        when:
        def result = userFunctionalities.getTeachers()

        then: 'orchestration outcome only — persistence is asserted in T2'
        result.size() == 2
        result.collect { it.aggregateId } as Set == [firstTeacherAggregateId, secondTeacherAggregateId] as Set
        result.find { it.aggregateId == secondTeacherAggregateId }.name == "Dave Kent"
        sagaStateOf(firstTeacherAggregateId) == GenericSagaState.NOT_IN_SAGA
        sagaStateOf(secondTeacherAggregateId) == GenericSagaState.NOT_IN_SAGA
        sagaStateOf(studentAggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
