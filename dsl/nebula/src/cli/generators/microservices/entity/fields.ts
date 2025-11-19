import { Entity } from "../../../../language/generated/ast.js";
import { TypeResolver } from "../../common/resolvers/type-resolver.js";
import { getGlobalConfig } from "../../common/config.js";
import { ImportRequirements } from "./types.js";

const resolveJavaType = (type: any, fieldName?: string) => {
    return TypeResolver.resolveJavaType(type);
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

const isEnumTypeByNaming = (javaType: string) => {
    return false;
};

export function generateFields(properties: any[], entity: Entity, isRootEntity: boolean, projectName: string): { code: string, imports: ImportRequirements } {
    const imports: ImportRequirements = {
        usesLocalDateTime: false,
        usesBigDecimal: false,
        usesSet: false,
        usesList: false,
        usesOneToOne: false,
        usesOneToMany: false,
        usesCascadeType: false,
        usesFetchType: false,
        customImports: new Set<string>()
    };

    const processedProperties = [...properties];
    if (!isRootEntity) {
        const hasIdField = processedProperties.some(prop => prop?.name === 'id');
        if (!hasIdField) {
            processedProperties.unshift({
                name: 'id',
                type: { $type: 'PrimitiveType', name: 'Long' }
            });
        }
    }

    let fields = processedProperties.map((prop: any, index: number) => {
        const javaType = resolveJavaType(prop.type, prop.name);
        if (javaType.includes('LocalDateTime')) imports.usesLocalDateTime = true;
        if (javaType.includes('BigDecimal')) imports.usesBigDecimal = true;
        if (javaType.startsWith('Set<')) imports.usesSet = true;
        if (javaType.startsWith('List<')) imports.usesList = true;

        // Check if this is an enum type
        if (isEnumType(prop.type) || isEnumTypeByNaming(javaType)) {
            const enumImport = `import ${getGlobalConfig().buildPackageName(projectName, 'shared', 'enums')}.${javaType};`;
            imports.customImports!.add(enumImport);
            imports.usesEnumerated = true;
        }

        // Check if this is AggregateState type
        if (javaType === 'AggregateState') {
            imports.usesAggregateState = true;
        }

        // Add @Id annotation only for non-root entities (root entities inherit ID from Aggregate)
        let idAnnotation = '';
        if (index === 0 && !isRootEntity) {
            idAnnotation = '    @Id\n    @GeneratedValue\n';
            imports.usesGeneratedValue = true;
        }

        // Determine if this is an entity relationship
        const isEntityType = TypeResolver.isEntityType(javaType) && !isEnumType(prop.type);
        const isCollection = javaType.startsWith('Set<') || javaType.startsWith('List<');
        const elementType = TypeResolver.getElementType(prop.type);
        const isEntityCollection = isCollection && elementType && TypeResolver.isEntityType(elementType);
        const isPrimitiveCollection = isCollection && elementType && TypeResolver.isPrimitiveType(elementType);

        let relationshipAnnotation = '';
        let initialization = '';

        if (isEntityType && !isCollection && !isEnumType(prop.type)) {
            // Single entity relationship - @OneToOne
            relationshipAnnotation = `    @OneToOne(cascade = CascadeType.ALL, mappedBy = "${entity.name.toLowerCase()}")\n`;
            imports.usesOneToOne = true;
            imports.usesCascadeType = true;
        } else if (isEntityCollection) {
            // Collection of entities - @OneToMany
            relationshipAnnotation = `    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "${entity.name.toLowerCase()}")\n`;
            if (javaType.startsWith('List<')) {
                initialization = ' = new ArrayList<>()';
            } else if (javaType.startsWith('Set<')) {
                initialization = ' = new HashSet<>()';
            }
            imports.usesOneToMany = true;
            imports.usesCascadeType = true;
            imports.usesFetchType = true;
        } else if (isPrimitiveCollection) {
            // Collection of primitives - no JPA annotations needed
            if (javaType.startsWith('List<')) {
                initialization = ' = new ArrayList<>()';
            } else if (javaType.startsWith('Set<')) {
                initialization = ' = new HashSet<>()';
            }
        }

        // Add @Enumerated annotation for enum types
        let enumAnnotation = '';
        if (isEnumType(prop.type) || isEnumTypeByNaming(javaType)) {
            enumAnnotation = `    @Enumerated(EnumType.STRING)\n`;
        }

        // Check if field is final
        const finalModifier = (prop as any).isFinal ? 'final ' : '';

        return `${idAnnotation}${relationshipAnnotation}${enumAnnotation}    private ${finalModifier}${javaType} ${prop.name}${initialization};`;
    }).join('\n');

    // Add back-reference field for non-root entities
    if (!isRootEntity && entity.$container) {
        const parentEntityName = entity.$container.name;
        const backRefField = `    @OneToOne\n    private ${parentEntityName} ${parentEntityName.toLowerCase()};`;
        fields = fields + '\n' + backRefField;
        imports.usesOneToOne = true;
    }

    return { code: fields, imports };
}
