package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.Option;

public class OptionDto implements Serializable {
    private Long id;
    private Integer key;
    private Integer sequence;
    private Boolean correct;
    private String content;

    public OptionDto() {
    }

    public OptionDto(Option option) {
        this.id = option.getId();
        this.key = option.getKey();
        this.sequence = option.getSequence();
        this.correct = option.isCorrect();
        this.content = option.getContent();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getKey() {
        return key;
    }

    public void setKey(Integer key) {
        this.key = key;
    }

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    public Boolean isCorrect() {
        return correct;
    }

    public void setCorrect(Boolean correct) {
        this.correct = correct;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}