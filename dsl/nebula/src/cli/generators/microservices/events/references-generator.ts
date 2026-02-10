import { Aggregate, Entity, References, ReferenceConstraint } from "../../../../language/generated/ast.js";
import { EventBaseGenerator } from "./event-base-generator.js";
import { EventGenerationOptions } from "./event-types.js";
import { capitalize } from "../../../utils/generator-utils.js";

/**
 * Generates event handlers for referential integrity constraints.
 * Creates subscriptions to DeletedEvents that enforce reference constraints.
 */
export class ReferencesGenerator extends EventBaseGenerator {

    async generateReferenceHandlers(aggregate: Aggregate, rootEntity: Entity, options: EventGenerationOptions): Promise<{ [key: string]: string }> {
        const results: { [key: string]: string } = {};

        const references = (aggregate as any).references as References | undefined;
        if (!references || !references.constraints || references.constraints.length === 0) {
            return results;
        }

        const baseContext = this.createBaseEventContext(aggregate, rootEntity, options);
        const projectName = (this as any).projectName?.toLowerCase() || 'unknown';
        const basePackage = (this as any).getBasePackage?.() || 'pt.ulisboa.tecnico.socialsoftware';

        for (const constraint of references.constraints) {
            const refConstraint = constraint as ReferenceConstraint;
            const subscriptionCode = this.generateReferenceSubscription(
                aggregate,
                rootEntity,
                refConstraint,
                baseContext,
                projectName,
                basePackage
            );

            const handlerCode = this.generateReferenceHandler(
                aggregate,
                rootEntity,
                refConstraint,
                baseContext,
                projectName,
                basePackage
            );

            const targetAggregateName = refConstraint.targetAggregate;
            results[`ref-subscription-${targetAggregateName}`] = subscriptionCode;
            results[`ref-handler-${targetAggregateName}`] = handlerCode;
        }

        return results;
    }

    private generateReferenceSubscription(
        aggregate: Aggregate,
        rootEntity: Entity,
        constraint: ReferenceConstraint,
        baseContext: any,
        projectName: string,
        basePackage: string
    ): string {
        const aggregateName = aggregate.name;
        const targetAggregate = constraint.targetAggregate;
        const eventType = `${targetAggregate}DeletedEvent`;
        const className = `${aggregateName}Subscribes${targetAggregate}Deleted`;
        const handlerClassName = `${targetAggregate}DeletedEventHandler`;

        return `package ${basePackage}.${projectName}.microservices.${aggregateName.toLowerCase()}.events.subscribe;

import ${basePackage}.ms.domain.event.EventSubscription;
import ${basePackage}.${projectName}.microservices.${aggregateName.toLowerCase()}.aggregate.${aggregateName};
import ${basePackage}.${projectName}.microservices.${targetAggregate.toLowerCase()}.events.publish.${eventType};
import ${basePackage}.${projectName}.microservices.${aggregateName.toLowerCase()}.events.handling.handlers.${handlerClassName};

public class ${className} extends EventSubscription {
    public ${className}(${aggregateName} ${aggregateName.toLowerCase()}) {
        super(${aggregateName.toLowerCase()},
                ${eventType}.class,
                ${handlerClassName}.class);
    }
}
`;
    }

    private generateReferenceHandler(
        aggregate: Aggregate,
        rootEntity: Entity,
        constraint: ReferenceConstraint,
        baseContext: any,
        projectName: string,
        basePackage: string
    ): string {
        const aggregateName = aggregate.name;
        const targetAggregate = constraint.targetAggregate;
        const eventType = `${targetAggregate}DeletedEvent`;
        const className = `${targetAggregate}DeletedEventHandler`;
        const action = constraint.action;
        const message = constraint.message.replace(/"/g, ''); // Remove quotes
        const fieldName = constraint.fieldName;

        // Generate the handler logic based on the action
        const handlerLogic = this.generateHandlerLogic(
            aggregate,
            constraint,
            aggregateName,
            targetAggregate,
            fieldName,
            action,
            message
        );

        return `package ${basePackage}.${projectName}.microservices.${aggregateName.toLowerCase()}.events.handling.handlers;

import ${basePackage}.ms.coordination.eventProcessing.EventProcessingHandler;
import ${basePackage}.ms.domain.aggregate.Aggregate;
import ${basePackage}.ms.exception.SimulatorException;
import static ${basePackage}.ms.exception.SimulatorErrorMessage.AGGREGATE_DELETED;
import ${basePackage}.${projectName}.microservices.${aggregateName.toLowerCase()}.aggregate.${aggregateName};
import ${basePackage}.${projectName}.microservices.${aggregateName.toLowerCase()}.repository.${aggregateName}Repository;
import ${basePackage}.${projectName}.microservices.${targetAggregate.toLowerCase()}.events.publish.${eventType};

import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class ${className} implements EventProcessingHandler<${eventType}, ${aggregateName}> {

    private final ${aggregateName}Repository ${aggregateName.toLowerCase()}Repository;

    public ${className}(${aggregateName}Repository ${aggregateName.toLowerCase()}Repository) {
        this.${aggregateName.toLowerCase()}Repository = ${aggregateName.toLowerCase()}Repository;
    }

    @Override
    public void handleEvent(${aggregateName} ${aggregateName.toLowerCase()}, ${eventType} event) {
${handlerLogic}
    }
}
`;
    }

    private generateHandlerLogic(
        aggregate: Aggregate,
        constraint: ReferenceConstraint,
        aggregateName: string,
        targetAggregate: string,
        fieldName: string,
        action: string,
        message: string
    ): string {
        const aggregateVar = aggregateName.toLowerCase();
        const fieldGetter = `get${capitalize(fieldName)}`;

        switch (action) {
            case 'prevent':
                return `        // Reference constraint: prevent deletion if references exist
        // Check if this ${aggregateName} references the deleted ${targetAggregate}
        if (${aggregateVar}.${fieldGetter}() != null) {
            Integer referenced${targetAggregate}Id = ${aggregateVar}.${fieldGetter}().get${targetAggregate}AggregateId();
            if (referenced${targetAggregate}Id != null && referenced${targetAggregate}Id.equals(event.getPublisherAggregateId())) {
                throw new SimulatorException(AGGREGATE_DELETED, "${message}");
            }
        }`;

            case 'cascade':
                return `        // Reference constraint: cascade deletion
        // If this ${aggregateName} references the deleted ${targetAggregate}, delete it too
        if (${aggregateVar}.${fieldGetter}() != null) {
            Integer referenced${targetAggregate}Id = ${aggregateVar}.${fieldGetter}().get${targetAggregate}AggregateId();
            if (referenced${targetAggregate}Id != null && referenced${targetAggregate}Id.equals(event.getPublisherAggregateId())) {
                ${aggregateVar}.remove();
            }
        }`;

            case 'setNull':
                return `        // Reference constraint: set reference to null
        // If this ${aggregateName} references the deleted ${targetAggregate}, set reference to null
        if (${aggregateVar}.${fieldGetter}() != null) {
            Integer referenced${targetAggregate}Id = ${aggregateVar}.${fieldGetter}().get${targetAggregate}AggregateId();
            if (referenced${targetAggregate}Id != null && referenced${targetAggregate}Id.equals(event.getPublisherAggregateId())) {
                ${aggregateVar}.set${capitalize(fieldName)}(null);
            }
        }`;

            default:
                return `        // Unknown action: ${action}`;
        }
    }
}
