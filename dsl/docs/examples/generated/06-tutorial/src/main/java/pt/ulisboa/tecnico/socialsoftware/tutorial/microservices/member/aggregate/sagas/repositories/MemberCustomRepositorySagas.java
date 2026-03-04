package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.member.aggregate.sagas.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.member.aggregate.sagas.SagaMember;

@Repository
public interface MemberCustomRepositorySagas extends JpaRepository<SagaMember, Integer> {
}