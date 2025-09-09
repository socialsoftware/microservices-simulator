import { Entity } from "../../../../language/generated/ast.js";
import { capitalize } from "../../../utils/generator-utils.js";
import { TypeResolver } from "../../base/type-resolver.js";

const resolveJavaType = (type: any) => TypeResolver.resolveJavaType(type);
const resolveParamReturnType = (type: any) => TypeResolver.resolveJavaType(type);

export type ImportRequirements = {
    usesPersistence?: boolean;
    usesLocalDateTime?: boolean;
    usesBigDecimal?: boolean;
    usesSet?: boolean;
    usesList?: boolean;
    usesAggregate?: boolean;
    usesStreams?: boolean;
    usesBusinessRules?: boolean;
    usesUserDto?: boolean;
    customImports?: Set<string>;
};

export function generateEntityCode(entity: Entity, projectName: string): string {
    const isRootEntity = entity.isRoot || false;
    const importReqs: ImportRequirements = {
        usesPersistence: true,
        usesLocalDateTime: false,
        usesBigDecimal: false,
        usesSet: false,
        usesList: false,
        usesAggregate: isRootEntity,
        usesStreams: false,
        usesBusinessRules: entity.rules && entity.rules.length > 0,
        customImports: new Set<string>()
    };

    const { code: fields, imports: fieldImports } = generateFields(entity.properties, entity, entity.isRoot || false);
    Object.assign(importReqs, fieldImports);

    const { code: constructor } = generateConstructor(entity.name, entity.properties);

    const { code: copyConstructor, imports: copyImports } = generateCopyConstructor(entity);
    Object.assign(importReqs, copyImports);

    const { code: gettersSetters } = generateGettersSetters(entity.properties, entity, projectName);

    const { code: invariants, imports: invariantImports } = generateInvariants(entity);

    if (invariantImports) {
        Object.assign(importReqs, invariantImports);
    }

    const { code: constructors, imports: constructorImports } = generateConstructors(entity);

    if (constructorImports) {
        Object.assign(importReqs, constructorImports);
    }

    const { code: methods, imports: methodImports } = generateMethods(entity);

    if (methodImports) {
        Object.assign(importReqs, methodImports);
    }


    const aggregateName = entity.$container?.name || 'unknown';
    const imports = generateImports(importReqs, projectName, isRootEntity, aggregateName, entity.name);
    const extendsClause = isRootEntity
        ? (aggregateName !== entity.name ? ` extends ${aggregateName}` : ' extends Aggregate')
        : '';
    const abstractModifier = '';
    const packageName = `pt.ulisboa.tecnico.socialsoftware.${projectName.toLowerCase()}.microservices.${aggregateName.toLowerCase()}.aggregate`;

    const jpaAnnotation = isRootEntity ? '@Entity' : '@Embeddable';

    return `package ${packageName};

${imports}

${jpaAnnotation}
public ${abstractModifier}class ${entity.name}${extendsClause} {
${fields} 
${constructor}
${copyConstructor}
${constructors}
${gettersSetters}
${methods}
${invariants}
}`;
}

export function generateDtoCode(entity: Entity, projectName: string): string {
    const packageName = `pt.ulisboa.tecnico.socialsoftware.${projectName.toLowerCase()}.microservices.${entity.$container?.name?.toLowerCase() || 'unknown'}.aggregate`;
    const fields = generateDtoFields(entity);
    const constructor = generateDtoConstructor(entity);
    const gettersSetters = generateGettersSetters(entity.properties);

    const importReqs: ImportRequirements = {
        usesLocalDateTime: entity.properties.some((p: any) => resolveJavaType(p.type) === 'LocalDateTime'),
        usesSet: entity.properties.some((p: any) => resolveJavaType(p.type).startsWith('Set<')),
        customImports: new Set(['import java.io.Serializable;'])
    };

    return `package ${packageName};

import java.io.Serializable;
${importReqs.usesLocalDateTime ? 'import java.time.LocalDateTime;' : ''}
${importReqs.usesSet ? 'import java.util.Set;\nimport java.util.HashSet;' : ''}

public class ${entity.name}Dto implements Serializable {
${fields}
${constructor}
${gettersSetters}
}`;
}

function generateDtoFields(entity: Entity): string {
    const simpleFields = entity.properties.map((property: any) => {
        const javaType = TypeResolver.resolveJavaType(property.type);
        return `\tprivate ${javaType} ${property.name};`;
    }).join('\n');

    return simpleFields;
}

function generateDtoConstructor(entity: Entity): string {
    const params = entity.properties
        .map((p: any) => {
            return `${resolveJavaType(p.type)} ${p.name}`;
        })
        .join(', ');
    const assignments = entity.properties
        .map((p: any) => `\t\tthis.${p.name} = ${p.name};`)
        .join('\n');

    return `
\tpublic ${entity.name}Dto() {}

\tpublic ${entity.name}Dto(${params}) {
${assignments}
\t}`;
}

function generateFields(properties: any[], entity: Entity, isRootEntity: boolean): { code: string, imports: ImportRequirements } {
    const imports: ImportRequirements = {
        usesLocalDateTime: false,
        usesBigDecimal: false,
        usesSet: false,
        usesList: false
    };

    const fields = properties.map((prop: any, index: number) => {
        const javaType = resolveJavaType(prop.type);
        if (javaType.includes('LocalDateTime')) imports.usesLocalDateTime = true;
        if (javaType.includes('BigDecimal')) imports.usesBigDecimal = true;
        if (javaType.startsWith('Set<')) imports.usesSet = true;
        if (javaType.startsWith('List<')) imports.usesList = true;

        const idAnnotation = (index === 0 && isRootEntity) ? '    @Id\n' : '';
        return `${idAnnotation}    private ${javaType} ${prop.name};`;
    }).join('\n');

    return { code: fields, imports };
}

