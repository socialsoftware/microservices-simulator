import { OrchestrationBase } from "../../common/orchestration-base.js";
import { getGlobalConfig } from "../../common/config.js";
import { DtoDefinition, DtoField, Model } from "../../../../language/generated/ast.js";

export interface SharedDtoGenerationOptions {
    projectName: string;
    architecture?: string;
    features?: string[];
}

export class SharedDtoGenerator extends OrchestrationBase {


    async generateSharedDtoFromDefinition(
        dtoDefinition: DtoDefinition,
        options: SharedDtoGenerationOptions,
        allSharedDtos?: DtoDefinition[],
        models?: Model[]
    ): Promise<string> {
        const config = getGlobalConfig();
        const packageName = config.buildPackageName(options.projectName, 'shared', 'dtos');

        const standardAggregateFields = this.getStandardAggregateFields(dtoDefinition.name, allSharedDtos);
        const dslFields = dtoDefinition.fields.map((field: DtoField) => ({
            type: this.resolveDtoFieldType(field),
            name: field.name,
            capitalizedName: this.capitalize(field.name),
            sourceEntity: field.sourceEntity
        }));

        const context = {
            packageName,
            dtoName: dtoDefinition.name,
            isGeneric: !!dtoDefinition.genericParams,
            genericParams: dtoDefinition.genericParams?.params || [],
            fields: [
                ...standardAggregateFields,
                ...dslFields
            ],
            allSharedDtos,
            dtoMappings: [],
            models: models || [],
            projectName: options.projectName
        };

        return this.generateSharedDtoCode(context);
    }

    async generateSharedDto(
        dtoName: string,
        fields: any[],
        options: SharedDtoGenerationOptions
    ): Promise<string> {
        const config = getGlobalConfig();
        const packageName = config.buildPackageName(options.projectName, 'shared', 'dtos');

        const context = {
            packageName,
            dtoName,
            isGeneric: false,
            genericParams: [],
            fields: fields.map(field => ({
                type: this.resolveJavaType(field.type),
                name: field.name,
                capitalizedName: this.capitalize(field.name)
            })),
            projectName: options.projectName
        };

        return this.generateSharedDtoCode(context);
    }

    private resolveDtoFieldType(field: DtoField): string {
        const fieldType = field.type;

        if (!fieldType) return 'Object';

        if (fieldType.$type === 'PrimitiveType') {
            return this.resolveJavaType(fieldType);
        }

        if (fieldType.$type === 'DtoReference') {
            return fieldType.dto?.ref?.name || fieldType.dto?.$refText || 'UnknownDto';
        }

        if (fieldType.$type === 'ListType' && fieldType.elementType) {
            const elementType = this.extractElementTypeName(fieldType.elementType);
            return `List<${elementType}>`;
        }

        if (fieldType.$type === 'SetType' && fieldType.elementType) {
            const elementType = this.extractElementTypeName(fieldType.elementType);
            return `Set<${elementType}>`;
        }


        return this.resolveJavaType(fieldType);
    }

    private extractElementTypeName(elementType: any): string {
        if (typeof elementType === 'string') {
            return elementType;
        }
        if (elementType && typeof elementType === 'object') {
            if (elementType.$type === 'PrimitiveType' && elementType.name) {
                return elementType.name;
            }
            if (elementType.$type === 'DtoReference') {
                return elementType.dto?.ref?.name || elementType.dto?.$refText || 'UnknownDto';
            }
            if (elementType.$refText) {
                return elementType.$refText;
            }
            if (elementType.ref && elementType.ref.name) {
                return elementType.ref.name;
            }
            if (elementType.name) {
                return elementType.name;
            }
        }
        return 'Object';
    }

