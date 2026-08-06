package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.events.subscribe.RouteSubscribesStationDeletedRouteStationsExist;

import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.RouteDto;

import static pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage.INVARIANT_BREAK;

@Entity
public abstract class Route extends Aggregate {
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "route")
    private List<RouteStation> stations = new ArrayList<>();

    public Route() {

    }

    public Route(Integer aggregateId, RouteDto routeDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setStations(routeDto.getStations() != null ? routeDto.getStations().stream().map(RouteStation::new).collect(Collectors.toList()) : null);
    }


    public Route(Route other) {
        super(other);
        setStations(other.getStations().stream().map(RouteStation::new).collect(Collectors.toList()));
    }

    public List<RouteStation> getStations() {
        return stations;
    }

    public void setStations(List<RouteStation> stations) {
        this.stations = stations;
        if (this.stations != null) {
            this.stations.forEach(item -> item.setRoute(this));
        }
    }

    public void addRouteStation(RouteStation routeStation) {
        if (this.stations == null) {
            this.stations = new ArrayList<>();
        }
        this.stations.add(routeStation);
        if (routeStation != null) {
            routeStation.setRoute(this);
        }
    }

    public void removeRouteStation(Integer id) {
        if (this.stations != null) {
            this.stations.removeIf(item -> 
                item.getStationOrder() != null && item.getStationOrder().equals(id));
        }
    }

    public boolean containsRouteStation(Integer id) {
        if (this.stations == null) {
            return false;
        }
        return this.stations.stream().anyMatch(item -> 
            item.getStationOrder() != null && item.getStationOrder().equals(id));
    }

    public RouteStation findRouteStationById(Integer id) {
        if (this.stations == null) {
            return null;
        }
        return this.stations.stream()
            .filter(item -> item.getStationOrder() != null && item.getStationOrder().equals(id))
            .findFirst()
            .orElse(null);
    }


    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        Set<EventSubscription> eventSubscriptions = new HashSet<>();
        if (this.getState() == AggregateState.ACTIVE) {
            interInvariantRouteStationsExist(eventSubscriptions);
        }
        return eventSubscriptions;
    }
    private void interInvariantRouteStationsExist(Set<EventSubscription> eventSubscriptions) {
        for (RouteStation item : this.stations) {
            eventSubscriptions.add(new RouteSubscribesStationDeletedRouteStationsExist(item));
        }
    }


    private boolean invariantStationsNotNull() {
        return this.stations != null;
    }
    @Override
    public void verifyInvariants() {
        if (!invariantStationsNotNull()) {
            throw new SimulatorException(INVARIANT_BREAK, "Route must have a stations collection");
        }
    }

    public RouteDto buildDto() {
        RouteDto dto = new RouteDto();
        dto.setAggregateId(getAggregateId());
        dto.setVersion(getVersion());
        dto.setState(getState());
        dto.setStations(getStations() != null ? getStations().stream().map(RouteStation::buildDto).collect(Collectors.toList()) : null);
        return dto;
    }
}