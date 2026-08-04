package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.sagas.dtos;

import jakarta.persistence.Convert;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaStateConverter;

import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.Station;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.Station;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.StationDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.sagas.SagaStation;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public class SagaStationDto extends StationDto {
@Convert(converter = SagaStateConverter.class)
private SagaState sagaState;

public SagaStationDto(Station station) {
super((Station) station);
this.sagaState = ((SagaStation)station).getSagaState();
}

public SagaState getSagaState() {
return this.sagaState;
}

public void setSagaState(SagaState sagaState) {
this.sagaState = sagaState;
}
}