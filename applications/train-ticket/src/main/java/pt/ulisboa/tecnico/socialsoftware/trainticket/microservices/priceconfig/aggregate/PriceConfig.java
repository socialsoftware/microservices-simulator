package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.events.subscribe.PriceConfigSubscribesRouteDeleted;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.events.subscribe.PriceConfigSubscribesRouteDeletedPriceConfigRouteExists;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.events.subscribe.PriceConfigSubscribesTrainDeleted;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.events.subscribe.PriceConfigSubscribesTrainDeletedPriceConfigTraintypeExists;

import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.PriceConfigDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.PriceConfigRouteDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.PriceConfigTrainDto;

import static pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage.INVARIANT_BREAK;

@Entity
public abstract class PriceConfig extends Aggregate {
    private Double basicPriceRate;
    private Double firstClassPriceRate;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "priceconfig")
    private PriceConfigTrain trainType;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "priceconfig")
    private PriceConfigRoute route;

    public PriceConfig() {

    }

    public PriceConfig(Integer aggregateId, PriceConfigDto priceConfigDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setBasicPriceRate(priceConfigDto.getBasicPriceRate());
        setFirstClassPriceRate(priceConfigDto.getFirstClassPriceRate());
        setTrainType(priceConfigDto.getTrainType() != null ? new PriceConfigTrain(priceConfigDto.getTrainType()) : null);
        setRoute(priceConfigDto.getRoute() != null ? new PriceConfigRoute(priceConfigDto.getRoute()) : null);
    }


    public PriceConfig(PriceConfig other) {
        super(other);
        setBasicPriceRate(other.getBasicPriceRate());
        setFirstClassPriceRate(other.getFirstClassPriceRate());
        setTrainType(new PriceConfigTrain(other.getTrainType()));
        setRoute(new PriceConfigRoute(other.getRoute()));
    }

    public Double getBasicPriceRate() {
        return basicPriceRate;
    }

    public void setBasicPriceRate(Double basicPriceRate) {
        this.basicPriceRate = basicPriceRate;
    }

    public Double getFirstClassPriceRate() {
        return firstClassPriceRate;
    }

    public void setFirstClassPriceRate(Double firstClassPriceRate) {
        this.firstClassPriceRate = firstClassPriceRate;
    }

    public PriceConfigTrain getTrainType() {
        return trainType;
    }

    public void setTrainType(PriceConfigTrain trainType) {
        this.trainType = trainType;
        if (this.trainType != null) {
            this.trainType.setPriceConfig(this);
        }
    }

    public PriceConfigRoute getRoute() {
        return route;
    }

    public void setRoute(PriceConfigRoute route) {
        this.route = route;
        if (this.route != null) {
            this.route.setPriceConfig(this);
        }
    }


    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        Set<EventSubscription> eventSubscriptions = new HashSet<>();
        if (this.getState() == AggregateState.ACTIVE) {
            interInvariantPriceConfigTraintypeExists(eventSubscriptions);
            interInvariantPriceConfigRouteExists(eventSubscriptions);
            eventSubscriptions.add(new PriceConfigSubscribesTrainDeleted(this));
            eventSubscriptions.add(new PriceConfigSubscribesRouteDeleted(this));
        }
        return eventSubscriptions;
    }
    private void interInvariantPriceConfigTraintypeExists(Set<EventSubscription> eventSubscriptions) {
        eventSubscriptions.add(new PriceConfigSubscribesTrainDeletedPriceConfigTraintypeExists(this.getTrainType()));
    }

    private void interInvariantPriceConfigRouteExists(Set<EventSubscription> eventSubscriptions) {
        eventSubscriptions.add(new PriceConfigSubscribesRouteDeletedPriceConfigRouteExists(this.getRoute()));
    }


    private boolean invariantBasicPriceRateNonNegative() {
        return basicPriceRate >= 0;
    }

    private boolean invariantFirstClassPriceRateNonNegative() {
        return firstClassPriceRate >= 0;
    }

    private boolean invariantTrainTypeSet() {
        return this.trainType != null;
    }

    private boolean invariantRouteSet() {
        return this.route != null;
    }
    @Override
    public void verifyInvariants() {
        if (!invariantBasicPriceRateNonNegative()) {
            throw new SimulatorException(INVARIANT_BREAK, "Basic price rate cannot be negative");
        }
        if (!invariantFirstClassPriceRateNonNegative()) {
            throw new SimulatorException(INVARIANT_BREAK, "First-class price rate cannot be negative");
        }
        if (!invariantTrainTypeSet()) {
            throw new SimulatorException(INVARIANT_BREAK, "Price config must reference a train type");
        }
        if (!invariantRouteSet()) {
            throw new SimulatorException(INVARIANT_BREAK, "Price config must reference a route");
        }
    }

    public PriceConfigDto buildDto() {
        PriceConfigDto dto = new PriceConfigDto();
        dto.setAggregateId(getAggregateId());
        dto.setVersion(getVersion());
        dto.setState(getState());
        dto.setBasicPriceRate(getBasicPriceRate());
        dto.setFirstClassPriceRate(getFirstClassPriceRate());
        dto.setTrainType(getTrainType() != null ? new PriceConfigTrainDto(getTrainType()) : null);
        dto.setRoute(getRoute() != null ? new PriceConfigRouteDto(getRoute()) : null);
        return dto;
    }
}