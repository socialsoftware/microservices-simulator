import { OrchestrationBase } from "../../../base/orchestration-base.js";
import { getGlobalConfig } from "../../../base/config.js";
import { DtoDefinition, DtoField, Model } from "../../../../../language/generated/ast.js";

export interface SharedDtoGenerationOptions {
    projectName: string;
    architecture?: string;
    features?: string[];
}

export class SharedDtoGenerator extends OrchestrationBase {


    /**
     * Generates a shared DTO from DSL definition
     */
    async generateSharedDtoFromDefinition(
        dtoDefinition: DtoDefinition,
        options: SharedDtoGenerationOptions,
        allSharedDtos?: DtoDefinition[],
        models?: Model[]
    ): Promise<string> {
        const config = getGlobalConfig();
        const packageName = config.buildPackageName(options.projectName, 'shared', 'dtos');

        // Add standard aggregate fields automatically for root entity DTOs
        const standardAggregateFields = this.getStandardAggregateFields(dtoDefinition.name, allSharedDtos);
        const dslFields = dtoDefinition.fields.map((field: DtoField) => ({
            type: this.resolveDtoFieldType(field),
            name: field.name,
            capitalizedName: this.capitalize(field.name)
        }));

        const context = {
            packageName,
            dtoName: dtoDefinition.name,
            isGeneric: !!dtoDefinition.genericParams,
            genericParams: dtoDefinition.genericParams?.params || [],
            fields: [...standardAggregateFields, ...dslFields],
            allSharedDtos,
            dtoMappings: dtoDefinition.mappings || [],
            models: models || []
        };

        return this.generateSharedDtoCode(context);
    }

