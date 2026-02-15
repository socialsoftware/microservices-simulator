import { Aggregate, Entity } from "../../../../../language/generated/ast.js";
import { MethodGeneratorTemplate, MethodMetadata, GenerationOptions } from "../../../common/base/method-generator-template.js";
import { UnifiedTypeResolver as TypeResolver } from "../../../common/unified-type-resolver.js";
import { CrudHelpers } from "../../../common/crud-helpers.js";
import { EntityRelationshipExtractor } from "../crud/entity-relationship-extractor.js";



export class CrudCreateGenerator extends MethodGeneratorTemplate {

    protected override extractMetadata(aggregate: Aggregate, options: GenerationOptions): MethodMetadata {
        const rootEntity = aggregate.aggregateElements?.find(el => el.$type === 'Entity' && (el as Entity).isRoot) as Entity;
        if (!rootEntity) {
            throw new Error(`No root entity found in aggregate ${aggregate.name}`);
        }

        const aggregateName = aggregate.name;
        const entityName = rootEntity.name;
        const projectName = options.projectName || 'project';

        return {
            methodName: `create${this.capitalize(aggregateName)}`,
            aggregateName,
            entityName,
            projectName,
            parameters: [
                {
                    name: 'createRequest',
                    type: `Create${this.capitalize(aggregateName)}RequestDto`
                },
                {
                    name: 'unitOfWork',
                    type: 'UnitOfWork'
                }
            ],
            returnType: `${entityName}Dto`,
            rootEntity,
            aggregate
        };
    }

    protected override buildMethodSignature(metadata: MethodMetadata): string {
        const paramList = this.buildParameterList(metadata.parameters);
        return `public ${metadata.returnType} ${metadata.methodName}(${paramList})`;
    }

    protected override buildMethodBody(metadata: MethodMetadata): string {
        const rootEntity = metadata.rootEntity as Entity;
        const aggregate = metadata.aggregate as Aggregate;
        const aggregateName = metadata.aggregateName;
        const entityName = metadata.entityName;
        const lowerAggregate = this.lowercase(aggregateName);
        const capitalizedAggregate = this.capitalize(aggregateName);

        
        const entityRelationships = EntityRelationshipExtractor.findEntityRelationshipsWithDetails(rootEntity, aggregate);

        
        const primitiveProps = rootEntity.properties?.filter(prop => {
            const javaType = TypeResolver.resolveJavaType(prop.type);
            const isCollection = javaType.startsWith('Set<') || javaType.startsWith('List<');
            const isEntityType = !CrudHelpers.isEnumType(prop.type) && TypeResolver.isEntityType(javaType);
            const isPrimitive = !isCollection && !isEntityType;
            return isPrimitive && prop.name.toLowerCase() !== 'id';
        }) || [];

        const primitiveSetters = primitiveProps.map(prop => {
            const capitalizedName = this.capitalize(prop.name);
            const javaType = TypeResolver.resolveJavaType(prop.type);
            const isEnum = CrudHelpers.isEnumType(prop.type) || javaType.match(/^[A-Z][a-zA-Z]*(Type|State|Role)$/);

            if (isEnum) {
                return `            ${lowerAggregate}Dto.set${capitalizedName}(createRequest.get${capitalizedName}() != null ? createRequest.get${capitalizedName}().name() : null);`;
            }
            return `            ${lowerAggregate}Dto.set${capitalizedName}(createRequest.get${capitalizedName}());`;
        }).join('\n');

        
        const entitySetters = entityRelationships.map(rel => {
            const capitalizedName = this.capitalize(rel.paramName);

            if (rel.aggregateRef) {
                const projectionDtoName = `${rel.entityName}Dto`;

                if (rel.isCollection) {
                    return `            if (createRequest.get${capitalizedName}() != null) {
                ${lowerAggregate}Dto.set${capitalizedName}(createRequest.get${capitalizedName}().stream().map(srcDto -> {
                    ${projectionDtoName} projDto = new ${projectionDtoName}();
                    projDto.setAggregateId(srcDto.getAggregateId());
                    projDto.setVersion(srcDto.getVersion());
                    projDto.setState(srcDto.getState());
                    return projDto;
                }).collect(Collectors.to${rel.collectionType}()));
            }`;
                } else {
                    return `            if (createRequest.get${capitalizedName}() != null) {
                ${projectionDtoName} ${rel.paramName}Dto = new ${projectionDtoName}();
                ${rel.paramName}Dto.setAggregateId(createRequest.get${capitalizedName}().getAggregateId());
                ${rel.paramName}Dto.setVersion(createRequest.get${capitalizedName}().getVersion());
                ${rel.paramName}Dto.setState(createRequest.get${capitalizedName}().getState());
                ${lowerAggregate}Dto.set${capitalizedName}(${rel.paramName}Dto);
            }`;
                }
            } else {
                return `            ${lowerAggregate}Dto.set${capitalizedName}(createRequest.get${capitalizedName}());`;
            }
        }).join('\n');

        const allSetters = [primitiveSetters, entitySetters].filter(s => s).join('\n');

        return `            ${entityName}Dto ${lowerAggregate}Dto = new ${entityName}Dto();
${allSetters}

            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            ${entityName} ${lowerAggregate} = ${lowerAggregate}Factory.create${capitalizedAggregate}(aggregateId, ${lowerAggregate}Dto);
            unitOfWorkService.registerChanged(${lowerAggregate}, unitOfWork);
            return ${lowerAggregate}Factory.create${entityName}Dto(${lowerAggregate});`;
    }

    protected override buildEventHandling(metadata: MethodMetadata): string {
        
        return '';
    }

    
}
