package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.grpc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.annotation.Retry;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.*;
import pt.ulisboa.tecnico.socialsoftware.ms.grpc.CommandReply;
import pt.ulisboa.tecnico.socialsoftware.ms.grpc.CommandRequest;
import pt.ulisboa.tecnico.socialsoftware.ms.grpc.CommandServiceGrpc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@Profile("grpc")
public class GrpcCommandGateway extends CommandGateway {
    private final DiscoveryClient discoveryClient;
    private final Environment environment;
    private final ObjectMapper objectMapper;

    @Value("${grpc.fallback-service:}")
    private String fallbackService;

    @Value("${grpc.service-suffix:}")
    private String serviceSuffix;

    @Autowired
    public GrpcCommandGateway(ApplicationContext applicationContext,
            RetryRegistry retryRegistry,
            DiscoveryClient discoveryClient,
            Environment environment,
            MessagingObjectMapperProvider mapperProvider) {
        super(applicationContext, retryRegistry);
        this.discoveryClient = discoveryClient;
        this.environment = environment;
        this.objectMapper = mapperProvider.newMapper();
    }

    @PostConstruct
    public void logDiscoveryStatus() {
        logger.info("=== GrpcCommandGateway Service Discovery Status ===");
        logger.info("Discovery client type: " + discoveryClient.getClass().getSimpleName());

        List<String> allServices = discoveryClient.getServices();
        if (allServices.isEmpty()) {
            logger.info("No services discovered yet. This is normal during startup as services register with the discovery server.");
        } else {
            logger.info("Total services found: " + allServices.size());
            logger.info("All discovered services: " + allServices);
        }

        logger.info("Service suffix configured: '" + serviceSuffix + "'");
        logger.info("Fallback service configured: '" + fallbackService + "'");
        logger.info("=================================================");
    }

    @Override
    @Retry(name = "commandGateway", fallbackMethod = "fallbackSend")
    public Object send(Command command) {
        if (command.getServiceName() == null || command.getServiceName().isEmpty()) {
            throw new IllegalArgumentException("Command serviceName is required for gRPC routing");
        }
        String appName = applicationContext.getEnvironment().getProperty("spring.application.name");
        String service = command.getServiceName();

        // If a local CommandHandler bean exists for this service, dispatch locally
        String handlerBeanName = command.getServiceName() + "CommandHandler";
        if (service.equals(appName) && applicationContext.containsBean(handlerBeanName)) {
            CommandHandler handler = (CommandHandler) applicationContext.getBean(handlerBeanName);
            logger.info("Dispatching command locally: " + command.getClass().getSimpleName());
            return handler.handle(command);
        }

        String correlationId = UUID.randomUUID().toString();
        String commandJson;
        try {
            commandJson = objectMapper.writeValueAsString(command);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize command", e);
        }

        CommandRequest request = CommandRequest.newBuilder()
                .setCommandJson(commandJson)
                .setCorrelationId(correlationId)
                .build();

        CommandReply reply = callService(command.getServiceName(), request);

        CommandResponse response;
        try {
            response = objectMapper.readValue(reply.getResponseJson(), CommandResponse.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize command response", e);
        }

        mergeUnitOfWork(command.getUnitOfWork(), response.unitOfWork());

        if (response.isError()) {
            throwMatchingException(response.errorType(), response.errorMessage());
        }
        return response.result();
    }

    private CommandReply callService(String service, CommandRequest request) {
        String host;
        int port;

        // Using service discovery
        ServiceInstance instance = chooseInstance(service);
        if (instance == null) {
            throw new RuntimeException("No available instances for service: " + service);
        }
        host = instance.getHost();
        port = resolveGrpcPort(instance, service);

        logger.info("Calling service " + service + " at " + host + ":" + port);

        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(host, port)
                .usePlaintext()
                .build();

        try {
            CommandServiceGrpc.CommandServiceBlockingStub stub = CommandServiceGrpc.newBlockingStub(channel)
                    .withDeadlineAfter(commandTimeoutSeconds, java.util.concurrent.TimeUnit.SECONDS);
            return stub.send(request);
        } finally {
            channel.shutdown();
        }
    }

    private ServiceInstance chooseInstance(String service) {
        String resolvedService = service + (serviceSuffix != null ? serviceSuffix : "");
        var instances = discoveryClient.getInstances(resolvedService);
        logger.info("Found " + instances.size() + " instances for service " + resolvedService);

        // If no instances found and fallback service is configured, try fallback
        if (instances.isEmpty() && fallbackService != null && !fallbackService.isEmpty()
                && !service.equals(fallbackService)) {
            String resolvedFallback = fallbackService + (serviceSuffix != null ? serviceSuffix : "");
            logger.info("No instances for " + resolvedService + ", trying fallback service: " + resolvedFallback);
            instances = discoveryClient.getInstances(resolvedFallback);
            logger.info("Found " + instances.size() + " instances for fallback service " + resolvedFallback);
        }

        if (instances.isEmpty()) {
            return null;
        }
        return instances.getFirst();
    }

    private int resolveGrpcPort(ServiceInstance instance, String service) {
        Map<String, String> metadata = instance.getMetadata();
        if (metadata != null && metadata.containsKey("grpcPort")) {
            try {
                int port = Integer.parseInt(metadata.get("grpcPort"));
                logger.info("Using gRPC port " + port + " from metadata for service " + service);
                return port;
            } catch (NumberFormatException ignored) {
                logger.warning("Invalid grpcPort metadata for service " + service + ", falling back to properties");
            }
        }

        Integer servicePort = environment.getProperty("grpc.command." + service + ".port", Integer.class);
        if (servicePort != null) {
            logger.info("Using gRPC port " + servicePort + " from properties for service " + service);
            return servicePort;
        }

        int defaultPort = environment.getProperty("grpc.command.default-port", Integer.class, instance.getPort());
        logger.warning("No grpcPort metadata found for service " + service + ". Falling back to " + defaultPort);
        return defaultPort;
    }
}
