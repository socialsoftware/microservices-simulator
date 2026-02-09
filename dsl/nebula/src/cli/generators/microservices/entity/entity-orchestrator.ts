import { Entity, Aggregate } from "../../../../language/generated/ast.js";
import { getGlobalConfig } from "../../common/config.js";
import { EntityGenerationOptions } from "./types.js";
import { generateFields } from "./fields.js";
import { generateDefaultConstructor, generateEntityDtoConstructor, generateCopyConstructor, generateProjectionDtoConstructor } from "./constructors.js";
import { generateGettersSetters, generateBackReferenceGetterSetter } from "./methods.js";
import { generateInvariants } from "./invariants.js";
import { ImportManager, ImportManagerFactory } from "../../../utils/import-manager.js";
import { ErrorHandler, ErrorUtils, ErrorSeverity } from "../../../utils/error-handler.js";
import { TypeResolver } from "../../common/resolvers/type-resolver.js";
import type { DtoSchemaRegistry, DtoFieldSchema } from "../../../services/dto-schema-service.js";
import { getEffectiveFieldMappings, getEffectiveProperties, getEvents, getEntities } from "../../../utils/aggregate-helpers.js";

// ============================================================================
// ENTITY GENERATION ORCHESTRATION
// ============================================================================

// Type definitions for inter-invariant processing
interface EntityReference {
    fieldName: string;
    fieldType: string;
    isCollection: boolean;
}

interface EntitySubscriptionGroup {
    fieldName: string;
    fieldType: string;
    isCollection: boolean;
    subscriptions: SubscriptionInfo[];
}

interface SubscriptionInfo {
    subscriptionClass: string;
    eventType: string;
}

export class EntityOrchestrator {
    private importManager: ImportManager;
    private dtoRegistry?: DtoSchemaRegistry;

    constructor(projectName: string) {
        this.importManager = ImportManagerFactory.createForMicroservice(projectName);
    }

    generateEntityCode(entity: Entity, projectName: string, options?: EntityGenerationOptions): string {
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

    private generateEntityCodeInternal(entity: Entity, projectName: string, options?: EntityGenerationOptions): string {
        const opts = options || { projectName };
        const isRootEntity = entity.isRoot || false;

        this.dtoRegistry = opts.dtoSchemaRegistry;

        const components = this.generateEntityComponents(entity, projectName, opts, isRootEntity);

        const classStructure = this.buildClassStructure(entity, projectName, isRootEntity);

        const javaCode = this.assembleJavaCode(classStructure, components, entity.name);

        return this.finalizeWithImports(javaCode, projectName, isRootEntity, classStructure.aggregateName, entity.name, entity);
    }

    private generateEntityComponents(entity: Entity, projectName: string, opts: EntityGenerationOptions, isRootEntity: boolean) {
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
            interInvariantMethods: isRootEntity ? this.generateInterInvariantMethods(entity.$container as any) : '',
            // All entities now get their own DTOs, so all need buildDto() method
            buildDtoMethod: this.generateBuildDtoMethod(entity)
        };
    }

