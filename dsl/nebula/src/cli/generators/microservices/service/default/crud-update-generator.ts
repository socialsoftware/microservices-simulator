import { Aggregate, Entity } from "../../../../../language/generated/ast.js";
import { MethodGeneratorTemplate, MethodMetadata, GenerationOptions } from "../../../common/base/method-generator-template.js";
import { UnifiedTypeResolver as TypeResolver } from "../../../common/unified-type-resolver.js";
import { CrudHelpers } from "../../../common/crud-helpers.js";

/**
 * CRUD Update Method Generator
 *
 * Generates the update{Aggregate}() method for service classes.
 * Uses Template Method pattern for consistent structure.
 *
 * Generated method signature:
 * ```java
 * public EntityDto updateAggregate(EntityDto aggregateDto, UnitOfWork unitOfWork)
 * ```
 *
 * Pattern:
 * 1. Load aggregate (old version)
 * 2. Create immutable copy (new version)
 * 3. Update all mutable primitive fields
 * 4. Register changed aggregate
 * 5. Publish UpdatedEvent
 * 6. Return DTO
 */
export class CrudUpdateGenerator extends MethodGeneratorTemplate {

    protected override extractMetadata(aggregate: Aggregate, options: GenerationOptions): MethodMetadata {
        const rootEntity = aggregate.aggregateElements?.find(el => el.$type === 'Entity' && (el as Entity).isRoot) as Entity;
        if (!rootEntity) {
            throw new Error(`No root entity found in aggregate ${aggregate.name}`);
        }

        const aggregateName = aggregate.name;
        const entityName = rootEntity.name;
        const projectName = options.projectName || 'project';

        return {
            methodName: `update${this.capitalize(aggregateName)}`,
            aggregateName,
            entityName,
            projectName,
            parameters: [
                {
                    name: `${this.lowercase(aggregateName)}Dto`,
                    type: `${entityName}Dto`
                },
                {
                    name: 'unitOfWork',
                    type: 'UnitOfWork'
                }
            ],
            returnType: `${entityName}Dto`,
            rootEntity
        };
    }

    protected override buildMethodSignature(metadata: MethodMetadata): string {
        const paramList = this.buildParameterList(metadata.parameters);
        return `public ${metadata.returnType} ${metadata.methodName}(${paramList})`;
    }

    protected override buildMethodBody(metadata: MethodMetadata): string {
        const rootEntity = metadata.rootEntity as Entity;
        const entityName = metadata.entityName;
        const lowerAggregate = this.lowercase(metadata.aggregateName);
        const capitalizedAggregate = this.capitalize(metadata.aggregateName);

        // Generate field update logic
        const updateLogic = this.generateUpdateLogic(rootEntity, metadata.aggregateName);

        return `            Integer id = ${lowerAggregate}Dto.getAggregateId();
            ${entityName} old${capitalizedAggregate} = (${entityName}) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            ${entityName} new${capitalizedAggregate} = ${lowerAggregate}Factory.create${entityName}FromExisting(old${capitalizedAggregate});
${updateLogic}

            unitOfWorkService.registerChanged(new${capitalizedAggregate}, unitOfWork);`;
    }

    protected override buildEventHandling(metadata: MethodMetadata): string {
        const rootEntity = metadata.rootEntity as Entity;
        const capitalizedAggregate = this.capitalize(metadata.aggregateName);
        const lowerAggregate = this.lowercase(metadata.aggregateName);

        // Generate event constructor arguments
        const eventArgs = this.generateUpdateEventArgs(rootEntity, metadata.aggregateName);

        return `            ${capitalizedAggregate}UpdatedEvent event = new ${capitalizedAggregate}UpdatedEvent(${eventArgs});
            event.setPublisherAggregateVersion(new${capitalizedAggregate}.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return ${lowerAggregate}Factory.create${metadata.entityName}Dto(new${capitalizedAggregate});`;
    }

    // Use default error handling from MethodGeneratorTemplate (with ExceptionGenerator)


