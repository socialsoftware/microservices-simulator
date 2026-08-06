package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate.AggregateState;

import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.PriceConfigRouteDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.RouteDto;

@Entity
public class PriceConfigRoute {
    @Id
    @GeneratedValue
    private Long id;
    private Integer routeAggregateId;
    private Long routeVersion;
    private AggregateState routeState;
    @OneToOne
    private PriceConfig priceconfig;

    public PriceConfigRoute() {

    }

    public PriceConfigRoute(RouteDto routeDto) {
        setRouteAggregateId(routeDto.getAggregateId());
        setRouteVersion(routeDto.getVersion());
        setRouteState(routeDto.getState());
    }

    public PriceConfigRoute(PriceConfigRouteDto priceConfigRouteDto) {
        setRouteAggregateId(priceConfigRouteDto.getAggregateId());
        setRouteVersion(priceConfigRouteDto.getVersion());
        setRouteState(priceConfigRouteDto.getState() != null ? AggregateState.valueOf(priceConfigRouteDto.getState()) : null);
    }

    public PriceConfigRoute(PriceConfigRoute other) {
        setRouteAggregateId(other.getRouteAggregateId());
        setRouteVersion(other.getRouteVersion());
        setRouteState(other.getRouteState());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getRouteAggregateId() {
        return routeAggregateId;
    }

    public void setRouteAggregateId(Integer routeAggregateId) {
        this.routeAggregateId = routeAggregateId;
    }

    public Long getRouteVersion() {
        return routeVersion;
    }

    public void setRouteVersion(Long routeVersion) {
        this.routeVersion = routeVersion;
    }

    public AggregateState getRouteState() {
        return routeState;
    }

    public void setRouteState(AggregateState routeState) {
        this.routeState = routeState;
    }

    public PriceConfig getPriceConfig() {
        return priceconfig;
    }

    public void setPriceConfig(PriceConfig priceconfig) {
        this.priceconfig = priceconfig;
    }




    public PriceConfigRouteDto buildDto() {
        PriceConfigRouteDto dto = new PriceConfigRouteDto();
        dto.setAggregateId(getRouteAggregateId());
        dto.setVersion(getRouteVersion());
        dto.setState(getRouteState() != null ? getRouteState().name() : null);
        return dto;
    }
}