    private generateEventSubscriptionsMethod(aggregate: Aggregate | undefined): string {
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

        // Add inter-invariant method calls (only for ACTIVE aggregates)
        if (hasInterInvariants) {
            methodBody += `\n        if (this.getState() == AggregateState.ACTIVE) {`;
            for (const invariant of interInvariants) {
                const methodName = `interInvariant${this.toCamelCase(invariant.name)}`;
                methodBody += `\n            ${methodName}(eventSubscriptions);`;
            }
            methodBody += `\n        }`;
        }

        // Add simple subscriptions
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
                methodBody += `\n        eventSubscriptions.add(new ${subscriptionClassName}());`;
            }
        }

        methodBody += `\n        return eventSubscriptions;\n    }`;

        return methodBody;
    }

    private toCamelCase(snakeCaseUpper: string): string {
        // Convert COURSE_EXISTS to CourseExists
        return snakeCaseUpper
            .split('_')
            .map(word => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
            .join('');
    }

    private generateInterInvariantMethods(aggregate: Aggregate | undefined): string {
        if (!aggregate) return '';

        const events = getEvents(aggregate);
        const interInvariants = (events as any)?.interInvariants || [];
        if (interInvariants.length === 0) return '';

        const entities = getEntities(aggregate);
        const rootEntity = entities.find(e => e.isRoot);
        if (!rootEntity) return '';

        return interInvariants.map((invariant: any) =>
            this.generateInterInvariantMethod(invariant, aggregate, rootEntity)
        ).join('\n\n');
    }

    private generateInterInvariantMethod(invariant: any, aggregate: Aggregate, rootEntity: Entity): string {
        const methodName = `interInvariant${this.toCamelCase(invariant.name)}`;
        const subscribedEvents = invariant.subscribedEvents || [];

        // Group subscriptions by entity field, passing invariant name for proper class naming
        const groupedSubs = this.groupSubscriptionsByEntity(subscribedEvents, rootEntity, invariant.name);

        let methodBody = `    private void ${methodName}(Set<EventSubscription> eventSubscriptions) {`;

        for (const group of groupedSubs) {
            if (group.isCollection) {
                // Generate for-loop for collections
                const elementType = this.extractElementType(group.fieldType);
                methodBody += `\n        for (${elementType} item : this.${group.fieldName}) {`;
                for (const sub of group.subscriptions) {
                    methodBody += `\n            eventSubscriptions.add(new ${sub.subscriptionClass}(item));`;
                }
                methodBody += `\n        }`;
            } else {
                // Generate direct subscription for single reference
                for (const sub of group.subscriptions) {
                    methodBody += `\n        eventSubscriptions.add(new ${sub.subscriptionClass}(this.get${this.capitalize(group.fieldName)}()));`;
                }
            }
        }

        methodBody += `\n    }`;
        return methodBody;
    }

    private groupSubscriptionsByEntity(subscriptions: any[], rootEntity: Entity, invariantName: string): EntitySubscriptionGroup[] {
        const groups = new Map<string, EntitySubscriptionGroup>();

        for (const sub of subscriptions) {
            const entityRef = this.extractEntityReference(sub, rootEntity);
            if (!entityRef) {
                continue;
            }

            if (!groups.has(entityRef.fieldName)) {
                groups.set(entityRef.fieldName, {
                    fieldName: entityRef.fieldName,
                    fieldType: entityRef.fieldType,
                    isCollection: entityRef.isCollection,
                    subscriptions: []
                });
            }

            const subscriptionClass = this.buildSubscriptionClassName(sub, rootEntity, invariantName);
            groups.get(entityRef.fieldName)!.subscriptions.push({
                subscriptionClass,
                eventType: this.extractEventTypeName(sub)
            });
        }

        return Array.from(groups.values());
    }

    private extractEntityReference(subscription: any, rootEntity: Entity): EntityReference | null {
        // Parse condition: "course.courseAggregateId == event.aggregateId"
        // Extract: "course" as the field name
        const conditions = subscription.conditions || [];
        if (conditions.length === 0) return null;

        const condition = conditions[0];

        // Extract text from CST node
        let conditionText = '';
        if (condition.condition?.$cstNode?.text) {
            conditionText = condition.condition.$cstNode.text.trim();
        } else if (typeof condition === 'string') {
            conditionText = condition;
        } else if (typeof condition.condition === 'string') {
            conditionText = condition.condition;
        } else {
            return null;
        }

        // Simple regex to extract field name from "fieldName.property == ..."
        const match = conditionText.match(/^(\w+)\./);
        if (!match) {
            return null;
        }

        const fieldName = match[1];

        // Find property in root entity
        const property = rootEntity.properties.find(p => p.name === fieldName);
        if (!property) {
            return null;
        }

        const javaType = TypeResolver.resolveJavaType(property.type);
        const isCollection = javaType.startsWith('Set<') || javaType.startsWith('List<');

        return {
            fieldName,
            fieldType: javaType,
            isCollection
        };
    }

    private extractEventTypeName(subscription: any): string {
        if (typeof subscription.eventType === 'string') {
            return subscription.eventType;
        } else if (subscription.eventType?.ref?.name) {
            return subscription.eventType.ref.name;
        } else if (subscription.eventType?.$refText) {
            return subscription.eventType.$refText;
        }
        return 'UnknownEvent';
    }

    private buildSubscriptionClassName(subscription: any, rootEntity: Entity, invariantName: string): string {
        const eventTypeName = this.extractEventTypeName(subscription);
        // Remove "Event" suffix and common prefixes to get base name
        const baseEventName = eventTypeName
            .replace(/Event$/, '')
            .replace(/^(Update|Delete|Create|Disenroll|Anonymize|Invalidate|Answer)/, '');

        const aggregate = rootEntity.$container as Aggregate;

        // Convert invariant name from ANSWER_EXECUTION_EXISTS to AnswerExecutionExists
        const interInvariantSuffix = invariantName
            .split('_')
            .map((word: string) => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
            .join('');

        return `${aggregate.name}Subscribes${baseEventName}${interInvariantSuffix}`;
    }

    private extractElementType(javaType: string): string {
        const match = javaType.match(/<(.+)>/);
        return match ? match[1] : 'Object';
    }

    private capitalize(str: string): string {
        return str.charAt(0).toUpperCase() + str.slice(1);
    }

    private buildClassStructure(entity: Entity, projectName: string, isRootEntity: boolean) {
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

    private generateBuildDtoMethod(entity: Entity): string {
        const entityName = entity.name;

        // All entities now get their own DTOs, so all entities should have a buildDto() method
        const dtoTypeName = `${entityName}Dto`;
        const dtoSchema = this.dtoRegistry?.dtoByName?.[dtoTypeName];

        if (!dtoSchema) {
            // Fallback: generate simple constructor-based buildDto if no schema found
            return `\n    public ${dtoTypeName} buildDto() {\n        return new ${dtoTypeName}(this);\n    }`;
        }

        const dtoFieldOverrides = this.resolveDtoFieldMappings(entity);
        const setterLines = dtoSchema.fields
            .map(field => this.buildDtoSetterFromSchema(field, entity, dtoFieldOverrides))
            .filter((line): line is string => !!line);

        return `\n    public ${dtoTypeName} buildDto() {\n        ${dtoTypeName} dto = new ${dtoTypeName}();\n${setterLines.join('\n')}\n        return dto;\n    }`;
    }

    private finalizeWithImports(javaCode: string, projectName: string, isRootEntity: boolean, aggregateName: string, entityName: string, entity: Entity): string {
        const detectedImports = this.scanCodeForImports(javaCode, projectName, isRootEntity, aggregateName, entityName, entity);

        this.importManager.clear();
        detectedImports.forEach(imp => {
            this.importManager.addCustomImport(imp);
        });

        this.addRequiredImports(isRootEntity, aggregateName);

        const formattedImports = this.importManager.formatImports();
        const importsString = formattedImports.join('\n');

        return javaCode.replace('IMPORTS_PLACEHOLDER', importsString);
    }

    private scanCodeForImports(javaCode: string, projectName: string, isRoot: boolean, aggregateName?: string, entityName?: string, entity?: Entity): string[] {
        const imports: string[] = [];
        const dtoRegistry = this.dtoRegistry;

        if (javaCode.includes('@Entity')) imports.push('import jakarta.persistence.Entity;');
        if (javaCode.includes('@Id')) imports.push('import jakarta.persistence.Id;');
        if (javaCode.includes('@GeneratedValue')) imports.push('import jakarta.persistence.GeneratedValue;');
        if (javaCode.includes('@OneToOne')) imports.push('import jakarta.persistence.OneToOne;');
        if (javaCode.includes('@OneToMany')) imports.push('import jakarta.persistence.OneToMany;');
        if (javaCode.includes('CascadeType')) imports.push('import jakarta.persistence.CascadeType;');
        if (javaCode.includes('FetchType')) imports.push('import jakarta.persistence.FetchType;');
        if (javaCode.includes('@Enumerated')) imports.push('import jakarta.persistence.Enumerated;');
        if (javaCode.includes('EnumType')) imports.push('import jakarta.persistence.EnumType;');

        if (javaCode.includes('LocalDateTime')) imports.push('import java.time.LocalDateTime;');
        if (javaCode.includes('BigDecimal')) imports.push('import java.math.BigDecimal;');
        if (javaCode.includes('Set<') || javaCode.includes('HashSet')) {
            imports.push('import java.util.Set;');
            imports.push('import java.util.HashSet;');
        }
        if (javaCode.includes('List<') || javaCode.includes('ArrayList')) {
            imports.push('import java.util.List;');
            imports.push('import java.util.ArrayList;');
        }
        if (javaCode.includes('Collectors')) imports.push('import java.util.stream.Collectors;');

        const config = getGlobalConfig();

        if (javaCode.includes('AggregateState')) {
            const aggregateStateImport = `import ${config.getBasePackage()}.ms.domain.aggregate.Aggregate.AggregateState;`;
            imports.push(aggregateStateImport);
        }

        if (isRoot) {
            const aggregateImport = `import ${config.getBasePackage()}.ms.domain.aggregate.Aggregate;`;
            imports.push(aggregateImport);
            // Root entities need EventSubscription for getEventSubscriptions() method
            const eventSubscriptionImport = `import ${config.getBasePackage()}.ms.domain.event.EventSubscription;`;
            imports.push(eventSubscriptionImport);

            // Add imports for subscription classes used in getEventSubscriptions()
            const subscriptionPattern = /new\s+([A-Z][a-zA-Z]*Subscribes[A-Z][a-zA-Z]*)\(/g;
            let subscriptionMatch;
            while ((subscriptionMatch = subscriptionPattern.exec(javaCode)) !== null) {
                const subscriptionClassName = subscriptionMatch[1];
                const subscriptionImport = `import ${config.buildPackageName(projectName, 'microservices', aggregateName?.toLowerCase() || 'unknown', 'events', 'subscribe')}.${subscriptionClassName};`;
                if (!imports.includes(subscriptionImport)) {
                    imports.push(subscriptionImport);
                }
            }

            // Add imports for invariants if SimulatorException is used
            if (javaCode.includes('SimulatorException')) {
                imports.push(`import ${config.getBasePackage()}.ms.exception.SimulatorException;`);
            }
            if (javaCode.includes('INVARIANT_BREAK')) {
                imports.push(`import static ${config.getBasePackage()}.ms.exception.SimulatorErrorMessage.INVARIANT_BREAK;`);
            }
        }

        const dtoPattern = /\b([A-Z][a-zA-Z]*Dto)\b/g;
        let dtoMatch;
        while ((dtoMatch = dtoPattern.exec(javaCode)) !== null) {
            const dtoType = dtoMatch[1];
            const importPath = this.resolveDtoImportPath(dtoType, projectName, aggregateName, dtoRegistry);
            if (importPath) {
                imports.push(importPath);
            }
        }

        const entityPattern = /\b([A-Z][a-zA-Z]*(?:User|Course|Question|Quiz|Topic|Tournament|Answer|Execution|Option))\b/g;
        let entityMatch;
        const excludedEntityNames = ['String', 'Integer', 'Long', 'Double', 'Float', 'Boolean', 'LocalDateTime', 'BigDecimal'];

        const samePackageEntityNames = new Set<string>();
        if (entity && entity.$container) {
            const aggregate = entity.$container as any;
            if (aggregate.entities && Array.isArray(aggregate.entities)) {
                aggregate.entities.forEach((e: Entity) => {
                    if (e.name) {
                        samePackageEntityNames.add(e.name);
                    }
                });
            }
        }

        while ((entityMatch = entityPattern.exec(javaCode)) !== null) {
            const entityType = entityMatch[1];
            if (!excludedEntityNames.includes(entityType) && !entityType.endsWith('Dto')) {
                if (!samePackageEntityNames.has(entityType) && entityType !== entityName && entityType !== aggregateName) {
                    const entityImport = `import ${config.buildPackageName(projectName, 'microservices', aggregateName?.toLowerCase() || 'unknown', 'aggregate')}.${entityType};`;
                    imports.push(entityImport);
                }
            }
        }

        if (entity) {
            const enumTypes = new Set<string>();

            for (const prop of entity.properties) {
                if (prop.type && typeof prop.type === 'object' && prop.type.$type === 'EntityType' && prop.type.type) {
                    const ref = prop.type.type.ref;
                    if (ref && typeof ref === 'object' && '$type' in ref && (ref as any).$type === 'EnumDefinition') {
                        const enumName = (ref as any).name;
                        if (enumName) {
                            enumTypes.add(enumName);
                        }
                    } else if (prop.type.type.$refText) {
                        const refText = prop.type.type.$refText;
                        const javaType = TypeResolver.resolveJavaType(prop.type);
                        if (!TypeResolver.isPrimitiveType(javaType) &&
                            !TypeResolver.isEntityType(javaType) &&
                            !javaType.startsWith('List<') &&
                            !javaType.startsWith('Set<')) {
                            enumTypes.add(refText);
                        }
                    }
                }
            }

            const excludedEnums = ['EnumType', 'CascadeType', 'FetchType', 'AggregateState'];
            for (const enumType of enumTypes) {
                if (!excludedEnums.includes(enumType)) {
                    const enumImport = `import ${config.buildPackageName(projectName, 'shared', 'enums')}.${enumType};`;
                    if (!imports.includes(enumImport)) {
                        imports.push(enumImport);
                    }
                }
            }
        }

        const enumPattern = /\b([A-Z][a-zA-Z]*(?:Type|Role|State))\b/g;
        let enumMatch;
        const excludedEnums = ['EnumType', 'CascadeType', 'FetchType', 'AggregateState', 'LocalDateTime'];
        const foundEnums = new Set<string>();

        while ((enumMatch = enumPattern.exec(javaCode)) !== null) {
            const enumType = enumMatch[1];
            if (!excludedEnums.includes(enumType) &&
                !enumType.endsWith('Dto') &&
                !enumType.includes('List') &&
                !enumType.includes('Set')) {
                foundEnums.add(enumType);
            }
        }

        for (const enumType of foundEnums) {
            const enumImport = `import ${config.buildPackageName(projectName, 'shared', 'enums')}.${enumType};`;
            if (!imports.includes(enumImport)) {
                imports.push(enumImport);
            }
        }

        return imports.flat();
    }

    private buildDtoSetterFromSchema(
        field: DtoFieldSchema,
        entity: Entity,
        dtoFieldOverrides?: Map<string, { property: any; extractField?: string }>
    ): string | null {
        const capName = field.name.charAt(0).toUpperCase() + field.name.slice(1);
        const override = dtoFieldOverrides?.get(field.name);
        const isRootEntity = entity.isRoot || false;

        if (field.isAggregateField && !override) {
            if (!isRootEntity) {
                return null;
            }
            switch (field.name) {
                case 'aggregateId':
                    return '        dto.setAggregateId(getAggregateId());';
                case 'version':
                    return '        dto.setVersion(getVersion());';
                case 'state':
                    return '        dto.setState(getState());';
                default:
                    return null;
            }
        }

        const effectiveProps = getEffectiveProperties(entity);
        const prop = override?.property || effectiveProps.find((p: any) => p.name === (field.sourceName || field.name));
        if (!prop) {
            return null;
        }
        const belongsToEntity = prop.$container?.name === entity.name || (prop as any).$fromMapping;
        if (!belongsToEntity && !override) {
            return null;
        }

        const getterCall = this.buildEntityGetterCall(prop);
        if (!getterCall) {
            return null;
        }

        const effectiveExtractField = override?.extractField || field.extractField;

        if (override) {
            if (this.isEnumProperty(prop)) {
                return `        dto.set${capName}(${getterCall} != null ? ${getterCall}.name() : null);`;
            }
            if (effectiveExtractField) {
                const extractMethod = `get${effectiveExtractField.charAt(0).toUpperCase() + effectiveExtractField.slice(1)}()`;
                const javaType = TypeResolver.resolveJavaType(prop.type);
                const isEntityCollection = javaType.startsWith('List<') || javaType.startsWith('Set<');
                const isDtoCollection = field.isCollection;

                if (isEntityCollection) {
                    const elementTypeMatch = javaType.match(/<(.*)>/);
                    if (elementTypeMatch) {
                        const elementType = elementTypeMatch[1];
                        if (TypeResolver.isPrimitiveType(elementType) ||
                            elementType === 'Integer' ||
                            elementType === 'String' ||
                            elementType === 'Boolean' ||
                            elementType === 'Long') {
                            return null;
                        }
                    }
                }

                if (isEntityCollection && isDtoCollection) {
                    const collector = field.javaType.startsWith('Set<') ? 'Collectors.toSet()' : 'Collectors.toList()';
                    let elementType = 'item';
                    if (javaType) {
                        const elementTypeMatch = javaType.match(/<(.*)>/);
                        if (elementTypeMatch) {
                            elementType = elementTypeMatch[1];
                        }
                    }
                    return `        dto.set${capName}(${getterCall} != null ? ${getterCall}.stream().map((${elementType} item) -> item.${extractMethod}).collect(${collector}) : null);`;
                } else {
                    return `        dto.set${capName}(${getterCall} != null ? ${getterCall}.${extractMethod} : null);`;
                }
            }
            return `        dto.set${capName}(${getterCall});`;
        }

        if (field.derivedAggregateId && field.sourceProperty) {
            const accessor = field.derivedAccessor || 'getAggregateId';
            if (field.isCollection) {
                // For collections, stream and map to extract aggregateIds
                const collector = field.javaType.startsWith('Set<') ? 'Collectors.toSet()' : 'Collectors.toList()';
                return `        dto.set${capName}(${getterCall} != null ? ${getterCall}.stream().map(item -> item.${accessor}()).collect(${collector}) : null);`;
            }
            return `        dto.set${capName}(${getterCall} != null ? ${getterCall}.${accessor}() : null);`;
        }

        // Check if this is an enum field - convert to string using .name()
        // Check field.isEnum first (from schema), then fall back to property check
        if (field.isEnum || (prop && this.isEnumProperty(prop))) {
            if (field.isCollection) {
                const collector = field.javaType.startsWith('Set<') ? 'Collectors.toSet()' : 'Collectors.toList()';
                return `        dto.set${capName}(${getterCall} != null ? ${getterCall}.stream().map(value -> value != null ? value.name() : null).collect(${collector}) : null);`;
            }
            return `        dto.set${capName}(${getterCall} != null ? ${getterCall}.name() : null);`;
        }

        if (field.requiresConversion) {
            if (field.isCollection && field.referencedEntityName) {
                const collector = field.javaType.startsWith('Set<') ? 'Collectors.toSet()' : 'Collectors.toList()';
                if (field.referencedEntityIsRoot || !field.referencedEntityHasGenerateDto) {
                    return `        dto.set${capName}(${getterCall} != null ? ${getterCall}.stream().map(${field.referencedEntityName}::buildDto).collect(${collector}) : null);`;
                } else {
                    return `        dto.set${capName}(${getterCall} != null ? ${getterCall}.stream().map(${field.referencedDtoName}::new).collect(${collector}) : null);`;
                }
            }
            if (!field.isCollection) {
                if (field.referencedEntityIsRoot || !field.referencedEntityHasGenerateDto) {
                    return `        dto.set${capName}(${getterCall} != null ? ${getterCall}.buildDto() : null);`;
                } else {
                    return `        dto.set${capName}(${getterCall} != null ? new ${field.referencedDtoName}(${getterCall}) : null);`;
                }
            }
        }

        if (prop.name.endsWith('Type')) {
            return `        dto.set${capName}(${getterCall} != null ? ${getterCall}.toString() : null);`;
        }

        if (effectiveExtractField) {
            const extractMethod = `get${effectiveExtractField.charAt(0).toUpperCase() + effectiveExtractField.slice(1)}()`;
            const javaType = TypeResolver.resolveJavaType(prop.type);
            const isEntityCollection = javaType.startsWith('List<') || javaType.startsWith('Set<');
            const isDtoCollection = field.isCollection;

            if (isEntityCollection) {
                const elementTypeMatch = javaType.match(/<(.*)>/);
                if (elementTypeMatch) {
                    const elementType = elementTypeMatch[1];
                    if (TypeResolver.isPrimitiveType(elementType) ||
                        elementType === 'Integer' ||
                        elementType === 'String' ||
                        elementType === 'Boolean' ||
                        elementType === 'Long') {
                        return null;
                    }
                }
            }

            if (isEntityCollection && isDtoCollection) {
                const collector = field.javaType.startsWith('Set<') ? 'Collectors.toSet()' : 'Collectors.toList()';
                let elementType = 'item';
                if (javaType) {
                    const elementTypeMatch = javaType.match(/<(.*)>/);
                    if (elementTypeMatch) {
                        elementType = elementTypeMatch[1];
                    }
                }
                return `        dto.set${capName}(${getterCall} != null ? ${getterCall}.stream().map((${elementType} item) -> item.${extractMethod}).collect(${collector}) : null);`;
            } else {
                return `        dto.set${capName}(${getterCall} != null ? ${getterCall}.${extractMethod} : null);`;
            }
        }

        return `        dto.set${capName}(${getterCall});`;
    }

    private buildEntityGetterCall(prop: any): string | null {
        if (!prop?.name) {
            return null;
        }
        const capName = prop.name.charAt(0).toUpperCase() + prop.name.slice(1);
        return `get${capName}()`;
    }

    private resolveDtoFieldMappings(entity: Entity): Map<string, { property: any; extractField?: string }> {
        const overrides = new Map<string, { property: any; extractField?: string }>();
        const fieldMappings = getEffectiveFieldMappings(entity);
        if (!fieldMappings || fieldMappings.length === 0) return overrides;

        const effectiveProps = getEffectiveProperties(entity);
        for (const mapping of fieldMappings) {
            if (!mapping?.dtoField || !mapping?.entityField) continue;
            const targetProp = effectiveProps.find((prop: any) => prop.name === mapping.entityField);
            if (targetProp) {
                overrides.set(mapping.dtoField, {
                    property: targetProp,
                    extractField: mapping.extractField
                });
            }
        }

        return overrides;
    }

    private isEnumProperty(prop: any): boolean {
        if (!prop?.type) {
            return false;
        }
        const javaType = TypeResolver.resolveJavaType(prop.type);
        return TypeResolver.isEnumType(javaType);
    }

    private resolveDtoImportPath(dtoType: string, projectName: string, owningAggregate?: string, dtoRegistry?: DtoSchemaRegistry): string | null {
        const dtoInfo = dtoRegistry?.dtoByName?.[dtoType];
        let targetAggregate = dtoInfo?.aggregateName;

        if (!targetAggregate && dtoType.endsWith('Dto')) {
            targetAggregate = dtoType.slice(0, -3);
        }

        if (!targetAggregate) {
            return null;
        }

        const config = getGlobalConfig();
        const dtoPackage = config.buildPackageName(projectName, 'shared', 'dtos');
        const importPath = `${dtoPackage}.${dtoType}`;
        return `import ${importPath};`;
    }

    private addRequiredImports(isRootEntity: boolean, aggregateName: string): void {
        this.importManager.addJakartaImport('persistence.Entity');

        if (isRootEntity) {
            this.importManager.addBaseFrameworkImport('ms.domain.aggregate.Aggregate');
        }
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
    async generateEntity(entity: Entity, options: EntityGenerationOptions): Promise<string> {
        return this.orchestrator.generateEntityCode(entity, options.projectName, options);
    }
}