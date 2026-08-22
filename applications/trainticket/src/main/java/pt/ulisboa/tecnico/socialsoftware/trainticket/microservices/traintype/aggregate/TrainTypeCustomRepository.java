package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.aggregate;

import java.util.List;

public interface TrainTypeCustomRepository {
    List<TrainType> findAllLatestActive();
}
