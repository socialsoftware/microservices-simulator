import { Aggregate, Entity } from "../../../../../language/generated/ast.js";
import { MethodGeneratorTemplate, MethodMetadata, GenerationOptions } from "../../../common/base/method-generator-template.js";
import { findPreventReferencesTo } from "../../../../utils/aggregate-helpers.js";



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
        const projectName = metadata.projectName || 'project';
        const projectException = `${this.capitalize(projectName)}Exception`;

        const preventRefs = findPreventReferencesTo(metadata.aggregateName);
        let preventChecks = '';
        for (const ref of preventRefs) {
            const sourceCap = ref.sourceAggregateName;
            const sourceLc = sourceCap.toLowerCase();
            const fieldGetter = 'get' + ref.fieldName.charAt(0).toUpperCase() + ref.fieldName.slice(1);
            const targetLc = metadata.aggregateName.toLowerCase();
            const projectionIdGetter = 'get' + targetLc.charAt(0).toUpperCase() + targetLc.slice(1) + 'AggregateId';
            const escapedMessage = (ref.message || `Cannot delete ${lowerAggregate} that has referencing ${sourceLc}`).replace(/"/g, '\\"');
            preventChecks += `            ${sourceCap}Repository ${sourceLc}RepositoryRef = applicationContext.getBean(${sourceCap}Repository.class);
            boolean has${sourceCap}References = ${sourceLc}RepositoryRef.findAll().stream()
                .filter(s -> s.getState() != ${entityName}.AggregateState.DELETED)
                .anyMatch(s -> s.${fieldGetter}() != null && id.equals(s.${fieldGetter}().${projectionIdGetter}()));
            if (has${sourceCap}References) {
                throw new ${projectException}("${escapedMessage}");
            }
`;
        }

        return `${preventChecks}            ${entityName} old${capitalizedAggregate} = (${entityName}) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            ${entityName} new${capitalizedAggregate} = ${lowerAggregate}Factory.create${entityName}FromExisting(old${capitalizedAggregate});
            new${capitalizedAggregate}.remove();
            unitOfWorkService.registerChanged(new${capitalizedAggregate}, unitOfWork);`;
    }

    protected override buildEventHandling(metadata: MethodMetadata): string {
        const capitalizedAggregate = this.capitalize(metadata.aggregateName);

        return `            unitOfWorkService.registerEvent(new ${capitalizedAggregate}DeletedEvent(new${capitalizedAggregate}.getAggregateId()), unitOfWork);`;
    }

    


    protected override assembleMethod(
        signature: string,
        body: string,
        eventHandling: string,
        errorHandling: string,
        metadata: MethodMetadata
    ): string {
        
        return `    ${signature} {
        try {
${body}${eventHandling}
${errorHandling}
    }`;
    }
}