    private generateSharedDtoCode(context: any): string {
        const { packageName, dtoName, fields, isGeneric, genericParams, allSharedDtos, dtoMappings, models, projectName } = context;

        const genericDeclaration = isGeneric ? `<${genericParams.join(', ')}>` : '';

        const aggregateConstructor = this.generateAggregateConstructor(dtoName, fields, allSharedDtos, dtoMappings, models, projectName);

        return `package ${packageName};

${this.generateDynamicImports(dtoName, fields, dtoMappings, projectName)}

public class ${dtoName}${genericDeclaration} implements Serializable {
    
${this.generateGroupedFields(fields, dtoName)}
    
    public ${dtoName}() {
    }
    ${aggregateConstructor}
    
${fields.map((field: any) => `    public ${field.type} get${field.capitalizedName}() {
        return ${field.name};
    }
    
    public void set${field.capitalizedName}(${field.type} ${field.name}) {
        this.${field.name} = ${field.name};
    }`).join('\n\n')}
}`;
    }

    private generateGroupedFields(fields: any[], dtoName: string): string {
        const lines: string[] = [];

        if (!this.isRootEntityDto(dtoName)) {
            return fields.map((field: any) => `    private ${field.type} ${field.name};`).join('\n');
        }

        const standardFields = fields.filter(f => ['aggregateId', 'version', 'state'].includes(f.name));
        const rootFields = fields.filter(f => !['aggregateId', 'version', 'state'].includes(f.name) && this.isRootEntityField(f.name));
        const nestedFields = fields.filter(f => !['aggregateId', 'version', 'state'].includes(f.name) && !this.isRootEntityField(f.name) && !this.isCollectionField(f.name));
        const collectionFields = fields.filter(f => this.isCollectionField(f.name));

        if (standardFields.length > 0) {
            lines.push('    // Standard aggregate fields');
            for (const field of standardFields) {
                lines.push(`    private ${field.type} ${field.name};`);
            }
            if (rootFields.length > 0 || nestedFields.length > 0 || collectionFields.length > 0) {
                lines.push('');
            }
        }

        if (rootFields.length > 0) {
            lines.push('    // Root entity fields');
            for (const field of rootFields) {
                lines.push(`    private ${field.type} ${field.name};`);
            }
            if (nestedFields.length > 0 || collectionFields.length > 0) {
                lines.push('');
            }
        }

        if (nestedFields.length > 0) {
            const aggregateName = dtoName.replace('Dto', '');
            const nestedEntityName = this.inferNestedEntityName(aggregateName);
            lines.push(`    // Fields from ${nestedEntityName}`);
            for (const field of nestedFields) {
                lines.push(`    private ${field.type} ${field.name};`);
            }
            if (collectionFields.length > 0) {
                lines.push('');
            }
        }

        if (collectionFields.length > 0) {
            lines.push('    // Collections');
            for (const field of collectionFields) {
                lines.push(`    private ${field.type} ${field.name};`);
            }
        }

        return lines.join('\n');
    }

    private generateDynamicImports(dtoName: string, fields: any[], dtoMappings?: any[], projectName?: string): string {
        const imports = new Set<string>();

        imports.add('import java.io.Serializable;');

        const needsLocalDateTime = fields.some(f => f.type === 'LocalDateTime');
        const needsList = fields.some(f => f.type.startsWith('List<'));
        const needsSet = fields.some(f => f.type.startsWith('Set<'));
        const needsAggregateState = fields.some(f => f.type === 'AggregateState');
        const needsCollectors = (dtoMappings && dtoMappings.length > 0) || fields.some(f => f.type.startsWith('List<') || f.type.startsWith('Set<')); // If we have mappings or collection fields, we use stream collectors

        if (needsLocalDateTime) {
            imports.add('import java.time.LocalDateTime;');
        }

        if (needsList) {
            imports.add('import java.util.List;');
        }

        if (needsSet) {
            imports.add('import java.util.Set;');
        }

        if (needsAggregateState) {
            const config = getGlobalConfig();
            imports.add(`import ${config.getBasePackage()}.ms.domain.aggregate.Aggregate.AggregateState;`);
        }

        if (needsCollectors) {
            imports.add('import java.util.stream.Collectors;');
        }

        const needsAggregateConstructor = (dtoMappings && dtoMappings.length > 0) || this.shouldGenerateAggregateConstructorFromFields(fields);
        if (needsAggregateConstructor && projectName) {
            const config = getGlobalConfig();
            const aggregateName = dtoName.replace('Dto', '');
            const aggregateVariable = aggregateName.toLowerCase();

            const aggregatePackage = config.buildPackageName(
                projectName,
                'microservices',
                aggregateVariable,
                'aggregate'
            );

            imports.add(`import ${aggregatePackage}.${aggregateName};`);
            const entityImports = new Set<string>();
            for (const f of fields as any[]) {
                const t: string = typeof f.type === 'string' ? f.type : '';
                const mapped: string | undefined = f.sourceEntity?.ref?.name || f.sourceEntity?.$refText;
                if (mapped) {
                    entityImports.add(mapped);
                    continue;
                }
                const isCollection = t.startsWith('List<') || t.startsWith('Set<');
                if (isCollection) {
                    const inferredColl = this.inferEntityNameFromCollection(f.name, aggregateName);
                    if (inferredColl) entityImports.add(inferredColl);
                }
            }
            for (const name of entityImports) {
                imports.add(`import ${aggregatePackage}.${name};`);
            }
        }

        const sortedImports = Array.from(imports).sort();
        return sortedImports.join('\n');
    }

