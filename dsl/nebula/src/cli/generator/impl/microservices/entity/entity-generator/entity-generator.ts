import { Entity } from "../../../../../../language/generated/ast.js";
import { TypeResolver } from "../../../../base/type-resolver.js";
import { getGlobalConfig } from "../../../../base/config.js";
import { capitalize } from "../../../../../utils/generator-utils.js";
import { EntityGenerationOptions } from "./types.js";
import { scanCodeForImports } from "./import-detector.js";

// ============================================================================
// UTILITY FUNCTIONS
// ============================================================================

const resolveJavaType = (type: any, fieldName?: string) => {
    return TypeResolver.resolveJavaType(type);
};

const toCamelCase = (str: string) => {
    return str.charAt(0).toLowerCase() + str.slice(1);
};

const isEnumType = (type: any) => {
    if (type && typeof type === 'object' &&
        type.$type === 'EntityType' &&
        type.type) {
        if (type.type.$refText && type.type.$refText.match(/^[A-Z][a-zA-Z]*Type$/)) {
            return true;
        }
        if (type.type.ref && type.type.ref.$type === 'EnumDefinition') {
            return true;
        }
    }
    return false;
};

// ============================================================================
// MAIN ENTITY GENERATION
// ============================================================================

export function generateEntityCode(entity: Entity, projectName: string, options?: EntityGenerationOptions): string {
    const opts = options || { projectName };
    const isRootEntity = entity.isRoot || false;

    // Generate all components
    const fields = generateFields(entity.properties, entity, isRootEntity, projectName);
    const defaultConstructor = generateDefaultConstructor(entity.name);
    const dtoConstructor = generateEntityDtoConstructor(entity, projectName, opts.allSharedDtos, opts.dtoMappings);
    const copyConstructor = generateCopyConstructor(entity);
    const gettersSetters = generateGettersSetters(entity.properties, entity, projectName, opts.allEntities);
    const backRefGetterSetter = (!isRootEntity && entity.$container) ? generateBackReferenceGetterSetter(entity.$container.name) : '';
    const invariants = isRootEntity ? generateInvariants(entity) : '';
    const constructors = generateConstructors(entity);
    const methods = generateMethods(entity);

    // Build the complete Java class
    const aggregateName = entity.$container?.name || 'unknown';
    const extendsClause = isRootEntity
        ? (aggregateName !== entity.name ? ` extends ${aggregateName}` : ' extends Aggregate')
        : '';
    const abstractModifier = isRootEntity ? 'abstract ' : '';
    const config = getGlobalConfig();
    const packageName = config.buildPackageName(
        projectName,
        'microservices',
        aggregateName.toLowerCase(),
        'aggregate'
    );

    // Generate the complete Java code first
    const javaCode = `package ${packageName};

IMPORTS_PLACEHOLDER

@Entity
public ${abstractModifier}class ${entity.name}${extendsClause} {
${fields}
${defaultConstructor}
${dtoConstructor}
${copyConstructor}
${constructors}
${gettersSetters}
${backRefGetterSetter}
${methods}
${invariants}
}`;

    // Scan the generated code for imports and replace placeholder
    const detectedImports = scanCodeForImports(javaCode, projectName, isRootEntity, aggregateName, entity.name);
    const importsString = detectedImports.join('\n');

    return javaCode.replace('IMPORTS_PLACEHOLDER', importsString);
}

// ============================================================================
// FIELD GENERATION
// ============================================================================

