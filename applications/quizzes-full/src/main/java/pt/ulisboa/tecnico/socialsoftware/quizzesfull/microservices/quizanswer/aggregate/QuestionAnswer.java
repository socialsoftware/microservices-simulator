package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "question_answer")
public class QuestionAnswer {

    @Id
    @GeneratedValue
    private Integer id;

    private Integer questionAggregateId;
    private Long questionVersion;
    private Integer optionSequenceChoice;
    private Integer optionKey;
    private Boolean correct;
    private Integer timeTaken;

    public QuestionAnswer() {}

    public QuestionAnswer(Integer questionAggregateId, Long questionVersion,
                          Integer optionSequenceChoice, Integer optionKey,
                          Boolean correct, Integer timeTaken) {
        this.questionAggregateId = questionAggregateId;
        this.questionVersion = questionVersion;
        this.optionSequenceChoice = optionSequenceChoice;
        this.optionKey = optionKey;
        this.correct = correct;
        this.timeTaken = timeTaken;
    }

    public QuestionAnswer(QuestionAnswer other) {
        this.questionAggregateId = other.getQuestionAggregateId();
        this.questionVersion = other.getQuestionVersion();
        this.optionSequenceChoice = other.getOptionSequenceChoice();
        this.optionKey = other.getOptionKey();
        this.correct = other.getCorrect();
        this.timeTaken = other.getTimeTaken();
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getQuestionAggregateId() { return questionAggregateId; }
    public void setQuestionAggregateId(Integer questionAggregateId) { this.questionAggregateId = questionAggregateId; }

    public Long getQuestionVersion() { return questionVersion; }
    public void setQuestionVersion(Long questionVersion) { this.questionVersion = questionVersion; }

    public Integer getOptionSequenceChoice() { return optionSequenceChoice; }
    public void setOptionSequenceChoice(Integer optionSequenceChoice) { this.optionSequenceChoice = optionSequenceChoice; }

    public Integer getOptionKey() { return optionKey; }
    public void setOptionKey(Integer optionKey) { this.optionKey = optionKey; }

    public Boolean getCorrect() { return correct; }
    public void setCorrect(Boolean correct) { this.correct = correct; }

    public Integer getTimeTaken() { return timeTaken; }
    public void setTimeTaken(Integer timeTaken) { this.timeTaken = timeTaken; }
}
