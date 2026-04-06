import { Aggregate, Entity } from "../../../../../language/generated/ast.js";
import { MethodGeneratorTemplate, MethodMetadata, GenerationOptions } from "../../../common/base/method-generator-template.js";
import { UnifiedTypeResolver as TypeResolver } from "../../../common/unified-type-resolver.js";
import { CrudHelpers } from "../../../common/crud-helpers.js";
import { EntityRelationshipExtractor } from "../crud/entity-relationship-extractor.js";
import { findRootAggregateByName, getEntities } from "../../../../utils/aggregate-helpers.js";



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
            const isEnum = CrudHelpers.isEnumType(prop.type);

            if (isEnum) {
                return `            ${lowerAggregate}Dto.set${capitalizedName}(createRequest.get${capitalizedName}() != null ? createRequest.get${capitalizedName}().name() : null);`;
            }
            return `            ${lowerAggregate}Dto.set${capitalizedName}(createRequest.get${capitalizedName}());`;
        }).join('\n');


        const entitySetters = entityRelationships.map(rel => {
            const capitalizedName = this.capitalize(rel.paramName);

            if (rel.aggregateRef) {
                const projectionDtoName = `${rel.entityName}Dto`;
                const sourceAggregateName = rel.aggregateRef;

                const sourceAggregate = findRootAggregateByName(sourceAggregateName);
                const sourceRoot = sourceAggregate
                    ? getEntities(sourceAggregate).find(e => (e as any).isRoot)
                    : undefined;

                const projectionEntity = (aggregate.aggregateElements || []).find(
                    (el: any) => el.$type === 'Entity' && el.name === rel.entityName
                ) as any;

                let enrichableMappings: Array<{ field: string; isEnumState: boolean }> = [];
                if (sourceRoot && projectionEntity?.fieldMappings) {
                    for (const m of projectionEntity.fieldMappings) {
                        const parts = m.dtoField?.parts || [];
                        if (parts.length !== 1) continue;
                        const dtoFieldName = parts[0];
                        const sourceProp = (sourceRoot as any).properties?.find(
                            (p: any) => p.name === dtoFieldName
                        );
                        if (!sourceProp) continue;
                        enrichableMappings.push({ field: dtoFieldName, isEnumState: dtoFieldName === 'state' });
                    }
                }

                const canEnrich = sourceRoot !== undefined && enrichableMappings.length > 0;

                if (canEnrich) {
                    const sourceClass = sourceAggregateName;
                    const sourceDtoClass = `${sourceAggregateName}Dto`;
                    const enrichLines = enrichableMappings.map(m => {
                        const cap = this.capitalize(m.field);
                        return `                ${rel.paramName}Dto.set${cap}(refSourceDto.get${cap}());`;
                    }).join('\n');

                    if (rel.isCollection) {
                        const itemEnrichLines = enrichableMappings.map(m => {
                            const cap = this.capitalize(m.field);
                            return `                    projDto.set${cap}(refItemDto.get${cap}());`;
                        }).join('\n');
                        return `            if (createRequest.get${capitalizedName}() != null) {
                ${lowerAggregate}Dto.set${capitalizedName}(createRequest.get${capitalizedName}().stream().map(reqDto -> {
                    ${sourceClass} refItem = (${sourceClass}) unitOfWorkService.aggregateLoadAndRegisterRead(reqDto.getAggregateId(), unitOfWork);
                    ${sourceDtoClass} refItemDto = new ${sourceDtoClass}(refItem);
                    ${projectionDtoName} projDto = new ${projectionDtoName}();
                    projDto.setAggregateId(refItemDto.getAggregateId());
                    projDto.setVersion(refItemDto.getVersion());
                    projDto.setState(refItemDto.getState() != null ? refItemDto.getState().name() : null);
${itemEnrichLines}
                    return projDto;
                }).collect(Collectors.to${rel.collectionType}()));
            }`;
                    }
                    return `            if (createRequest.get${capitalizedName}() != null) {
                ${sourceClass} refSource = (${sourceClass}) unitOfWorkService.aggregateLoadAndRegisterRead(createRequest.get${capitalizedName}().getAggregateId(), unitOfWork);
                ${sourceDtoClass} refSourceDto = new ${sourceDtoClass}(refSource);
                ${projectionDtoName} ${rel.paramName}Dto = new ${projectionDtoName}();
                ${rel.paramName}Dto.setAggregateId(refSourceDto.getAggregateId());
                ${rel.paramName}Dto.setVersion(refSourceDto.getVersion());
                ${rel.paramName}Dto.setState(refSourceDto.getState() != null ? refSourceDto.getState().name() : null);
${enrichLines}
                ${lowerAggregate}Dto.set${capitalizedName}(${rel.paramName}Dto);
            }`;
                }

                if (rel.isCollection) {
                    return `            if (createRequest.get${capitalizedName}() != null) {
                ${lowerAggregate}Dto.set${capitalizedName}(createRequest.get${capitalizedName}().stream().map(srcDto -> {
                    ${projectionDtoName} projDto = new ${projectionDtoName}();
                    projDto.setAggregateId(srcDto.getAggregateId());
                    projDto.setVersion(srcDto.getVersion());
                    projDto.setState(srcDto.getState() != null ? srcDto.getState().name() : null);
                    return projDto;
                }).collect(Collectors.to${rel.collectionType}()));
            }`;
                }
                return `            if (createRequest.get${capitalizedName}() != null) {
                ${projectionDtoName} ${rel.paramName}Dto = new ${projectionDtoName}();
                ${rel.paramName}Dto.setAggregateId(createRequest.get${capitalizedName}().getAggregateId());
                ${rel.paramName}Dto.setVersion(createRequest.get${capitalizedName}().getVersion());
                ${rel.paramName}Dto.setState(createRequest.get${capitalizedName}().getState() != null ? createRequest.get${capitalizedName}().getState().name() : null);
                ${lowerAggregate}Dto.set${capitalizedName}(${rel.paramName}Dto);
            }`;
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
