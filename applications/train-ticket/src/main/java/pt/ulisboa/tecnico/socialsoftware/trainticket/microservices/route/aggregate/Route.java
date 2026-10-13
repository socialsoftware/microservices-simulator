package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainTicketErrorMessage;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainTicketException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "routes")
public abstract class Route extends Aggregate {
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RouteStation> stations = new ArrayList<>();

    public Route() {
    }

    public Route(Integer aggregateId, List<RouteStation> stations) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        this.stations = new ArrayList<>(stations);
    }

    public Route(Route other) {
        super(other);
        this.stations = other.getStations().stream()
                .map(RouteStation::new)
                .collect(Collectors.toList());
    }

    @Override
    public void verifyInvariants() {
        atLeastTwoStops();
        sequenceContiguous();
        originAtZero();
        distanceIncreasing();
        noRepeatedStation();
    }

    private void atLeastTwoStops() {
        if (this.stations.size() < 2) {
            throw new TrainTicketException(TrainTicketErrorMessage.ROUTE_AT_LEAST_TWO_STOPS);
        }
    }

    // the sequence values are exactly {0, ..., n-1}
    private void sequenceContiguous() {
        Set<Integer> sequences = this.stations.stream()
                .map(RouteStation::getSequence)
                .collect(Collectors.toSet());
        boolean contiguous = sequences.size() == this.stations.size()
                && sequences.stream().allMatch(s -> s != null && s >= 0 && s < this.stations.size());
        if (!contiguous) {
            throw new TrainTicketException(TrainTicketErrorMessage.ROUTE_SEQUENCE_CONTIGUOUS);
        }
    }

    // relies on sequenceContiguous(): the stop with sequence 0 exists
    private void originAtZero() {
        if (!Objects.equals(getOrderedStations().get(0).getDistanceFromStart(), 0)) {
            throw new TrainTicketException(TrainTicketErrorMessage.ROUTE_ORIGIN_AT_ZERO);
        }
    }

    private void distanceIncreasing() {
        List<RouteStation> ordered = getOrderedStations();
        for (int i = 1; i < ordered.size(); i++) {
            Integer previous = ordered.get(i - 1).getDistanceFromStart();
            Integer current = ordered.get(i).getDistanceFromStart();
            if (previous == null || current == null || previous >= current) {
                throw new TrainTicketException(TrainTicketErrorMessage.ROUTE_DISTANCE_INCREASING);
            }
        }
    }

    private void noRepeatedStation() {
        long distinctStations = this.stations.stream()
                .map(RouteStation::getStationAggregateId)
                .distinct()
                .count();
        if (distinctStations != this.stations.size()) {
            throw new TrainTicketException(TrainTicketErrorMessage.ROUTE_NO_REPEATED_STATION);
        }
    }

    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        return new HashSet<>();
    }

    public List<RouteStation> getStations() {
        return stations;
    }

    public void setStations(List<RouteStation> stations) {
        this.stations = stations;
    }

    public List<RouteStation> getOrderedStations() {
        return this.stations.stream()
                .sorted(Comparator.comparing(RouteStation::getSequence))
                .collect(Collectors.toList());
    }
}
