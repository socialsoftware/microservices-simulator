package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.aggregate;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface CourseRepository extends JpaRepository<Course, Integer> {
    @Query("select c from Course c " +
            "where c.state = 'ACTIVE' " +
            "and c.version = (select max(c2.version) from Course c2 where c2.aggregateId = c.aggregateId)")
    List<Course> findAllLatestActive();

    @Query(value = "select a1 from Course a1 where a1.aggregateId = :aggregateId AND a1.state = 'ACTIVE' AND a1.version = (select max(a2.version) from Aggregate a2 where a2.aggregateId = :aggregateId)")
    Optional<Course> findLastAggregateVersion(Integer aggregateId);

    Optional<Course> findTopByOrderByVersionDesc();
}
