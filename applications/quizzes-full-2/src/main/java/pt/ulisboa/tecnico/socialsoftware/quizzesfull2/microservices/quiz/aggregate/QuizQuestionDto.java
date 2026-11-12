package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate;

public class QuizQuestionDto {
    private Integer questionAggregateId;
    private Long questionVersion;
    private String title;
    private String content;

    public QuizQuestionDto() {
    }

    public QuizQuestionDto(Integer questionAggregateId, Long questionVersion, String title, String content) {
        this.questionAggregateId = questionAggregateId;
        this.questionVersion = questionVersion;
        this.title = title;
        this.content = content;
    }

    public QuizQuestionDto(QuizQuestion quizQuestion) {
        this.questionAggregateId = quizQuestion.getQuestionAggregateId();
        this.questionVersion = quizQuestion.getQuestionVersion();
        this.title = quizQuestion.getTitle();
        this.content = quizQuestion.getContent();
    }

    public Integer getQuestionAggregateId() {
        return questionAggregateId;
    }

    public void setQuestionAggregateId(Integer questionAggregateId) {
        this.questionAggregateId = questionAggregateId;
    }

    public Long getQuestionVersion() {
        return questionVersion;
    }

    public void setQuestionVersion(Long questionVersion) {
        this.questionVersion = questionVersion;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
