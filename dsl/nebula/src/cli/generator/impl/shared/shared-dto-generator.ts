import { OrchestrationBase } from "../../base/orchestration-base.js";
import { getGlobalConfig } from "../../base/config.js";
import { DtoDefinition, DtoField } from "../../../../language/generated/ast.js";

export interface SharedDtoGenerationOptions {
    projectName: string;
    architecture?: string;
    features?: string[];
}

export class SharedDtoGenerator extends OrchestrationBase {


    /**
     * Generates a shared DTO from DSL definition (legacy approach)
     */
    async generateSharedDtoFromDefinition(
        dtoDefinition: DtoDefinition,
        options: SharedDtoGenerationOptions,
        allSharedDtos?: DtoDefinition[]
    ): Promise<string> {
        const config = getGlobalConfig();
        const packageName = config.buildPackageName(options.projectName, 'shared', 'dtos');

        // Add standard aggregate fields automatically for root entity DTOs
        const standardAggregateFields = this.getStandardAggregateFields(dtoDefinition.name);
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
            dtoMappings: dtoDefinition.mappings || []
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

        if (fieldType.$type === 'GenericType') {
            return fieldType.name;
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
        const { packageName, dtoName, fields, isGeneric, genericParams, allSharedDtos, dtoMappings } = context;

        const genericDeclaration = isGeneric ? `<${genericParams.join(', ')}>` : '';

        // Generate aggregate constructor for specific DTOs
        const aggregateConstructor = this.generateAggregateConstructor(dtoName, fields, allSharedDtos, dtoMappings);

        return `package ${packageName};

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
${this.generateAdditionalImports(dtoName, fields)}

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
        const rootEntityDtos = ['ExecutionDto', 'CourseDto', 'QuizDto', 'TournamentDto', 'AnswerDto', 'QuestionDto', 'TopicDto'];

        if (!rootEntityDtos.includes(dtoName)) {
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

    private generateAdditionalImports(dtoName: string, fields: any[]): string {
        // Since we're creating DTOs directly instead of using buildDto methods,
        // we no longer need to import entity classes
        return '';
    }

    private generateAggregateConstructor(dtoName: string, fields: any[], allSharedDtos?: DtoDefinition[], dtoMappings?: any[]): string {
        // Only generate aggregate constructors for root entity DTOs
        const rootEntityDtos = ['ExecutionDto', 'CourseDto', 'QuizDto', 'TournamentDto', 'AnswerDto', 'QuestionDto', 'TopicDto'];

        if (!rootEntityDtos.includes(dtoName)) {
            // For non-root entity DTOs like UserDto, generate simple parameter constructor
            if (dtoName === 'UserDto') {
                return this.generateParameterConstructor(dtoName, fields);
            }
            return '';
        }

        // Generate dynamic aggregate constructor for root entity DTOs
        const aggregateName = dtoName.replace('Dto', '');
        const aggregateVariable = aggregateName.toLowerCase();
        const packageName = `pt.ulisboa.tecnico.socialsoftware.answers.microservices.${aggregateVariable}.aggregate`;

        return `
    public ${dtoName}(${packageName}.${aggregateName} ${aggregateVariable}) {
${this.generateConstructorBody(fields, aggregateName, aggregateVariable, allSharedDtos, dtoMappings)}
    }`;
    }

    private generateParameterConstructor(dtoName: string, fields: any[]): string {
        // Generate constructor with individual parameters for utility DTOs like UserDto
        const params = fields
            .filter(field => !['aggregateId', 'version', 'state'].includes(field.name))
            .map(field => `${field.type} ${field.name}`)
            .join(', ');

        const setterCalls = fields
            .filter(field => !['aggregateId', 'version', 'state'].includes(field.name))
            .map(field => `        set${field.capitalizedName}(${field.name});`)
            .join('\n');

        return `
    public ${dtoName}(${params}) {
${setterCalls}
    }`;
    }

    private generateConstructorBody(fields: any[], aggregateName: string, aggregateVariable: string, allSharedDtos?: DtoDefinition[], dtoMappings?: any[]): string {
        const lines: string[] = [];

        // Group fields by their source
        const standardFields = fields.filter(f => ['aggregateId', 'version', 'state'].includes(f.name));
        const rootFields = fields.filter(f => !['aggregateId', 'version', 'state'].includes(f.name) && this.isRootEntityField(f.name));
        const nestedFields = fields.filter(f => !['aggregateId', 'version', 'state'].includes(f.name) && !this.isRootEntityField(f.name) && !this.isCollectionField(f.name));
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
                if (this.isCollectionFieldType(field.name)) {
                    // Handle collections from root entity - create DTOs directly
                    lines.push(this.generateCollectionMapping(field.name, field.capitalizedName, aggregateVariable, aggregateName, allSharedDtos, dtoMappings));
                } else {
                    // Handle regular root entity fields
                    lines.push(`        set${field.capitalizedName}(${aggregateVariable}.get${field.capitalizedName}());`);
                }
            }
            lines.push('');
        }

        // Nested entity fields (from child entities)
        if (nestedFields.length > 0) {
            const nestedEntityName = this.inferNestedEntityName(aggregateName);
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
                const entityName = this.inferEntityNameFromCollection(field.name, aggregateName);
                lines.push(`        set${field.capitalizedName}(${aggregateVariable}.get${field.capitalizedName}().stream().map(${entityName}::buildDto).collect(Collectors.toSet()));`);
            }
        }

        return lines.join('\n');
    }

    private isRootEntityField(fieldName: string): boolean {
        // Common root entity fields (including collections that belong to the root entity)
        const rootFields = ['acronym', 'academicTerm', 'endDate', 'title', 'content', 'description', 'startTime', 'endTime', 'students', 'questions', 'topics', 'participants', 'answers'];
        return rootFields.includes(fieldName);
    }

    private isCollectionField(fieldName: string): boolean {
        // Collection fields that are NOT part of the root entity (rare cases)
        // Most collections belong to the root entity, so this should be empty or very specific
        const separateCollectionFields: string[] = [];
        return separateCollectionFields.includes(fieldName);
    }

    private isCollectionFieldType(fieldName: string): boolean {
        // Check if a field is a collection type (Set, List, etc.)
        const collectionFieldNames = ['students', 'questions', 'topics', 'participants', 'answers'];
        return collectionFieldNames.includes(fieldName);
    }

    private inferNestedEntityName(aggregateName: string): string {
        // Infer nested entity name based on aggregate name
        const nestedEntityMap: { [key: string]: string } = {
            'Execution': 'ExecutionCourse',
            'Course': 'CourseDetails',
            'Quiz': 'QuizDetails',
            'Tournament': 'TournamentDetails',
            'Question': 'QuestionDetails',
            'Topic': 'TopicDetails',
            'Answer': 'AnswerDetails'
        };
        return nestedEntityMap[aggregateName] || `${aggregateName}Details`;
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

    private inferEntityNameFromCollection(fieldName: string, aggregateName: string): string {
        // Infer entity name from collection field name
        const entityMap: { [key: string]: string } = {
            'students': `${aggregateName}Student`,
            'questions': `${aggregateName}Question`,
            'topics': `${aggregateName}Topic`,
            'participants': `${aggregateName}Participant`,
            'answers': `${aggregateName}Answer`
        };

        return entityMap[fieldName] || `${aggregateName}${this.capitalize(fieldName.slice(0, -1))}`;
    }

    private generateCollectionMapping(fieldName: string, capitalizedFieldName: string, aggregateVariable: string, aggregateName: string, allSharedDtos?: DtoDefinition[], dtoMappings?: any[]): string {
        // Generate direct DTO creation for collections based on DSL definitions
        const targetDtoName = this.inferTargetDtoFromCollection(fieldName);
        const entityName = this.inferEntityNameFromCollection(fieldName, aggregateName);

        if (targetDtoName) {
            const dtoConstructorCall = this.generateDtoConstructorCall(targetDtoName, entityName.toLowerCase(), aggregateName, allSharedDtos, dtoMappings);
            return `        set${capitalizedFieldName}(${aggregateVariable}.get${capitalizedFieldName}().stream()
            .map(${entityName.toLowerCase()} -> ${dtoConstructorCall})
            .collect(Collectors.toSet()));`;
        }

        // Fallback to buildDto if no DTO mapping found
        return `        set${capitalizedFieldName}(${aggregateVariable}.get${capitalizedFieldName}().stream().map(${entityName}::buildDto).collect(Collectors.toSet()));`;
    }

    private inferTargetDtoFromCollection(fieldName: string): string | null {
        // Map collection field names to their target DTO types
        const collectionToDtoMap: { [key: string]: string } = {
            'students': 'UserDto',
            'courses': 'CourseDto',
            'questions': 'QuestionDto',
            'topics': 'TopicDto',
            'quizzes': 'QuizDto',
            'tournaments': 'TournamentDto',
            'answers': 'AnswerDto'
        };

        return collectionToDtoMap[fieldName] || null;
    }

    private generateDtoConstructorCall(targetDtoName: string, entityVariable: string, aggregateName: string, allSharedDtos?: DtoDefinition[], dtoMappings?: any[]): string {
        // Find the DTO definition from the shared DTOs
        const dtoDefinition = this.findDtoDefinition(targetDtoName, allSharedDtos);

        if (!dtoDefinition) {
            // Fallback to hardcoded mapping if DTO not found
            return this.generateHardcodedDtoConstructor(targetDtoName, entityVariable, aggregateName);
        }

        // Generate constructor parameters based on DTO fields
        const constructorParams = dtoDefinition.fields.map((field: any) => {
            return this.mapEntityFieldToDtoField(field.name, entityVariable, aggregateName, dtoMappings);
        }).join(', ');

        return `new ${targetDtoName}(${constructorParams})`;
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

        // Fallback to hardcoded mappings for backward compatibility
        const fieldMappings: { [key: string]: { [key: string]: string } } = {
            'UserDto': {
                'id': `${entityVariable}.getStudentAggregateId()`,
                'name': `${entityVariable}.getStudentName()`,
                'username': `${entityVariable}.getStudentUsername()`,
                'email': `${entityVariable}.getStudentEmail()`
            }
        };

        // Get the target DTO name from the context
        const targetDtoName = this.inferTargetDtoFromCollection(`${entityVariable}s`);

        if (targetDtoName && fieldMappings[targetDtoName] && fieldMappings[targetDtoName][dtoFieldName]) {
            return fieldMappings[targetDtoName][dtoFieldName];
        }

        // Final fallback: try to infer getter method name
        const capitalizedField = this.capitalize(dtoFieldName);
        return `${entityVariable}.get${capitalizedField}()`;
    }

    private generateHardcodedDtoConstructor(targetDtoName: string, entityVariable: string, aggregateName: string): string {
        // Fallback hardcoded constructors for known DTOs
        if (targetDtoName === 'UserDto') {
            return `new UserDto(${entityVariable}.getStudentAggregateId(), ${entityVariable}.getStudentName(), ${entityVariable}.getStudentUsername(), ${entityVariable}.getStudentEmail())`;
        }

        // Generic fallback
        return `${entityVariable}.buildDto()`;
    }

    /**
     * Returns standard aggregate fields that should be automatically added to root entity DTOs
     */
    private getStandardAggregateFields(dtoName: string): any[] {
        // Only add aggregate fields to root entity DTOs (those ending with main entity names)
        const rootEntityDtos = ['ExecutionDto', 'CourseDto', 'QuizDto', 'TournamentDto', 'AnswerDto', 'QuestionDto', 'TopicDto'];

        if (rootEntityDtos.includes(dtoName)) {
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
                    type: 'String',
                    name: 'state',
                    capitalizedName: 'State'
                }
            ];
        }

        return [];
    }


    /**
     * Determines if a DTO should be in the shared module
     * Common DTOs like UserDto, CourseDto that are referenced across aggregates
     */
    static isSharedDto(dtoName: string): boolean {
        const sharedDtos = [
            'UserDto',
            'CourseDto',
            'CourseExecutionDto',
            'QuestionDto',
            'QuizDto',
            'TopicDto',
            'TournamentDto',
            'QuizAnswerDto'
        ];

        return sharedDtos.includes(dtoName);
    }

    /**
     * Gets the import path for a DTO (either shared or aggregate-specific)
     */
    static getDtoImportPath(dtoName: string, options: SharedDtoGenerationOptions): string {
        const config = getGlobalConfig();

        if (this.isSharedDto(dtoName)) {
            return `${config.buildPackageName(options.projectName, 'shared', 'dtos')}.${dtoName}`;
        }

        // For aggregate-specific DTOs, determine the aggregate from the DTO name
        const aggregateName = this.getAggregateFromDtoName(dtoName);
        return `${config.buildPackageName(options.projectName, 'microservices', aggregateName.toLowerCase(), 'aggregate')}.${dtoName}`;
    }

    private static getAggregateFromDtoName(dtoName: string): string {
        // Remove 'Dto' suffix and return the base name
        // This is a simplified approach - in practice, you might need a mapping
        const baseName = dtoName.replace('Dto', '');

        // Map common patterns
        const aggregateMap: { [key: string]: string } = {
            'CourseExecution': 'CourseExecution',
            'User': 'User',
            'Course': 'Course',
            'Question': 'Question',
            'Quiz': 'Quiz',
            'Topic': 'Topic',
            'Tournament': 'Tournament',
            'QuizAnswer': 'QuizAnswer'
        };

        return aggregateMap[baseName] || baseName;
    }
}
