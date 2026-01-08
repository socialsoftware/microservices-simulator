import { Entity, Property } from "../../../../language/generated/ast.js";
import { capitalize } from "../../../utils/generator-utils.js";
import { TypeResolver } from "../../common/resolvers/type-resolver.js";
import { getGlobalConfig } from "../../common/config.js";
import type { DtoSchemaRegistry, DtoFieldSchema } from "../../../services/dto-schema-service.js";

export function generateDtoCode(entity: Entity, projectName: string, dtoSchemaRegistry?: DtoSchemaRegistry): string {
    const aggregateName = entity.$container?.name || entity.name;
    const packageName = `${getGlobalConfig().buildPackageName(projectName, 'shared', 'dtos')}`;
    const dtoFields = resolveDtoFields(entity, dtoSchemaRegistry);

    const imports = generateDtoImports(entity, projectName, aggregateName, dtoFields);
    const fields = generateDtoFieldDeclarations(dtoFields);
    const constructors = generateDtoConstructors(entity, dtoFields);
    const gettersSetters = generateDtoGettersSetters(dtoFields);
    const embeddableAnnotation = entity.isRoot ? '' : '@Embeddable\n';

    return `package ${packageName};

${imports}

${embeddableAnnotation}public class ${entity.name}Dto implements Serializable {
${fields}

${constructors}

${gettersSetters}
}`;
}

function resolveDtoFields(entity: Entity, dtoSchemaRegistry?: DtoSchemaRegistry): DtoFieldSchema[] {
    const schema = dtoSchemaRegistry?.entityToDto?.[entity.name];
    if (schema) {
        return schema.fields;
    }

    const fields: DtoFieldSchema[] = [];

    if (entity.isRoot) {
        fields.push(
            {
                name: 'aggregateId',
                javaType: 'Integer',
                isCollection: false,
                isAggregateField: true,
                requiresConversion: false
            },
            {
                name: 'version',
                javaType: 'Integer',
                isCollection: false,
                isAggregateField: true,
                requiresConversion: false
            },
            {
                name: 'state',
                javaType: 'AggregateState',
                isCollection: false,
                isAggregateField: true,
                requiresConversion: false
            }
        );
    }

    for (const property of entity.properties || []) {
        if ((property as any).dtoExclude) {
            continue;
        }

        const javaType = TypeResolver.resolveJavaType(property.type);
        fields.push({
            name: property.name,
            javaType,
            isCollection: javaType.startsWith('List<') || javaType.startsWith('Set<'),
            sourceName: property.name,
            sourceProperty: property,
            requiresConversion: false
        });
    }

    return fields;
}

function generateDtoImports(entity: Entity, projectName: string, aggregateName: string, fields: DtoFieldSchema[]): string {
    const imports = new Set<string>();
    imports.add('import java.io.Serializable;');

    const needsLocalDateTime = fields.some(field => field.javaType.includes('LocalDateTime'));
    if (needsLocalDateTime) {
        imports.add('import java.time.LocalDateTime;');
    }

    const needsBigDecimal = fields.some(field => field.javaType.includes('BigDecimal'));
    if (needsBigDecimal) {
        imports.add('import java.math.BigDecimal;');
    }

    const needsList = fields.some(field => field.javaType.startsWith('List<'));
    if (needsList) {
        imports.add('import java.util.List;');
    }

    const needsSet = fields.some(field => field.javaType.startsWith('Set<'));
    if (needsSet) {
        imports.add('import java.util.Set;');
    }

    const needsCollectors = fields.some(field =>
        (field.requiresConversion && field.isCollection) ||
        (field.isEnum && field.isCollection) ||
        (field.derivedAggregateId && field.isCollection)
    );
    if (needsCollectors) {
        imports.add('import java.util.stream.Collectors;');
    }

    const needsAggregateState = fields.some(field => field.name === 'state' && field.javaType === 'AggregateState');
    if (needsAggregateState) {
        imports.add('import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;');
    }

    if (!entity.isRoot) {
        imports.add('import jakarta.persistence.Embeddable;');
    }

    const config = getGlobalConfig();
    const entityPackage = config.buildPackageName(projectName, 'microservices', aggregateName.toLowerCase(), 'aggregate');
    imports.add(`import ${entityPackage}.${entity.name};`);

    const currentDtoPackage = config.buildPackageName(projectName, 'shared', 'dtos');

    const referencedImports = new Set<string>();
    for (const field of fields) {
        if (field.referencedDtoName && field.referencedAggregateName) {
            const dtoPackage = config.buildPackageName(projectName, 'shared', 'dtos');
            if (dtoPackage !== currentDtoPackage) {
                referencedImports.add(`import ${dtoPackage}.${field.referencedDtoName};`);
            }
        }
        if (field.referencedEntityName && field.referencedAggregateName && field.requiresConversion) {
            if (field.isCollection && (field.referencedEntityIsRoot || !field.referencedEntityHasGenerateDto)) {
                const referencedPackage = config.buildPackageName(
                    projectName,
                    'microservices',
                    field.referencedAggregateName.toLowerCase(),
                    'aggregate'
                );
                referencedImports.add(`import ${referencedPackage}.${field.referencedEntityName};`);
            }
        }
    }

    referencedImports.forEach(imp => imports.add(imp));

    return Array.from(imports).join('\n');
}