function generateFields(properties: any[], entity: Entity, isRootEntity: boolean, projectName: string): string {
    let fields = properties.map((prop: any, index: number) => {
        const javaType = resolveJavaType(prop.type, prop.name);

        // Add @Id annotation only for non-root entities
        let idAnnotation = '';
        if (index === 0 && !isRootEntity) {
            idAnnotation = '    @Id\n    @GeneratedValue\n';
        }

        // Determine relationship annotations
        const isEntityType = TypeResolver.isEntityType(javaType) && !isEnumType(prop.type);
        const isCollection = javaType.startsWith('Set<') || javaType.startsWith('List<');
        const elementType = TypeResolver.getElementType(prop.type);
        const isEntityCollection = isCollection && elementType && TypeResolver.isEntityType(elementType);

        let relationshipAnnotation = '';
        let initialization = '';

        if (isEntityType && !isCollection && !isEnumType(prop.type)) {
            relationshipAnnotation = `    @OneToOne(cascade = CascadeType.ALL, mappedBy = "${entity.name.toLowerCase()}")\n`;
        } else if (isEntityCollection) {
            relationshipAnnotation = `    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "${entity.name.toLowerCase()}")\n`;
            initialization = javaType.startsWith('List<') ? ' = new ArrayList<>()' : ' = new HashSet<>()';
        } else if (isCollection) {
            initialization = javaType.startsWith('List<') ? ' = new ArrayList<>()' : ' = new HashSet<>()';
        }

        // Add @Enumerated annotation for enum types
        let enumAnnotation = '';
        if (isEnumType(prop.type)) {
            enumAnnotation = `    @Enumerated(EnumType.STRING)\n`;
        }

        return `${idAnnotation}${relationshipAnnotation}${enumAnnotation}    private ${javaType} ${prop.name}${initialization};`;
    }).join('\n');

    // Add back-reference field for non-root entities
    if (!isRootEntity && entity.$container) {
        const parentEntityName = entity.$container.name;
        const backRefField = `    @OneToOne\n    private ${parentEntityName} ${parentEntityName.toLowerCase()};`;
        fields = fields + '\n' + backRefField;
    }

    return fields;
}

// ============================================================================
// CONSTRUCTOR GENERATION
// ============================================================================

function generateDefaultConstructor(name: string): string {
    return `\n    public ${name}() {\n    }`;
}

function generateEntityDtoConstructor(entity: Entity, projectName: string, allSharedDtos?: any[], dtoMappings?: any[]): string {
    const entityName = entity.name;
    const isRootEntity = entity.isRoot || false;

    // Check if entity specifies a custom DTO type
    const entityAny = entity as any;
    const customDtoType = entityAny.dtoType?.ref?.name || entityAny.dtoType?.$refText;

    // Determine DTO name
    const dtoTypeName = customDtoType || `${isRootEntity ? entityName : (entity.$container?.name || entityName)}Dto`;
    const dtoParamName = customDtoType ?
        customDtoType.charAt(0).toLowerCase() + customDtoType.slice(1) :
        `${(isRootEntity ? entityName : (entity.$container?.name || entityName)).toLowerCase()}Dto`;

    // Find entity relationships for constructor parameters
    const entityRelationships = entity.properties.filter((prop: any) => {
        const javaType = resolveJavaType(prop.type);
        return TypeResolver.isEntityType(javaType) && !javaType.startsWith('Set<') && !javaType.startsWith('List<');
    });

    const relationshipParams = entityRelationships.map((prop: any) =>
        `${resolveJavaType(prop.type)} ${prop.name}`
    ).join(', ');

    const params = isRootEntity ?
        (relationshipParams ?
            `Integer aggregateId, ${dtoTypeName} ${dtoParamName}, ${relationshipParams}` :
            `Integer aggregateId, ${dtoTypeName} ${dtoParamName}`) :
        `${dtoTypeName} ${dtoParamName}`;

    // Generate setter calls
    const setterCalls = entity.properties.map((prop: any, index: number) => {
        if (!isRootEntity && index === 0) return '';

        const javaType = resolveJavaType(prop.type);
        const capitalizedName = prop.name.charAt(0).toUpperCase() + prop.name.slice(1);
        let dtoGetterName = capitalizedName;

        // Handle custom DTO mapping
        if (customDtoType) {
            const mappedName = mapEntityFieldToDtoField(prop.name, customDtoType, dtoMappings, entity);
            if (mappedName === null) return '';
            dtoGetterName = mappedName;
        }

        if (javaType === 'LocalDateTime') {
            return `        set${capitalizedName}(${dtoParamName}.get${dtoGetterName}());`;
        } else if (isEnumType(prop.type)) {
            return `        set${capitalizedName}(${javaType}.valueOf(${dtoParamName}.get${dtoGetterName}()));`;
        } else if (!javaType.startsWith('Set<') && !javaType.startsWith('List<')) {
            return `        set${capitalizedName}(${dtoParamName}.get${dtoGetterName}());`;
        }
        return '';
    }).filter(call => call !== '').join('\n');

    const constructorBody = isRootEntity ?
        `        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
${setterCalls}` :
        setterCalls;

    return `\n    public ${entityName}(${params}) {
${constructorBody}
    }`;
}

