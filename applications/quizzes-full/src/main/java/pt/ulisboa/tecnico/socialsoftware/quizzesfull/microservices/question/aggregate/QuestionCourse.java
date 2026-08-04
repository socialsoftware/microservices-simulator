package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.CourseDto;

@Entity
public class QuestionCourse {
    @Id
    @GeneratedValue
    private Integer id;
    private Integer courseAggregateId;
    private Long courseVersion;
    @OneToOne
    private Question question;

    public QuestionCourse() {}

    public QuestionCourse(CourseDto courseDto) {
        setCourseAggregateId(courseDto.getAggregateId());
        setCourseVersion(courseDto.getVersion());
    }

    public QuestionCourse(QuestionCourse other) {
        setCourseAggregateId(other.getCourseAggregateId());
        setCourseVersion(other.getCourseVersion());
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getCourseAggregateId() { return courseAggregateId; }
    public void setCourseAggregateId(Integer courseAggregateId) { this.courseAggregateId = courseAggregateId; }

    public Long getCourseVersion() { return courseVersion; }
    public void setCourseVersion(Long courseVersion) { this.courseVersion = courseVersion; }

    @JsonIgnore
    public Question getQuestion() { return question; }
    public void setQuestion(Question question) { this.question = question; }
}
