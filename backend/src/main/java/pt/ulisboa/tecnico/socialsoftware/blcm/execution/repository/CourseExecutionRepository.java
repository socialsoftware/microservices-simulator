package pt.ulisboa.tecnico.socialsoftware.blcm.execution.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pt.ulisboa.tecnico.socialsoftware.blcm.execution.domain.CourseExecution;
import pt.ulisboa.tecnico.socialsoftware.blcm.quiz.Quiz;
import pt.ulisboa.tecnico.socialsoftware.blcm.tournament.domain.Tournament;

import javax.transaction.Transactional;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;


@Repository
@Transactional
public interface CourseExecutionRepository extends JpaRepository<CourseExecution, Integer> {
    @Query(value = "select * from course_executions ce where ce.aggregate_id = :aggregateId AND ce.version <= :maxVersion AND ce.state != 'INACTIVE'", nativeQuery = true)
    Optional<CourseExecution> findByAggregateIdAndVersion(Integer aggregateId, Integer maxVersion);

    @Query(value = "select * from course_executions ce where ce.aggregate_id = :aggregateId AND ce.version > :version AND ce.state != 'INACTIVE'", nativeQuery = true)
    Set<CourseExecution> findConcurrentVersions(Integer aggregateId, Integer version);
}
