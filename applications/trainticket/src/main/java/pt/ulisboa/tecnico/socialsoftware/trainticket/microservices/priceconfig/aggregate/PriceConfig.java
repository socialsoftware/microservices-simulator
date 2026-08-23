package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketException;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketErrorMessage.PRICE_RATES_POSITIVE;

@Entity
@Table(name = "price_configs")
public abstract class PriceConfig extends Aggregate {
    // The route and train type references are immutable (domain model §2), so they are final and have no setters.
    @Column(name = "route_aggregate_id")
    private final Integer routeAggregateId;
    @Column(name = "train_type_aggregate_id")
    private final Integer trainTypeAggregateId;
    @Column(name = "basic_price_rate", precision = 19, scale = 4)
    private BigDecimal basicPriceRate;
    @Column(name = "first_class_price_rate", precision = 19, scale = 4)
    private BigDecimal firstClassPriceRate;

    public PriceConfig() {
        this.routeAggregateId = null;
        this.trainTypeAggregateId = null;
    }

    public PriceConfig(Integer aggregateId, PriceConfigDto priceConfigDto) {
        super(aggregateId);
        this.routeAggregateId = priceConfigDto.getRouteAggregateId();
        this.trainTypeAggregateId = priceConfigDto.getTrainTypeAggregateId();
        setBasicPriceRate(priceConfigDto.getBasicPriceRate());
        setFirstClassPriceRate(priceConfigDto.getFirstClassPriceRate());
        setAggregateType(getClass().getSimpleName());
    }

    public PriceConfig(PriceConfig other) {
        super(other);
        this.routeAggregateId = other.getRouteAggregateId();
        this.trainTypeAggregateId = other.getTrainTypeAggregateId();
        setBasicPriceRate(other.getBasicPriceRate());
        setFirstClassPriceRate(other.getFirstClassPriceRate());
    }

    @Override
    public void verifyInvariants() {
        if (!isPositive(this.basicPriceRate) || !isPositive(this.firstClassPriceRate)) {
            throw new TrainticketException(PRICE_RATES_POSITIVE);
        }
    }

    private boolean isPositive(BigDecimal rate) {
        return rate != null && rate.compareTo(BigDecimal.ZERO) > 0;
    }

    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        return new HashSet<>();
    }

    public Integer getRouteAggregateId() {
        return this.routeAggregateId;
    }

    public Integer getTrainTypeAggregateId() {
        return this.trainTypeAggregateId;
    }

    public BigDecimal getBasicPriceRate() {
        return this.basicPriceRate;
    }

    public void setBasicPriceRate(BigDecimal basicPriceRate) {
        this.basicPriceRate = basicPriceRate;
    }

    public BigDecimal getFirstClassPriceRate() {
        return this.firstClassPriceRate;
    }

    public void setFirstClassPriceRate(BigDecimal firstClassPriceRate) {
        this.firstClassPriceRate = firstClassPriceRate;
    }
}
