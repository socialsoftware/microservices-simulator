import { EntityExt, AggregateExt, TypeGuards } from "../../../types/ast-extensions.js";
import { getGlobalConfig } from "../../common/config.js";
import { EntityGenerationOptions } from "./types.js";
import { generateFields } from "./fields.js";
import { generateDefaultConstructor, generateEntityDtoConstructor, generateCopyConstructor, generateProjectionDtoConstructor } from "./constructors.js";
import { generateGettersSetters, generateBackReferenceGetterSetter } from "./methods.js";
import { generateInvariants } from "./invariants.js";
import { ImportManager, ImportManagerFactory } from "../../../utils/import-manager.js";
import { ErrorHandler, ErrorUtils, ErrorSeverity } from "../../../utils/error-handler.js";
import type { DtoSchemaRegistry } from "../../../services/dto-schema-service.js";
import { getEffectiveProperties, getEvents } from "../../../utils/aggregate-helpers.js";
import { ImportScanner } from "./builders/import-scanner.js";
import { InterInvariantBuilder } from "./builders/inter-invariant-builder.js";

// ============================================================================
// ENTITY GENERATION ORCHESTRATION
// ============================================================================

export class EntityOrchestrator {
    private importManager: ImportManager;
    private importScanner?: ImportScanner;
    private interInvariantBuilder: InterInvariantBuilder;
    private dtoRegistry?: DtoSchemaRegistry;

    constructor(projectName: string) {
        this.importManager = ImportManagerFactory.createForMicroservice(projectName);
        this.interInvariantBuilder = new InterInvariantBuilder();
    }

    private getImportScanner(): ImportScanner {
        if (!this.importScanner) {
            this.importScanner = new ImportScanner(this.importManager, this.dtoRegistry!);
        }
        return this.importScanner;
    }

    generateEntityCode(entity: EntityExt, projectName: string, options?: EntityGenerationOptions): string {
        return ErrorHandler.wrap(
            () => this.generateEntityCodeInternal(entity, projectName, options),
            ErrorUtils.entityContext(
                'generate entity code',
                entity.$container?.name || 'unknown',
                entity.name,
                'entity-orchestrator',
                { isRoot: entity.isRoot, projectName }
            ),
            ErrorSeverity.FATAL
        ) || '';
    }

    private generateEntityCodeInternal(entity: EntityExt, projectName: string, options?: EntityGenerationOptions): string {
        const opts = options || { projectName };
        const isRootEntity = TypeGuards.isRootEntity(entity);

        this.dtoRegistry = opts.dtoSchemaRegistry;

        const components = this.generateEntityComponents(entity, projectName, opts, isRootEntity);

        const classStructure = this.buildClassStructure(entity, projectName, isRootEntity);

        const javaCode = this.assembleJavaCode(classStructure, components, entity.name);

        return this.getImportScanner().finalizeWithImports(javaCode, projectName, isRootEntity, classStructure.aggregateName, entity.name, entity);
    }

    private generateEntityComponents(entity: EntityExt, projectName: string, opts: EntityGenerationOptions, isRootEntity: boolean) {
        // Get effective properties including those from mapping definitions
        const effectiveProps = getEffectiveProperties(entity);

        // For non-root entities with aggregateRef, also generate a projection DTO constructor
        const projectionDtoResult = generateProjectionDtoConstructor(entity, projectName, this.dtoRegistry);

        return {
            fields: generateFields(effectiveProps, entity, isRootEntity, projectName).code,
            defaultConstructor: generateDefaultConstructor(entity).code,
            dtoConstructor: generateEntityDtoConstructor(entity, projectName, this.dtoRegistry).code,
            projectionDtoConstructor: projectionDtoResult?.code || '',
            copyConstructor: generateCopyConstructor(entity).code,
            gettersSetters: generateGettersSetters(effectiveProps, entity, projectName, opts.allEntities).code,
            backRefGetterSetter: (!isRootEntity && entity.$container)
                ? generateBackReferenceGetterSetter(entity.$container.name)
                : '',
            invariants: isRootEntity ? generateInvariants(entity).code : '',
            // Root entities need getEventSubscriptions() for the Aggregate interface
            eventSubscriptions: isRootEntity ? this.generateEventSubscriptionsMethod(entity.$container as any) : '',
            // Inter-invariant methods
            interInvariantMethods: isRootEntity ? this.interInvariantBuilder.generateInterInvariantMethods(entity.$container as any) : '',
            // All entities now get their own DTOs, so all need buildDto() method
            buildDtoMethod: this.generateBuildDtoMethod(entity)
        };
    }