    private generateAggregateConstructor(dtoName: string, fields: any[], allSharedDtos?: DtoDefinition[], dtoMappings?: any[], models?: Model[], projectName?: string): string {
        const hasAggregateMapping = dtoMappings && dtoMappings.length > 0;
        const shouldUseAggregateFromFields = this.shouldGenerateAggregateConstructorFromFields(fields);

        if (!hasAggregateMapping && !shouldUseAggregateFromFields) {
            return this.generateParameterConstructor(dtoName, fields);
        }

        const aggregateName = dtoName.replace('Dto', '');
        const aggregateVariable = aggregateName.toLowerCase();

        return `
    public ${dtoName}(${aggregateName} ${aggregateVariable}) {
${this.generateConstructorBody(fields, aggregateName, aggregateVariable, allSharedDtos, dtoMappings, models)}
    }`;
    }

    private generateParameterConstructor(dtoName: string, fields: any[]): string {
        const isUtilityDto = !this.isRootEntityDto(dtoName);
        const fieldsToInclude = isUtilityDto ? fields : fields.filter(field => !['aggregateId', 'version', 'state'].includes(field.name));

        const params = fieldsToInclude
            .map(field => `${field.type} ${field.name}`)
            .join(', ');

        const setterCalls = fieldsToInclude
            .map(field => `        set${field.capitalizedName}(${field.name});`)
            .join('\n');

        return `
    public ${dtoName}(${params}) {
${setterCalls}
    }`;
    }

    private generateConstructorBody(fields: any[], aggregateName: string, aggregateVariable: string, allSharedDtos?: DtoDefinition[], dtoMappings?: any[], models?: Model[]): string {
        const lines: string[] = [];

        for (const field of fields) {
            const setterCall = this.generateFieldSetterCall(field, aggregateVariable, aggregateName, allSharedDtos, dtoMappings, models);
            lines.push(setterCall);
        }

        return lines.join('\n');
    }


