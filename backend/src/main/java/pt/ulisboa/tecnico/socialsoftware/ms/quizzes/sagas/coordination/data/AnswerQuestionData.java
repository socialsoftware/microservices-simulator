package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowData;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.aggregate.QuizAnswer;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionDto;

public class AnswerQuestionData extends WorkflowData {
    private QuestionDto questionDto;
    private QuizAnswer oldQuizAnswer;

    public QuestionDto getQuestionDto() {
        return questionDto;
    }

    public void setQuestionDto(QuestionDto questionDto) {
        this.questionDto = questionDto;
    }

    public QuizAnswer getOldQuizAnswer() {
        return oldQuizAnswer;
    }

    public void setOldQuizAnswer(QuizAnswer oldQuizAnswer) {
        this.oldQuizAnswer = oldQuizAnswer;
    }
}