function generateDtoFieldDeclarations(fields: DtoFieldSchema[]): string {
    const declarations: string[] = [];

    for (const field of fields) {
        const javaType = field.javaType === 'Boolean' ? 'Boolean' : field.javaType;
        const declarationType = javaType === 'boolean' ? 'boolean' : javaType;
        declarations.push(`    private ${declarationType} ${field.name};`);
    }

    return declarations.join('\n');
}

function generateDtoConstructors(entity: Entity, fields: DtoFieldSchema[]): string {
    const lines: string[] = [];
    const entityVar = entity.name.charAt(0).toLowerCase() + entity.name.slice(1);

    lines.push(`    public ${entity.name}Dto() {`);
    lines.push('    }');
    lines.push('');
    lines.push(`    public ${entity.name}Dto(${entity.name} ${entityVar}) {`);

    for (const field of fields) {
        const assignment = buildFieldAssignment(field, entity, entityVar);
        if (assignment) {
            lines.push(assignment);
        }
    }

    lines.push('    }');

    return lines.join('\n');
}

function buildFieldAssignment(field: DtoFieldSchema, entity: Entity, entityVar: string): string | null {
    if (field.isAggregateField) {
        switch (field.name) {
            case 'aggregateId':
                return `        this.aggregateId = ${entityVar}.getAggregateId();`;
            case 'version':
                return `        this.version = ${entityVar}.getVersion();`;
            case 'state':
                return `        this.state = ${entityVar}.getState();`;
            default:
                return null;
        }
    }

    const getterCall = buildEntityGetterCall(field, entityVar);
    if (!getterCall) {
        return null;
    }

    if (field.derivedAggregateId) {
        const accessor = field.derivedAccessor || 'getAggregateId';
        if (field.isCollection) {
            // For collections, stream and map to extract aggregateIds
            const collector = field.javaType.startsWith('Set<') ? 'Collectors.toSet()' : 'Collectors.toList()';
            return `        this.${field.name} = ${getterCall} != null ? ${getterCall}.stream().map(item -> item.${accessor}()).collect(${collector}) : null;`;
        }
        return `        this.${field.name} = ${getterCall} != null ? ${getterCall}.${accessor}() : null;`;
    }

    if (field.isEnum) {
        if (field.isCollection) {
            const collector = field.javaType.startsWith('Set<') ? 'Collectors.toSet()' : 'Collectors.toList()';
            return `        this.${field.name} = ${getterCall} != null ? ${getterCall}.stream().map(value -> value != null ? value.name() : null).collect(${collector}) : null;`;
        }
        return `        this.${field.name} = ${getterCall} != null ? ${getterCall}.name() : null;`;
    }

    if (field.requiresConversion) {
        if (field.isCollection && field.referencedEntityName) {
            const collector = field.javaType.startsWith('Set<') ? 'Collectors.toSet()' : 'Collectors.toList()';
            if (field.referencedEntityIsRoot || !field.referencedEntityHasGenerateDto) {
                return `        this.${field.name} = ${getterCall} != null ? ${getterCall}.stream().map(${field.referencedEntityName}::buildDto).collect(${collector}) : null;`;
            } else {
                return `        this.${field.name} = ${getterCall} != null ? ${getterCall}.stream().map(${field.referencedDtoName}::new).collect(${collector}) : null;`;
            }
        }
        if (!field.isCollection) {
            if (field.referencedEntityIsRoot || !field.referencedEntityHasGenerateDto) {
                return `        this.${field.name} = ${getterCall} != null ? ${getterCall}.buildDto() : null;`;
            } else {
                return `        this.${field.name} = ${getterCall} != null ? new ${field.referencedDtoName}(${getterCall}) : null;`;
            }
        }
    }

    return `        this.${field.name} = ${getterCall};`;
}

function buildEntityGetterCall(field: DtoFieldSchema, entityVar: string): string | null {
    const property = field.sourceProperty as Property | undefined;
    if (!property) {
        return null;
    }

    const capitalized = capitalize(property.name);
    return `${entityVar}.get${capitalized}()`;
}

function generateDtoGettersSetters(fields: DtoFieldSchema[]): string {
    const methods: string[] = [];

    for (const field of fields) {
        const javaType = field.javaType === 'boolean' ? 'boolean' : field.javaType;
        const capitalized = capitalize(field.name);

        methods.push(`    public ${javaType} get${capitalized}() {`);
        methods.push(`        return ${field.name};`);
        methods.push('    }');
        methods.push('');
        methods.push(`    public void set${capitalized}(${javaType} ${field.name}) {`);
        methods.push(`        this.${field.name} = ${field.name};`);
        methods.push('    }');

        methods.push('');
    }

    // Remove trailing empty line if present
    if (methods.length > 0 && methods[methods.length - 1] === '') {
        methods.pop();
    }

    return methods.join('\n');
}

export class DtoGenerator {
    async generateDto(entity: Entity, options: { projectName: string; dtoSchemaRegistry?: DtoSchemaRegistry }): Promise<string> {
        return generateDtoCode(entity, options.projectName, options.dtoSchemaRegistry);
    }
}