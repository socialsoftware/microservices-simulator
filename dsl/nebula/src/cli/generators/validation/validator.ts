
import {
    TemplateAST,
    TemplateNode,
    ValidationResult,
    ValidationError,
    ValidationWarning,
    TemplateContext
} from '../shared/types.js';
import { Utils } from '../../utils/generator-utils.js';

export class Validator {

    constructor(options: { maxDepth?: number; maxIterations?: number; strictMode?: boolean } = {}) {
    }

    validate(template: string): ValidationResult {
        const errors: ValidationError[] = [];
        const warnings: ValidationWarning[] = [];

        try {
            const ast = this.parseTemplate(template);

            this.validateAST(ast, errors, warnings);

            this.validateSyntax(template, errors, warnings);

            this.validateSemantics(ast, errors, warnings);

        } catch (error) {
            errors.push(Utils.createValidationError(
                'syntax',
                `Template parsing failed: ${error instanceof Error ? error.message : 'Unknown error'}`
            ));
        }

        return {
            isValid: errors.length === 0,
            errors,
            warnings
        };
    }

    validateWithContext(template: string, context: TemplateContext): ValidationResult {
        const result = this.validate(template);

        if (result.isValid) {
            this.validateContext(template, context, result.errors, result.warnings);
        }

        return result;
    }

    private parseTemplate(template: string): TemplateAST {
        const nodes: TemplateNode[] = [];
        let position = 0;
        let line = 1;
        let column = 1;

        while (position < template.length) {
            const start = position;
            const { line: startLine, column: startColumn } = { line, column };

            const tagMatch = template.slice(position).match(/{{([^}]+)}}/);

            if (tagMatch) {
                if (position > start) {
                    const textContent = template.slice(start, position);
                    nodes.push({
                        type: 'text',
                        content: textContent,
                        start,
                        end: position,
                        line: startLine,
                        column: startColumn
                    });
                }

                const tagContent = tagMatch[1].trim();
                const tagStart = position + tagMatch.index!;
                const tagEnd = tagStart + tagMatch[0].length;

                position = tagEnd;
                const textBeforeTag = template.slice(start, tagStart);
                line += (textBeforeTag.match(/\n/g) || []).length;
                if (textBeforeTag.includes('\n')) {
                    column = textBeforeTag.length - textBeforeTag.lastIndexOf('\n');
                } else {
                    column += textBeforeTag.length;
                }

                if (tagContent.startsWith('#')) {
                    const blockType = tagContent.split(' ')[0].substring(1);
                    nodes.push({
                        type: 'block',
                        content: tagContent,
                        start: tagStart,
                        end: tagEnd,
                        line,
                        column,
                        expression: tagContent.substring(blockType.length + 1).trim(),
                        children: []
                    });
                } else if (tagContent.startsWith('/')) {
                    const blockType = tagContent.substring(1);
                    nodes.push({
                        type: 'block',
                        content: tagContent,
                        start: tagStart,
                        end: tagEnd,
                        line,
                        column,
                        expression: blockType
                    });
                } else if (tagContent.startsWith('!')) {
                    nodes.push({
                        type: 'comment',
                        content: tagContent,
                        start: tagStart,
                        end: tagEnd,
                        line,
                        column
                    });
                } else if (tagContent.startsWith('>')) {
                    nodes.push({
                        type: 'partial',
                        content: tagContent,
                        start: tagStart,
                        end: tagEnd,
                        line,
                        column,
                        expression: tagContent.substring(1).trim()
                    });
                } else {
                    nodes.push({
                        type: 'variable',
                        content: tagContent,
                        start: tagStart,
                        end: tagEnd,
                        line,
                        column,
                        expression: tagContent
                    });
                }
            } else {
                const textContent = template.slice(position);
                nodes.push({
                    type: 'text',
                    content: textContent,
                    start: position,
                    end: template.length,
                    line,
                    column
                });
                break;
            }
        }

