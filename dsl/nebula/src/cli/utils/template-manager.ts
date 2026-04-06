


import * as fs from 'fs';
import * as path from 'path';
import { fileURLToPath } from 'url';
import Handlebars from 'handlebars';
import { ErrorHandler, ErrorUtils, ErrorSeverity } from './error-handler.js';



interface TemplateCacheEntry {
    rawContent: string;
    compiledTemplate: HandlebarsTemplateDelegate<any>;
    lastModified: Date;
    filePath: string;
}



interface TemplateManagerConfig {
    enableCache: boolean;
    enableDevMode: boolean; 
    templateRoot?: string;
}



export class TemplateManager {
    private static instance: TemplateManager;
    private templateCache = new Map<string, TemplateCacheEntry>();
    private templateRoot: string;
    private config: TemplateManagerConfig;
    private helpersRegistered = false;

    private constructor(config: TemplateManagerConfig = { enableCache: true, enableDevMode: false }) {
        this.config = config;
        this.templateRoot = this.resolveTemplateRoot(config.templateRoot);
        this.registerHandlebarsHelpers();
        this.registerPartials();
    }

    

    static getInstance(config?: TemplateManagerConfig): TemplateManager {
        if (!TemplateManager.instance) {
            TemplateManager.instance = new TemplateManager(config);
        }
        return TemplateManager.instance;
    }

    

    loadTemplate(templatePath: string): HandlebarsTemplateDelegate<any> {
        const normalizedPath = this.normalizePath(templatePath);

        
        if (this.config.enableCache && this.templateCache.has(normalizedPath)) {
            const cached = this.templateCache.get(normalizedPath)!;

            
            if (this.config.enableDevMode && this.isFileModified(cached)) {
                this.invalidateTemplate(normalizedPath);
            } else {
                return cached.compiledTemplate;
            }
        }

        
        return this.loadAndCacheTemplate(normalizedPath);
    }

    

    loadRawTemplate(templatePath: string): string {
        const normalizedPath = this.normalizePath(templatePath);

        if (this.config.enableCache && this.templateCache.has(normalizedPath)) {
            const cached = this.templateCache.get(normalizedPath)!;

            if (this.config.enableDevMode && this.isFileModified(cached)) {
                this.invalidateTemplate(normalizedPath);
            } else {
                return cached.rawContent;
            }
        }

        this.loadAndCacheTemplate(normalizedPath);
        return this.templateCache.get(normalizedPath)!.rawContent;
    }

    

    renderTemplate(templatePath: string, context: any): string {
        const compiledTemplate = this.loadTemplate(templatePath);
        return compiledTemplate(context);
    }

    

    async preloadTemplates(templatePaths: string[]): Promise<void> {
        const loadPromises = templatePaths.map(async (templatePath) => {
            return ErrorHandler.wrap(
                () => this.loadTemplate(templatePath),
                ErrorUtils.templateContext('preload template', templatePath),
                ErrorSeverity.WARNING
            );
        });

        await Promise.all(loadPromises);
    }

    

    async preloadTemplatesFromDirectory(directoryPath: string): Promise<void> {
        const templatePaths = this.discoverTemplates(directoryPath);
        await this.preloadTemplates(templatePaths);
    }

    

    clearCache(): void {
        this.templateCache.clear();
    }

    

    invalidateTemplate(templatePath: string): void {
        const normalizedPath = this.normalizePath(templatePath);
        this.templateCache.delete(normalizedPath);
    }

    

    getCacheStats(): { size: number; hitRate: number; templates: string[] } {
        return {
            size: this.templateCache.size,
            hitRate: this.calculateHitRate(),
            templates: Array.from(this.templateCache.keys())
        };
    }

    

    setDevMode(enabled: boolean): void {
        this.config.enableDevMode = enabled;
    }

    

    private loadAndCacheTemplate(normalizedPath: string): HandlebarsTemplateDelegate<any> {
        const fullPath = path.join(this.templateRoot, normalizedPath);

        try {
            const rawContent = fs.readFileSync(fullPath, 'utf-8');
            const compiledTemplate = Handlebars.compile(rawContent, { noEscape: true });
            const stats = fs.statSync(fullPath);

            const cacheEntry: TemplateCacheEntry = {
                rawContent,
                compiledTemplate,
                lastModified: stats.mtime,
                filePath: fullPath
            };

            if (this.config.enableCache) {
                this.templateCache.set(normalizedPath, cacheEntry);
            }

            return compiledTemplate;
        } catch (error) {
            ErrorHandler.handle(
                error instanceof Error ? error : new Error(String(error)),
                ErrorUtils.templateContext('load template', normalizedPath, { fullPath }),
                ErrorSeverity.FATAL
            );
            throw error; 
        }
    }

    private isFileModified(cached: TemplateCacheEntry): boolean {
        try {
            const stats = fs.statSync(cached.filePath);
            return stats.mtime > cached.lastModified;
        } catch {
            
            return true;
        }
    }

