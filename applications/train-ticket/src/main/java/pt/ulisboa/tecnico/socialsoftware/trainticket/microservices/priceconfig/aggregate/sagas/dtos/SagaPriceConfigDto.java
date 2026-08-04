package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.sagas.dtos;

import jakarta.persistence.Convert;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaStateConverter;

import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.PriceConfig;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.PriceConfig;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.PriceConfigDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.sagas.SagaPriceConfig;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public class SagaPriceConfigDto extends PriceConfigDto {
@Convert(converter = SagaStateConverter.class)
private SagaState sagaState;

public SagaPriceConfigDto(PriceConfig priceconfig) {
super((PriceConfig) priceconfig);
this.sagaState = ((SagaPriceConfig)priceconfig).getSagaState();
}

public SagaState getSagaState() {
return this.sagaState;
}

public void setSagaState(SagaState sagaState) {
this.sagaState = sagaState;
}
}