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
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandResponse;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.stream.MessagingObjectMapperProvider;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;
import pt.ulisboa.tecnico.socialsoftware.ms.grpc.CommandReply;
import pt.ulisboa.tecnico.socialsoftware.ms.grpc.CommandRequest;
import pt.ulisboa.tecnico.socialsoftware.ms.grpc.CommandServiceGrpc;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

@Component
@Profile("grpc")
public class GrpcCommandGateway extends CommandGateway {
    private final DiscoveryClient discoveryClient;
    private final Environment environment;
    private final ObjectMapper objectMapper;

    @Value("${version-service:}")
    private String versionServiceUrl;

    @Value("${grpc.fallback-service:}")
    private String fallbackService;

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

    @Override
    @Retry(name = "commandGateway", fallbackMethod = "fallbackSend")
    public Object send(Command command) {
        if (command.getServiceName() == null || command.getServiceName().isEmpty()) {
            throw new IllegalArgumentException("Command serviceName is required for gRPC routing");
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

        Object result = response.result();
        if (result instanceof SimulatorException) {
            throw (SimulatorException) result;
        } else if (result instanceof TransientDataAccessException) {
            throw (TransientDataAccessException) result;
        } else if (response.isError()) {
            throw new SimulatorException(response.errorMessage());
        }
        return result;
    }

    private CommandReply callService(String service, CommandRequest request) {
        String host;
        int port;

        // For version service, always use hardcoded URL
        if ("version".equals(service) && versionServiceUrl != null && !versionServiceUrl.isEmpty()) {
            try {
                URI uri = URI.create(versionServiceUrl);
                host = uri.getHost();
                port = environment.getProperty("grpc.command.version.port", Integer.class, 9091);
                logger.info("Using hardcoded version service: " + host + ":" + port);
            } catch (Exception e) {
                throw new RuntimeException("Invalid version-service URL: " + versionServiceUrl, e);
            }
        } else {
            // For other services, use discovery
            ServiceInstance instance = chooseInstance(service);
            if (instance == null) {
                throw new RuntimeException("No available instances for service: " + service);
            }
            host = instance.getHost();
            port = resolveGrpcPort(instance, service);
        }

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
        var instances = discoveryClient.getInstances(service);
        logger.info("Found " + instances.size() + " instances for service " + service);

        // If no instances found and fallback service is configured, try fallback
        if (instances.isEmpty() && fallbackService != null && !fallbackService.isEmpty()
                && !service.equals(fallbackService)) {
            logger.info("No instances for " + service + ", trying fallback service: " + fallbackService);
            instances = discoveryClient.getInstances(fallbackService);
            logger.info("Found " + instances.size() + " instances for fallback service " + fallbackService);
        }

        if (instances.isEmpty()) {
            return null;
        }
        int idx = Math.abs(service.hashCode()) % instances.size();
        return instances.get(idx);
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

    private void mergeUnitOfWork(UnitOfWork target, UnitOfWork source) {
        if (target == null || source == null)
            return;
        if (source.getId() != null)
            target.setId(source.getId());
        if (source.getVersion() != null)
            target.setVersion(source.getVersion());
        if (source.getAggregatesToCommit() != null) {
            for (Aggregate sourceAgg : source.getAggregatesToCommit()) {
                boolean alreadyExists = target.getAggregatesToCommit().stream()
                        .anyMatch(targetAgg -> targetAgg.getAggregateType().equals(sourceAgg.getAggregateType())
                                && targetAgg.getAggregateId().equals(sourceAgg.getAggregateId()));
                if (!alreadyExists) {
                    target.getAggregatesToCommit().add(sourceAgg);
                }
            }
        }
        if (source.getEventsToEmit() != null)
            target.getEventsToEmit().addAll(source.getEventsToEmit());
        logger.info("Merging UnitOfWork - target aggregatesToCommit after: " +
                (target.getAggregatesToCommit() != null
                        ? target.getAggregatesToCommit().size() + " aggregates"
                        : "null"));

        if (target instanceof SagaUnitOfWork t && source instanceof SagaUnitOfWork s) {
            if (s.getAggregatesInSaga() != null) {
                s.getAggregatesInSaga().forEach((aggregateId, aggregateType) -> {
                    if (!t.getAggregatesInSaga().containsKey(aggregateId)) {
                        t.getAggregatesInSaga().put(aggregateId, aggregateType);
                    }
                });
            }
            if (s.getPreviousStates() != null) {
                t.getPreviousStates().putAll(s.getPreviousStates());
            }
        }
    }
}
