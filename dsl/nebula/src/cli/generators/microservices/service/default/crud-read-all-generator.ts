import { Aggregate, Entity } from "../../../../../language/generated/ast.js";
import { MethodGeneratorTemplate, MethodMetadata, GenerationOptions } from "../../../common/base/method-generator-template.js";

/**
 * CRUD ReadAll Method Generator
 *
 * Generates the getAll{Aggregate}s() method for service classes.
 * Uses Template Method pattern for consistent structure.
 *
 * Generated method signature:
 * ```java
 * public List<EntityDto> getAllAggregates(UnitOfWork unitOfWork)
 * ```
 *
 * Pattern:
 * 1. Query repository for all aggregate IDs
 * 2. Load each aggregate via UnitOfWork (read-only)
 * 3. Convert to DTOs
 * 4. Return list of DTOs
 */
export class CrudReadAllGenerator extends MethodGeneratorTemplate {

    protected override extractMetadata(aggregate: Aggregate, options: GenerationOptions): MethodMetadata {
        const rootEntity = aggregate.aggregateElements?.find(el => el.$type === 'Entity' && (el as Entity).isRoot) as Entity;
        if (!rootEntity) {
            throw new Error(`No root entity found in aggregate ${aggregate.name}`);
        }

        const aggregateName = aggregate.name;
        const entityName = rootEntity.name;
        const projectName = options.projectName || 'project';

        return {
            methodName: `getAll${this.capitalize(aggregateName)}s`,
            aggregateName,
            entityName,
            projectName,
            parameters: [
                {
                    name: 'unitOfWork',
                    type: 'UnitOfWork'
                }
            ],
            returnType: `List<${entityName}Dto>`
        };
    }

    protected override buildMethodSignature(metadata: MethodMetadata): string {
        const paramList = this.buildParameterList(metadata.parameters);
        return `public ${metadata.returnType} ${metadata.methodName}(${paramList})`;
    }

    protected override buildMethodBody(metadata: MethodMetadata): string {
        const entityName = metadata.entityName;
        const lowerAggregate = this.lowercase(metadata.aggregateName);

        return `            Set<Integer> aggregateIds = ${lowerAggregate}Repository.findAll().stream()
                .map(${entityName}::getAggregateId)
                .collect(Collectors.toSet());

            return aggregateIds.stream()
                .map(id -> (${entityName}) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork))
                .map(${lowerAggregate}Factory::create${entityName}Dto)
                .collect(Collectors.toList());`;
    }

    protected override buildEventHandling(metadata: MethodMetadata): string {
        // Read operations don't publish events
        return '';
    }

    // Use default error handling from MethodGeneratorTemplate (with ExceptionGenerator)
}
