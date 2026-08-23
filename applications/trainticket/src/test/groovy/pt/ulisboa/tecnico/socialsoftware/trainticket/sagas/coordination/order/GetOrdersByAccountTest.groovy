package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.coordination.order

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class GetOrdersByAccountTest extends TrainticketSpockTest {

    def "getOrdersByAccount: success"() {
        // Spec: plan.md §8 Order - GetOrdersByAccount
        given: 'two accounts booking on the same departure'
        def tripAggregateId = createBookableTrip()
        def userAggregateId = createUser()
        def contactsAggregateId = createContacts(userAggregateId)
        def otherUserAggregateId = createUser(USER_NAME_TWO, USER_PASSWORD_TWO, USER_GENDER_TWO,
                USER_DOCUMENT_TYPE_TWO, USER_DOCUMENT_NUMBER_TWO, USER_EMAIL_TWO)
        def otherContactsAggregateId = createContacts(otherUserAggregateId, CONTACTS_NAME_TWO,
                CONTACTS_DOCUMENT_TYPE_TWO, CONTACTS_DOCUMENT_NUMBER_TWO, CONTACTS_PHONE_NUMBER_TWO)
        def orderAggregateId = createOrder(userAggregateId, contactsAggregateId, tripAggregateId)
        createOrder(otherUserAggregateId, otherContactsAggregateId, tripAggregateId)

        when:
        def result = orderFunctionalities.getOrdersByAccount(userAggregateId)

        then: 'the saga returns only the orders of the requested account'
        result.size() == 1
        result[0].aggregateId == orderAggregateId
        result[0].userAggregateId == userAggregateId
        result[0].contactsAggregateId == contactsAggregateId
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
