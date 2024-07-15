package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowData;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.SagaQuizAnswer;

public class ConcludeQuizData extends WorkflowData {
    private SagaQuizAnswer quizAnswer;

    public SagaQuizAnswer getQuizAnswer() {
        return quizAnswer;
    }

    public void setQuizAnswer(SagaQuizAnswer quizAnswer) {
        this.quizAnswer = quizAnswer;
    }
}