    private generateFieldSetterCall(
        field: any,
        aggregateVariable: string,
        aggregateName: string,
        allSharedDtos?: DtoDefinition[],
        dtoMappings?: any[],
        models?: Model[]
    ): string {
        if (field.name === 'aggregateId') {
            return `        set${field.capitalizedName}(${aggregateVariable}.getAggregateId());`;
        } else if (field.name === 'version') {
            return `        set${field.capitalizedName}(${aggregateVariable}.getVersion());`;
        } else if (field.name === 'state') {
            return `        set${field.capitalizedName}(${aggregateVariable}.getState());`;
        }

        const isSingleDtoField = !field.type.startsWith('List<') && !field.type.startsWith('Set<') && field.type.endsWith('Dto');
        if (isSingleDtoField) {
            return `        set${field.capitalizedName}(${aggregateVariable}.get${field.capitalizedName}().buildDto());`;
        }

        if (this.isRootEntityField(field.name, dtoMappings)) {
            if (this.isCollectionFieldType(field.name, dtoMappings)) {
                return this.generateCollectionMapping(field, aggregateVariable, aggregateName, allSharedDtos, dtoMappings, models);
            } else {
                if (this.shouldConvertToString(field)) {
                    return `        set${field.capitalizedName}(${aggregateVariable}.get${field.capitalizedName}().toString());`;
                } else {
                    return `        set${field.capitalizedName}(${aggregateVariable}.get${field.capitalizedName}());`;
                }
            }
        }

        const nestedEntityName = this.inferNestedEntityName(aggregateName, models);
        if (nestedEntityName) {
            const getterMethod = this.mapFieldToEntityGetter(field.name);
            if (this.shouldConvertToString(field)) {
                return `        set${field.capitalizedName}(${aggregateVariable}.get${nestedEntityName}().${getterMethod}().toString());`;
            } else {
                return `        set${field.capitalizedName}(${aggregateVariable}.get${nestedEntityName}().${getterMethod}());`;
            }
        }

        return `        set${field.capitalizedName}(${aggregateVariable}.get${field.capitalizedName}());`;
    }


    private isCollectionField(fieldName: string): boolean {
        const separateCollectionFields: string[] = [];
        return separateCollectionFields.includes(fieldName);
    }

    private shouldConvertToString(field: any): boolean {
        if (!field.type.includes('String')) {
            return false;
        }

        const enumPatterns = ['type', 'state', 'status', 'role'];
        const fieldNameLower = field.name.toLowerCase();
        const isLikelyEnum = enumPatterns.some(pattern => fieldNameLower.includes(pattern));

        const datePatterns = ['date', 'time', 'datetime', 'timestamp'];
        const isLikelyDate = datePatterns.some(pattern => fieldNameLower.includes(pattern));

        return isLikelyEnum || isLikelyDate;
    }

    private inferNestedEntityName(aggregateName: string, models?: Model[]): string {
        if (models) {
            for (const model of models) {
                const aggregate = model.aggregates.find(a => a.name === aggregateName);
                if (aggregate && aggregate.entities) {
                    const nestedEntity = aggregate.entities.find(e => !e.isRoot);
                    if (nestedEntity) {
                        return nestedEntity.name;
                    }
                }
            }
        }

        return `${aggregateName}Course`;
    }

    private mapFieldToEntityGetter(fieldName: string): string {
        const fieldMap: { [key: string]: string } = {
            'courseAggregateId': 'getCourseAggregateId',
            'name': 'getCourseName',
            'type': 'getCourseType',
            'courseVersion': 'getCourseVersion'
        };

        return fieldMap[fieldName] || `get${this.capitalize(fieldName)}`;
    }

    private inferEntityNameFromCollection(fieldName: string, aggregateName: string, models?: Model[]): string {
        if (models) {
            for (const model of models) {
                const aggregate = model.aggregates.find(a => a.name === aggregateName);
                if (aggregate && aggregate.entities) {
                    const rootEntity = aggregate.entities.find(e => e.isRoot);
                    if (rootEntity && rootEntity.properties) {
                        const collectionProperty = rootEntity.properties.find(p => p.name === fieldName);
                        if (collectionProperty && collectionProperty.type) {
                            const entityName = this.extractEntityNameFromPropertyType(collectionProperty.type);
                            if (entityName) {
                                return entityName;
                            }
                        }
                    }
                }
            }
        }

        return `${aggregateName}${this.capitalize(fieldName.slice(0, -1))}`;
    }

    private extractEntityNameFromPropertyType(type: any): string | null {
        if (type.$type === 'SetType' || type.$type === 'ListType') {
            const elementType = type.elementType;
            if (elementType && elementType.$type === 'EntityType') {
                return elementType.type?.ref?.name || elementType.type?.$refText || null;
            }
        }
        return null;
    }

