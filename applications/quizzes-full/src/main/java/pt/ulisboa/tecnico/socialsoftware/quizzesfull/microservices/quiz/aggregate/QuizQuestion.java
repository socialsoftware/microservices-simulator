package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.QuestionDto;

@Entity
public class QuizQuestion {
    @Id
    @GeneratedValue
    private Integer id;
    private Integer questionAggregateId;
    private Long questionVersion;
    private String title;
    private String content;

    public QuizQuestion() {}

    public QuizQuestion(QuestionDto questionDto) {
        this.questionAggregateId = questionDto.getAggregateId();
        this.questionVersion = questionDto.getVersion();
        this.title = questionDto.getTitle();
        this.content = questionDto.getContent();
    }

    public QuizQuestion(QuizQuestion other) {
        this.questionAggregateId = other.getQuestionAggregateId();
        this.questionVersion = other.getQuestionVersion();
        this.title = other.getTitle();
        this.content = other.getContent();
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getQuestionAggregateId() { return questionAggregateId; }
    public void setQuestionAggregateId(Integer questionAggregateId) { this.questionAggregateId = questionAggregateId; }

    public Long getQuestionVersion() { return questionVersion; }
    public void setQuestionVersion(Long questionVersion) { this.questionVersion = questionVersion; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
