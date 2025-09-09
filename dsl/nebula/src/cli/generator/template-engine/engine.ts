import {
    TemplateEngine,
    TemplateContext,
    CompiledTemplate,
    ValidationResult,
    TemplateHelper,
    TemplateNode,
    TemplateAST,
    TemplateOptions,
    TemplateCache,
    TemplateMetrics
} from './types.js';
import { Utils } from '../../utils/generator-utils.js';
import { Validator } from '../validation/validator.js';
import { DefaultHelpers } from './default-helpers.js';

export class DefaultTemplateEngine implements TemplateEngine {
    private helpers: Map<string, TemplateHelper> = new Map();
    private partials: Map<string, string> = new Map();
    private cache: TemplateCache;
    private validator: Validator;
    private options: TemplateOptions;
    private metrics: TemplateMetrics;

    constructor(options: TemplateOptions = {}) {
        this.options = {
            strictMode: false,
            allowUnsafe: false,
            maxDepth: 100,
            maxIterations: 1000,
            timeout: 5000,
            ...options
        };

        this.cache = new MapTemplateCache();
        this.validator = new Validator({
            maxDepth: this.options.maxDepth,
            maxIterations: this.options.maxIterations,
            strictMode: this.options.strictMode
        });

        this.metrics = {
            renderTime: 0,
            compileTime: 0,
            cacheHits: 0,
            cacheMisses: 0,
            totalRenders: 0,
            totalCompiles: 0
        };

        this.registerDefaultHelpers();
    }

    render(template: string, context: TemplateContext): string {
        const startTime = Date.now();

        try {
            const compiled = this.compile(template);
            const result = compiled.render(context);

            this.metrics.renderTime += Date.now() - startTime;
            this.metrics.totalRenders++;

            return result;
        } catch (error) {
            throw new Error(`Template rendering failed: ${error instanceof Error ? error.message : 'Unknown error'}`);
        }
    }

    validate(template: string): ValidationResult {
        return this.validator.validate(template);
    }

    compile(template: string): CompiledTemplate {
        const startTime = Date.now();

        const cacheKey = this.getCacheKey(template);
        const cached = this.cache.get(cacheKey);
        if (cached) {
            this.metrics.cacheHits++;
            return cached;
        }

        this.metrics.cacheMisses++;

        try {
            const validation = this.validator.validate(template);
            if (!validation.isValid) {
                throw new Error(`Template validation failed: ${validation.errors.map((e: any) => e.message).join(', ')}`);
            }

            const ast = this.parseTemplate(template);

            const compiled = new DefaultCompiledTemplate(ast, this);

            this.cache.set(cacheKey, compiled);

            this.metrics.compileTime += Date.now() - startTime;
            this.metrics.totalCompiles++;

            return compiled;
        } catch (error) {
            throw new Error(`Template compilation failed: ${error instanceof Error ? error.message : 'Unknown error'}`);
        }
    }

    registerHelper(name: string, helper: TemplateHelper): void {
        this.helpers.set(name, helper);
    }

    registerPartial(name: string, partial: string): void {
        this.partials.set(name, partial);
    }

    getHelper(name: string): TemplateHelper | undefined {
        return this.helpers.get(name);
    }

    getPartial(name: string): string | undefined {
        return this.partials.get(name);
    }

    getMetrics(): TemplateMetrics {
        return { ...this.metrics };
    }

    clearCache(): void {
        this.cache.clear();
    }

