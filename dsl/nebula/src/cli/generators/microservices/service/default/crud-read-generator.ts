import { Aggregate, Entity } from "../../../../../language/generated/ast.js";
import { MethodGeneratorTemplate, MethodMetadata, GenerationOptions } from "../../../common/base/method-generator-template.js";

/**
 * CRUD Read Method Generator
 *
 * Generates the get{Aggregate}ById() method for service classes.
 * Uses Template Method pattern for consistent structure.
 *
 * Generated method signature:
 * ```java
 * public EntityDto getAggregateById(Integer id, UnitOfWork unitOfWork)
 * ```
 *
 * Pattern:
 * 1. Load aggregate via UnitOfWork (read-only)
 * 2. Convert to DTO
 * 3. Return DTO
 */
export class CrudReadGenerator extends MethodGeneratorTemplate {

    protected override extractMetadata(aggregate: Aggregate, options: GenerationOptions): MethodMetadata {
        const rootEntity = aggregate.aggregateElements?.find(el => el.$type === 'Entity' && (el as Entity).isRoot) as Entity;
        if (!rootEntity) {
            throw new Error(`No root entity found in aggregate ${aggregate.name}`);
        }

        const aggregateName = aggregate.name;
        const entityName = rootEntity.name;
        const projectName = options.projectName || 'project';

        return {
            methodName: `get${this.capitalize(aggregateName)}ById`,
            aggregateName,
            entityName,
            projectName,
            parameters: [
                {
                    name: 'id',
                    type: 'Integer'
                },
                {
                    name: 'unitOfWork',
                    type: 'UnitOfWork'
                }
            ],
            returnType: `${entityName}Dto`
        };
    }

    protected override buildMethodSignature(metadata: MethodMetadata): string {
        const paramList = this.buildParameterList(metadata.parameters);
        return `public ${metadata.returnType} ${metadata.methodName}(${paramList})`;
    }

    protected override buildMethodBody(metadata: MethodMetadata): string {
        const entityName = metadata.entityName;
        const lowerAggregate = this.lowercase(metadata.aggregateName);

        return `            ${entityName} ${lowerAggregate} = (${entityName}) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            return ${lowerAggregate}Factory.create${entityName}Dto(${lowerAggregate});`;
    }

    protected override buildEventHandling(metadata: MethodMetadata): string {
        // Read operations don't publish events
        return '';
    }

    // Use default error handling from MethodGeneratorTemplate (with ExceptionGenerator)
}
