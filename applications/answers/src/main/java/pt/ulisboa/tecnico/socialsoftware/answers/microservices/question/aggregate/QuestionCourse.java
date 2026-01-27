package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuestionCourseDto;

@Entity
public class QuestionCourse {
    @Id
    @GeneratedValue
    private Long id;
    private String courseName;
    private Integer courseAggregateId;
    private Integer courseVersion;
    private AggregateState courseState;
    @OneToOne
    private Question question;

    public QuestionCourse() {

    }

    public QuestionCourse(CourseDto courseDto) {
        setCourseAggregateId(courseDto.getAggregateId());
        setCourseVersion(courseDto.getVersion());
        setCourseState(courseDto.getState());
        setCourseName(courseDto.getName());
    }

    public QuestionCourse(QuestionCourse other) {
        setCourseAggregateId(other.getCourseAggregateId());
        setCourseVersion(other.getCourseVersion());
        setCourseState(other.getCourseState());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public Integer getCourseAggregateId() {
        return courseAggregateId;
    }

    public void setCourseAggregateId(Integer courseAggregateId) {
        this.courseAggregateId = courseAggregateId;
    }

    public Integer getCourseVersion() {
        return courseVersion;
    }

    public void setCourseVersion(Integer courseVersion) {
        this.courseVersion = courseVersion;
    }

    public AggregateState getCourseState() {
        return courseState;
    }

    public void setCourseState(AggregateState courseState) {
        this.courseState = courseState;
    }

    public Question getQuestion() {
        return question;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }



    public QuestionCourseDto buildDto() {
        QuestionCourseDto dto = new QuestionCourseDto();
        dto.setName(getCourseName());
        dto.setAggregateId(getCourseAggregateId());
        dto.setVersion(getCourseVersion());
        dto.setState(getCourseState());
        return dto;
    }
}