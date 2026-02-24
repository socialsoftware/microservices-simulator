package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.grpc;

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.logging.Logger;

@Component
@Profile("grpc")
public class GrpcServerRunner {
    private static final Logger logger = Logger.getLogger(GrpcServerRunner.class.getName());

    private final Server server;

    @Autowired
    public GrpcServerRunner(GrpcCommandService grpcCommandService, Environment environment) {
        int port = environment.getProperty("grpc.server.port", Integer.class, 9090);
        this.server = NettyServerBuilder.forPort(port)
                .addService(grpcCommandService)
                .build();
    }

    @PostConstruct
    public void start() throws IOException {
        server.start();
        logger.info("gRPC server started on port " + server.getPort());
    }

    @PreDestroy
    public void stop() {
        server.shutdown();
        logger.info("gRPC server stopped");
    }
}
