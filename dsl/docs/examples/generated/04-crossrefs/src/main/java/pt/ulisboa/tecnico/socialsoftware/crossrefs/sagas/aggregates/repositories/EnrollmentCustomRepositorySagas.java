package pt.ulisboa.tecnico.socialsoftware.crossrefs.sagas.aggregates.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.sagas.aggregates.SagaEnrollment;

@Repository
public interface EnrollmentCustomRepositorySagas extends JpaRepository<SagaEnrollment, Integer> {
}