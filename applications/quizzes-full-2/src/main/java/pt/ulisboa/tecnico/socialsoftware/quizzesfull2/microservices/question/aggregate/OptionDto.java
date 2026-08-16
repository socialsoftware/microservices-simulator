package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate;

public class OptionDto {
    private Integer sequence;
    private Integer optionKey;
    private String content;
    private Boolean correct;

    public OptionDto() {
    }

    public OptionDto(Integer sequence, Integer optionKey, String content, Boolean correct) {
        this.sequence = sequence;
        this.optionKey = optionKey;
        this.content = content;
        this.correct = correct;
    }

    public OptionDto(Option option) {
        this.sequence = option.getSequence();
        this.optionKey = option.getOptionKey();
        this.content = option.getContent();
        this.correct = option.isCorrect();
    }

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    public Integer getOptionKey() {
        return optionKey;
    }

    public void setOptionKey(Integer optionKey) {
        this.optionKey = optionKey;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Boolean isCorrect() {
        return correct;
    }

    public void setCorrect(Boolean correct) {
        this.correct = correct;
    }
}
