package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate;

import jakarta.persistence.*;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.course.aggregate.CourseDto;

@Entity
public class QuestionCourse {
    @Id
    @GeneratedValue
    private Long id;
    private Integer courseAggregateId;
    private String courseName;
    private Integer courseVersion;
    @OneToOne
    private Question question;

    public QuestionCourse() {

    }
    public QuestionCourse(CourseDto causalCourseRemote) {
        setCourseAggregateId(causalCourseRemote.getAggregateId());
        setCourseName(causalCourseRemote.getName());
        setCourseVersion(causalCourseRemote.getVersion());
    }

    public QuestionCourse(QuestionCourse other) {
        setCourseAggregateId(other.getCourseAggregateId());
        setCourseName(other.getCourseName());
        setCourseVersion(other.getCourseVersion());
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

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public Integer getCourseVersion() {
        return courseVersion;
    }

    public void setCourseVersion(Integer courseVersion) {
        this.courseVersion = courseVersion;
    }

    public Question getQuestion() {
        return question;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }

    public CourseDto buildDto() {
        CourseDto dto = new CourseDto();
        dto.setAggregateId(this.courseAggregateId);
        setCourseName(this.courseName);
        return dto;
    }
}