function generateConstructor(name: string, properties: any[]): { code: string } {
    const params = properties.filter((p: any) => !p.isOptional).map((p: any) =>
        `${resolveJavaType(p.type)} ${p.name}`
    ).join(', ');

    const assignments = properties.filter((p: any) => !p.isOptional).map((p: any) =>
        `        this.${p.name} = ${p.name};`
    ).join('\n');

    return {
        code: `\n    public ${name}(${params}) {\n${assignments}\n    }`
    };
}

function generateCopyConstructor(entity: Entity): { code: string, imports: ImportRequirements } {
    return {
        code: `\n    public ${entity.name}(${entity.name} other) {\n        // Copy constructor\n    }`,
        imports: {}
    };
}

function generateGettersSetters(properties: any[], entity?: Entity, projectName?: string): { code: string } {
    const methods = properties.map((prop: any) => {
        const javaType = resolveJavaType(prop.type);
        const capName = capitalize(prop.name);
        const getter = javaType === 'Boolean' ? `is${capName}` : `get${capName}`;

        return `\n    public ${javaType} ${getter}() {\n        return ${prop.name};\n    }\n\n    public void set${capName}(${javaType} ${prop.name}) {\n        this.${prop.name} = ${prop.name};\n    }`;
    }).join('\n');

    return { code: methods };
}

function generateInvariants(entity: Entity): { code: string, imports?: ImportRequirements } {
    return { code: '', imports: undefined };
}

function generateImports(importReqs: ImportRequirements, projectName: string, isRoot: boolean, aggregateName?: string, entityName?: string): string {
    const imports: string[] = [];

    if (importReqs.usesPersistence) {
        if (isRoot) {
            imports.push('import jakarta.persistence.Entity;');
            imports.push('import jakarta.persistence.Id;');
        } else {
            imports.push('import jakarta.persistence.Embeddable;');
        }
    }
    if (isRoot) {
        if (aggregateName && entityName && aggregateName !== entityName) {
        } else {
            imports.push('import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;');
        }
    }
    imports.push('import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;');
    imports.push('import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;');
    if (importReqs.usesLocalDateTime) imports.push('import java.time.LocalDateTime;');
    if (importReqs.usesBigDecimal) imports.push('import java.math.BigDecimal;');
    if (importReqs.usesSet) imports.push('import java.util.Set;\nimport java.util.HashSet;');
    if (importReqs.usesList) imports.push('import java.util.List;\nimport java.util.ArrayList;');
    if (importReqs.usesUserDto) imports.push(`import pt.ulisboa.tecnico.socialsoftware.${projectName.toLowerCase()}.microservices.user.aggregate.UserDto;`);

    if (importReqs.customImports) {
        imports.push(...Array.from(importReqs.customImports));
    }

    return imports.join('\n');
}

export function generateConstructors(entity: Entity): { code: string, imports?: ImportRequirements } {
    if (!entity.constructors || entity.constructors.length === 0) {
        return { code: '' };
    }

    const importReqs: ImportRequirements = {};

    const constructorCode = entity.constructors.map((constructor: any) => {
        const params = constructor.parameters ? constructor.parameters.map((param: any) => {
            const paramType = resolveParamReturnType(param.type as any);
            if (paramType === 'UserDto') {
                importReqs.usesUserDto = true;
            }
            return `${paramType} ${param.name}`;
        }).join(', ') : '';

        const constructorBody = constructor.body ? constructor.body.replace(/"/g, '').split('\n').map((line: any) => `\t\t${line}`).join('\n') : '';

        return `\tpublic ${entity.name}(${params}) {
${constructorBody}
\t}`;
    }).join('\n\n');

    return {
        code: constructorCode,
        imports: importReqs
    };
}

export function generateMethods(entity: Entity): { code: string, imports?: ImportRequirements } {
    if (!entity.methods || entity.methods.length === 0) {
        return { code: '' };
    }

    const importReqs: ImportRequirements = {};

    const methodCode = entity.methods.map((method: any) => {
        const params = method.parameters ? method.parameters.map((param: any) => {
            const paramType = resolveParamReturnType(param.type as any);
            if (paramType === 'UserDto') {
                importReqs.usesUserDto = true;
            }
            return `${paramType} ${param.name}`;
        }).join(', ') : '';

        const returnType = method.returnType ? resolveParamReturnType(method.returnType as any) : 'void';
        if (returnType === 'UserDto') {
            importReqs.usesUserDto = true;
        }
        const methodBody = method.body ? method.body.replace(/"/g, '').split('\n').map((line: any) => `\t\t${line}`).join('\n') : '';

        const needsReturn = returnType !== 'void' && (!method.body || method.body.trim() === '');
        const defaultReturn = needsReturn ? `\t\treturn null; // TODO: Implement method` : '';

        return `\tpublic ${returnType} ${method.name}(${params}) {
${methodBody}${needsReturn ? '\n' + defaultReturn : ''}
\t}`;
    }).join('\n\n');

    return { code: methodCode, imports: importReqs };
}




export class EntityGenerator {
    async generateEntity(entity: Entity, options: { projectName: string }): Promise<string> {
        const projectName = options.projectName;
        return generateEntityCode(entity, projectName);
    }
}

