package pt.ulisboa.tecnico.socialsoftware.ms.domain.version;

import java.sql.SQLException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.TutorException;

@Service
public class VersionService {

    @Autowired
    private VersionRepository versionRepository;

    /* cannot allow two transactions to get the same version number*/
    // Get version number of new transaction which is the last version of the last committed transaction + 1.
    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Integer getVersionNumber() {
        Optional<Version> versionOp = versionRepository.findAll().stream().findAny();
        Version version;
        if (versionOp.isEmpty()) {
            version = new Version();
            versionRepository.save(version);
        } else {
            version = versionOp.get();
        }
        return version.getVersionNumber();

    }

    // If a functionality has started and committed in the meanwhile this one will get a new version number to commit
    // If non has committed in between we commit with the same version as the functionality started
    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Integer incrementAndGetVersionNumber() {
        Version version = versionRepository.findAll().stream().findAny().orElseThrow(() -> new TutorException(ErrorMessage.VERSION_MANAGER_DOES_NOT_EXIST));
        version.incrementVersion();
        return version.getVersionNumber();
    }

    // It is only in tests to simulate concurrent execution of functionalities
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void decrementVersionNumber() {
        Version version = versionRepository.findAll().stream().findAny().orElseThrow(() -> new TutorException(ErrorMessage.VERSION_MANAGER_DOES_NOT_EXIST));
        version.decrementVersion();
    }
}
