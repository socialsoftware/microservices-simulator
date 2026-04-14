package pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.aggregate;

import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import jakarta.transaction.Transactional;

@Repository
@Transactional
public interface UserRepository extends JpaRepository<User, Integer> {
    @Query(value = "select u.aggregateId from User u where u.role = 'STUDENT' AND u.state != 'DELETED'")
    Set<Integer> findStudentIds();
    @Query(value = "select u.aggregateId from User u where u.role = 'TEACHER' AND u.state != 'DELETED'")
    Set<Integer> findTeacherIds();
}