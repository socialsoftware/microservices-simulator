package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.user

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketException
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.aggregate.UserDto

import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketErrorMessage.DUPLICATE_USER_NAME

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

    def "createUser: persisted and readable through a fresh UnitOfWork"() {
        // Spec: plan.md §3 User - CreateUser
        given:
        def userDto = new UserDto(USER_NAME, USER_PASSWORD, USER_GENDER, USER_DOCUMENT_TYPE,
                USER_DOCUMENT_NUMBER, USER_EMAIL)

        when:
        def result = userService.createUser(userDto, unitOfWorkService.createUnitOfWork("createUser"))

        then: 'read back through a second, fresh UnitOfWork'
        result.aggregateId != null
        def readBack = userService.getUserById(result.aggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        readBack.userName == USER_NAME
        readBack.password == USER_PASSWORD
        readBack.gender == USER_GENDER
        readBack.documentType == USER_DOCUMENT_TYPE
        readBack.documentNumber == USER_DOCUMENT_NUMBER
        readBack.email == USER_EMAIL
        readBack.isActive()
    }

    def "createUser: DUPLICATE_USER_NAME violation"() {
        // Spec: plan.md §3 User - rule UNIQUE_USER_NAME (P3, own table)
        given:
        createUser(USER_NAME, USER_PASSWORD, USER_GENDER, USER_DOCUMENT_TYPE,
                USER_DOCUMENT_NUMBER, USER_EMAIL)

        when:
        userService.createUser(new UserDto(USER_NAME, USER_PASSWORD_TWO, USER_GENDER_TWO,
                USER_DOCUMENT_TYPE_TWO, USER_DOCUMENT_NUMBER_TWO, USER_EMAIL_TWO),
                unitOfWorkService.createUnitOfWork("createUser"))

        then:
        def ex = thrown(TrainticketException)
        ex.message == DUPLICATE_USER_NAME
    }

    def "createUser: a user name freed by a soft-deleted user is reusable"() {
        // Spec: plan.md §3 User - rule UNIQUE_USER_NAME constrains active users only
        given:
        def deletedAggregateId = createUser(USER_NAME, USER_PASSWORD, USER_GENDER, USER_DOCUMENT_TYPE,
                USER_DOCUMENT_NUMBER, USER_EMAIL)
        userService.deleteUser(deletedAggregateId, unitOfWorkService.createUnitOfWork("deleteUser"))

        when:
        def result = userService.createUser(new UserDto(USER_NAME, USER_PASSWORD_TWO, USER_GENDER_TWO,
                USER_DOCUMENT_TYPE_TWO, USER_DOCUMENT_NUMBER_TWO, USER_EMAIL_TWO),
                unitOfWorkService.createUnitOfWork("createUser"))

        then:
        result.aggregateId != deletedAggregateId
        def readBack = userService.getUserById(result.aggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        readBack.userName == USER_NAME
        readBack.password == USER_PASSWORD_TWO
        readBack.email == USER_EMAIL_TWO
    }

    def "updateUser: new password, gender, document and email persisted, user name untouched"() {
        // Spec: plan.md §3 User - UpdateUser; rule USER_NAME_FINAL (userName is not updatable)
        given:
        def userAggregateId = createUser(USER_NAME, USER_PASSWORD, USER_GENDER, USER_DOCUMENT_TYPE,
                USER_DOCUMENT_NUMBER, USER_EMAIL)

        when:
        userService.updateUser(userAggregateId,
                new UserDto(USER_NAME_TWO, USER_PASSWORD_TWO, USER_GENDER_TWO,
                        USER_DOCUMENT_TYPE_TWO, USER_DOCUMENT_NUMBER_TWO, USER_EMAIL_TWO),
                unitOfWorkService.createUnitOfWork("updateUser"))

        then: 'read back through a second, fresh UnitOfWork'
        def readBack = userService.getUserById(userAggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        readBack.password == USER_PASSWORD_TWO
        readBack.gender == USER_GENDER_TWO
        readBack.documentType == USER_DOCUMENT_TYPE_TWO
        readBack.documentNumber == USER_DOCUMENT_NUMBER_TWO
        readBack.email == USER_EMAIL_TWO
        readBack.userName == USER_NAME
    }

    def "updateUser: unknown aggregate id"() {
        // Spec: plan.md §3 User - UpdateUser; Path A (aggregateLoadAndRegisterRead)
        when:
        userService.updateUser(NONEXISTENT_AGGREGATE_ID,
                new UserDto(USER_NAME, USER_PASSWORD_TWO, USER_GENDER_TWO,
                        USER_DOCUMENT_TYPE_TWO, USER_DOCUMENT_NUMBER_TWO, USER_EMAIL_TWO),
                unitOfWorkService.createUnitOfWork("updateUser"))

        then:
        thrown(SimulatorException)
    }

    def "deleteUser: soft-deleted user is no longer loadable"() {
        // Spec: plan.md §3 User - DeleteUser (soft delete)
        given:
        def userAggregateId = createUser(USER_NAME, USER_PASSWORD, USER_GENDER, USER_DOCUMENT_TYPE,
                USER_DOCUMENT_NUMBER, USER_EMAIL)

        when:
        userService.deleteUser(userAggregateId, unitOfWorkService.createUnitOfWork("deleteUser"))

        and: 'read back through a second, fresh UnitOfWork'
        userService.getUserById(userAggregateId, unitOfWorkService.createUnitOfWork("check"))

        then: 'DELETED aggregate is not loadable'
        thrown(SimulatorException)
    }

    def "deleteUser: the deleted user stops being an active user"() {
        // Spec: plan.md §3 User - DeleteUser (soft delete)
        given:
        def deletedAggregateId = createUser(USER_NAME, USER_PASSWORD, USER_GENDER, USER_DOCUMENT_TYPE,
                USER_DOCUMENT_NUMBER, USER_EMAIL)
        def survivingAggregateId = createUser(USER_NAME_TWO, USER_PASSWORD_TWO, USER_GENDER_TWO,
                USER_DOCUMENT_TYPE_TWO, USER_DOCUMENT_NUMBER_TWO, USER_EMAIL_TWO)

        when:
        userService.deleteUser(deletedAggregateId, unitOfWorkService.createUnitOfWork("deleteUser"))

        then:
        def remaining = userService.getUsers(unitOfWorkService.createUnitOfWork("check"))
        remaining.collect { it.aggregateId } == [survivingAggregateId]
    }

    def "deleteUser: unknown aggregate id"() {
        // Spec: plan.md §3 User - DeleteUser; Path A (aggregateLoadAndRegisterRead)
        when:
        userService.deleteUser(NONEXISTENT_AGGREGATE_ID,
                unitOfWorkService.createUnitOfWork("deleteUser"))

        then:
        thrown(SimulatorException)
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
