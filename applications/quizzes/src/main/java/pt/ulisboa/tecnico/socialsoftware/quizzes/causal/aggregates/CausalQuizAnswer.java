package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.aggregates;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.collections4.SetUtils;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.aggregate.CausalAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.aggregate.AnswerCourseExecution;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.aggregate.AnswerStudent;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.aggregate.AnsweredQuiz;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.aggregate.QuestionAnswer;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.aggregate.QuizAnswer;

@Entity
public class CausalQuizAnswer extends QuizAnswer implements CausalAggregate {
    public CausalQuizAnswer() {
        super();
    }

    public CausalQuizAnswer(Integer aggregateId, AnswerCourseExecution answerCourseExecution, AnswerStudent answerStudent, AnsweredQuiz answeredQuiz) {
        super(aggregateId, answerCourseExecution, answerStudent, answeredQuiz);
    }

    public CausalQuizAnswer(CausalQuizAnswer other) {
        super(other);
    }

    @Override
    public Set<String> getMutableFields() {
        return Set.of("questionAnswers", "answerDate");
    }

    @Override
    public Set<String[]> getIntentions() {
        return new HashSet<>();
    }

    @Override
    public Aggregate mergeFields(Set<String> toCommitVersionChangedFields, Aggregate committedVersion, Set<String> committedVersionChangedFields) {
        QuizAnswer committedQuizAnswer = (QuizAnswer) committedVersion;
        mergeCourseExecution(this, committedQuizAnswer);
        mergeUser(this, committedQuizAnswer);
        mergeQuiz(this, committedQuizAnswer);
        mergeAnswerDate(toCommitVersionChangedFields, this, committedQuizAnswer);
        mergeQuestionAnswers((QuizAnswer)getPrev(), this, committedQuizAnswer, this);
        return this;
    }

    private void mergeCourseExecution(QuizAnswer toCommitQuizAnswer, QuizAnswer committedQuizAnswer) {
        // The course execution version determines which user is more recent because the user is an execution student
        if (toCommitQuizAnswer.getAnswerCourseExecution().getCourseExecutionVersion() >= committedQuizAnswer.getAnswerCourseExecution().getCourseExecutionVersion()) {
            toCommitQuizAnswer.getAnswerCourseExecution().setCourseExecutionVersion(toCommitQuizAnswer.getAnswerCourseExecution().getCourseExecutionVersion());
        } else {
            toCommitQuizAnswer.getAnswerCourseExecution().setCourseExecutionVersion(committedQuizAnswer.getAnswerCourseExecution().getCourseExecutionVersion());
        }
    }

    private void mergeUser(QuizAnswer toCommitQuizAnswer, QuizAnswer committedQuizAnswer) {
        // The course execution version determines which user is more recent because the user is an execution student
        /*if(toCommitAnswer.getCourseExecution().getVersion() >= committedAnswer.getCourseExecution().getVersion()) {
            toCommitAnswer.getUser(getUser());
        } else {
            toCommitAnswer.getUser().(committedAnswer.getUser());
        }*/
    }

    private void mergeQuiz(QuizAnswer toCommitQuizAnswer, QuizAnswer committedQuizAnswer) {
        if(getQuiz().getQuizVersion() >= committedQuizAnswer.getQuiz().getQuizVersion()) {
            toCommitQuizAnswer.setQuiz(new AnsweredQuiz(toCommitQuizAnswer.getQuiz()));
        } else {
            toCommitQuizAnswer.setQuiz(new AnsweredQuiz(committedQuizAnswer.getQuiz()));

        }
    }

    private void mergeAnswerDate(Set<String> toCommitVersionChangedFields, QuizAnswer toCommitQuizAnswer, QuizAnswer committedQuizAnswer) {
        if(toCommitVersionChangedFields.contains("answerDate")) {
            toCommitQuizAnswer.setAnswerDate(getAnswerDate());
        } else {
            toCommitQuizAnswer.setAnswerDate(committedQuizAnswer.getAnswerDate());
        }
    }

    private static void mergeQuestionAnswers(QuizAnswer prev, QuizAnswer v1, QuizAnswer v2, QuizAnswer mergedQuizAnswer) {
        /* Here we "calculate" the result of the incremental fields. This fields will always be the same regardless
         * of the base we choose. */

        Set<QuestionAnswer> prevQuestionAnswersPre = new HashSet<>(prev.getQuestionAnswers());
        Set<QuestionAnswer> v1QuestionAnswersPre = new HashSet<>(v1.getQuestionAnswers());
        Set<QuestionAnswer> v2QuestionAnswersPre = new HashSet<>(v2.getQuestionAnswers());

        Set<QuestionAnswer> prevQuestionAnswers = new HashSet<>(prevQuestionAnswersPre);
        Set<QuestionAnswer> v1QuestionAnswers = new HashSet<>(v1QuestionAnswersPre);
        Set<QuestionAnswer> v2QuestionAnswers = new HashSet<>(v2QuestionAnswersPre);


        Set<QuestionAnswer> addedQuestionAnswers =  SetUtils.union(
                SetUtils.difference(v1QuestionAnswers, prevQuestionAnswers),
                SetUtils.difference(v2QuestionAnswers, prevQuestionAnswers)
        );

        Set<QuestionAnswer> removedQuestionAnswers = SetUtils.union(
                SetUtils.difference(prevQuestionAnswers, v1QuestionAnswers),
                SetUtils.difference(prevQuestionAnswers, v2QuestionAnswers)
        );

        Set<QuestionAnswer> mergedQuestionAnswers = SetUtils.union(SetUtils.difference(prevQuestionAnswers, removedQuestionAnswers), addedQuestionAnswers);
        mergedQuizAnswer.setQuestionAnswers(new ArrayList<>(mergedQuestionAnswers));

    }
}