    private generateEventSubscriptionsMethod(aggregate: AggregateExt | undefined): string {
        if (!aggregate) {
            return `
    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        return new HashSet<>();
    }`;
        }

        const events = getEvents(aggregate);
        const subscribedEvents = events?.subscribedEvents || [];
        const interInvariants = (events as any)?.interInvariants || [];

        // Filter for simple subscriptions (no conditions, no routing)
        const simpleSubscriptions = subscribedEvents.filter((sub: any) => {
            // Simple subscription: no conditions block or empty conditions, no routing
            const hasConditions = sub.conditions && sub.conditions.length > 0 &&
                sub.conditions.some((c: any) => c.condition);
            const hasRouting = (sub as any).routingIdExpr;
            return !hasConditions && !hasRouting;
        });

        const hasInterInvariants = interInvariants.length > 0;

        if (simpleSubscriptions.length === 0 && !hasInterInvariants) {
            return `
    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        return new HashSet<>();
    }`;
        }

        let methodBody = `
    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        Set<EventSubscription> eventSubscriptions = new HashSet<>();`;

        // All subscriptions should only be added for ACTIVE aggregates
        const hasAnySubscriptions = hasInterInvariants || simpleSubscriptions.length > 0;

        if (hasAnySubscriptions) {
            methodBody += `\n        if (this.getState() == AggregateState.ACTIVE) {`;

            // Add inter-invariant method calls
            if (hasInterInvariants) {
                for (const invariant of interInvariants) {
                    const methodName = `interInvariant${this.toCamelCase(invariant.name)}`;
                    methodBody += `\n            ${methodName}(eventSubscriptions);`;
                }
            }

            // Add simple subscriptions (inside ACTIVE guard)
            if (simpleSubscriptions.length > 0) {
                for (const sub of simpleSubscriptions) {
                    // Handle different AST structures for event types
                    let eventTypeName = 'UnknownEvent';
                    if (typeof sub.eventType === 'string') {
                        eventTypeName = sub.eventType;
                    } else if ((sub.eventType as any)?.ref?.name) {
                        eventTypeName = (sub.eventType as any).ref.name;
                    } else if ((sub.eventType as any)?.$refText) {
                        eventTypeName = (sub.eventType as any).$refText;
                    } else if ((sub as any).eventType) {
                        // Fallback: try to extract from the raw eventType
                        eventTypeName = (sub as any).eventType;
                    }

                    // Extract aggregate name from event name (e.g., UpdateTopicEvent -> Topic, UserDeletedEvent -> User)
                    const eventNameWithoutPrefix = eventTypeName.replace(/^(Update|Delete|Create)/, '').replace(/Event$/, '');
                    const subscriptionClassName = `${aggregate.name}Subscribes${eventNameWithoutPrefix}`;
                    methodBody += `\n            eventSubscriptions.add(new ${subscriptionClassName}());`;
                }
            }

            methodBody += `\n        }`;
        }

        methodBody += `\n        return eventSubscriptions;\n    }`;

        return methodBody;
    }

    /**
     * Converts snake_case_upper to PascalCase (e.g., COURSE_EXISTS -> CourseExists)
     */
    private toCamelCase(snakeCaseUpper: string): string {
        return snakeCaseUpper
            .split('_')
            .map(word => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
            .join('');
    }

    private buildClassStructure(entity: EntityExt, projectName: string, isRootEntity: boolean) {
        const aggregateName = entity.$container?.name || 'unknown';
        const config = getGlobalConfig();

        return {
            aggregateName,
            extendsClause: isRootEntity
                ? (aggregateName !== entity.name ? ` extends ${aggregateName}` : ' extends Aggregate')
                : '',
            abstractModifier: isRootEntity ? 'abstract ' : '',
            packageName: config.buildPackageName(
                projectName,
                'microservices',
                aggregateName.toLowerCase(),
                'aggregate'
            )
        };
    }

    private assembleJavaCode(classStructure: any, components: any, entityName: string): string {
        return `package ${classStructure.packageName};

IMPORTS_PLACEHOLDER

@Entity
public ${classStructure.abstractModifier}class ${entityName}${classStructure.extendsClause} {
${components.fields}
${components.defaultConstructor}
${components.dtoConstructor}
${components.projectionDtoConstructor}
${components.copyConstructor}
${components.gettersSetters}
${components.backRefGetterSetter}
${components.eventSubscriptions}
${components.interInvariantMethods}
${components.invariants}
${components.buildDtoMethod}
}`;
    }

    private generateBuildDtoMethod(entity: EntityExt): string {
        const entityName = entity.name;

        // All entities now get their own DTOs, so all entities should have a buildDto() method
        const dtoTypeName = `${entityName}Dto`;
        const dtoSchema = this.dtoRegistry?.dtoByName?.[dtoTypeName];

        if (!dtoSchema) {
            // Fallback: generate simple constructor-based buildDto if no schema found
            return `\n    public ${dtoTypeName} buildDto() {\n        return new ${dtoTypeName}(this);\n    }`;
        }

        const scanner = this.getImportScanner();
        const dtoFieldOverrides = scanner.resolveDtoFieldMappings(entity);
        const setterLines = dtoSchema.fields
            .map(field => scanner.buildDtoSetterFromSchema(field, entity, dtoFieldOverrides))
            .filter((line): line is string => !!line);

        return `\n    public ${dtoTypeName} buildDto() {\n        ${dtoTypeName} dto = new ${dtoTypeName}();\n${setterLines.join('\n')}\n        return dto;\n    }`;
    }
}



/**
 * Entity generator facade that uses the new orchestrator
 */
export class EntityGenerator {
    private orchestrator: EntityOrchestrator;

    constructor(projectName: string = 'project') {
        this.orchestrator = new EntityOrchestrator(projectName);
    }

    /**
     * Main entry point for entity generation
     */
    async generateEntity(entity: EntityExt, options: EntityGenerationOptions): Promise<string> {
        return this.orchestrator.generateEntityCode(entity, options.projectName, options);
    }
}