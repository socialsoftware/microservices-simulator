import { Aggregate } from "../../../../language/generated/ast.js";
import { capitalize } from "../../../utils/generator-utils.js";

export class ServiceEventGenerator {
    static generateEventProcessingMethods(aggregateName: string, aggregate: Aggregate): string {
        const eventMethods = [];

        eventMethods.push(this.generateEventPublishingMethods(aggregateName));

        const methods = eventMethods.filter(method => method.trim().length > 0).join('\n\n');

        return methods.length > 0 ? `    // Event Processing Methods
${methods}` : '    // No event processing methods defined';
    }

    private static generateEventPublishingMethods(aggregateName: string): string {
        const capitalizedAggregate = capitalize(aggregateName);
        const lowerAggregate = aggregateName.toLowerCase();

        return `    private void publish${capitalizedAggregate}CreatedEvent(${capitalizedAggregate} ${lowerAggregate}) {
        try {
            // TODO: Implement event publishing for ${capitalizedAggregate}Created
            // eventPublisher.publishEvent(new ${capitalizedAggregate}CreatedEvent(${lowerAggregate}));
        } catch (Exception e) {
            // Log error but don't fail the transaction
            logger.error("Failed to publish ${capitalizedAggregate}CreatedEvent", e);
        }
    }

    private void publish${capitalizedAggregate}UpdatedEvent(${capitalizedAggregate} ${lowerAggregate}) {
        try {
            // TODO: Implement event publishing for ${capitalizedAggregate}Updated
            // eventPublisher.publishEvent(new ${capitalizedAggregate}UpdatedEvent(${lowerAggregate}));
        } catch (Exception e) {
            // Log error but don't fail the transaction
            logger.error("Failed to publish ${capitalizedAggregate}UpdatedEvent", e);
        }
    }

    private void publish${capitalizedAggregate}DeletedEvent(Long ${lowerAggregate}Id) {
        try {
            // TODO: Implement event publishing for ${capitalizedAggregate}Deleted
            // eventPublisher.publishEvent(new ${capitalizedAggregate}DeletedEvent(${lowerAggregate}Id));
        } catch (Exception e) {
            // Log error but don't fail the transaction
            logger.error("Failed to publish ${capitalizedAggregate}DeletedEvent", e);
        }
    }`;
    }

    static generateEventListenerMethods(aggregateName: string): string {
        const capitalizedAggregate = capitalize(aggregateName);

        return `    @EventListener
    @Async
    public void handleAsync${capitalizedAggregate}Event(${capitalizedAggregate}Event event) {
        try {
            // Asynchronous event processing
            logger.info("Async processing ${capitalizedAggregate} event: " + event.getEventType());
            
            // TODO: Implement asynchronous event processing logic
            
        } catch (Exception e) {
            logger.error("Error in async ${capitalizedAggregate} event processing", e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAfterCommit${capitalizedAggregate}Event(${capitalizedAggregate}Event event) {
        try {
            // Post-transaction event processing
            logger.info("Post-commit processing ${capitalizedAggregate} event: " + event.getEventType());
            
            // TODO: Implement post-commit event processing logic
            
        } catch (Exception e) {
            logger.error("Error in post-commit ${capitalizedAggregate} event processing", e);
        }
    }`;
    }
}
