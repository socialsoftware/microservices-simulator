import { Entity } from "../../../../language/generated/ast.js";
import { UnifiedTypeResolver as TypeResolver } from "../../common/unified-type-resolver.js";
import { getGlobalConfig } from "../../common/config.js";
import { ImportRequirements } from "./types.js";

const SQL_RESERVED_WORDS = new Set<string>([
    'all', 'analyse', 'analyze', 'and', 'any', 'array', 'as', 'asc', 'asymmetric',
    'authorization', 'between', 'both', 'case', 'cast', 'check', 'collate', 'column',
    'constraint', 'create', 'cross', 'current_catalog', 'current_date', 'current_role',
    'current_schema', 'current_time', 'current_timestamp', 'current_user', 'default',
    'deferrable', 'desc', 'distinct', 'do', 'else', 'end', 'except', 'false', 'fetch',
    'for', 'foreign', 'from', 'full', 'grant', 'group', 'having', 'in', 'initially',
    'inner', 'intersect', 'into', 'is', 'isnull', 'join', 'key', 'lateral', 'leading',
    'left', 'like', 'limit', 'localtime', 'localtimestamp', 'natural', 'not', 'notnull',
    'null', 'offset', 'on', 'only', 'or', 'order', 'outer', 'overlaps', 'placing',
    'primary', 'range', 'references', 'returning', 'right', 'rows', 'select', 'session_user',
    'set', 'similar', 'some', 'symmetric', 'system_user', 'table', 'then', 'to',
    'trailing', 'true', 'union', 'unique', 'user', 'using', 'value', 'values', 'verbose',
    'when', 'where', 'window', 'with', 'year'
]);

function camelToSnake(name: string): string {
    return name.replace(/([a-z0-9])([A-Z])/g, '$1_$2').toLowerCase();
}

function isReservedColumnName(fieldName: string): boolean {
    const lower = fieldName.toLowerCase();
    if (SQL_RESERVED_WORDS.has(lower)) return true;
    const snake = camelToSnake(fieldName);
    return SQL_RESERVED_WORDS.has(snake);
}

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


        if (isEnumType(prop.type) || isEnumTypeByNaming(javaType)) {
            const enumImport = `import ${getGlobalConfig().buildPackageName(projectName, 'shared', 'enums')}.${javaType};`;
            imports.customImports!.add(enumImport);
            imports.usesEnumerated = true;
        }


        if (javaType === 'AggregateState') {
            imports.usesAggregateState = true;
        }


        let idAnnotation = '';
        if (index === 0 && !isRootEntity) {
            idAnnotation = '    @Id\n    @GeneratedValue\n';
            imports.usesGeneratedValue = true;
        }


        const isEntityType = TypeResolver.isEntityType(javaType) && !isEnumType(prop.type);
        const isCollection = javaType.startsWith('Set<') || javaType.startsWith('List<');
        const elementType = TypeResolver.getElementType(prop.type);
        const isEntityCollection = isCollection && elementType && TypeResolver.isEntityType(elementType);
        const isPrimitiveCollection = isCollection && elementType && TypeResolver.isPrimitiveType(elementType);

        let relationshipAnnotation = '';
        let initialization = '';

        if (isEntityType && !isCollection && !isEnumType(prop.type)) {

            if (isRootEntity) {
                relationshipAnnotation = `    @OneToOne(cascade = CascadeType.ALL, mappedBy = "${entity.name.charAt(0).toLowerCase() + entity.name.slice(1)}")\n`;
            } else {
                relationshipAnnotation = `    @OneToOne(cascade = CascadeType.ALL)\n`;
            }
            imports.usesOneToOne = true;
            imports.usesCascadeType = true;
        } else if (isEntityCollection) {

            relationshipAnnotation = isRootEntity
                ? `    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "${entity.name.charAt(0).toLowerCase() + entity.name.slice(1)}")\n`
                : `    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)\n`;
            if (javaType.startsWith('List<')) {
                initialization = ' = new ArrayList<>()';
            } else if (javaType.startsWith('Set<')) {
                initialization = ' = new HashSet<>()';
            }
            imports.usesOneToMany = true;
            imports.usesCascadeType = true;
            imports.usesFetchType = true;
        } else if (isPrimitiveCollection) {

            if (javaType.startsWith('List<')) {
                initialization = ' = new ArrayList<>()';
            } else if (javaType.startsWith('Set<')) {
                initialization = ' = new HashSet<>()';
            }
        }


        let enumAnnotation = '';
        if (isEnumType(prop.type) || isEnumTypeByNaming(javaType)) {
            enumAnnotation = `    @Enumerated(EnumType.STRING)\n`;
        }

        const finalModifier = (prop as any).isFinal ? 'final ' : '';
        const fieldInitialization = initialization;

        let columnAnnotation = '';
        if (!relationshipAnnotation && !isCollection && isReservedColumnName(prop.name)) {
            columnAnnotation = `    @Column(name = "\\"${camelToSnake(prop.name)}\\"")\n`;
        }

        return `${idAnnotation}${relationshipAnnotation}${enumAnnotation}${columnAnnotation}    private ${finalModifier}${javaType} ${prop.name}${fieldInitialization};`;
    }).join('\n');


    if (!isRootEntity && entity.$container) {
        const parentEntityName = entity.$container.name;
        const backRefField = `    @OneToOne\n    private ${parentEntityName} ${parentEntityName.toLowerCase()};`;
        fields = fields + '\n' + backRefField;
        imports.usesOneToOne = true;
    }

    return { code: fields, imports };
}
