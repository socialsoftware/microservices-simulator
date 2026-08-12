package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@Transactional
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findTopByOrderByVersionDesc();

    default Optional<User> findLatestUser() {
        return findTopByOrderByVersionDesc();
    }
}