    private parseTemplate(template: string): TemplateAST {
        const nodes: TemplateNode[] = [];
        let position = 0;
        let line = 1;
        let column = 1;

        while (position < template.length) {
            const { line: startLine, column: startColumn } = { line, column };

            const tagMatch = template.slice(position).match(/{{([^}]+)}}/);

            if (tagMatch) {
                const tagStart = position + tagMatch.index!;
                if (tagStart > position) {
                    const textContent = template.slice(position, tagStart);
                    nodes.push({
                        type: 'text',
                        content: textContent,
                        start: position,
                        end: tagStart,
                        line: startLine,
                        column: startColumn
                    });
                }

                const tagContent = tagMatch[1].trim();
                const tagEnd = tagStart + tagMatch[0].length;

                const textBeforeTag = template.slice(position, tagStart);
                line += (textBeforeTag.match(/\n/g) || []).length;
                if (textBeforeTag.includes('\n')) {
                    column = textBeforeTag.length - textBeforeTag.lastIndexOf('\n');
                } else {
                    column += textBeforeTag.length;
                }
                position = tagEnd;

                const node = this.createTemplateNode(tagContent, tagStart, tagEnd, line, column);
                nodes.push(node);
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

    private createTemplateNode(content: string, start: number, end: number, line: number, column: number): TemplateNode {
        if (content.startsWith('#')) {
            const blockType = content.split(' ')[0].substring(1);
            return {
                type: 'block',
                content,
                start,
                end,
                line,
                column,
                expression: content.substring(blockType.length + 1).trim(),
                children: []
            };
        } else if (content.startsWith('/')) {
            const blockType = content.substring(1);
            return {
                type: 'block',
                content,
                start,
                end,
                line,
                column,
                expression: blockType
            };
        } else if (content.startsWith('!')) {
            return {
                type: 'comment',
                content,
                start,
                end,
                line,
                column
            };
        } else if (content.startsWith('>')) {
            return {
                type: 'partial',
                content,
                start,
                end,
                line,
                column,
                expression: content.substring(1).trim()
            };
        } else {
            return {
                type: 'variable',
                content,
                start,
                end,
                line,
                column,
                expression: content
            };
        }
    }

    private getCacheKey(template: string): string {
        let hash = 0;
        for (let i = 0; i < template.length; i++) {
            const char = template.charCodeAt(i);
            hash = ((hash << 5) - hash) + char;
            hash = hash & hash;
        }
        return hash.toString();
    }

    private registerDefaultHelpers(): void {
        DefaultHelpers.registerAll((name, helper) => this.registerHelper(name, helper));
    }
}

class DefaultCompiledTemplate implements CompiledTemplate {
    constructor(
        private ast: TemplateAST,
        private engine: DefaultTemplateEngine
    ) { }

    render(context: TemplateContext): string {
        return this.renderNodes(this.ast.nodes, context, 0);
    }

    validate(context: TemplateContext): ValidationResult {
        return this.engine.validate(this.ast.source);
    }

    getDependencies(): string[] {
        const dependencies: string[] = [];

        for (const node of this.ast.nodes) {
            if (node.type === 'partial' && node.expression) {
                dependencies.push(node.expression);
            }
        }

        return dependencies;
    }

    getVariables(): string[] {
        const variables: string[] = [];

        for (const node of this.ast.nodes) {
            if (node.type === 'variable' && node.expression) {
                const { path } = Utils.parseExpression(node.expression);
                if (path) {
                    variables.push(path);
                }
            }
        }

        return variables;
    }

    private renderNodes(nodes: TemplateNode[], context: TemplateContext, depth: number): string {
        if (depth > this.engine['options'].maxDepth!) {
            throw new Error('Maximum template depth exceeded');
        }

        let result = '';
        let i = 0;

        while (i < nodes.length) {
            const node = nodes[i];

            switch (node.type) {
                case 'text':
                    result += node.content;
                    break;

                case 'variable':
                    result += this.renderVariable(node, context);
                    break;

                case 'block':
                    const blockResult = this.renderBlock(node, nodes, i, context, depth);
                    result += blockResult.output;
                    i = blockResult.nextIndex;
                    break;

                case 'comment':
                    break;

                case 'partial':
                    result += this.renderPartial(node, context, depth);
                    break;
            }

            i++;
        }

        return result;
    }

    private renderVariable(node: TemplateNode, context: TemplateContext): string {
        if (!node.expression) {
            return '';
        }

        const { path, filters } = Utils.parseExpression(node.expression);
        let value = Utils.getProperty(context, path);

        value = Utils.applyFilters(value, filters);

        return Utils.toString(value);
    }

    private renderBlock(node: TemplateNode, nodes: TemplateNode[], index: number, context: TemplateContext, depth: number): { output: string; nextIndex: number } {
        if (node.content.startsWith('#')) {
            const blockType = node.content.split(' ')[0].substring(1);

            switch (blockType) {
                case 'if':
                    return this.renderIfBlock(node, nodes, index, context, depth);
                case 'unless':
                    return this.renderUnlessBlock(node, nodes, index, context, depth);
                case 'each':
                    return this.renderEachBlock(node, nodes, index, context, depth);
                case 'with':
                    return this.renderWithBlock(node, nodes, index, context, depth);
                default:
                    throw new Error(`Unknown block type: ${blockType}`);
            }
        } else if (node.content.startsWith('/')) {
            return { output: '', nextIndex: index };
        }

        return { output: '', nextIndex: index };
    }

    private renderIfBlock(node: TemplateNode, nodes: TemplateNode[], index: number, context: TemplateContext, depth: number): { output: string; nextIndex: number } {
        const expression = node.expression || '';
        const { path } = Utils.parseExpression(expression);
        const value = Utils.getProperty(context, path);

        if (Utils.isTruthy(value)) {
            const blockContent = this.findBlockContent(nodes, index, 'if');
            return {
                output: this.renderNodes(blockContent, context, depth + 1),
                nextIndex: this.findClosingTag(nodes, index, 'if')
            };
        } else {
            return {
                output: '',
                nextIndex: this.findClosingTag(nodes, index, 'if')
            };
        }
    }

    private renderUnlessBlock(node: TemplateNode, nodes: TemplateNode[], index: number, context: TemplateContext, depth: number): { output: string; nextIndex: number } {
        const expression = node.expression || '';
        const { path } = Utils.parseExpression(expression);
        const value = Utils.getProperty(context, path);

        if (Utils.isFalsy(value)) {
            const blockContent = this.findBlockContent(nodes, index, 'unless');
            return {
                output: this.renderNodes(blockContent, context, depth + 1),
                nextIndex: this.findClosingTag(nodes, index, 'unless')
            };
        } else {
            return {
                output: '',
                nextIndex: this.findClosingTag(nodes, index, 'unless')
            };
        }
    }

    private renderEachBlock(node: TemplateNode, nodes: TemplateNode[], index: number, context: TemplateContext, depth: number): { output: string; nextIndex: number } {
        const expression = node.expression || '';
        const { path } = Utils.parseExpression(expression);
        const value = Utils.getProperty(context, path);

        if (Array.isArray(value)) {
            const blockContent = this.findBlockContent(nodes, index, 'each');
            let result = '';

            for (let i = 0; i < value.length; i++) {
                const itemContext = {
                    ...context,
                    '@index': i,
                    '@first': i === 0,
                    '@last': i === value.length - 1,
                    'this': value[i]
                };

                result += this.renderNodes(blockContent, itemContext, depth + 1);
            }

            return {
                output: result,
                nextIndex: this.findClosingTag(nodes, index, 'each')
            };
        } else {
            return {
                output: '',
                nextIndex: this.findClosingTag(nodes, index, 'each')
            };
        }
    }

    private renderWithBlock(node: TemplateNode, nodes: TemplateNode[], index: number, context: TemplateContext, depth: number): { output: string; nextIndex: number } {
        const expression = node.expression || '';
        const { path } = Utils.parseExpression(expression);
        const value = Utils.getProperty(context, path);

        if (value !== undefined) {
            const blockContent = this.findBlockContent(nodes, index, 'with');
            const newContext = { ...context, ...value };

            return {
                output: this.renderNodes(blockContent, newContext, depth + 1),
                nextIndex: this.findClosingTag(nodes, index, 'with')
            };
        } else {
            return {
                output: '',
                nextIndex: this.findClosingTag(nodes, index, 'with')
            };
        }
    }

    private renderPartial(node: TemplateNode, context: TemplateContext, depth: number): string {
        if (!node.expression) {
            return '';
        }

        const partialName = node.expression;
        const partialTemplate = this.engine.getPartial(partialName);

        if (!partialTemplate) {
            throw new Error(`Partial '${partialName}' not found`);
        }

        const compiledPartial = this.engine.compile(partialTemplate);
        return compiledPartial.render(context);
    }

    private findBlockContent(nodes: TemplateNode[], startIndex: number, blockType: string): TemplateNode[] {
        const content: TemplateNode[] = [];
        let depth = 1;
        let i = startIndex + 1;

        while (i < nodes.length && depth > 0) {
            const node = nodes[i];

            if (node.type === 'block') {
                if (node.content.startsWith('#')) {
                    const currentBlockType = node.content.split(' ')[0].substring(1);
                    if (currentBlockType === blockType) {
                        depth++;
                    }
                } else if (node.content.startsWith('/')) {
                    const currentBlockType = node.content.substring(1);
                    if (currentBlockType === blockType) {
                        depth--;
                    }
                }
            }

            if (depth > 0) {
                content.push(node);
            }

            i++;
        }

        return content;
    }

    private findClosingTag(nodes: TemplateNode[], startIndex: number, blockType: string): number {
        let depth = 1;
        let i = startIndex + 1;

        while (i < nodes.length && depth > 0) {
            const node = nodes[i];

            if (node.type === 'block') {
                if (node.content.startsWith('#')) {
                    const currentBlockType = node.content.split(' ')[0].substring(1);
                    if (currentBlockType === blockType) {
                        depth++;
                    }
                } else if (node.content.startsWith('/')) {
                    const currentBlockType = node.content.substring(1);
                    if (currentBlockType === blockType) {
                        depth--;
                    }
                }
            }

            i++;
        }

        return i;
    }
}

class MapTemplateCache implements TemplateCache {
    private cache = new Map<string, CompiledTemplate>();

    get(key: string): CompiledTemplate | undefined {
        return this.cache.get(key);
    }

    set(key: string, template: CompiledTemplate): void {
        this.cache.set(key, template);
    }

    clear(): void {
        this.cache.clear();
    }

    size(): number {
        return this.cache.size;
    }
}
