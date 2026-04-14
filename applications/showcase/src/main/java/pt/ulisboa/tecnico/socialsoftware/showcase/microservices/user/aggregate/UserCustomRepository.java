package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.user.aggregate;

import java.util.Set;

public interface UserCustomRepository {
    Set<Integer> findActiveUserIds();
}