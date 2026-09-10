package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.aggregate;

import java.util.List;

public interface UserCustomRepository {
    List<User> findAllLatestActive();
}
