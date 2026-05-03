package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class Option {
    @Id
    @GeneratedValue
    private Integer id;
    private Integer sequence;
    private Integer optionKey;
    private String content;
    private Boolean correct;

    public Option() {}

    public Option(Integer sequence, Integer optionKey, String content, Boolean correct) {
        this.sequence = sequence;
        this.optionKey = optionKey;
        this.content = content;
        this.correct = correct;
    }

    public Option(Option other) {
        this.sequence = other.getSequence();
        this.optionKey = other.getOptionKey();
        this.content = other.getContent();
        this.correct = other.getCorrect();
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getSequence() { return sequence; }
    public void setSequence(Integer sequence) { this.sequence = sequence; }

    public Integer getOptionKey() { return optionKey; }
    public void setOptionKey(Integer optionKey) { this.optionKey = optionKey; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Boolean getCorrect() { return correct; }
    public void setCorrect(Boolean correct) { this.correct = correct; }
}
