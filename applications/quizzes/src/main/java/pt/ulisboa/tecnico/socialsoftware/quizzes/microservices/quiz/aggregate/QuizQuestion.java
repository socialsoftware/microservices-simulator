package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate;

import jakarta.persistence.*;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionDto;

@Entity
public class QuizQuestion {
    @Id
    @GeneratedValue
    private Long id;
    private Integer questionAggregateId;
    private Integer questionVersion;
    private String title;
    private String content;
    private Integer sequence;
    private Aggregate.AggregateState state;
    @ManyToOne
    private Quiz quiz;

    public QuizQuestion() {
    }

    public QuizQuestion(QuestionDto questionDto) {
        setQuestionAggregateId(questionDto.getAggregateId());
        setQuestionVersion(questionDto.getVersion());
        setTitle(questionDto.getTitle());
        setContent(questionDto.getContent());
        setSequence(questionDto.getSequence());
        setState(Aggregate.AggregateState.ACTIVE);
    }

    public QuizQuestion(QuizQuestion other) {
        setQuestionAggregateId(other.getQuestionAggregateId());
        setQuestionVersion(other.getQuestionVersion());
        setTitle(other.getTitle());
        setContent(other.getContent());
        setSequence(other.getSequence());
        setState(other.getState());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getQuestionAggregateId() {
        return questionAggregateId;
    }

    public void setQuestionAggregateId(Integer questionAggregateId) {
        this.questionAggregateId = questionAggregateId;
    }

    public Integer getQuestionVersion() {
        return questionVersion;
    }

    public void setQuestionVersion(Integer questionVersion) {
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

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    public Aggregate.AggregateState getState() {
        return state;
    }

    public void setState(Aggregate.AggregateState state) {
        this.state = state;
    }

    public Quiz getQuiz() {
        return quiz;
    }

    public void setQuiz(Quiz quiz) {
        this.quiz = quiz;
    }

    public QuestionDto buildDto() {
        QuestionDto questionDto = new QuestionDto();
        questionDto.setAggregateId(getQuestionAggregateId());
        questionDto.setVersion(getQuestionVersion());
        questionDto.setTitle(getTitle());
        questionDto.setContent(getContent());

        return questionDto;
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof QuizQuestion)) {
            return false;
        }
        QuizQuestion quizQuestion = (QuizQuestion) obj;
        return getQuestionAggregateId() != null && getQuestionAggregateId().equals(quizQuestion.getQuestionAggregateId()) &&
                getQuestionVersion() != null && getQuestionVersion().equals(quizQuestion.getQuestionVersion());
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + getQuestionAggregateId();
        hash = 31 * hash + (getQuestionVersion() == null ? 0 : getQuestionVersion().hashCode());
        return hash;
    }

}