    private normalizePath(templatePath: string): string {
        return templatePath.replace(/\\/g, '/');
    }

    private resolveTemplateRoot(customRoot?: string): string {
        if (customRoot) {
            return path.resolve(customRoot);
        }

        
        const currentFileUrl = import.meta.url;
        const currentFilePath = fileURLToPath(currentFileUrl);
        const currentDir = path.dirname(currentFilePath);
        
        return path.join(currentDir, '../templates');
    }

    private discoverTemplates(directoryPath: string): string[] {
        const templates: string[] = [];
        const fullDirPath = path.join(this.templateRoot, directoryPath);

        try {
            const discoverRecursive = (dir: string, relativePath: string = '') => {
                const items = fs.readdirSync(dir);

                for (const item of items) {
                    const itemPath = path.join(dir, item);
                    const relativeItemPath = path.join(relativePath, item);

                    if (fs.statSync(itemPath).isDirectory()) {
                        discoverRecursive(itemPath, relativeItemPath);
                    } else if (item.endsWith('.hbs')) {
                        templates.push(path.join(directoryPath, relativeItemPath).replace(/\\/g, '/'));
                    }
                }
            };

            discoverRecursive(fullDirPath);
        } catch (error) {
            ErrorHandler.handle(
                error instanceof Error ? error : new Error(String(error)),
                ErrorUtils.templateContext('discover templates', directoryPath),
                ErrorSeverity.WARNING,
                false
            );
        }

        return templates;
    }

    private calculateHitRate(): number {
        
        
        return this.templateCache.size > 0 ? 0.85 : 0; 
    }

    private registerHandlebarsHelpers(): void {
        if (this.helpersRegistered) return;

        
        Handlebars.registerHelper('eq', function (a: any, b: any): boolean {
            return a === b;
        });

        Handlebars.registerHelper('ne', function (a: any, b: any): boolean {
            return a !== b;
        });

        Handlebars.registerHelper('lt', function (a: any, b: any): boolean {
            return a < b;
        });

        Handlebars.registerHelper('gt', function (a: any, b: any): boolean {
            return a > b;
        });

        Handlebars.registerHelper('lte', function (a: any, b: any): boolean {
            return a <= b;
        });

        Handlebars.registerHelper('gte', function (a: any, b: any): boolean {
            return a >= b;
        });

        Handlebars.registerHelper('and', function (a: any, b: any): boolean {
            return !!(a && b);
        });

        Handlebars.registerHelper('or', function (a: any, b: any): boolean {
            return !!(a || b);
        });

        Handlebars.registerHelper('not', function (a: any): boolean {
            return !a;
        });

        Handlebars.registerHelper('capitalize', function (str: string): string {
            if (!str) return '';
            return str.charAt(0).toUpperCase() + str.slice(1);
        });

        Handlebars.registerHelper('lowercase', function (str: string): string {
            return str ? str.toLowerCase() : '';
        });

        Handlebars.registerHelper('uppercase', function (str: string): string {
            return str ? str.toUpperCase() : '';
        });

        Handlebars.registerHelper('json', function (context: any): string {
            return JSON.stringify(context);
        });

        Handlebars.registerHelper('length', function (array: any[]): number {
            return Array.isArray(array) ? array.length : 0;
        });

        this.helpersRegistered = true;
    }

    private registerPartials(): void {
        const partialsDir = path.join(this.templateRoot, '_partials');

        if (!fs.existsSync(partialsDir)) {
            return; 
        }

        try {
            const partialFiles = fs.readdirSync(partialsDir)
                .filter(file => file.endsWith('.hbs'));

            partialFiles.forEach(file => {
                const partialName = file.replace('.hbs', '');
                const partialPath = path.join(partialsDir, file);
                const partialTemplate = fs.readFileSync(partialPath, 'utf-8');
                Handlebars.registerPartial(partialName, partialTemplate);
            });

        } catch (error) {
            ErrorHandler.handle(
                error instanceof Error ? error : new Error(String(error)),
                ErrorUtils.templateContext('register partials', partialsDir),
                ErrorSeverity.WARNING,
                false
            );
        }
    }
}



export class TemplateUtils {
    private static manager = TemplateManager.getInstance();

    

    static render(templatePath: string, context: any): string {
        return this.manager.renderTemplate(templatePath, context);
    }

    

    static load(templatePath: string): HandlebarsTemplateDelegate<any> {
        return this.manager.loadTemplate(templatePath);
    }

    

    static async initialize(preloadPaths?: string[]): Promise<void> {
        if (preloadPaths && preloadPaths.length > 0) {
            await this.manager.preloadTemplates(preloadPaths);
        }
    }

    

    static enableDevMode(): void {
        this.manager.setDevMode(true);
    }

    

    static getStats(): { size: number; hitRate: number; templates: string[] } {
        return this.manager.getCacheStats();
    }
}