    /**
     * Legacy method for backward compatibility
     */
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
            }))
        };

        return this.generateSharedDtoCode(context);
    }

    private resolveDtoFieldType(field: DtoField): string {
        const fieldType = field.type;

        if (!fieldType) return 'Object';

        // Handle different field types
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
        const { packageName, dtoName, fields, isGeneric, genericParams, allSharedDtos, dtoMappings, models } = context;

        const genericDeclaration = isGeneric ? `<${genericParams.join(', ')}>` : '';

        // Generate aggregate constructor for specific DTOs
        const aggregateConstructor = this.generateAggregateConstructor(dtoName, fields, allSharedDtos, dtoMappings, models);

        return `package ${packageName};

${this.generateDynamicImports(dtoName, fields, dtoMappings)}

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

        // Only group fields for root entity DTOs
        if (!this.isRootEntityDto(dtoName)) {
            // For non-root DTOs, just list fields normally
            return fields.map((field: any) => `    private ${field.type} ${field.name};`).join('\n');
        }

        // Group fields by their source
        const standardFields = fields.filter(f => ['aggregateId', 'version', 'state'].includes(f.name));
        const rootFields = fields.filter(f => !['aggregateId', 'version', 'state'].includes(f.name) && this.isRootEntityField(f.name));
        const nestedFields = fields.filter(f => !['aggregateId', 'version', 'state'].includes(f.name) && !this.isRootEntityField(f.name) && !this.isCollectionField(f.name));
        const collectionFields = fields.filter(f => this.isCollectionField(f.name));

        // Standard aggregate fields
        if (standardFields.length > 0) {
            lines.push('    // Standard aggregate fields');
            for (const field of standardFields) {
                lines.push(`    private ${field.type} ${field.name};`);
            }
            if (rootFields.length > 0 || nestedFields.length > 0 || collectionFields.length > 0) {
                lines.push('');
            }
        }

        // Root entity fields
        if (rootFields.length > 0) {
            lines.push('    // Root entity fields');
            for (const field of rootFields) {
                lines.push(`    private ${field.type} ${field.name};`);
            }
            if (nestedFields.length > 0 || collectionFields.length > 0) {
                lines.push('');
            }
        }

        // Nested entity fields
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

        // Collection fields (only for collections NOT from root entity - rare cases)
        if (collectionFields.length > 0) {
            lines.push('    // Collections');
            for (const field of collectionFields) {
                lines.push(`    private ${field.type} ${field.name};`);
            }
        }

        return lines.join('\n');
    }

    private generateDynamicImports(dtoName: string, fields: any[], dtoMappings?: any[]): string {
        const imports = new Set<string>();

        // Always needed for DTOs
        imports.add('import java.io.Serializable;');

        // Check if we need specific imports based on field types
        const needsLocalDateTime = fields.some(f => f.type === 'LocalDateTime');
        const needsList = fields.some(f => f.type.startsWith('List<'));
        const needsSet = fields.some(f => f.type.startsWith('Set<'));
        const needsAggregateState = fields.some(f => f.type === 'AggregateState');
        const needsCollectors = dtoMappings && dtoMappings.length > 0; // If we have mappings, we use stream collectors

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
            imports.add('import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;');
        }

        if (needsCollectors) {
            imports.add('import java.util.stream.Collectors;');
        }

        // Sort imports for consistency
        const sortedImports = Array.from(imports).sort();
        return sortedImports.join('\n');
    }

    private generateAggregateConstructor(dtoName: string, fields: any[], allSharedDtos?: DtoDefinition[], dtoMappings?: any[], models?: Model[]): string {
        // Determine if this DTO needs an aggregate constructor or parameter constructor
        const hasAggregateMapping = dtoMappings && dtoMappings.length > 0;

        if (!hasAggregateMapping) {
            // For utility DTOs (no mappings), generate simple parameter constructor
            return this.generateParameterConstructor(dtoName, fields);
        }

        // Generate dynamic aggregate constructor for root entity DTOs
        const aggregateName = dtoName.replace('Dto', '');
        const aggregateVariable = aggregateName.toLowerCase();
        const packageName = `pt.ulisboa.tecnico.socialsoftware.answers.microservices.${aggregateVariable}.aggregate`;

        return `
    public ${dtoName}(${packageName}.${aggregateName} ${aggregateVariable}) {
${this.generateConstructorBody(fields, aggregateName, aggregateVariable, allSharedDtos, dtoMappings, models)}
    }`;
    }

    private generateParameterConstructor(dtoName: string, fields: any[]): string {
        // Generate constructor with individual parameters for utility DTOs like UserDto
        // Only filter out standard aggregate fields for aggregate root DTOs, not utility DTOs
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

        // Group fields by their source
        const standardFields = fields.filter(f => ['aggregateId', 'version', 'state'].includes(f.name));
        const rootFields = fields.filter(f => !['aggregateId', 'version', 'state'].includes(f.name) && this.isRootEntityField(f.name, dtoMappings));
        const nestedFields = fields.filter(f => !['aggregateId', 'version', 'state'].includes(f.name) && !this.isRootEntityField(f.name, dtoMappings) && !this.isCollectionField(f.name));
        const collectionFields = fields.filter(f => this.isCollectionField(f.name));

        // Standard aggregate fields
        if (standardFields.length > 0) {
            lines.push('        // Standard aggregate fields');
            for (const field of standardFields) {
                if (field.name === 'aggregateId') {
                    lines.push(`        set${field.capitalizedName}(${aggregateVariable}.getAggregateId());`);
                } else if (field.name === 'version') {
                    lines.push(`        set${field.capitalizedName}(${aggregateVariable}.getVersion());`);
                } else if (field.name === 'state') {
                    lines.push(`        set${field.capitalizedName}(${aggregateVariable}.getState().toString());`);
                }
            }
            lines.push('');
        }

        // Root entity fields
        if (rootFields.length > 0) {
            lines.push('        // Root entity fields');
            for (const field of rootFields) {
                if (this.isCollectionFieldType(field.name, dtoMappings)) {
                    // Handle collections from root entity - create DTOs directly
                    lines.push(this.generateCollectionMapping(field.name, field.capitalizedName, aggregateVariable, aggregateName, allSharedDtos, dtoMappings, models));
                } else {
                    // Handle regular root entity fields
                    lines.push(`        set${field.capitalizedName}(${aggregateVariable}.get${field.capitalizedName}());`);
                }
            }
            lines.push('');
        }

        // Nested entity fields (from child entities)
        if (nestedFields.length > 0) {
            const nestedEntityName = this.inferNestedEntityName(aggregateName, models);
            lines.push(`        // Fields from ${nestedEntityName}`);
            for (const field of nestedFields) {
                const getterMethod = this.mapFieldToEntityGetter(field.name);
                if (field.type.includes('String') && (field.name === 'type' || field.name.toLowerCase().includes('type'))) {
                    lines.push(`        set${field.capitalizedName}(${aggregateVariable}.get${nestedEntityName}().${getterMethod}().toString());`);
                } else {
                    lines.push(`        set${field.capitalizedName}(${aggregateVariable}.get${nestedEntityName}().${getterMethod}());`);
                }
            }
            lines.push('');
        }

        // Collection fields (only for collections NOT from root entity - rare cases)
        if (collectionFields.length > 0) {
            lines.push('        // Collections');
            for (const field of collectionFields) {
                const entityName = this.inferEntityNameFromCollection(field.name, aggregateName, models);
                lines.push(`        set${field.capitalizedName}(${aggregateVariable}.get${field.capitalizedName}().stream().map(${entityName}::buildDto).collect(Collectors.toSet()));`);
            }
        }

        return lines.join('\n');
    }

    private isCollectionField(fieldName: string): boolean {
        // Collection fields that are NOT part of the root entity (rare cases)
        // Most collections belong to the root entity, so this should be empty or very specific
        const separateCollectionFields: string[] = [];
        return separateCollectionFields.includes(fieldName);
    }

    private inferNestedEntityName(aggregateName: string, models?: Model[]): string {
        // Find the aggregate in the models and get the first non-root entity
        if (models) {
            for (const model of models) {
                const aggregate = model.aggregates.find(a => a.name === aggregateName);
                if (aggregate && aggregate.entities) {
                    // Find the first non-root entity (nested entity)
                    const nestedEntity = aggregate.entities.find(e => !e.isRoot);
                    if (nestedEntity) {
                        return nestedEntity.name;
                    }
                }
            }
        }

        // Fallback: use common pattern if aggregate not found
        return `${aggregateName}Course`;
    }

    private mapFieldToEntityGetter(fieldName: string): string {
        // Map DTO field names to entity getter methods
        const fieldMap: { [key: string]: string } = {
            'courseAggregateId': 'getCourseAggregateId',
            'name': 'getCourseName',
            'type': 'getCourseType',
            'courseVersion': 'getCourseVersion'
        };

        return fieldMap[fieldName] || `get${this.capitalize(fieldName)}`;
    }

    private inferEntityNameFromCollection(fieldName: string, aggregateName: string, models?: Model[]): string {
        // Try to find entity name from aggregate model by looking at property types
        if (models) {
            for (const model of models) {
                const aggregate = model.aggregates.find(a => a.name === aggregateName);
                if (aggregate && aggregate.entities) {
                    // Find root entity and check its properties for Set/List of this field
                    const rootEntity = aggregate.entities.find(e => e.isRoot);
                    if (rootEntity && rootEntity.properties) {
                        const collectionProperty = rootEntity.properties.find(p => p.name === fieldName);
                        if (collectionProperty && collectionProperty.type) {
                            // Extract entity name from collection type
                            const entityName = this.extractEntityNameFromPropertyType(collectionProperty.type);
                            if (entityName) {
                                return entityName;
                            }
                        }
                    }
                }
            }
        }

        // Fallback: use naming convention
        return `${aggregateName}${this.capitalize(fieldName.slice(0, -1))}`;
    }

    private extractEntityNameFromPropertyType(type: any): string | null {
        // Handle Set<EntityName> or List<EntityName>
        if (type.$type === 'SetType' || type.$type === 'ListType') {
            const elementType = type.elementType;
            if (elementType && elementType.$type === 'EntityType') {
                return elementType.type?.ref?.name || elementType.type?.$refText || null;
            }
        }
        return null;
    }

    private generateCollectionMapping(fieldName: string, capitalizedFieldName: string, aggregateVariable: string, aggregateName: string, allSharedDtos?: DtoDefinition[], dtoMappings?: any[], models?: Model[]): string {
        // Generate direct DTO creation for collections based on DSL definitions
        const targetDtoName = this.inferTargetDtoFromCollection(fieldName, dtoMappings);
        const entityName = this.inferEntityNameFromCollection(fieldName, aggregateName, models);

        if (targetDtoName) {
            const dtoConstructorCall = this.generateDtoConstructorCall(targetDtoName, entityName.toLowerCase(), aggregateName, allSharedDtos, dtoMappings);
            return `        set${capitalizedFieldName}(${aggregateVariable}.get${capitalizedFieldName}().stream()
            .map(${entityName.toLowerCase()} -> ${dtoConstructorCall})
            .collect(Collectors.toSet()));`;
        }

        // Fallback to buildDto if no DTO mapping found
        return `        set${capitalizedFieldName}(${aggregateVariable}.get${capitalizedFieldName}().stream().map(${entityName}::buildDto).collect(Collectors.toSet()));`;
    }


    private generateDtoConstructorCall(targetDtoName: string, entityVariable: string, aggregateName: string, allSharedDtos?: DtoDefinition[], dtoMappings?: any[]): string {
        // Find the DTO definition from the shared DTOs
        const dtoDefinition = this.findDtoDefinition(targetDtoName, allSharedDtos);

        if (!dtoDefinition) {
            // Fallback to hardcoded mapping if DTO not found
            return this.generateHardcodedDtoConstructor(targetDtoName, entityVariable, aggregateName);
        }

        // Generate DTO creation with empty constructor + setters
        const dtoVariable = `${targetDtoName.toLowerCase()}`;
        const setterCalls = dtoDefinition.fields.map((field: any) => {
            const entityFieldCall = this.mapEntityFieldToDtoField(field.name, entityVariable, aggregateName, dtoMappings);
            const capitalizedFieldName = field.name.charAt(0).toUpperCase() + field.name.slice(1);
            return `                ${dtoVariable}.set${capitalizedFieldName}(${entityFieldCall});`;
        }).join('\n');

        // Generate inline block for DTO creation using Supplier pattern
        return `((java.util.function.Supplier<${targetDtoName}>) () -> {
            ${targetDtoName} ${dtoVariable} = new ${targetDtoName}();
${setterCalls}
            return ${dtoVariable};
        }).get()`;
    }

    private findDtoDefinition(dtoName: string, allSharedDtos?: DtoDefinition[]): any | null {
        // Search through the shared DTOs to find the DTO definition
        if (allSharedDtos) {
            return allSharedDtos.find((dto: any) => dto.name === dtoName);
        }
        return null;
    }

    private mapEntityFieldToDtoField(dtoFieldName: string, entityVariable: string, aggregateName: string, dtoMappings?: any[]): string {
        // First, try to find explicit mapping from DSL
        if (dtoMappings) {
            for (const mapping of dtoMappings) {
                // Try to match the collection name with the entity variable
                const collectionName = mapping.collectionName;
                const entityVariablePlural = `${entityVariable}s`;

                // Flexible matching: check if collection name matches entity variable patterns
                const isMatch = collectionName === entityVariablePlural ||
                    collectionName === entityVariable ||
                    (collectionName.endsWith('s') && entityVariable.includes(collectionName.slice(0, -1)));

                if (isMatch) {
                    // Find the field mapping for this DTO field
                    const fieldMapping = mapping.fieldMappings?.find((fm: any) => fm.dtoField === dtoFieldName);
                    if (fieldMapping) {
                        const capitalizedEntityField = this.capitalize(fieldMapping.entityField);
                        return `${entityVariable}.get${capitalizedEntityField}()`;
                    }
                }
            }
        }

        // Fallback: try to infer getter method name from field name and entity variable
        const capitalizedField = this.capitalize(dtoFieldName);
        return `${entityVariable}.get${capitalizedField}()`;
    }

    private generateHardcodedDtoConstructor(targetDtoName: string, entityVariable: string, aggregateName: string): string {
        // Generic fallback - use buildDto method if DTO definition not found
        return `${entityVariable}.buildDto()`;
    }

    /**
     * Returns standard aggregate fields that should be automatically added to root entity DTOs
     */
    private getStandardAggregateFields(dtoName: string, allSharedDtos?: DtoDefinition[]): any[] {
        // Add aggregate fields to ALL DTOs
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

    /**
     * Dynamically determines if a DTO is a root entity DTO based on DSL definitions
     */
    private isRootEntityDto(dtoName: string, allSharedDtos?: DtoDefinition[]): boolean {
        // Primary check: DTOs with mappings are root entity DTOs
        if (allSharedDtos) {
            const dtoDefinition = allSharedDtos.find(dto => dto.name === dtoName);
            if (dtoDefinition && dtoDefinition.mappings && dtoDefinition.mappings.length > 0) {
                return true;
            }
        }

        // Secondary check: DTOs ending with specific aggregate patterns
        // TODO: This should be read from aggregate DSL definitions instead of hardcoded
        const rootEntityPattern = /Dto$/;
        if (!rootEntityPattern.test(dtoName)) {
            return false;
        }

        // Utility DTOs (without mappings) are not root entities
        // Examples: UserDto, CourseDto (when used as utility, not aggregate root)
        return false;
    }

    /**
     * Dynamically determines root entity fields from DTO mappings and field patterns
     * TODO: This should read the actual aggregate structure from DSL instead of using patterns
     */
    private isRootEntityField(fieldName: string, dtoMappings?: any[]): boolean {
        // Primary check: If we have mappings, check if this field is mapped from a collection
        if (dtoMappings) {
            const isMappedCollection = dtoMappings.some(mapping => mapping.collectionName === fieldName);
            if (isMappedCollection) {
                return true;
            }
        }

        // Fallback heuristic: Common field name patterns that indicate root entity fields
        // TODO: Replace with actual aggregate structure reading
        const isDateTimeField = fieldName.endsWith('Date') || fieldName.endsWith('Time');
        const isSimpleRootField = /^(acronym|academicTerm|title|content|description|startTime|endTime)$/.test(fieldName);
        const isCollectionField = /s$/.test(fieldName); // Fields ending in 's' are likely collections

        return isSimpleRootField || isDateTimeField || isCollectionField;
    }

    /**
     * Determines if a field is a collection type based on mappings and patterns
     * TODO: Should read field type information from DTO definition instead of guessing
     */
    private isCollectionFieldType(fieldName: string, dtoMappings?: any[]): boolean {
        // Primary check: If this field has a mapping, it's a collection
        if (dtoMappings) {
            const hasMapping = dtoMappings.some(mapping => mapping.collectionName === fieldName);
            if (hasMapping) {
                return true;
            }
        }

        // Fallback heuristic: Fields ending in 's' are likely collections
        // TODO: Read actual field type (Set<>, List<>) from DTO definition
        return fieldName.endsWith('s') && fieldName.length > 1;
    }

    /**
     * Dynamically infers target DTO from mappings
     * TODO: Remove conventional fallback - all mappings should be explicit in DSL
     */
    private inferTargetDtoFromCollection(fieldName: string, dtoMappings?: any[]): string | null {
        // Check if we have explicit mapping from DSL
        if (dtoMappings) {
            const mapping = dtoMappings.find(m => m.collectionName === fieldName);
            if (mapping && mapping.targetDto) {
                return mapping.targetDto.ref?.name || mapping.targetDto.$refText;
            }
        }

        // If no explicit mapping found, return null to trigger buildDto fallback
        // TODO: Make all collection mappings explicit in DSL to avoid this fallback
        return null;
    }

    /**
     * Determines if a DTO should be in the shared module
     * Checks if the DTO is defined in any SharedDtos block
     */
    static isSharedDto(dtoName: string, allSharedDtos?: DtoDefinition[]): boolean {
        // If we have access to shared DTOs from DSL, check if this DTO is defined there
        if (allSharedDtos) {
            return allSharedDtos.some(dto => dto.name === dtoName);
        }

        // Fallback: assume DTOs ending with "Dto" that are commonly shared
        // This should rarely be used if SharedDtos are properly defined
        return true; // Default to shared if we can't determine
    }

    /**
     * Gets the import path for a DTO (either shared or aggregate-specific)
     * Dynamically determines the path based on DSL definitions
     */
    static getDtoImportPath(dtoName: string, options: SharedDtoGenerationOptions, allSharedDtos?: DtoDefinition[], models?: Model[]): string {
        const config = getGlobalConfig();

        // Check if it's a shared DTO
        if (this.isSharedDto(dtoName, allSharedDtos)) {
            return `${config.buildPackageName(options.projectName, 'shared', 'dtos')}.${dtoName}`;
        }

        // For aggregate-specific DTOs, find the aggregate from models
        const aggregateName = this.getAggregateFromDtoName(dtoName, models);
        return `${config.buildPackageName(options.projectName, 'microservices', aggregateName.toLowerCase(), 'aggregate')}.${dtoName}`;
    }

    /**
     * Dynamically determines which aggregate a DTO belongs to by reading from models
     */
    private static getAggregateFromDtoName(dtoName: string, models?: Model[]): string {
        // Remove 'Dto' suffix to get entity name
        const entityName = dtoName.replace('Dto', '');

        // Search through models to find which aggregate contains this entity
        if (models) {
            for (const model of models) {
                for (const aggregate of model.aggregates) {
                    if (aggregate.entities) {
                        // Check if this aggregate has an entity with this name
                        const hasEntity = aggregate.entities.some(e => e.name === entityName);
                        if (hasEntity) {
                            return aggregate.name;
                        }
                    }
                }
            }
        }

        // Fallback: assume entity name is the aggregate name
        return entityName;
    }
}
