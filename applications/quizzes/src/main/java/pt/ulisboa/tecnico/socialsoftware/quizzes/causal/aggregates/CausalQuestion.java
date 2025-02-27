package pt.ulisboa.tecnico.socialsoftware.quizzes.causal.aggregates;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.SetUtils;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.aggregate.CausalAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.Question;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionCourse;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionTopic;

@Entity
public class CausalQuestion extends Question implements CausalAggregate {
    public CausalQuestion() {
        super();
    }

    public CausalQuestion(Integer aggregateId, QuestionCourse questionCourse, QuestionDto questionDto, List<QuestionTopic> questionTopics) {
        super(aggregateId, questionCourse, questionDto, questionTopics);
    }

    public CausalQuestion(CausalQuestion other) {
        super(other);
    }

    @Override
    public Set<String> getMutableFields() {
        return Set.of("title", "content", "options", "questionTopics");
    }

    @Override
    public Set<String[]> getIntentions() {

        return Set.of(
                new String[]{"title", "content"},
                new String[]{"title", "options"},
                new String[]{"content", "options"}
        );
    }

    @Override
    public Aggregate mergeFields(Set<String> toCommitVersionChangedFields, Aggregate committedVersion, Set<String> committedVersionChangedFields) {
        CausalQuestion mergedQuestion = new CausalQuestion(this);
        CausalQuestion committedQuestion = (CausalQuestion) committedVersion;

        mergeTitle(toCommitVersionChangedFields, mergedQuestion, committedQuestion);
        mergeContent(toCommitVersionChangedFields, mergedQuestion, committedQuestion);
        mergeOptions(toCommitVersionChangedFields, mergedQuestion, committedQuestion);
        mergeTopics((Question)getPrev(), this, committedQuestion, mergedQuestion);
        return mergedQuestion;
    }

    private void mergeTitle(Set<String> toCommitVersionChangedFields, Question mergedQuestion, Question committedQuestion) {
        if (toCommitVersionChangedFields.contains("title")) {
            mergedQuestion.setTitle(getTitle());
        } else {
            mergedQuestion.setTitle(committedQuestion.getTitle());
        }
    }

    private void mergeContent(Set<String> toCommitVersionChangedFields, Question mergedQuestion, Question committedQuestion) {
        if (toCommitVersionChangedFields.contains("content")) {
            mergedQuestion.setContent(getContent());
        } else {
            mergedQuestion.setContent(committedQuestion.getContent());
        }
    }

    private void mergeOptions(Set<String> toCommitVersionChangedFields, Question mergedQuestion, Question committedQuestion) {
        if (toCommitVersionChangedFields.contains("options")) {
            mergedQuestion.setOptions(getOptions());
        } else {
            mergedQuestion.setOptions(committedQuestion.getOptions());
        }
    }

    private static void mergeTopics(Question prev, Question v1, Question v2, Question mergedTournament) {
        /* Here we "calculate" the result of the incremental fields. This fields will always be the same regardless
         * of the base we choose. */

        Set<QuestionTopic> prevTopicsPre = new HashSet<>(prev.getQuestionTopics());
        Set<QuestionTopic> v1TopicsPre = new HashSet<>(v1.getQuestionTopics());
        Set<QuestionTopic> v2TopicsPre = new HashSet<>(v2.getQuestionTopics());

        CausalQuestion.syncTopicVersions(prevTopicsPre, v1TopicsPre, v2TopicsPre);

        Set<QuestionTopic> prevTopics = new HashSet<>(prevTopicsPre);
        Set<QuestionTopic> v1Topics = new HashSet<>(v1TopicsPre);
        Set<QuestionTopic> v2Topics = new HashSet<>(v2TopicsPre);

        Set<QuestionTopic> addedTopics =  SetUtils.union(
                SetUtils.difference(v1Topics, prevTopics),
                SetUtils.difference(v2Topics, prevTopics)
        );

        Set<QuestionTopic> removedTopics = SetUtils.union(
                SetUtils.difference(prevTopics, v1Topics),
                SetUtils.difference(prevTopics, v2Topics)
        );

        Set<QuestionTopic> mergedTopics = SetUtils.union(SetUtils.difference(prevTopics, removedTopics), addedTopics);
        mergedTournament.setQuestionTopics(mergedTopics);
    }

    private static void syncTopicVersions(Set<QuestionTopic> prevTopics, Set<QuestionTopic> v1Topics, Set<QuestionTopic> v2Topics) {
        for (QuestionTopic t1 : v1Topics) {
            for (QuestionTopic t2 : v2Topics) {
                if (t1.getTopicAggregateId().equals(t2.getTopicAggregateId())) {
                    if (t1.getTopicVersion() > t2.getTopicVersion()) {
                        t2.setTopicVersion(t1.getTopicVersion());
                        t2.setTopicName(t1.getTopicName());
                    }

                    if( t2.getTopicVersion() > t1.getTopicVersion()) {
                        t1.setTopicVersion(t2.getTopicVersion());
                        t1.setTopicName(t2.getTopicName());
                    }
                }
            }

            // no need to check again because the prev does not contain any newer version than v1 an v2
            for (QuestionTopic tp2 : prevTopics) {
                if (t1.getTopicAggregateId().equals(tp2.getTopicAggregateId())) {
                    if (t1.getTopicVersion() > tp2.getTopicVersion()) {
                        tp2.setTopicVersion(t1.getTopicVersion());
                        tp2.setTopicName(t1.getTopicName());
                    }

                    if (tp2.getTopicVersion() > t1.getTopicVersion()) {
                        t1.setTopicVersion(tp2.getTopicVersion());
                        t1.setTopicName(tp2.getTopicName());
                    }
                }
            }
        }
    }
}
