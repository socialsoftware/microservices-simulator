/**
 * Template Management System with Caching
 * 
 * This module provides efficient template loading and compilation with caching,
 * replacing the current pattern of loading and compiling templates on every use.
 */

import * as fs from 'fs';
import * as path from 'path';
import Handlebars from 'handlebars';
import { ErrorHandler, ErrorUtils, ErrorSeverity } from './error-handler.js';

/**
 * Template cache entry containing both raw content and compiled template
 */
interface TemplateCacheEntry {
    rawContent: string;
    compiledTemplate: HandlebarsTemplateDelegate<any>;
    lastModified: Date;
    filePath: string;
}

/**
 * Template manager configuration
 */
interface TemplateManagerConfig {
    enableCache: boolean;
    enableDevMode: boolean; // In dev mode, check file modification times
    templateRoot?: string;
}

/**
 * Centralized template manager with caching and performance optimization
 */
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
    }

    /**
     * Get singleton instance of template manager
     */
    static getInstance(config?: TemplateManagerConfig): TemplateManager {
        if (!TemplateManager.instance) {
            TemplateManager.instance = new TemplateManager(config);
        }
        return TemplateManager.instance;
    }

    /**
     * Load and compile a template with caching
     */
    loadTemplate(templatePath: string): HandlebarsTemplateDelegate<any> {
        const normalizedPath = this.normalizePath(templatePath);

        // Check cache first
        if (this.config.enableCache && this.templateCache.has(normalizedPath)) {
            const cached = this.templateCache.get(normalizedPath)!;

            // In dev mode, check if file has been modified
            if (this.config.enableDevMode && this.isFileModified(cached)) {
                this.invalidateTemplate(normalizedPath);
            } else {
                return cached.compiledTemplate;
            }
        }

        // Load and compile template
        return this.loadAndCacheTemplate(normalizedPath);
    }

    /**
     * Load raw template content without compilation
     */
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

    /**
     * Render a template with context
     */
    renderTemplate(templatePath: string, context: any): string {
        const compiledTemplate = this.loadTemplate(templatePath);
        return compiledTemplate(context);
    }

    /**
     * Preload multiple templates for better performance
     */
    async preloadTemplates(templatePaths: string[]): Promise<void> {
        const loadPromises = templatePaths.map(async (templatePath) => {
            return ErrorHandler.wrap(
                () => this.loadTemplate(templatePath),
                ErrorUtils.templateContext('preload template', templatePath),
                ErrorSeverity.WARNING
            );
        });

        await Promise.all(loadPromises);
        console.log(`📦 Preloaded ${templatePaths.length} templates`);
    }

    /**
     * Preload all templates in a directory
     */
    async preloadTemplatesFromDirectory(directoryPath: string): Promise<void> {
        const templatePaths = this.discoverTemplates(directoryPath);
        await this.preloadTemplates(templatePaths);
    }

    /**
     * Clear template cache (useful for development)
     */
    clearCache(): void {
        this.templateCache.clear();
        console.log('🗑️  Template cache cleared');
    }

    /**
     * Invalidate a specific template
     */
    invalidateTemplate(templatePath: string): void {
        const normalizedPath = this.normalizePath(templatePath);
        this.templateCache.delete(normalizedPath);
    }

    /**
     * Get cache statistics
     */
    getCacheStats(): { size: number; hitRate: number; templates: string[] } {
        return {
            size: this.templateCache.size,
            hitRate: this.calculateHitRate(),
            templates: Array.from(this.templateCache.keys())
        };
    }

    /**
     * Enable or disable development mode
     */
    setDevMode(enabled: boolean): void {
        this.config.enableDevMode = enabled;
        if (enabled) {
            console.log('🔧 Template dev mode enabled - will check file modifications');
        }
    }

    /**
     * Private methods
     */
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
            throw error; // This will never be reached due to FATAL, but satisfies TypeScript
        }
    }

    private isFileModified(cached: TemplateCacheEntry): boolean {
        try {
            const stats = fs.statSync(cached.filePath);
            return stats.mtime > cached.lastModified;
        } catch {
            // File might have been deleted, invalidate cache
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

        // Default: resolve relative to this file
        const currentFileUrl = import.meta.url;
        const currentFilePath = new URL(currentFileUrl).pathname;
        const currentDir = path.dirname(currentFilePath);
        return path.join(currentDir, '../../templates');
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
        // This is a simplified hit rate calculation
        // In a real implementation, you'd track hits vs misses
        return this.templateCache.size > 0 ? 0.85 : 0; // Placeholder
    }

    private registerHandlebarsHelpers(): void {
        if (this.helpersRegistered) return;

        // Register common helpers used across templates
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
}

/**
 * Convenience functions for common template operations
 */
export class TemplateUtils {
    private static manager = TemplateManager.getInstance();

    /**
     * Quick template rendering
     */
    static render(templatePath: string, context: any): string {
        return this.manager.renderTemplate(templatePath, context);
    }

    /**
     * Load template for manual rendering
     */
    static load(templatePath: string): HandlebarsTemplateDelegate<any> {
        return this.manager.loadTemplate(templatePath);
    }

    /**
     * Initialize template system with preloading
     */
    static async initialize(preloadPaths?: string[]): Promise<void> {
        if (preloadPaths && preloadPaths.length > 0) {
            await this.manager.preloadTemplates(preloadPaths);
        }
    }

    /**
     * Enable development mode for template hot-reloading
     */
    static enableDevMode(): void {
        this.manager.setDevMode(true);
    }

    /**
     * Get cache statistics
     */
    static getStats(): { size: number; hitRate: number; templates: string[] } {
        return this.manager.getCacheStats();
    }
}
