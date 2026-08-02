package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.aggregate;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateRepository;

import java.util.List;

@Repository
public interface CourseRepository extends AggregateRepository {
    @Query("select c from Course c " +
            "where c.state = 'ACTIVE' " +
            "and c.version = (select max(c2.version) from Course c2 where c2.aggregateId = c.aggregateId)")
    List<Course> findAllLatestActive();
}
