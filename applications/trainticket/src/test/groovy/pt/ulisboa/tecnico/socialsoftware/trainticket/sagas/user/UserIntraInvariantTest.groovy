package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.user

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest
import pt.ulisboa.tecnico.socialsoftware.trainticket.enums.DocumentType
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketException
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.aggregate.Gender
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.aggregate.UserDto
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.aggregate.sagas.SagaUser

import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketErrorMessage.USER_DOCUMENT_NUMBER_PRESENT

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class UserIntraInvariantTest extends TrainticketSpockTest {

    private static SagaUser userWith(Gender gender, DocumentType documentType, String documentNumber) {
        return new SagaUser(1, new UserDto(USER_NAME, USER_PASSWORD, gender, documentType, documentNumber, USER_EMAIL))
    }

    def "create user"() {
        // Spec: plan.md §3 User - field list (userName, password, gender, documentType, documentNumber, email)
        when:
        def user = userWith(USER_GENDER, USER_DOCUMENT_TYPE, USER_DOCUMENT_NUMBER)
        user.verifyInvariants()

        then:
        user.aggregateId == 1
        user.userName == USER_NAME
        user.password == USER_PASSWORD
        user.gender == USER_GENDER
        user.documentType == USER_DOCUMENT_TYPE
        user.documentNumber == USER_DOCUMENT_NUMBER
        user.email == USER_EMAIL
        user.state == Aggregate.AggregateState.ACTIVE
        user.sagaState == GenericSagaState.NOT_IN_SAGA
    }

    def "user: USER_DOCUMENT_NUMBER_PRESENT violation - document type is set and the number is null"() {
        // Spec: plan.md §3.1 - USER_DOCUMENT_NUMBER_PRESENT
        given:
        def user = userWith(USER_GENDER, USER_DOCUMENT_TYPE, USER_DOCUMENT_NUMBER)
        user.setDocumentNumber(null)

        when:
        user.verifyInvariants()

        then:
        def ex = thrown(TrainticketException)
        ex.message == USER_DOCUMENT_NUMBER_PRESENT
    }

    def "user: USER_DOCUMENT_NUMBER_PRESENT violation - document type is set and the number is blank"() {
        // Spec: plan.md §3.1 - USER_DOCUMENT_NUMBER_PRESENT
        given:
        def user = userWith(USER_GENDER, USER_DOCUMENT_TYPE_TWO, USER_DOCUMENT_NUMBER_BLANK)

        when:
        user.verifyInvariants()

        then:
        def ex = thrown(TrainticketException)
        ex.message == USER_DOCUMENT_NUMBER_PRESENT
    }

    def "user: USER_DOCUMENT_NUMBER_PRESENT satisfied - document type is NONE and the number is blank"() {
        // Spec: plan.md §3.1 - USER_DOCUMENT_NUMBER_PRESENT, the implication's vacuous class
        given:
        def user = userWith(USER_GENDER, USER_DOCUMENT_TYPE_NONE, USER_DOCUMENT_NUMBER_BLANK)

        when:
        user.verifyInvariants()

        then:
        notThrown(TrainticketException)
        user.documentType == USER_DOCUMENT_TYPE_NONE
        user.documentNumber == USER_DOCUMENT_NUMBER_BLANK
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