function generateCopyConstructor(entity: Entity): string {
    const entityName = entity.name;
    const isRootEntity = entity.isRoot || false;

    const setterCalls = entity.properties.map((prop: any, index: number) => {
        if (!isRootEntity && index === 0) return '';

        const javaType = resolveJavaType(prop.type);
        const capitalizedName = prop.name.charAt(0).toUpperCase() + prop.name.slice(1);

        if (TypeResolver.isEntityType(javaType) && !javaType.startsWith('Set<') && !javaType.startsWith('List<')) {
            return `        set${capitalizedName}(new ${javaType}(other.get${capitalizedName}()));`;
        } else if (javaType.startsWith('Set<')) {
            const elementType = TypeResolver.getElementType(prop.type);
            if (elementType && TypeResolver.isEntityType(elementType)) {
                return `        set${capitalizedName}(other.get${capitalizedName}().stream().map(${elementType}::new).collect(Collectors.toSet()));`;
            } else {
                return `        set${capitalizedName}(new HashSet<>(other.get${capitalizedName}()));`;
            }
        } else if (javaType.startsWith('List<')) {
            const elementType = TypeResolver.getElementType(prop.type);
            if (elementType && TypeResolver.isEntityType(elementType)) {
                return `        set${capitalizedName}(other.get${capitalizedName}().stream().map(${elementType}::new).collect(Collectors.toList()));`;
            } else {
                return `        set${capitalizedName}(new ArrayList<>(other.get${capitalizedName}()));`;
            }
        } else {
            return `        set${capitalizedName}(other.get${capitalizedName}());`;
        }
    }).filter(call => call !== '').join('\n');

    const constructorBody = isRootEntity ?
        `        super(other);
${setterCalls}` :
        setterCalls;

    return `\n    public ${entityName}(${entityName} other) {
${constructorBody}
    }`;
}

// ============================================================================
// METHOD GENERATION
// ============================================================================

function generateGettersSetters(properties: any[], entity?: Entity, projectName?: string, allEntities?: Entity[]): string {
    const entityName = entity?.name || 'Unknown';

    const methods = properties.map((prop: any) => {
        const javaType = resolveJavaType(prop.type, prop.name);
        const capName = capitalize(prop.name);

        const getterMethod = `\n    public ${javaType} get${capName}() {\n        return ${prop.name};\n    }`;
        const setterMethod = generateBidirectionalSetter(prop, javaType, capName, entityName);
        const collectionMethods = generateCollectionMethods(prop, javaType, capName, entityName, allEntities);

        return `${getterMethod}\n\n${setterMethod}${collectionMethods}`;
    }).join('\n');

    return methods;
}

