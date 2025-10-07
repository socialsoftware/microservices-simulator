package pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.aggregate;

import java.util.Optional;

public interface UserCustomRepository {
    Optional<Integer> findUserIdByUsername(String username);
}