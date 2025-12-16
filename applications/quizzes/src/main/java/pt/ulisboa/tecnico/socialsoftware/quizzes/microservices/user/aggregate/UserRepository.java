package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@Transactional
public interface UserRepository extends JpaRepository<User, Integer> {
    @Query(value = "select u1 from User u1 where u1.version = (select max(u2.version) from User u2)")
    Optional<User> findLatestUser();
}
