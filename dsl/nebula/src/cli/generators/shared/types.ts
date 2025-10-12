/**
 * Base types for generators
 */

export interface BaseGenerationOptions {
    architecture?: string;
    features?: string[];
    projectName: string;
}

// ===== Template Engine Types =====
// (Moved from template-engine/types.ts)

export interface TemplateContext {
    [key: string]: any;
}

export interface ValidationResult {
    isValid: boolean;
    errors: ValidationError[];
    warnings: ValidationWarning[];
}

export interface ValidationError {
    type: 'syntax' | 'semantic' | 'reference';
    message: string;
    line?: number;
    column?: number;
    position?: number;
}

export interface ValidationWarning {
    type: 'performance' | 'style' | 'deprecation';
    message: string;
    line?: number;
    column?: number;
    position?: number;
}

export interface CompiledTemplate {
    render(context: TemplateContext): string;
    validate(context: TemplateContext): ValidationResult;
    getDependencies(): string[];
    getVariables(): string[];
}

export interface TemplateEngine {
    render(template: string, context: TemplateContext): string;
    validate(template: string): ValidationResult;
    compile(template: string): CompiledTemplate;
    registerHelper(name: string, helper: TemplateHelper): void;
    registerPartial(name: string, partial: string): void;
}

export interface TemplateHelper {
    (context: any, options: HelperOptions): string;
}

export interface HelperOptions {
    fn: (context: any) => string;
    inverse: (context: any) => string;
    hash: { [key: string]: any };
    data: { [key: string]: any };
}

export interface TemplateNode {
    type: 'text' | 'variable' | 'block' | 'comment' | 'partial';
    content: string;
    start: number;
    end: number;
    line: number;
    column: number;
    children?: TemplateNode[];
    expression?: string;
    parameters?: string[];
    hash?: { [key: string]: any };
}

export interface TemplateAST {
    nodes: TemplateNode[];
    source: string;
}

export interface TemplateOptions {
    strictMode?: boolean;
    allowUnsafe?: boolean;
    maxDepth?: number;
    maxIterations?: number;
    timeout?: number;
}

export interface TemplateCache {
    get(key: string): CompiledTemplate | undefined;
    set(key: string, template: CompiledTemplate): void;
    clear(): void;
    size(): number;
}

export interface TemplateMetrics {
    renderTime: number;
    compileTime: number;
    cacheHits: number;
    cacheMisses: number;
    totalRenders: number;
    totalCompiles: number;
}
