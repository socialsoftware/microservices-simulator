package pt.ulisboa.tecnico.socialsoftware.ms.domain.version;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

import java.util.Optional;

@Service
@Profile("version-service | (!stream & !grpc)")
public class VersionService implements IVersionService {

    @Autowired
    private VersionRepository versionRepository;

    /* cannot allow two transactions to get the same version number */
    // Get version number of new transaction which is the last version of the last
    // committed transaction + 1.
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

    // If a functionality has started and committed in the meanwhile this one will
    // get a new version number to commit
    // If non has committed in between we commit with the same version as the
    // functionality started
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Integer incrementAndGetVersionNumber() {
        Version version = versionRepository.findAll().stream().findAny()
                .orElseThrow(() -> new SimulatorException(SimulatorErrorMessage.VERSION_MANAGER_DOES_NOT_EXIST));
        version.incrementVersion();
        return version.getVersionNumber();
    }

    // It is only in tests to simulate concurrent execution of functionalities
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void decrementVersionNumber() {
        Version version = versionRepository.findAll().stream().findAny()
                .orElseThrow(() -> new SimulatorException(SimulatorErrorMessage.VERSION_MANAGER_DOES_NOT_EXIST));
        version.decrementVersion();
    }
}
