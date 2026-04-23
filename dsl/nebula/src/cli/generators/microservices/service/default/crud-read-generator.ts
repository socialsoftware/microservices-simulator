import { Aggregate, Entity } from "../../../../../language/generated/ast.js";
import { MethodGeneratorTemplate, MethodMetadata, GenerationOptions } from "../../../common/base/method-generator-template.js";



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
        
        return '';
    }

    
}
