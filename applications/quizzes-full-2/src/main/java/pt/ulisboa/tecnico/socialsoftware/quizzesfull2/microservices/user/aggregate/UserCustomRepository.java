package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.aggregate;

import java.util.Set;

public interface UserCustomRepository {
    Set<Integer> findUserIdsByRole(Role role);
}
