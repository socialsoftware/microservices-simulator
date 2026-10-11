package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.aggregate;

import java.util.Optional;

public interface TrainTypeCustomRepository {
    Optional<Integer> findTrainTypeIdByName(String name);
}
