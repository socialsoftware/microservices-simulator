package pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import org.springframework.beans.factory.annotation.Autowired;,import org.springframework.context.ApplicationEvent;,import org.springframework.context.event.EventListener;,import org.springframework.scheduling.annotation.Async;,import org.springframework.stereotype.Component;,import org.springframework.stereotype.Service;,import org.springframework.transaction.annotation.Transactional;,import org.springframework.transaction.event.TransactionPhase;,import org.springframework.transaction.event.TransactionalEventListener;,,import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate.*;,,import java.time.LocalDateTime;,import java.io.Serializable;,import com.fasterxml.jackson.annotation.JsonFormat;,

@Entity
public class  extends Event {

    public () {
    }

    public (Integer aggregateId) {
        super(aggregateId);
    }

}