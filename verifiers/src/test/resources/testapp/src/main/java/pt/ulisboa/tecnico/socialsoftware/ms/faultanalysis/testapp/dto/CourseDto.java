package pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.testapp.dto;

/**
 * Simple DTO used in test fixtures for SpockTestVisitor validation.
 */
public class CourseDto {
    private String name;
    private String acronym;
    private Integer aggregateId;

    public CourseDto() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAcronym() {
        return acronym;
    }

    public void setAcronym(String acronym) {
        this.acronym = acronym;
    }

    public Integer getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(Integer aggregateId) {
        this.aggregateId = aggregateId;
    }
}