    /**
     * Generate update logic for all mutable primitive fields.
     */
    private generateUpdateLogic(rootEntity: Entity, aggregateName: string): string {
        if (!rootEntity.properties) return '';

        const lowerAggregate = this.lowercase(aggregateName);
        const capitalizedAggregate = this.capitalize(aggregateName);
        const targetVar = `new${capitalizedAggregate}`;

        const updates = rootEntity.properties
            .filter(prop => {
                const propName = prop.name.toLowerCase();
                if (propName === 'id') return false;

                // Skip final fields - they can't be updated
                if ((prop as any).isFinal) return false;

                // Skip entity relationships - they shouldn't be updated directly from DTO
                const javaType = TypeResolver.resolveJavaType(prop.type);
                const isCollection = javaType.startsWith('Set<') || javaType.startsWith('List<');
                const isEntityType = !CrudHelpers.isEnumType(prop.type) && TypeResolver.isEntityType(javaType);
                if (isCollection || isEntityType) return false;

                return true;
            })
            .map(prop => {
                const setterName = `set${this.capitalize(prop.name)}`;
                const getterName = this.getGetterMethodName(prop);
                const isBoolean = this.isBooleanProperty(prop);
                const javaType = TypeResolver.resolveJavaType(prop.type);
                const isEnum = CrudHelpers.isEnumType(prop.type) || javaType.match(/^[A-Z][a-zA-Z]*(Type|State|Role)$/);

                if (isBoolean) {
                    return `            ${targetVar}.${setterName}(${lowerAggregate}Dto.${getterName}());`;
                } else if (isEnum) {
                    // For enum types, convert String from DTO to enum using valueOf
                    return `            if (${lowerAggregate}Dto.${getterName}() != null) {
                ${targetVar}.${setterName}(${javaType}.valueOf(${lowerAggregate}Dto.${getterName}()));
            }`;
                } else {
                    return `            if (${lowerAggregate}Dto.${getterName}() != null) {
                ${targetVar}.${setterName}(${lowerAggregate}Dto.${getterName}());
            }`;
                }
            });

        return updates.join('\n');
    }

    /**
     * Build argument list for <Aggregate>UpdatedEvent constructor.
     * Convention: first argument is aggregateId, followed by all primitive (non-relationship)
     * updatable properties of the root entity, in declaration order.
     */
    private generateUpdateEventArgs(rootEntity: Entity, aggregateName: string): string {
        const capitalizedAggregate = this.capitalize(aggregateName);
        const targetVar = `new${capitalizedAggregate}`;

        const args: string[] = [];
        // Always pass aggregateId first
        args.push(`${targetVar}.getAggregateId()`);

        if (!rootEntity.properties) {
            return args.join(', ');
        }

        for (const prop of rootEntity.properties) {
            const propName = (prop as any).name?.toLowerCase?.() ?? '';
            if (propName === 'id') continue;

            if ((prop as any).isFinal) continue;

            const javaType = TypeResolver.resolveJavaType((prop as any).type);
            const isCollection = javaType.startsWith('Set<') || javaType.startsWith('List<');
            const isEntityType =
                !CrudHelpers.isEnumType((prop as any).type) && TypeResolver.isEntityType(javaType);
            if (isCollection || isEntityType) continue;

            // Skip enum-like properties; current *UpdatedEvent classes usually
            // don't carry enum fields such as Role/Type/State
            const isEnum =
                CrudHelpers.isEnumType((prop as any).type) ||
                javaType.match(/^[A-Z][a-zA-Z]*(Type|State|Role)$/);
            if (isEnum) continue;

            const getterName = this.getGetterMethodName(prop as any);
            args.push(`${targetVar}.${getterName}()`);
        }

        return args.join(', ');
    }

    private getGetterMethodName(property: any): string {
        return `get${this.capitalize(property.name)}`;
    }

    private isBooleanProperty(property: any): boolean {
        if (!property.type) return false;
        if (property.type.$type === 'PrimitiveType') {
            return property.type.typeName?.toLowerCase() === 'boolean';
        }

        if (typeof property.type === 'string') {
            return property.type.toLowerCase() === 'boolean';
        }

        return false;
    }
}
