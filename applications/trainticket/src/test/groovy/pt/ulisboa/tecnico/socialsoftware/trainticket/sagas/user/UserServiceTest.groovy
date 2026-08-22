package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.user

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class UserServiceTest extends TrainticketSpockTest {

    def "getUserById: reads back the persisted user through a fresh UnitOfWork"() {
        // Spec: plan.md §3 User - GetUserById
        given:
        def userAggregateId = createUser(USER_NAME, USER_PASSWORD, USER_GENDER, USER_DOCUMENT_TYPE,
                USER_DOCUMENT_NUMBER, USER_EMAIL)

        when:
        def result = userService.getUserById(userAggregateId,
                unitOfWorkService.createUnitOfWork("getUserById"))

        then:
        result.aggregateId == userAggregateId
        result.userName == USER_NAME
        result.password == USER_PASSWORD
        result.gender == USER_GENDER
        result.documentType == USER_DOCUMENT_TYPE
        result.documentNumber == USER_DOCUMENT_NUMBER
        result.email == USER_EMAIL
        result.isActive()
    }

    def "getUserById: unknown aggregate id"() {
        // Spec: plan.md §3 User - GetUserById; Path A (aggregateLoadAndRegisterRead)
        when:
        userService.getUserById(NONEXISTENT_AGGREGATE_ID,
                unitOfWorkService.createUnitOfWork("getUserById"))

        then:
        thrown(SimulatorException)
    }

    def "getUsers: returns every persisted user"() {
        // Spec: plan.md §3 User - GetUsers
        given:
        def firstAggregateId = createUser(USER_NAME, USER_PASSWORD, USER_GENDER, USER_DOCUMENT_TYPE,
                USER_DOCUMENT_NUMBER, USER_EMAIL)
        def secondAggregateId = createUser(USER_NAME_TWO, USER_PASSWORD_TWO, USER_GENDER_TWO,
                USER_DOCUMENT_TYPE_TWO, USER_DOCUMENT_NUMBER_TWO, USER_EMAIL_TWO)

        when:
        def result = userService.getUsers(unitOfWorkService.createUnitOfWork("getUsers"))

        then:
        result.size() == 2
        def first = result.find { it.aggregateId == firstAggregateId }
        def second = result.find { it.aggregateId == secondAggregateId }
        first.userName == USER_NAME
        first.password == USER_PASSWORD
        first.gender == USER_GENDER
        first.documentType == USER_DOCUMENT_TYPE
        first.documentNumber == USER_DOCUMENT_NUMBER
        first.email == USER_EMAIL
        second.userName == USER_NAME_TWO
        second.password == USER_PASSWORD_TWO
        second.gender == USER_GENDER_TWO
        second.documentType == USER_DOCUMENT_TYPE_TWO
        second.documentNumber == USER_DOCUMENT_NUMBER_TWO
        second.email == USER_EMAIL_TWO
    }

    def "getUsers: returns an empty list when no user exists"() {
        // Spec: plan.md §3 User - GetUsers
        when:
        def result = userService.getUsers(unitOfWorkService.createUnitOfWork("getUsers"))

        then:
        result.isEmpty()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
