package pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Serializable;
import java.util.Map;

public class AggregateDto implements Serializable {
    private Integer aggregateId;
    private String aggregateClass;
    private String jsonAggregate;

    public String getJsonAggregate() {
        return jsonAggregate;
    }

    public AggregateDto(Aggregate aggregate, ObjectMapper mapper) {
        try {
            this.aggregateId = aggregate.getAggregateId();
            this.aggregateClass = aggregate.getClass().getName();
            this.jsonAggregate = mapper.writeValueAsString(aggregate);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize aggregate " + aggregate.getAggregateId(), e);
        }
    }

    public Aggregate toAggregate(ObjectMapper mapper) {
        try {
            Class<?> clazz = Class.forName(aggregateClass);
            return (Aggregate) mapper.readValue(jsonAggregate, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize aggregate " + aggregateId, e);
        }
    }
}
