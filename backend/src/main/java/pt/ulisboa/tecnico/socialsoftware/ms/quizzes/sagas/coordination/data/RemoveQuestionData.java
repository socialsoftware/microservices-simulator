package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowData;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.SagaQuestion;

public class RemoveQuestionData extends WorkflowData {
    private SagaQuestion question;

    public SagaQuestion getQuestion() {
        return question;
    }

    public void setQuestion(SagaQuestion question) {
        this.question = question;
    }
}