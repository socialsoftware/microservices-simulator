package pt.ulisboa.tecnico.socialsoftware.tutorial.sagas.aggregates.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.ulisboa.tecnico.socialsoftware.tutorial.sagas.aggregates.SagaMember;

@Repository
public interface MemberCustomRepositorySagas extends JpaRepository<SagaMember, Integer> {
}