package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate;

import java.util.List;

public interface ContactsCustomRepository {
    List<Contacts> findAllLatestActiveByAccount(Integer userAggregateId);
}
