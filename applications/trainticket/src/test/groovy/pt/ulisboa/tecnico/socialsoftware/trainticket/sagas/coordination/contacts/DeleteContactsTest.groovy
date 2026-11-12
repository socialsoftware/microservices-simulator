package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.coordination.contacts

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.sagas.states.ContactsSagaState
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.coordination.sagas.DeleteContactsFunctionalitySagas

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class DeleteContactsTest extends TrainticketSpockTest {

    @Autowired
    CommandGateway commandGateway

    // No happy-path case: deleteContacts makes its own aggregate unresolvable, so sagaStateOf throws.
    // See docs/concepts/testing.md § T4 - "Exception — a functionality whose success makes
    // its own aggregate unresolvable". The delete's effect is asserted in ContactsServiceTest (T2).

    def "deleteContacts: getContactsStep acquires IN_DELETE_CONTACTS semantic lock"() {
        // Spec: plan.md §5 Contacts - DeleteContacts; primary-aggregate lock acquisition
        given:
        def contactsAggregateId = createContacts()
        def uow = unitOfWorkService.createUnitOfWork("deleteContacts")
        def func = new DeleteContactsFunctionalitySagas(unitOfWorkService, contactsAggregateId,
                uow, commandGateway)
        func.executeUntilStep("getContactsStep", uow)

        expect: 'acquire transition: NOT_IN_SAGA -> IN_DELETE_CONTACTS'
        sagaStateOf(contactsAggregateId) == ContactsSagaState.IN_DELETE_CONTACTS

        when:
        func.resumeWorkflow(uow)

        then: 'traversal completes'
        noExceptionThrown()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
