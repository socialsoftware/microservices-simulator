package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketException;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketErrorMessage.ROUTE_DISTANCES_MONOTONIC;
import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketErrorMessage.ROUTE_ENDPOINTS_MATCH_STATION_LIST;
import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketErrorMessage.ROUTE_FIRST_DISTANCE_IS_ZERO;
import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketErrorMessage.ROUTE_HAS_AT_LEAST_TWO_STATIONS;
import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketErrorMessage.ROUTE_SEQUENCE_CONTIGUOUS;
import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketErrorMessage.ROUTE_STATIONS_DISTINCT;

@Entity
@Table(name = "routes")
public abstract class Route extends Aggregate {
    @Column(name = "start_station_name")
    private String startStationName;
    @Column(name = "end_station_name")
    private String endStationName;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<RouteStation> routeStations = new HashSet<>();

    public Route() {
    }

    public Route(Integer aggregateId, RouteDto routeDto) {
        super(aggregateId);
        setStartStationName(routeDto.getStartStationName());
        setEndStationName(routeDto.getEndStationName());
        setRouteStations(routeDto.getRouteStations().stream()
                .map(RouteStation::new)
                .collect(Collectors.toSet()));
        setAggregateType(getClass().getSimpleName());
    }

    public Route(Route other) {
        super(other);
        setStartStationName(other.getStartStationName());
        setEndStationName(other.getEndStationName());
        // Deep copy: each version owns its own RouteStation rows, so a later UpdateRoute
        // cannot mutate the station list of an earlier version through a shared reference.
        setRouteStations(other.getRouteStations().stream()
                .map(RouteStation::new)
                .collect(Collectors.toSet()));
    }

    @Override
    public void verifyInvariants() {
        List<RouteStation> ordered = orderedRouteStations();
        verifyHasAtLeastTwoStations(ordered);
        verifySequenceContiguous(ordered);
        verifyFirstDistanceIsZero(ordered);
        verifyDistancesMonotonic(ordered);
        verifyStationsDistinct(ordered);
        verifyEndpointsMatchStationList(ordered);
    }

    private List<RouteStation> orderedRouteStations() {
        return this.routeStations.stream()
                .sorted(Comparator.comparing(RouteStation::getSequence,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    private void verifyHasAtLeastTwoStations(List<RouteStation> ordered) {
        if (ordered.size() < 2) {
            throw new TrainticketException(ROUTE_HAS_AT_LEAST_TWO_STATIONS);
        }
    }

    private void verifySequenceContiguous(List<RouteStation> ordered) {
        Set<Integer> sequences = ordered.stream()
                .map(RouteStation::getSequence)
                .collect(Collectors.toSet());
        Set<Integer> expected = IntStream.range(0, ordered.size()).boxed().collect(Collectors.toSet());
        if (sequences.size() != ordered.size() || !sequences.equals(expected)) {
            throw new TrainticketException(ROUTE_SEQUENCE_CONTIGUOUS);
        }
    }

    private void verifyFirstDistanceIsZero(List<RouteStation> ordered) {
        if (!Integer.valueOf(0).equals(ordered.get(0).getDistanceFromStart())) {
            throw new TrainticketException(ROUTE_FIRST_DISTANCE_IS_ZERO);
        }
    }

    private void verifyDistancesMonotonic(List<RouteStation> ordered) {
        for (int i = 1; i < ordered.size(); i++) {
            Integer previous = ordered.get(i - 1).getDistanceFromStart();
            Integer current = ordered.get(i).getDistanceFromStart();
            if (current == null || current <= previous) {
                throw new TrainticketException(ROUTE_DISTANCES_MONOTONIC);
            }
        }
    }

    private void verifyStationsDistinct(List<RouteStation> ordered) {
        long distinct = ordered.stream()
                .map(RouteStation::getStationAggregateId)
                .distinct()
                .count();
        if (distinct != ordered.size()) {
            throw new TrainticketException(ROUTE_STATIONS_DISTINCT);
        }
    }

    private void verifyEndpointsMatchStationList(List<RouteStation> ordered) {
        if (!Objects.equals(this.startStationName, ordered.get(0).getStationName())
                || !Objects.equals(this.endStationName, ordered.get(ordered.size() - 1).getStationName())) {
            throw new TrainticketException(ROUTE_ENDPOINTS_MATCH_STATION_LIST);
        }
    }

    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        return new HashSet<>();
    }

    public String getStartStationName() {
        return this.startStationName;
    }

    public void setStartStationName(String startStationName) {
        this.startStationName = startStationName;
    }

    public String getEndStationName() {
        return this.endStationName;
    }

    public void setEndStationName(String endStationName) {
        this.endStationName = endStationName;
    }

    public Set<RouteStation> getRouteStations() {
        return this.routeStations;
    }

    public void setRouteStations(Set<RouteStation> routeStations) {
        this.routeStations.clear();
        this.routeStations.addAll(routeStations);
    }
}
