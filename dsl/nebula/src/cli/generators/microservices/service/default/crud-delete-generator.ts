import { Aggregate, Entity } from "../../../../../language/generated/ast.js";
import { MethodGeneratorTemplate, MethodMetadata, GenerationOptions } from "../../../common/base/method-generator-template.js";

/**
 * CRUD Delete Method Generator
 *
 * Generates the delete{Aggregate}() method for service classes.
 * Uses Template Method pattern for consistent structure.
 *
 * Generated method signature:
 * ```java
 * public void deleteAggregate(Integer id, UnitOfWork unitOfWork)
 * ```
 *
 * Pattern:
 * 1. Load aggregate (old version)
 * 2. Create immutable copy (new version)
 * 3. Call remove() to mark as deleted
 * 4. Register changed aggregate
 * 5. Publish DeletedEvent
 */
export class CrudDeleteGenerator extends MethodGeneratorTemplate {

    protected override extractMetadata(aggregate: Aggregate, options: GenerationOptions): MethodMetadata {
        const rootEntity = aggregate.aggregateElements?.find(el => el.$type === 'Entity' && (el as Entity).isRoot) as Entity;
        if (!rootEntity) {
            throw new Error(`No root entity found in aggregate ${aggregate.name}`);
        }

        const aggregateName = aggregate.name;
        const entityName = rootEntity.name;
        const projectName = options.projectName || 'project';

        return {
            methodName: `delete${this.capitalize(aggregateName)}`,
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
            returnType: 'void'
        };
    }

    protected override buildMethodSignature(metadata: MethodMetadata): string {
        const paramList = this.buildParameterList(metadata.parameters);
        return `public ${metadata.returnType} ${metadata.methodName}(${paramList})`;
    }

    protected override buildMethodBody(metadata: MethodMetadata): string {
        const entityName = metadata.entityName;
        const lowerAggregate = this.lowercase(metadata.aggregateName);
        const capitalizedAggregate = this.capitalize(metadata.aggregateName);

        return `            ${entityName} old${capitalizedAggregate} = (${entityName}) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            ${entityName} new${capitalizedAggregate} = ${lowerAggregate}Factory.create${entityName}FromExisting(old${capitalizedAggregate});
            new${capitalizedAggregate}.remove();
            unitOfWorkService.registerChanged(new${capitalizedAggregate}, unitOfWork);`;
    }

    protected override buildEventHandling(metadata: MethodMetadata): string {
        const capitalizedAggregate = this.capitalize(metadata.aggregateName);

        return `            unitOfWorkService.registerEvent(new ${capitalizedAggregate}DeletedEvent(new${capitalizedAggregate}.getAggregateId()), unitOfWork);`;
    }

    // Use default error handling from MethodGeneratorTemplate (with ExceptionGenerator)


    protected override assembleMethod(
        signature: string,
        body: string,
        eventHandling: string,
        errorHandling: string,
        metadata: MethodMetadata
    ): string {
        // Delete method returns void, so no return statement needed after event
        return `    ${signature} {
        try {
${body}${eventHandling}
${errorHandling}
    }`;
    }
}
