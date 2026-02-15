import { Aggregate, Entity } from "../../../../../language/generated/ast.js";
import { MethodGeneratorTemplate, MethodMetadata, GenerationOptions } from "../../../common/base/method-generator-template.js";
import { UnifiedTypeResolver as TypeResolver } from "../../../common/unified-type-resolver.js";
import { CrudHelpers } from "../../../common/crud-helpers.js";



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

        
        const eventArgs = this.generateUpdateEventArgs(rootEntity, metadata.aggregateName);

        return `            ${capitalizedAggregate}UpdatedEvent event = new ${capitalizedAggregate}UpdatedEvent(${eventArgs});
            event.setPublisherAggregateVersion(new${capitalizedAggregate}.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return ${lowerAggregate}Factory.create${metadata.entityName}Dto(new${capitalizedAggregate});`;
    }

    


    

    private generateUpdateLogic(rootEntity: Entity, aggregateName: string): string {
        if (!rootEntity.properties) return '';

        const lowerAggregate = this.lowercase(aggregateName);
        const capitalizedAggregate = this.capitalize(aggregateName);
        const targetVar = `new${capitalizedAggregate}`;

        const updates = rootEntity.properties
            .filter(prop => {
                const propName = prop.name.toLowerCase();
                if (propName === 'id') return false;

                
                if ((prop as any).isFinal) return false;

                
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

    

    private generateUpdateEventArgs(rootEntity: Entity, aggregateName: string): string {
        const capitalizedAggregate = this.capitalize(aggregateName);
        const targetVar = `new${capitalizedAggregate}`;

        const args: string[] = [];
        
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
