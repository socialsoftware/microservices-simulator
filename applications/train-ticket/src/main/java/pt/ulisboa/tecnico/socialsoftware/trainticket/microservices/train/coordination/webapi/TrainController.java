package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.train.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.train.coordination.functionalities.TrainFunctionalities;
import org.springframework.http.HttpStatus;
import java.util.List;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.TrainDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.train.coordination.webapi.requestDtos.CreateTrainRequestDto;

@RestController
public class TrainController {
    @Autowired
    private TrainFunctionalities trainFunctionalities;

    @PostMapping("/trains/create")
    @ResponseStatus(HttpStatus.CREATED)
    public TrainDto createTrain(@RequestBody CreateTrainRequestDto createRequest) {
        return trainFunctionalities.createTrain(createRequest);
    }

    @GetMapping("/trains/{trainAggregateId}")
    public TrainDto getTrainById(@PathVariable Integer trainAggregateId) {
        return trainFunctionalities.getTrainById(trainAggregateId);
    }

    @PutMapping("/trains")
    public TrainDto updateTrain(@RequestBody TrainDto trainDto) {
        return trainFunctionalities.updateTrain(trainDto);
    }

    @DeleteMapping("/trains/{trainAggregateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTrain(@PathVariable Integer trainAggregateId) {
        trainFunctionalities.deleteTrain(trainAggregateId);
    }

    @GetMapping("/trains")
    public List<TrainDto> getAllTrains() {
        return trainFunctionalities.getAllTrains();
    }
}
