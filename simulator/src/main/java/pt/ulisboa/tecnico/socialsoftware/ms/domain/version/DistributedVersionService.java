package pt.ulisboa.tecnico.socialsoftware.ms.domain.version;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Distributed version service for sagas using Snowflake ID generation.
 *
 * Each service instance generates locally unique, monotonically increasing
 * version IDs without requiring a centralized database or remote calls.
 *
 * The machine ID is derived from the Spring application name to ensure
 * each microservice gets a unique, stable ID automatically.
 */
@Service
@Profile("distributed_version")
public class DistributedVersionService implements IVersionService {

    private static final Logger logger = LoggerFactory.getLogger(DistributedVersionService.class);

    private static final long MAX_MACHINE_ID = 1023;

    private final SnowflakeIdGenerator idGenerator;

    public DistributedVersionService(
            @Value("${spring.application.name:default}") String applicationName) {
        long machineId = Math.abs(applicationName.hashCode()) % (MAX_MACHINE_ID + 1);
        this.idGenerator = new SnowflakeIdGenerator(machineId);
        logger.info("Initialized DistributedVersionService with Snowflake machine ID: {} (from application name: {})", machineId, applicationName);
    }

    @Override
    public Long getVersionNumber() {
        long id = idGenerator.nextId();
        logger.debug("Generated distributed version number: {}", id);
        return id;
    }

    @Override
    public Long incrementAndGetVersionNumber() {
        long id = idGenerator.nextId();
        logger.debug("Generated distributed commit version number: {}", id);
        return id;
    }

    @Override
    public void decrementVersionNumber() {
        throw new UnsupportedOperationException(
                "Decrementing version number is not supported in distributed mode. " +
                        "This operation is only available in the centralized TCC/Causal version service.");
    }
}
