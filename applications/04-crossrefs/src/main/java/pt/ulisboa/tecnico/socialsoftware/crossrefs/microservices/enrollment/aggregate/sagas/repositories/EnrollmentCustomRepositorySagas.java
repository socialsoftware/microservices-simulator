package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.aggregate.sagas.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.aggregate.sagas.SagaEnrollment;

@Repository
public interface EnrollmentCustomRepositorySagas extends JpaRepository<SagaEnrollment, Integer> {
}