function generateBidirectionalSetter(prop: any, javaType: string, capName: string, entityName: string): string {
    const propName = prop.name;
    const isEntityType = TypeResolver.isEntityType(javaType);
    const isCollection = javaType.startsWith('Set<') || javaType.startsWith('List<');

    if (isEntityType && !isCollection) {
        const backRefMethod = `set${entityName}`;
        return `    public void set${capName}(${javaType} ${propName}) {
        this.${propName} = ${propName};
        if (this.${propName} != null) {
            this.${propName}.${backRefMethod}(this);
        }
    }`;
    } else if (isCollection && TypeResolver.isEntityType(TypeResolver.getElementType(prop.type) || '')) {
        const backRefMethod = `set${entityName}`;
        return `    public void set${capName}(${javaType} ${propName}) {
        this.${propName} = ${propName};
        if (this.${propName} != null) {
            this.${propName}.forEach(item -> item.${backRefMethod}(this));
        }
    }`;
    } else {
        return `    public void set${capName}(${javaType} ${propName}) {
        this.${propName} = ${propName};
    }`;
    }
}

function generateCollectionMethods(prop: any, javaType: string, capName: string, entityName: string, allEntities?: Entity[]): string {
    if (!javaType.startsWith('Set<') && !javaType.startsWith('List<')) {
        return '';
    }

    const elementType = TypeResolver.getElementType(prop.type);
    if (!elementType || !TypeResolver.isEntityType(elementType)) {
        return '';
    }

    const propName = prop.name;
    const elementTypeCamelCase = toCamelCase(elementType);
    const backRefMethod = `set${entityName}`;
    const collectionImpl = javaType.startsWith('List<') ? 'ArrayList' : 'HashSet';

    // Detect primary key
    const { pkType, pkGetter } = detectPrimaryKey(elementType, allEntities);

    return `

    public void add${elementType}(${elementType} ${elementTypeCamelCase}) {
        if (this.${propName} == null) {
            this.${propName} = new ${collectionImpl}<>();
        }
        this.${propName}.add(${elementTypeCamelCase});
        if (${elementTypeCamelCase} != null) {
            ${elementTypeCamelCase}.${backRefMethod}(this);
        }
    }

    public void remove${elementType}(${pkType} id) {
        if (this.${propName} != null) {
            this.${propName}.removeIf(item -> 
                item.${pkGetter}() != null && item.${pkGetter}().equals(id));
        }
    }

    public boolean contains${elementType}(${pkType} id) {
        if (this.${propName} == null) {
            return false;
        }
        return this.${propName}.stream().anyMatch(item -> 
            item.${pkGetter}() != null && item.${pkGetter}().equals(id));
    }

    public ${elementType} find${elementType}ById(${pkType} id) {
        if (this.${propName} == null) {
            return null;
        }
        return this.${propName}.stream()
            .filter(item -> item.${pkGetter}() != null && item.${pkGetter}().equals(id))
            .findFirst()
            .orElse(null);
    }`;
}

function generateBackReferenceGetterSetter(parentEntityName: string): string {
    const fieldName = parentEntityName.toLowerCase();
    const capName = capitalize(parentEntityName);

    return `
    public ${parentEntityName} get${capName}() {
        return ${fieldName};
    }

    public void set${capName}(${parentEntityName} ${fieldName}) {
        this.${fieldName} = ${fieldName};
    }`;
}

// ============================================================================
// INVARIANT GENERATION
// ============================================================================

function generateInvariants(entity: Entity): string {
    if (!entity.invariants || entity.invariants.length === 0) {
        return '';
    }

    // Generate individual invariant methods
    const invariantMethods = entity.invariants.map((invariant: any, index: number) => {
        const methodName = `invariant${capitalize(invariant.name)}`;
        const condition = getInvariantConditionText(invariant);

        // Only add divider comment before the first invariant
        const dividerComment = index === 0 ? '\n    // ============================================================================\n    // INVARIANTS\n    // ============================================================================\n' : '';

        return `${dividerComment}
    public boolean ${methodName}() {
        return ${condition};
    }`;
    }).join('\n');

    // Generate verifyInvariants method
    const invariantCalls = entity.invariants.map((invariant: any) =>
        `invariant${capitalize(invariant.name)}()`
    ).join('\n               && ');

    const verifyMethod = `
    @Override
    public void verifyInvariants() {
        if (!(${invariantCalls})) {
            throw new SimulatorException(INVARIANT_BREAK, getAggregateId());
        }
    }`;

    return invariantMethods + verifyMethod;
}

