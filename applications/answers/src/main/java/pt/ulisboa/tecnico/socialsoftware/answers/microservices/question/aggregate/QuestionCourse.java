package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.OneToOne;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuestionDto;

@Entity
public class QuestionCourse {
    @Id
    @GeneratedValue
    private Integer courseAggregateId;
    private String courseName;
    private String courseAcronym;
    @OneToOne
    private Question question; 

    public QuestionCourse() {
    }

    public QuestionCourse(QuestionDto questionDto) {
        setCourseName(questionDto.getCourseName());
        setCourseAcronym(questionDto.getCourseAcronym());
    }

    public QuestionCourse(QuestionCourse other) {
        setCourseName(other.getCourseName());
        setCourseAcronym(other.getCourseAcronym());
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

    public String getCourseAcronym() {
        return courseAcronym;
    }

    public void setCourseAcronym(String courseAcronym) {
        this.courseAcronym = courseAcronym;
    }

    public Question getQuestion() {
        return question;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }


}