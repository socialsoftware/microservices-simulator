package pt.ulisboa.tecnico.socialsoftware.quizzes;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("grpc")
public class VersionServiceDiscoveryLogger {

    private static final Logger logger = LoggerFactory.getLogger(VersionServiceDiscoveryLogger.class);

    @Value("${version-service:}")
    private String versionServiceUrl;

    @Value("${grpc.command.version.port:9091}")
    private int versionGrpcPort;

    @PostConstruct
    public void logDiscoveryStatus() {
        logger.info("=== Version Service Configuration ===");

        if (versionServiceUrl != null && !versionServiceUrl.isEmpty()) {
            logger.info("Version service URL: {}", versionServiceUrl);
            logger.info("Version service gRPC port: {}", versionGrpcPort);
        } else {
            logger.warn("Version service URL not configured!");
        }

        logger.info("======================================");
    }

    public String getVersionServiceUrl() {
        return versionServiceUrl;
    }
}
