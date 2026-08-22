package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.aggregate.sagas.repositories;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.aggregate.TrainTypeCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.aggregate.TrainTypeRepository;

@Service
@Profile("sagas")
public class TrainTypeCustomRepositorySagas implements TrainTypeCustomRepository {
    @Autowired
    private TrainTypeRepository trainTypeRepository;
}
