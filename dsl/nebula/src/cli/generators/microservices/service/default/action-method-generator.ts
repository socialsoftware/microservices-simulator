import { Entity, Method } from "../../../../../language/generated/ast.js";
import { capitalize } from "../../../../utils/generator-utils.js";
import { UnifiedTypeResolver as TypeResolver } from "../../../common/unified-type-resolver.js";
import { ExceptionGenerator } from "../../../common/utils/exception-generator.js";

export class ActionMethodGenerator {

    static hasActionBody(method: Method): boolean {
        return !!(method as any).actionBody;
    }

    static generate(
        method: Method,
        aggregateName: string,
        rootEntity: Entity,
        projectName: string
    ): string {
        const actionBody = (method as any).actionBody;
        if (!actionBody) {
            throw new Error(`generate() called on method without actionBody: ${method.name}`);
        }

        const lowerAggregate = aggregateName.toLowerCase();
        const capitalizedAggregate = capitalize(aggregateName);

        const params = this.renderParameters(method);
        const returnType = this.deriveReturnType(method, actionBody, capitalizedAggregate);

        const preconditionLines = this.renderPreconditions(actionBody.preconditions || [], projectName);
        const { actionLines, resultVar } = this.renderActionStatements(
            actionBody.statements || [],
            aggregateName,
            lowerAggregate,
            capitalizedAggregate,
            rootEntity,
            projectName
        );

        const publishesLines = this.renderPublishes(
            (actionBody as any).publishes || [],
            resultVar,
            projectName
        );

        const returnLine = (returnType !== 'void' && resultVar)
            ? `        return ${lowerAggregate}Factory.create${capitalizedAggregate}Dto(${resultVar});`
            : '';

        const body = [
            ...preconditionLines,
            ...actionLines,
            ...publishesLines,
            returnLine
        ].filter(line => line.length > 0).join('\n');

        const tryWrapped = ExceptionGenerator.generateTryCatchWrapper(
            projectName,
            `in ${method.name}`,
            aggregateName,
            body
        );

        return `    @Transactional
    public ${returnType} ${method.name}(${params}) {
${tryWrapped}
    }`;
    }

    private static renderParameters(method: Method): string {
        const userParams = (method.parameters || []).map(param => {
            const javaType = TypeResolver.resolveJavaType((param as any).type);
            return `${javaType} ${param.name}`;
        });
        userParams.push('UnitOfWork unitOfWork');
        return userParams.join(', ');
    }

    private static deriveReturnType(method: Method, actionBody: any, capitalizedAggregate: string): string {
        if (method.returnType) {
            return TypeResolver.resolveJavaType(method.returnType as any);
        }
        const hasCreate = (actionBody.statements || []).some((s: any) => s.$type === 'CreateActionStatement');
        return hasCreate ? `${capitalizedAggregate}Dto` : 'void';
    }

    private static renderPreconditions(preconditions: any[], projectName: string): string[] {
        const lines: string[] = [];
        const exceptionClass = `${capitalize(projectName)}Exception`;
        for (const pre of preconditions) {
            const left = this.renderExpression(pre.left);
            const right = this.renderExpression(pre.right);
            const op = pre.op;
            const message = pre.message ? `"${pre.message}"` : '"precondition failed"';
            lines.push(`        if (!(${left} ${op} ${right})) {`);
            lines.push(`            throw new ${exceptionClass}(${message});`);
            lines.push(`        }`);
        }
        return lines;
    }

    private static renderActionStatements(
        statements: any[],
        aggregateName: string,
        lowerAggregate: string,
        capitalizedAggregate: string,
        rootEntity: Entity,
        projectName: string
    ): { actionLines: string[]; resultVar: string | null } {
        const lines: string[] = [];
        let resultVar: string | null = null;

        const dirtyAliases = new Map<string, string>();
        const aliasTypes = new Map<string, string>();

        for (const stmt of statements) {
            switch (stmt.$type) {
                case 'CreateActionStatement': {
                    const aggregateRef = stmt.aggregateRef;
                    if (aggregateRef !== aggregateName) {
                        lines.push(`        // unsupported: cross-aggregate create — ${aggregateRef}`);
                        break;
                    }
                    const dtoVar = 'dto';
                    lines.push(`        ${capitalizedAggregate}Dto ${dtoVar} = new ${capitalizedAggregate}Dto();`);
                    for (const fa of (stmt.fields || [])) {
                        const setter = `set${capitalize(fa.field)}`;
                        const value = this.renderExpression(fa.value);
                        lines.push(`        ${dtoVar}.${setter}(${value});`);
                    }
                    const createdVar = lowerAggregate;
                    lines.push(`        Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();`);
                    lines.push(`        ${capitalizedAggregate} ${createdVar} = ${lowerAggregate}Factory.create${capitalizedAggregate}(aggregateId, ${dtoVar});`);
                    lines.push(`        unitOfWorkService.registerChanged(${createdVar}, unitOfWork);`);
                    resultVar = createdVar;
                    break;
                }

                case 'LoadActionStatement': {
                    const targetAgg = stmt.aggregateRef;
                    const targetAggLower = targetAgg.toLowerCase();
                    const targetAggCap = capitalize(targetAgg);
                    const alias = stmt.alias;
                    const idExpr = this.renderExpression(stmt.id);

                    if (targetAgg !== aggregateName) {
                        lines.push(`        // unsupported: cross-aggregate load — ${targetAgg}`);
                        break;
                    }

                    const oldVar = `${alias}Old`;
                    lines.push(`        ${targetAggCap} ${oldVar} = (${targetAggCap}) unitOfWorkService.aggregateLoadAndRegisterRead(${idExpr}, unitOfWork);`);
                    lines.push(`        ${targetAggCap} ${alias} = ${targetAggLower}Factory.create${targetAggCap}FromExisting(${oldVar});`);

                    aliasTypes.set(alias, targetAggCap);
                    break;
                }

                case 'AssignActionStatement': {
                    const alias = stmt.aliasRef;
                    const field = stmt.field;

                    const fieldEnumType = this.lookupEnumTypeOfField(rootEntity, field);
                    const value = fieldEnumType
                        ? this.renderEnumOrExpression(stmt.value, fieldEnumType)
                        : this.renderExpression(stmt.value);

                    const setter = `set${capitalize(field)}`;
                    lines.push(`        ${alias}.${setter}(${value});`);
                    if (aliasTypes.has(alias)) {
                        dirtyAliases.set(alias, aliasTypes.get(alias)!);
                    } else {
                        lines.push(`        // warn: assignment to unknown alias '${alias}'`);
                    }
                    break;
                }

                case 'ExtensionActionStatement': {
                    const fn = stmt.fnName;
                    const args = (stmt.args || []).map((a: any) => this.renderExpression(a));
                    lines.push(`        extension.${fn}(${args.join(', ')});`);
                    break;
                }

                case 'FindActionStatement': {
                    const collection = this.renderExpression(stmt.collection);
                    const field = stmt.field;
                    const value = this.renderExpression(stmt.value);
                    const alias = stmt.alias;
                    const capitalize = (s: string) => s.charAt(0).toUpperCase() + s.slice(1);
                    lines.push(`        var ${alias} = ${collection}.stream()`);
                    lines.push(`            .filter(el -> el.get${capitalize(field)}() != null && el.get${capitalize(field)}().equals(${value}))`);
                    lines.push(`            .findFirst()`);
                    const projectCap = projectName.charAt(0).toUpperCase() + projectName.slice(1);
                    lines.push(`            .orElseThrow(() -> new ${projectCap}Exception("Element not found in collection"));`);
                    dirtyAliases.set(alias, '__child');
                    break;
                }

                default:
                    lines.push(`        // unsupported statement: ${stmt.$type}`);
            }
        }

        for (const [alias, _type] of dirtyAliases) {
            if (_type === '__child') continue;
            lines.push(`        unitOfWorkService.registerChanged(${alias}, unitOfWork);`);
        }

        void projectName;

        return { actionLines: lines, resultVar };
    }

