package pt.ulisboa.tecnico.socialsoftware.ms.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Integer> {
    @Query("SELECT e FROM Event e WHERE TYPE(e) = :eventType AND e.published = true " +
            "AND e.publisherAggregateId = :pubAggId AND e.publisherAggregateVersion > :subVersion " +
            "ORDER BY e.timestamp ASC")
    List<Event> findUnprocessedEvents(
            @Param("eventType") Class<? extends Event> eventType,
            @Param("pubAggId") Integer publisherAggregateId,
            @Param("subVersion") Long subscribedVersion
    );
}
