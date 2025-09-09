package pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.aggregate;

import java.util.Optional;
import java.util.List;
import java.util.Set;

public interface UserCustomRepository {
    Optional<Integer> findUserIdByUsername(String username);
}