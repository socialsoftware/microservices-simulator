package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.grpc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.annotation.Retry;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandResponse;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.MessagingObjectMapperProvider;

import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;
import pt.ulisboa.tecnico.socialsoftware.ms.grpc.CommandReply;
import pt.ulisboa.tecnico.socialsoftware.ms.grpc.CommandRequest;
import pt.ulisboa.tecnico.socialsoftware.ms.grpc.CommandServiceGrpc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.annotation.PostConstruct;

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
        logger.info("Total services found: " + allServices.size());

        if (allServices.isEmpty()) {
            logger.warning("No services discovered! Check if Kubernetes discovery is configured correctly.");
            logger.warning("Make sure spring-cloud-starter-kubernetes-client-all is included in the build.");
        } else {
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
            throw new SimulatorException(response.errorMessage());
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

        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(host, port)
                .usePlaintext()
                .build();

        try {
            CommandServiceGrpc.CommandServiceBlockingStub stub = CommandServiceGrpc.newBlockingStub(channel);
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
                return Integer.parseInt(metadata.get("grpcPort"));
            } catch (NumberFormatException ignored) {
                logger.warning("Invalid grpcPort metadata for service " + service + ", falling back to properties");
            }
        }

        Integer servicePort = environment.getProperty("grpc.command." + service + ".port", Integer.class);
        if (servicePort != null) {
            return servicePort;
        }

        return environment.getProperty("grpc.command.default-port", Integer.class, instance.getPort());
    }
}
