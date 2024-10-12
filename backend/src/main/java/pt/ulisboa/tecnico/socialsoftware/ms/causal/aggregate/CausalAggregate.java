package pt.ulisboa.tecnico.socialsoftware.ms.causal.aggregate;

import static pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage.AGGREGATE_DELETED;
import static pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage.AGGREGATE_MERGE_FAILURE;
import static pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage.AGGREGATE_MERGE_FAILURE_DUE_TO_INTENSIONS_CONFLICT;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.TutorException;

public interface CausalAggregate {
    Aggregate mergeFields(Set<String> toCommitVersionChangedFields, Aggregate committedVersion, Set<String> committedVersionChangedFields);
    Set<String[]> getIntentions();
    Set<String> getMutableFields();

    default Aggregate merge(Aggregate toCommitVersion, Aggregate committedVersion) {
        Aggregate prev = toCommitVersion.getPrev();

        if (prev.getClass() != toCommitVersion.getClass() || prev.getClass() != committedVersion.getClass()) {
            throw new TutorException(AGGREGATE_MERGE_FAILURE, toCommitVersion.getAggregateId());
        }

        /*if(toCommitVersion.getState() == DELETED) {
            throw new TutorException(AGGREGATE_DELETED, toCommitVersion.getAggregateId());
        }*/
        /* take the state into account because we don't want to override a deleted object*/

        if (committedVersion.getState() == AggregateState.DELETED) {
            throw new TutorException(AGGREGATE_DELETED, committedVersion.getAggregateType().toString(), committedVersion.getAggregateId());
        }

        Set<String> toCommitVersionChangedFields = getChangedFields(prev, toCommitVersion);
        Set<String> committedVersionChangedFields = getChangedFields(prev, committedVersion);

        checkIntentions(toCommitVersionChangedFields, committedVersionChangedFields);

        Aggregate mergedAggregate = mergeFields(toCommitVersionChangedFields, committedVersion, committedVersionChangedFields);

        mergedAggregate.setPrev(toCommitVersion.getPrev());

        return mergedAggregate;
    }
    private Set<String> getChangedFields(Aggregate prevObj, Aggregate obj) {
        Set<String> changedFields = new HashSet<>();
        if (prevObj.getClass() != obj.getClass()) {
            throw new TutorException(AGGREGATE_MERGE_FAILURE, prevObj.getAggregateId());
        }

        try {
            for (String fieldName : getMutableFields()) {
                Field field = obj.getClass().getSuperclass().getDeclaredField(fieldName);

                field.setAccessible(true);

                Object currentFieldValue = field.get(obj);
                Object prevFieldValue = field.get(prevObj);

                if (currentFieldValue != null && prevFieldValue != null && !(currentFieldValue.equals(prevFieldValue))) {
                    changedFields.add(fieldName);
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new TutorException(AGGREGATE_MERGE_FAILURE, prevObj.getAggregateId());
        }
        return changedFields;
    }

    private void checkIntentions(Set<String> changedFields1, Set<String> changedFields2) {
        for (String [] intention : getIntentions()) {
            if (!(changedFields1.contains(intention[0]) && changedFields1.contains(intention[1]))
                    && ((changedFields1.contains(intention[0]) && changedFields2.contains(intention[1]))
                    || (changedFields1.contains(intention[1]) && changedFields2.contains(intention[0])))) {
                throw new TutorException(AGGREGATE_MERGE_FAILURE_DUE_TO_INTENSIONS_CONFLICT, intention[0] + ":" + intention[1]);
            }
        }
    }
}
