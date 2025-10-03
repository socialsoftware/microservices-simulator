import { Entity, Property } from "../../../../language/generated/ast.js";
import { capitalize } from "../../../utils/generator-utils.js";
import { TypeResolver } from "../../base/type-resolver.js";
import { getGlobalConfig } from "../../base/config.js";

export function generateDtoCode(entity: Entity, projectName: string): string {

    const packageName = `${getGlobalConfig().buildPackageName(projectName, 'microservices', entity.$container.name.toLowerCase(), 'aggregate')}`;

    const imports = generateDtoImports(entity);
    const fields = generateDtoFields(entity);
    const constructors = generateDtoConstructors(entity);
    const gettersSetters = generateDtoGettersSetters(entity);

    return `package ${packageName};

${imports}

public class ${entity.name}Dto implements Serializable {
${fields}

${constructors}

${gettersSetters}
}`;
}

function generateDtoImports(entity: Entity): string {
    const imports: string[] = [
        'import java.io.Serializable;'
    ];

    const needsLocalDateTime = entity.properties.some((p: any) =>
        p.type.$type === 'PrimitiveType' && p.type.typeName === 'LocalDateTime'
    );

    const needsSet = entity.properties.some((p: any) =>
        p.type.$type === 'CollectionType'
    );

    if (needsLocalDateTime) {
        imports.push('import java.time.LocalDateTime;');
    }

    if (needsSet) {
        imports.push('import java.util.Set;');
    }

    imports.push('');
    imports.push('import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;');

    return imports.join('\n');
}

function generateDtoFields(entity: Entity): string {
    const fields: string[] = [];

    if (entity.isRoot) {
        fields.push('\tprivate Integer aggregateId;');
    }

    for (const property of entity.properties) {
        if (!property.isKey) {
            const javaType = TypeResolver.resolveJavaType(property.type);

            if (javaType === 'Boolean' || javaType === 'boolean') {
                fields.push(`\tprivate boolean ${property.name};`);
            } else {
                fields.push(`\tprivate ${javaType} ${property.name};`);
            }
        }
    }

    if (entity.isRoot) {
        fields.push('\tprivate Integer version;');
        fields.push('\tprivate AggregateState state;');
    }

    return fields.join('\n');
}

function generateDtoConstructors(entity: Entity): string {
    const constructors: string[] = [];

    constructors.push('\tpublic ' + entity.name + 'Dto() {');
    constructors.push('\t}');
    constructors.push('');

    constructors.push('\tpublic ' + entity.name + 'Dto(' + entity.name + ' ' + entity.name.toLowerCase() + ') {');

    if (entity.isRoot) {
        constructors.push('\t\tthis.aggregateId = ' + entity.name.toLowerCase() + '.getAggregateId();');
    }

    for (const property of entity.properties) {
        if (!property.isKey) {
            const getter = getGetterName(property);

            // Check if this is an enum field (ends with "Type")
            if (property.name.endsWith('Type')) {
                // Convert enum to string using toString()
                constructors.push(`\t\tthis.${property.name} = ${entity.name.toLowerCase()}.${getter}() != null ? ${entity.name.toLowerCase()}.${getter}().toString() : null;`);
            } else {
                constructors.push(`\t\tthis.${property.name} = ${entity.name.toLowerCase()}.${getter}();`);
            }
        }
    }

    if (entity.isRoot) {
        constructors.push('\t\tthis.version = ' + entity.name.toLowerCase() + '.getVersion();');
        constructors.push('\t\tthis.state = ' + entity.name.toLowerCase() + '.getState();');
    }

    constructors.push('\t}');

    return constructors.join('\n');
}

function generateDtoGettersSetters(entity: Entity): string {
    const methods: string[] = [];

    if (entity.isRoot) {
        methods.push('\tpublic Integer getAggregateId() {');
        methods.push('\t\treturn aggregateId;');
        methods.push('\t}');
        methods.push('');
        methods.push('\tpublic void setAggregateId(Integer aggregateId) {');
        methods.push('\t\tthis.aggregateId = aggregateId;');
        methods.push('\t}');
        methods.push('');
    }

    for (const property of entity.properties) {
        if (!property.isKey) {
            const javaType = TypeResolver.resolveJavaType(property.type);
            const capitalizedName = capitalize(property.name);

            if (javaType === 'Boolean' || javaType === 'boolean') {
                methods.push(`\tpublic boolean is${capitalizedName}() {`);
                methods.push(`\t\treturn ${property.name};`);
                methods.push('\t}');
            } else {
                methods.push(`\tpublic ${javaType} get${capitalizedName}() {`);
                methods.push(`\t\treturn ${property.name};`);
                methods.push('\t}');
            }
            methods.push('');

            methods.push(`\tpublic void set${capitalizedName}(${javaType} ${property.name}) {`);
            methods.push(`\t\tthis.${property.name} = ${property.name};`);
            methods.push('\t}');
            methods.push('');
        }
    }

    if (entity.isRoot) {
        methods.push('\tpublic Integer getVersion() {');
        methods.push('\t\treturn version;');
        methods.push('\t}');
        methods.push('');
        methods.push('\tpublic void setVersion(Integer version) {');
        methods.push('\t\tthis.version = version;');
        methods.push('\t}');
        methods.push('');

        methods.push('\tpublic AggregateState getState() {');
        methods.push('\t\treturn state;');
        methods.push('\t}');
        methods.push('');
        methods.push('\tpublic void setState(AggregateState state) {');
        methods.push('\t\tthis.state = state;');
        methods.push('\t}');
    }

    return methods.join('\n');
}

function getGetterName(property: Property): string {
    const javaType = TypeResolver.resolveJavaType(property.type);
    const capitalizedName = capitalize(property.name);

    if (javaType === 'Boolean' || javaType === 'boolean') {
        return `is${capitalizedName}`;
    } else {
        return `get${capitalizedName}`;
    }
}

export class DtoGenerator {
    async generateDto(entity: Entity, options: { projectName: string }): Promise<string> {
        return generateDtoCode(entity, options.projectName);
    }
}