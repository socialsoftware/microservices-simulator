package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.train.aggregate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import jakarta.transaction.Transactional;

@Repository
@Transactional
public interface TrainRepository extends JpaRepository<Train, Integer> {

}