        return { nodes, source: template };
    }

    private validateAST(ast: TemplateAST, errors: ValidationError[], warnings: ValidationWarning[]): void {
        const blockStack: TemplateNode[] = [];

        for (const node of ast.nodes) {
            if (node.type === 'block') {
                if (node.content.startsWith('#')) {
                    const blockType = node.content.split(' ')[0].substring(1);

                    if (!['if', 'unless', 'each', 'with', 'partial'].includes(blockType)) {
                        errors.push(Utils.createValidationError(
                            'syntax',
                            `Unknown block type: ${blockType}`,
                            node.line,
                            node.column
                        ));
                    }

                    if (['if', 'unless', 'each', 'with'].includes(blockType) && !node.expression) {
                        errors.push(Utils.createValidationError(
                            'syntax',
                            `Block '${blockType}' requires an expression`,
                            node.line,
                            node.column
                        ));
                    }

                    blockStack.push(node);
                } else if (node.content.startsWith('/')) {
                    const blockType = node.content.substring(1);

                    if (blockStack.length === 0) {
                        errors.push(Utils.createValidationError(
                            'syntax',
                            `Unexpected closing block '${blockType}'`,
                            node.line,
                            node.column
                        ));
                    } else {
                        const openingBlock = blockStack.pop()!;
                        const openingType = openingBlock.content.split(' ')[0].substring(1);

                        if (openingType !== blockType) {
                            errors.push(Utils.createValidationError(
                                'syntax',
                                `Mismatched block tags: expected '${openingType}' but found '${blockType}'`,
                                node.line,
                                node.column
                            ));
                        }
                    }
                }
            }
        }

        if (blockStack.length > 0) {
            const unclosedBlock = blockStack[blockStack.length - 1];
            errors.push(Utils.createValidationError(
                'syntax',
                `Unclosed block '${unclosedBlock.content.split(' ')[0].substring(1)}'`,
                unclosedBlock.line,
                unclosedBlock.column
            ));
        }
    }

    private validateSyntax(template: string, errors: ValidationError[], warnings: ValidationWarning[]): void {
        let braceCount = 0;
        let inTag = false;

        for (let i = 0; i < template.length; i++) {
            const char = template[i];

            if (char === '{' && template[i + 1] === '{') {
                braceCount++;
                inTag = true;
                i++;
            } else if (char === '}' && template[i + 1] === '}' && inTag) {
                braceCount--;
                inTag = false;
                i++;
            }
        }

        if (braceCount !== 0) {
            errors.push(Utils.createValidationError(
                'syntax',
                'Unmatched braces in template'
            ));
        }

        const nestedTagRegex = /{{[^}]*{{[^}]*}}[^}]*}}/g;
        if (nestedTagRegex.test(template)) {
            errors.push(Utils.createValidationError(
                'syntax',
                'Nested template tags are not allowed'
            ));
        }
    }

    private validateSemantics(ast: TemplateAST, errors: ValidationError[], warnings: ValidationWarning[]): void {
        for (const node of ast.nodes) {
            if (node.type === 'variable' && node.expression) {
                this.validateVariableExpression(node.expression, node.line, node.column, errors, warnings);
            } else if (node.type === 'block' && node.expression) {
                this.validateBlockExpression(node.expression, node.line, node.column, errors, warnings);
            } else if (node.type === 'partial' && node.expression) {
                this.validatePartialReference(node.expression, node.line, node.column, errors, warnings);
            }
        }
    }

    private validateVariableExpression(expression: string, line: number, column: number, errors: ValidationError[], warnings: ValidationWarning[]): void {
        const { path, filters } = Utils.parseExpression(expression);

        if (!path) {
            errors.push(Utils.createValidationError(
                'semantic',
                'Variable expression cannot be empty',
                line,
                column
            ));
            return;
        }

        const pathParts = path.split('.');
        for (const part of pathParts) {
            if (!Utils.isValidIdentifier(part)) {
                errors.push(Utils.createValidationError(
                    'semantic',
                    `Invalid identifier in path: ${part}`,
                    line,
                    column
                ));
            }
        }

        for (const filter of filters) {
            if (!['upper', 'lower', 'capitalize', 'escape', 'unescape', 'json', 'length'].includes(filter)) {
                warnings.push(Utils.createValidationWarning(
                    'style',
                    `Unknown filter: ${filter}`,
                    line,
                    column
                ));
            }
        }
    }

    private validateBlockExpression(expression: string, line: number, column: number, errors: ValidationError[], warnings: ValidationWarning[]): void {
        if (!expression) {
            errors.push(Utils.createValidationError(
                'semantic',
                'Block expression cannot be empty',
                line,
                column
            ));
            return;
        }

        const { path } = Utils.parseExpression(expression);
        if (path) {
            const pathParts = path.split('.');
            for (const part of pathParts) {
                if (!Utils.isValidIdentifier(part)) {
                    errors.push(Utils.createValidationError(
                        'semantic',
                        `Invalid identifier in block expression: ${part}`,
                        line,
                        column
                    ));
                }
            }
        }
    }

    private validatePartialReference(expression: string, line: number, column: number, errors: ValidationError[], warnings: ValidationWarning[]): void {
        if (!expression) {
            errors.push(Utils.createValidationError(
                'semantic',
                'Partial reference cannot be empty',
                line,
                column
            ));
            return;
        }

        if (!Utils.isValidIdentifier(expression)) {
            errors.push(Utils.createValidationError(
                'semantic',
                `Invalid partial name: ${expression}`,
                line,
                column
            ));
        }
    }

    private validateContext(template: string, context: TemplateContext, errors: ValidationError[], warnings: ValidationWarning[]): void {
        const variableRegex = /{{([^}]+)}}/g;
        let match;

        while ((match = variableRegex.exec(template)) !== null) {
            const expression = match[1].trim();

            if (expression.startsWith('#') || expression.startsWith('/') || expression.startsWith('!') || expression.startsWith('>')) {
                continue;
            }

            const { path } = Utils.parseExpression(expression);
            if (path && !this.hasProperty(context, path)) {
                warnings.push(Utils.createValidationWarning(
                    'style',
                    `Variable '${path}' is not defined in context`,
                    undefined,
                    undefined,
                    match.index
                ));
            }
        }
    }

    private hasProperty(context: TemplateContext, path: string): boolean {
        return Utils.getProperty(context, path) !== undefined;
    }
}
