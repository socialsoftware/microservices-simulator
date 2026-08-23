package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.coordination.contacts

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class GetContactsByAccountTest extends TrainticketSpockTest {

    def "getContactsByAccount: success"() {
        // Spec: plan.md §5 Contacts - GetContactsByAccount
        given: 'two accounts, each owning a contact record'
        def ownedAggregateId = createContacts(CONTACTS_USER_AGGREGATE_ID, CONTACTS_NAME)
        createContacts(CONTACTS_USER_AGGREGATE_ID_TWO, CONTACTS_NAME_TWO)

        when:
        def result = contactsFunctionalities.getContactsByAccount(CONTACTS_USER_AGGREGATE_ID)

        then: 'the saga returns only the contact records of that account'
        result.collect { it.aggregateId } == [ownedAggregateId]
        result[0].userAggregateId == CONTACTS_USER_AGGREGATE_ID
        result[0].name == CONTACTS_NAME
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