function getInvariantConditionText(invariant: any): string {
    if (invariant.conditions && invariant.conditions.length > 0) {
        const firstCondition = invariant.conditions[0];
        const sourceText = firstCondition.expression?.$cstNode?.text;
        if (sourceText) {
            return convertDslToJava(sourceText.trim());
        }
    }
    return 'true';
}

function convertDslToJava(dslText: string): string {
    let javaCode = dslText;

    // Handle specific DSL patterns
    if (javaCode.includes('.isBefore(') || javaCode.includes('.isAfter(')) {
        javaCode = javaCode.replace(/\b(\w+)\.isBefore\((\w+)\)/g, 'this.$1.isBefore(this.$2)');
        javaCode = javaCode.replace(/\b(\w+)\.isAfter\((\w+)\)/g, 'this.$1.isAfter(this.$2)');
    } else if (javaCode.includes('.unique(')) {
        javaCode = javaCode.replace(/(\w+)\.unique\((\w+)\)/g,
            'this.$1.stream().map(item -> item.get${capitalize($2)}()).distinct().count() == this.$1.size()');
    } else if (javaCode.includes('.length()')) {
        javaCode = javaCode.replace(/\b(\w+)\.length\(\)/g, 'this.$1.length()');
    } else if (javaCode.includes('!=') || javaCode.includes('==')) {
        javaCode = javaCode.replace(/\b(\w+)\s*(!=|==)\s*(\w+)/g, 'this.$1 $2 $3');
    } else if (javaCode.includes('.isEmpty()')) {
        javaCode = javaCode.replace(/\b(\w+)\.isEmpty\(\)/g, 'this.$1.isEmpty()');
    } else {
        javaCode = javaCode.replace(/\b(startTime|endTime|numberOfQuestions|cancelled|tournamentParticipants|tournamentCreator)\b/g, 'this.$1');
    }

    return javaCode;
}

// ============================================================================
// HELPER FUNCTIONS
// ============================================================================

function detectPrimaryKey(elementType: string, allEntities?: Entity[]): { pkType: string, pkGetter: string } {
    if (allEntities) {
        const targetEntity = allEntities.find(e => e.name === elementType);
        if (targetEntity && targetEntity.properties.length > 0) {
            const firstProperty = targetEntity.properties[0];
            const pkType = resolveJavaType(firstProperty.type);
            const pkGetter = `get${capitalize(firstProperty.name)}`;
            return { pkType, pkGetter };
        }
    }

    console.warn(`Warning: Could not determine primary key for entity '${elementType}'. Using default Long id.`);
    return { pkType: 'Long', pkGetter: 'getId' };
}

function mapEntityFieldToDtoField(entityFieldName: string, dtoType: string, dtoMappings?: any[], entity?: Entity): string | null {
    const entityAny = entity as any;
    if (entityAny?.dtoMapping?.fieldMappings) {
        for (const fieldMapping of entityAny.dtoMapping.fieldMappings) {
            if (fieldMapping.entityField === entityFieldName) {
                return capitalize(fieldMapping.dtoField);
            }
        }
        return null;
    }

    return entityFieldName.charAt(0).toUpperCase() + entityFieldName.slice(1);
}

// Placeholder functions for custom constructors and methods
function generateConstructors(entity: Entity): string {
    return '';
}

function generateMethods(entity: Entity): string {
    return '';
}

// ============================================================================
// MAIN ENTITY GENERATOR CLASS
// ============================================================================

export class EntityGenerator {
    async generateEntity(entity: Entity, options: EntityGenerationOptions): Promise<string> {
        return generateEntityCode(entity, options.projectName, options);
    }
}