package pt.ulisboa.tecnico.socialsoftware.blcm.topic.domain;

import pt.ulisboa.tecnico.socialsoftware.blcm.aggregate.domain.Aggregate;

import javax.persistence.*;

@Entity
@Table(name = "topics")
public class Topic extends Aggregate {

    @ManyToOne(fetch = FetchType.LAZY)
    private Topic prev;

    @Column
    private String name;


    @Embedded
    private TopicCourse course;

    public Topic() {}

    public Topic(Integer aggregateId, Integer version, String name, TopicCourse course) {
        super(aggregateId, version);
        setName(name);
        setCourse(course);
    }

    public Topic(Topic other) {
        super(other.getAggregateId());
        setName(other.getName());
        setCourse(other.getCourse());
    }


    public static Topic merge(Topic prev, Topic v1, Topic v2) {
        // choose the object with lowest ts
        if(v2.getCreationTs().isBefore(v1.getCreationTs())) {
            return v2;
        } else {
            return v1;
        }

    }
    @Override
    public boolean verifyInvariants() {
        return false;
    }

    @Override
    public Aggregate getPrev() {
        return this.prev;
    }

    public void setPrev(Topic prev) {
        this.prev = prev;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TopicCourse getCourse() {
        return course;
    }

    public void setCourse(TopicCourse course) {
        this.course = course;
    }
}
