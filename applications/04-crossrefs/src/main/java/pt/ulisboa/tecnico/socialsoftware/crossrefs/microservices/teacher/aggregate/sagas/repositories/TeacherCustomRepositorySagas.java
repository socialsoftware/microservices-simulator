package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.teacher.aggregate.sagas.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.teacher.aggregate.sagas.SagaTeacher;

@Repository
public interface TeacherCustomRepositorySagas extends JpaRepository<SagaTeacher, Integer> {
}