    private static renderExpression(expr: any, aliases: Record<string, string> = {}): string {
        if (!expr) return 'null';
        switch (expr.$type) {
            case 'ActionLiteral':
                if (expr.stringValue !== undefined) return `"${expr.stringValue}"`;
                if (expr.boolLiteral !== undefined) return expr.boolLiteral;
                if (expr.literalValue !== undefined) return this.unquoteLiteral(expr.literalValue);
                return 'null';
            case 'ActionRef': {
                const rawHead = expr.name;
                const head = aliases[rawHead] ?? rawHead;
                const chain = (expr.chain || []) as string[];
                if (chain.length === 0) {
                    if (/^[A-Z][A-Z0-9_]*$/.test(head)) {
                        return `"${head}"`;
                    }
                    return head;
                }
                const accessors = chain.map(c => `.get${capitalize(c)}()`).join('');
                return `${head}${accessors}`;
            }
            default:
                return `null`;
        }
    }

    private static renderPublishes(
        publishes: any[],
        resultVar: string | null,
        projectName: string
    ): string[] {
        if (!publishes || publishes.length === 0) return [];

        const aliases: Record<string, string> = {};
        if (resultVar) aliases['result'] = resultVar;

        const lines: string[] = [];
        let counter = 0;
        for (const clause of publishes) {
            const eventType = clause.eventType;
            if (!eventType) continue;

            const evVar = `event${counter++}`;
            lines.push(`        ${eventType} ${evVar} = new ${eventType}();`);
            for (const a of (clause.assignments || [])) {
                const setter = `set${capitalize(a.field)}`;
                const value = this.renderExpression(a.value, aliases);
                lines.push(`        ${evVar}.${setter}(${value});`);
            }
            if (resultVar) {
                lines.push(`        ${evVar}.setPublisherAggregateVersion(${resultVar}.getVersion());`);
            }
            lines.push(`        unitOfWorkService.registerEvent(${evVar}, unitOfWork);`);
        }
        void projectName;
        return lines;
    }

    private static lookupEnumTypeOfField(rootEntity: Entity, field: string): string | null {
        if (!rootEntity || !(rootEntity as any).properties) return null;
        const prop = (rootEntity as any).properties.find((p: any) => p.name === field);
        if (!prop) return null;
        const propType = prop.type;
        if (!propType) return null;

        let typeName: string | undefined =
            propType.type?.$refText
            ?? propType.type?.ref?.name
            ?? (typeof propType === 'string' ? propType : undefined);

        if (!typeName) return null;

        const primitives = new Set([
            'String', 'Integer', 'Long', 'Float', 'Double', 'Boolean',
            'LocalDateTime', 'AggregateState', 'UnitOfWork'
        ]);
        if (primitives.has(typeName)) return null;
        if (typeName.startsWith('Set<') || typeName.startsWith('List<') || typeName.startsWith('Optional<')) return null;

        return typeName;
    }

    private static renderEnumOrExpression(expr: any, enumType: string): string {
        if (expr && expr.$type === 'ActionRef' && (!expr.chain || expr.chain.length === 0)) {
            const head = expr.name;
            if (/^[A-Z][A-Z0-9_]*$/.test(head)) {
                return `${enumType}.${head}`;
            }
        }
        return this.renderExpression(expr);
    }

    private static unquoteLiteral(literal: string): string {
        if (literal.startsWith("'") && literal.endsWith("'")) {
            return `"${literal.slice(1, -1)}"`;
        }
        return literal;
    }
}
