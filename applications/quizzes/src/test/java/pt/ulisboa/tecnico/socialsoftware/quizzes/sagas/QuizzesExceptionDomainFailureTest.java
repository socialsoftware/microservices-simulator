package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas;

import org.junit.jupiter.api.Test;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.DomainFailure;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesConfigurationException;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesException;

import static org.assertj.core.api.Assertions.assertThat;
import static pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesErrorMessage.TOPIC_MISSING_NAME;
import static pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesErrorMessage.UNDEFINED_TRANSACTIONAL_MODEL;

class QuizzesExceptionDomainFailureTest {

    @Test
    void businessInvariantExceptionCarriesDomainMarkerAndPreservesRemoteMessage() {
        QuizzesException local = new QuizzesException(TOPIC_MISSING_NAME);
        QuizzesException restored = QuizzesException.fromRemote(local.getErrorMessage(), local.getMessage());

        assertThat(local).isInstanceOf(DomainFailure.class);
        assertThat(restored).isInstanceOf(DomainFailure.class);
        assertThat(restored.getErrorMessage()).isEqualTo(TOPIC_MISSING_NAME);
        assertThat(restored.getMessage()).isEqualTo(TOPIC_MISSING_NAME);
    }

    @Test
    void undefinedTransactionalModelExceptionIsConfigurationAndUnmarked() {
        QuizzesConfigurationException failure = new QuizzesConfigurationException(UNDEFINED_TRANSACTIONAL_MODEL);

        assertThat(failure).isExactlyInstanceOf(QuizzesConfigurationException.class);
        assertThat(failure).isNotInstanceOf(DomainFailure.class);
        assertThat(failure.getMessage()).isEqualTo(UNDEFINED_TRANSACTIONAL_MODEL);
    }
}
