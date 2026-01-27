package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuestionCourseDto;

@Entity
public class QuestionCourse {
    @Id
    @GeneratedValue
    private Long id;
    private Integer courseAggregateId;
    private Integer courseVersion;
    private String courseName;
    @OneToOne
    private Question question;

    public QuestionCourse() {

    }

    public QuestionCourse(CourseDto courseDto) {
        setCourseAggregateId(courseDto.getAggregateId());
        setCourseVersion(courseDto.getVersion());
        setCourseName(courseDto.getName());
    }

    public QuestionCourse(QuestionCourse other) {
        setCourseVersion(other.getCourseVersion());
        setCourseName(other.getCourseName());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public Question getQuestion() {
        return question;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }


    public QuestionCourseDto buildDto() {
        QuestionCourseDto dto = new QuestionCourseDto();
        dto.setAggregateId(getCourseAggregateId());
        dto.setVersion(getCourseVersion());
        dto.setName(getCourseName());
        return dto;
    }
}