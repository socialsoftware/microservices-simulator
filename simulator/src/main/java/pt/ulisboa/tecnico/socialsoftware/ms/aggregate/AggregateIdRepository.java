package pt.ulisboa.tecnico.socialsoftware.ms.aggregate;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AggregateIdRepository extends JpaRepository<AggregateIdGenerator, Integer> {
}
