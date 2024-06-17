package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.aggregates;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.aggregate.CausalAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.Quiz;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizCourseExecution;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizQuestion;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizType;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizDto;

import java.util.Set;
import java.util.stream.Collectors;

@Entity
public class CausalQuiz extends Quiz implements CausalAggregate {
    public CausalQuiz() {
    }

    public CausalQuiz(Integer aggregateId, QuizCourseExecution quizCourseExecution, Set<QuizQuestion> quizQuestions, QuizDto quizDto, QuizType quizType) {
        super(aggregateId, quizCourseExecution, quizQuestions, quizDto, quizType);
    }

    public CausalQuiz(CausalQuiz other) {
        super(other);
    }

    public Set<String> getMutableFields() {
        // we dont add the courseExecution because it can only change through events and the only events that comes from it is the delete which deletes the quiz
        return Set.of("availableDate", "conclusionDate", "resultsDate", "title" ,"quizQuestions");
    }

    public Set<String[]> getIntentions() {
        return Set.of(
                new String[]{"availableDate", "conclusionDate"},
                new String[]{"availableDate", "resultsDate"},
                new String[]{"conclusionDate", "resultsDate"});
    }

    @Override
    public Aggregate mergeFields(Set<String> toCommitVersionChangedFields, Aggregate committedVersion, Set<String> committedVersionChangedFields) {
        CausalQuiz committedQuiz = (CausalQuiz) committedVersion;
        CausalQuiz mergedQuiz = new CausalQuiz(this);

        mergeAvailableDate(toCommitVersionChangedFields, committedQuiz, mergedQuiz);
        mergeConclusionDate(toCommitVersionChangedFields, committedQuiz, mergedQuiz);
        mergeResultsDate(toCommitVersionChangedFields, committedQuiz, mergedQuiz);
        mergeTitle(toCommitVersionChangedFields, committedQuiz, mergedQuiz);
        mergeQuizQuestions(toCommitVersionChangedFields, committedQuiz, mergedQuiz);
        return mergedQuiz;
    }

    private void mergeAvailableDate(Set<String> toCommitVersionChangedFields, Quiz committedQuiz, Quiz mergedQuiz) {
        if (toCommitVersionChangedFields.contains("availableDate")) {
            mergedQuiz.setAvailableDate(getAvailableDate());
        } else {
            mergedQuiz.setAvailableDate(committedQuiz.getAvailableDate());
        }
    }

    private void mergeConclusionDate(Set<String> toCommitVersionChangedFields, Quiz committedQuiz, Quiz mergedQuiz) {
        if(toCommitVersionChangedFields.contains("conclusionDate")) {
            mergedQuiz.setConclusionDate(getConclusionDate());
        } else {
            mergedQuiz.setConclusionDate(committedQuiz.getConclusionDate());
        }
    }

    private void mergeResultsDate(Set<String> toCommitVersionChangedFields, Quiz committedQuiz, Quiz mergedQuiz) {
        if(toCommitVersionChangedFields.contains("resultsDate")) {
            mergedQuiz.setResultsDate(getResultsDate());
        } else {
            mergedQuiz.setResultsDate(committedQuiz.getResultsDate());
        }
    }

    private void mergeTitle(Set<String> toCommitVersionChangedFields, Quiz committedQuiz, Quiz mergedQuiz) {
        if(toCommitVersionChangedFields.contains("title")) {
            mergedQuiz.setTitle(getTitle());
        } else {
            mergedQuiz.setTitle(committedQuiz.getTitle());
        }
    }

    private void mergeQuizQuestions(Set<String> toCommitVersionChangedFields, Quiz committedQuiz, Quiz mergedQuiz) {
        if(toCommitVersionChangedFields.contains("quizQuestions")) {
            mergedQuiz.setQuizQuestions(getQuizQuestions().stream().map(QuizQuestion::new).collect(Collectors.toSet()));
        } else {
            mergedQuiz.setQuizQuestions(committedQuiz.getQuizQuestions().stream().map(QuizQuestion::new).collect(Collectors.toSet()));
        }
    }
}
