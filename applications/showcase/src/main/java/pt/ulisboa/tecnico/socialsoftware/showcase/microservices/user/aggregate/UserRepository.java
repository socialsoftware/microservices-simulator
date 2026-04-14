package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.user.aggregate;

import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import jakarta.transaction.Transactional;

@Repository
@Transactional
public interface UserRepository extends JpaRepository<User, Integer> {
    @Query(value = "select u.aggregateId from User u where u.state != 'DELETED' and u.active = true")
    Set<Integer> findActiveUserIds();
}