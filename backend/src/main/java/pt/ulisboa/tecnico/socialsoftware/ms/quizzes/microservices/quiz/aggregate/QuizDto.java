package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate;

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionDto;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

public class QuizDto implements Serializable {
    private Integer aggregateId;
    private String title;
    private String availableDate;
    private String conclusionDate;
    private String resultsDate;
    private Integer version;
    private List<QuestionDto> questionDtos;
    private Integer courseExecutionAggregateId;
    private Integer courseExecutionVersion;
    private String state;

    public QuizDto() {

    }

    public QuizDto(Quiz quiz) {
        setAggregateId(quiz.getAggregateId());
        setTitle(quiz.getTitle());
        setAvailableDate(quiz.getAvailableDate().toString());
        setConclusionDate(quiz.getConclusionDate().toString());
        setResultsDate(quiz.getResultsDate().toString());
        setVersion(quiz.getVersion());
        setQuestionDtos(quiz.getQuizQuestions().stream()
                .map(qq -> qq.buildDto())
                .collect(Collectors.toList()));
        setCourseExecutionAggregateId(quiz.getQuizCourseExecution().getCourseExecutionAggregateId());
        setCourseExecutionVersion(quiz.getQuizCourseExecution().getCourseExecutionVersion());
    }

    public Integer getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(Integer aggregateId) {
        this.aggregateId = aggregateId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAvailableDate() {
        return availableDate;
    }

    public void setAvailableDate(String availableDate) {
        this.availableDate = availableDate;
    }

    public String getConclusionDate() {
        return conclusionDate;
    }

    public void setConclusionDate(String conclusionDate) {
        this.conclusionDate = conclusionDate;
    }

    public String getResultsDate() {
        return resultsDate;
    }

    public void setResultsDate(String resultsDate) {
        this.resultsDate = resultsDate;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public List<QuestionDto> getQuestionDtos() {
        return this.questionDtos;
    }

    public void setQuestionDtos(List<QuestionDto> questionDtos) {
        this.questionDtos = questionDtos;
    }

    public Integer getCourseExecutionAggregateId() {
        return courseExecutionAggregateId;
    }

    public void setCourseExecutionAggregateId(Integer courseExecutionAggregateId) {
        this.courseExecutionAggregateId = courseExecutionAggregateId;
    }

    public Integer getCourseExecutionVersion() {
        return courseExecutionVersion;
    }

    public void setCourseExecutionVersion(Integer courseExecutionVersion) {
        this.courseExecutionVersion = courseExecutionVersion;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}
