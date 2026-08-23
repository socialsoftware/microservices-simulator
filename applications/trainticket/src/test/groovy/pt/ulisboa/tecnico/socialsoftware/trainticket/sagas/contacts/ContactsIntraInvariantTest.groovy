package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.contacts

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest
import pt.ulisboa.tecnico.socialsoftware.trainticket.enums.DocumentType
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.ContactsDto
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.sagas.SagaContacts
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketException

import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketErrorMessage.CONTACTS_DOCUMENT_NUMBER_PRESENT

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class ContactsIntraInvariantTest extends TrainticketSpockTest {

    private static SagaContacts contactsWith(DocumentType documentType, String documentNumber) {
        return new SagaContacts(1, new ContactsDto(CONTACTS_USER_AGGREGATE_ID, CONTACTS_NAME,
                documentType, documentNumber, CONTACTS_PHONE_NUMBER))
    }

    def "create contacts"() {
        // Spec: plan.md §5 Contacts - field list (userAggregateId, name, documentType, documentNumber, phoneNumber)
        when:
        def contacts = contactsWith(CONTACTS_DOCUMENT_TYPE, CONTACTS_DOCUMENT_NUMBER)
        contacts.verifyInvariants()

        then:
        contacts.aggregateId == 1
        contacts.userAggregateId == CONTACTS_USER_AGGREGATE_ID
        contacts.name == CONTACTS_NAME
        contacts.documentType == CONTACTS_DOCUMENT_TYPE
        contacts.documentNumber == CONTACTS_DOCUMENT_NUMBER
        contacts.phoneNumber == CONTACTS_PHONE_NUMBER
        contacts.state == Aggregate.AggregateState.ACTIVE
        contacts.sagaState == GenericSagaState.NOT_IN_SAGA
    }

    def "contacts: CONTACTS_DOCUMENT_NUMBER_PRESENT violation - document type is set and the number is null"() {
        // Spec: plan.md §3.1 - CONTACTS_DOCUMENT_NUMBER_PRESENT
        given:
        def contacts = contactsWith(CONTACTS_DOCUMENT_TYPE, CONTACTS_DOCUMENT_NUMBER)
        contacts.setDocumentNumber(null)

        when:
        contacts.verifyInvariants()

        then:
        def ex = thrown(TrainticketException)
        ex.message == CONTACTS_DOCUMENT_NUMBER_PRESENT
    }

    def "contacts: CONTACTS_DOCUMENT_NUMBER_PRESENT violation - document type is set and the number is blank"() {
        // Spec: plan.md §3.1 - CONTACTS_DOCUMENT_NUMBER_PRESENT
        given:
        def contacts = contactsWith(CONTACTS_DOCUMENT_TYPE_TWO, CONTACTS_DOCUMENT_NUMBER_BLANK)

        when:
        contacts.verifyInvariants()

        then:
        def ex = thrown(TrainticketException)
        ex.message == CONTACTS_DOCUMENT_NUMBER_PRESENT
    }

    def "contacts: CONTACTS_DOCUMENT_NUMBER_PRESENT satisfied - document type is NONE and the number is blank"() {
        // Spec: plan.md §3.1 - CONTACTS_DOCUMENT_NUMBER_PRESENT, the implication's vacuous class
        given:
        def contacts = contactsWith(CONTACTS_DOCUMENT_TYPE_NONE, CONTACTS_DOCUMENT_NUMBER_BLANK)

        when:
        contacts.verifyInvariants()

        then:
        notThrown(TrainticketException)
        contacts.documentType == CONTACTS_DOCUMENT_TYPE_NONE
        contacts.documentNumber == CONTACTS_DOCUMENT_NUMBER_BLANK
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
