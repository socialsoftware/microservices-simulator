package pt.ulisboa.tecnico.socialsoftware.blcm.user.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public interface UserProcessedEventsRepository extends JpaRepository<UserProcessedEvents, Integer> {
}