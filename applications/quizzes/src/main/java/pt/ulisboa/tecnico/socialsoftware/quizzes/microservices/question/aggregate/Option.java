package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate;

import jakarta.persistence.*;

@Entity
public class Option {
    @Id
    @GeneratedValue
    private Long id;
    private Integer optionKey;
    private Integer sequence;
    private boolean correct;
    private String content;
    @ManyToOne
    private Question question;

    public Option() {
    }

    public Option(OptionDto optionDto) {
        setSequence(optionDto.getSequence());
        setCorrect(optionDto.isCorrect());
        setContent(optionDto.getContent());
    }

    public Option(Option other) {
        setSequence(other.getSequence());
        setCorrect(other.isCorrect());
        setContent(other.getContent());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getOptionKey() {
        return optionKey;
    }

    public void setOptionKey(Integer optionKey) {
        this.optionKey = optionKey;
    }

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    public boolean isCorrect() {
        return correct;
    }

    public void setCorrect(boolean correct) {
        this.correct = correct;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Question getQuestion() {
        return question;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }
}