    private generateCollectionMapping(field: any, aggregateVariable: string, aggregateName: string, allSharedDtos?: DtoDefinition[], dtoMappings?: any[], models?: Model[]): string {
        const fieldName: string = field.name;
        const capitalizedFieldName: string = field.capitalizedName;
        const fieldType: string = field.type as string;
        const mappedEntityName = field.sourceEntity?.ref?.name || field.sourceEntity?.$refText;
        const entityName = mappedEntityName || this.inferEntityNameFromCollection(fieldName, aggregateName, models);
        const collector = fieldType.startsWith('List<') ? 'toList()' : 'toSet()';
        return `        set${capitalizedFieldName}(${aggregateVariable}.get${capitalizedFieldName}().stream().map(${entityName}::buildDto).collect(Collectors.${collector}));`;
    }


    private getStandardAggregateFields(dtoName: string, allSharedDtos?: DtoDefinition[]): any[] {
        return [
            {
                type: 'Integer',
                name: 'aggregateId',
                capitalizedName: 'AggregateId'
            },
            {
                type: 'Integer',
                name: 'version',
                capitalizedName: 'Version'
            },
            {
                type: 'AggregateState',
                name: 'state',
                capitalizedName: 'State'
            }
        ];
    }

    private shouldGenerateAggregateConstructorFromFields(fields: any[]): boolean {
        if (!fields || fields.length === 0) return false;
        return fields.some((f: any) => {
            const t = typeof f.type === 'string' ? f.type : '';
            return t.endsWith('Dto') || t.startsWith('List<') || t.startsWith('Set<');
        });
    }

    private isRootEntityDto(dtoName: string, allSharedDtos?: DtoDefinition[]): boolean {
        return false;
    }

    private isRootEntityField(fieldName: string, dtoMappings?: any[]): boolean {
        if (dtoMappings) {
            const isMappedCollection = dtoMappings.some(mapping => mapping.collectionName === fieldName);
            if (isMappedCollection) {
                return true;
            }
        }

        const isDateTimeField = fieldName.endsWith('Date') || fieldName.endsWith('Time');
        const isSimpleRootField = /^(acronym|academicTerm|title|content|description|startTime|endTime)$/.test(fieldName);
        const isCollectionField = /s$/.test(fieldName);

        return isSimpleRootField || isDateTimeField || isCollectionField;
    }

    private isCollectionFieldType(fieldName: string, dtoMappings?: any[]): boolean {
        if (dtoMappings) {
            const hasMapping = dtoMappings.some(mapping => mapping.collectionName === fieldName);
            if (hasMapping) {
                return true;
            }
        }

        return fieldName.endsWith('s') && fieldName.length > 1;
    }

    static isSharedDto(dtoName: string, allSharedDtos?: DtoDefinition[]): boolean {
        if (allSharedDtos) {
            return allSharedDtos.some(dto => dto.name === dtoName);
        }

        return true;
    }

    static getDtoImportPath(dtoName: string, options: SharedDtoGenerationOptions, allSharedDtos?: DtoDefinition[], models?: Model[]): string {
        const config = getGlobalConfig();

        if (this.isSharedDto(dtoName, allSharedDtos)) {
            return `${config.buildPackageName(options.projectName, 'shared', 'dtos')}.${dtoName}`;
        }

        const aggregateName = this.getAggregateFromDtoName(dtoName, models);
        return `${config.buildPackageName(options.projectName, 'microservices', aggregateName.toLowerCase(), 'aggregate')}.${dtoName}`;
    }

    private static getAggregateFromDtoName(dtoName: string, models?: Model[]): string {
        const entityName = dtoName.replace('Dto', '');

        if (models) {
            for (const model of models) {
                for (const aggregate of model.aggregates) {
                    if (aggregate.entities) {
                        const hasEntity = aggregate.entities.some(e => e.name === entityName);
                        if (hasEntity) {
                            return aggregate.name;
                        }
                    }
                }
            }
        }

        return entityName;
    }
}
