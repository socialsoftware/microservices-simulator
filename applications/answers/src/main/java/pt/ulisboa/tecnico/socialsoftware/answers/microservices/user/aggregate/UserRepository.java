package pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.aggregate;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import jakarta.transaction.Transactional;

@Repository
@Transactional
public interface UserRepository extends JpaRepository<User, Integer> {
        @Query(value = "select user.id from User user where user.name = :name AND user.state = 'ACTIVE' AND user.sagaState = 'NOT_IN_SAGA'")
    Optional<Integer> findUserIdByNameForSaga(String name);

    @Query(value = "select user.id from User user where user.username = :username AND user.state = 'ACTIVE' AND user.sagaState = 'NOT_IN_SAGA'")
    Optional<Integer> findUserIdByUsernameForSaga(String username);


    }