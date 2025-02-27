package pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/* unique ID generator for each created version */
@Entity
public class AggregateIdGenerator {
    @Id
    @GeneratedValue
    private Integer id;

    public AggregateIdGenerator() {
    }

    public Integer getId() {
        return id;
    }
}
