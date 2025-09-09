/**
 * Template storage system for managing code generation templates
 */

import { promises as fs } from 'fs';
import * as path from 'path';
import { DefaultTemplateEngine } from './engine.js';
import { TemplateContext } from './types.js';

export interface TemplateMetadata {
    name: string;
    version: string;
    description?: string;
    author?: string;
    created: Date;
    modified: Date;
    tags: string[];
    dependencies: string[];
    architecture: string;
    component: string;
    category: string;
}

export interface TemplateInfo {
    metadata: TemplateMetadata;
    content: string;
    path: string;
}

export interface TemplateStorageOptions {
    templatesPath: string;
    cacheEnabled: boolean;
    autoReload: boolean;
    maxCacheSize: number;
}

export class Storage {
    private templatesPath: string;
    private cache: Map<string, TemplateInfo> = new Map();
    private cacheEnabled: boolean;
    private autoReload: boolean;
    private maxCacheSize: number;
    // private watchers: Map<string, any> = new Map();

    constructor(options: TemplateStorageOptions) {
        this.templatesPath = options.templatesPath;
        this.cacheEnabled = options.cacheEnabled ?? true;
        this.autoReload = options.autoReload ?? true;
        this.maxCacheSize = options.maxCacheSize ?? 1000;
    }

    /**
     * Initialize the template storage system
     */
    async initialize(): Promise<void> {
        // Ensure templates directory exists
        await this.ensureTemplatesDirectory();

        // Load all templates
        await this.loadAllTemplates();

        // Set up file watchers if auto-reload is enabled
        if (this.autoReload) {
            await this.setupFileWatchers();
        }
    }

    /**
     * Get a template by name and architecture
     */
    async getTemplate(name: string, architecture: string = 'default'): Promise<TemplateInfo | null> {
        const key = this.getTemplateKey(name, architecture);

        if (this.cacheEnabled && this.cache.has(key)) {
            return this.cache.get(key)!;
        }

        const templatePath = this.getTemplatePath(name, architecture);

        try {
            const templateInfo = await this.loadTemplate(templatePath);
            if (templateInfo) {
                if (this.cacheEnabled) {
                    this.cache.set(key, templateInfo);
                    this.enforceCacheLimit();
                }
                return templateInfo;
            }
        } catch (error) {
            console.warn(`Failed to load template ${name} for architecture ${architecture}:`, error);
        }

        return null;
    }

    /**
     * Get all templates for a specific architecture
     */
    async getTemplatesForArchitecture(architecture: string): Promise<TemplateInfo[]> {
        const templates: TemplateInfo[] = [];
        const architecturePath = path.join(this.templatesPath, architecture);

        try {
            const entries = await fs.readdir(architecturePath, { withFileTypes: true });

            for (const entry of entries) {
                if (entry.isFile() && entry.name.endsWith('.hbs')) {
                    const templatePath = path.join(architecturePath, entry.name);
                    const templateInfo = await this.loadTemplate(templatePath);
                    if (templateInfo) {
                        templates.push(templateInfo);
                    }
                }
            }
        } catch (error) {
            console.warn(`Failed to load templates for architecture ${architecture}:`, error);
        }

        return templates;
    }

    /**
     * Get all available architectures
     */
    async getArchitectures(): Promise<string[]> {
        try {
            const entries = await fs.readdir(this.templatesPath, { withFileTypes: true });
            return entries
                .filter(entry => entry.isDirectory())
                .map(entry => entry.name);
        } catch (error) {
            console.warn('Failed to load architectures:', error);
            return [];
        }
    }

    /**
     * Get all available components for an architecture
     */
    async getComponents(architecture: string): Promise<string[]> {
        const components = new Set<string>();
        const templates = await this.getTemplatesForArchitecture(architecture);

        for (const template of templates) {
            components.add(template.metadata.component);
        }

        return Array.from(components);
    }

    /**
     * Save a template
     */
    async saveTemplate(templateInfo: TemplateInfo): Promise<void> {
        const templatePath = this.getTemplatePath(templateInfo.metadata.name, templateInfo.metadata.architecture);

        // Ensure directory exists
        await fs.mkdir(path.dirname(templatePath), { recursive: true });

        // Write template content
        await fs.writeFile(templatePath, templateInfo.content, 'utf-8');

        // Update metadata
        await this.saveTemplateMetadata(templateInfo);

        // Update cache
        if (this.cacheEnabled) {
            const key = this.getTemplateKey(templateInfo.metadata.name, templateInfo.metadata.architecture);
            this.cache.set(key, templateInfo);
            this.enforceCacheLimit();
        }
    }

    /**
     * Delete a template
     */
    async deleteTemplate(name: string, architecture: string): Promise<void> {
        const templatePath = this.getTemplatePath(name, architecture);
        const metadataPath = this.getMetadataPath(name, architecture);

        // Remove files
        try {
            await fs.unlink(templatePath);
        } catch (error) {
            // File might not exist
        }

        try {
            await fs.unlink(metadataPath);
        } catch (error) {
            // Metadata might not exist
        }

        // Remove from cache
        if (this.cacheEnabled) {
            const key = this.getTemplateKey(name, architecture);
            this.cache.delete(key);
        }
    }

    /**
     * Check if a template exists
     */
    async templateExists(name: string, architecture: string): Promise<boolean> {
        const templatePath = this.getTemplatePath(name, architecture);
        try {
            await fs.access(templatePath);
            return true;
        } catch {
            return false;
        }
    }

    /**
     * Get template dependencies
     */
    async getTemplateDependencies(name: string, architecture: string): Promise<string[]> {
        const template = await this.getTemplate(name, architecture);
        if (!template) {
            return [];
        }

        return template.metadata.dependencies;
    }

    /**
     * Resolve template inheritance chain
     */
    async resolveTemplateInheritance(name: string, architecture: string): Promise<TemplateInfo[]> {
        const chain: TemplateInfo[] = [];
        const visited = new Set<string>();

        let currentName = name;
        let currentArchitecture = architecture;

        while (currentName && !visited.has(`${currentArchitecture}:${currentName}`)) {
            visited.add(`${currentArchitecture}:${currentName}`);

            const template = await this.getTemplate(currentName, currentArchitecture);
            if (!template) {
                break;
            }

            chain.push(template);

            // Check for inheritance in metadata
            const parentName = template.metadata.dependencies.find(dep => dep.startsWith('extends:'));
            if (parentName) {
                const parentInfo = parentName.substring(8); // Remove 'extends:' prefix
                const [parentArch, parentTemplate] = parentInfo.includes(':')
                    ? parentInfo.split(':')
                    : [currentArchitecture, parentInfo];
                currentName = parentTemplate;
                currentArchitecture = parentArch;
            } else {
                break;
            }
        }

        return chain;
    }

    /**
     * Clear template cache
     */
    clearCache(): void {
        this.cache.clear();
    }

    /**
     * Get cache statistics
     */
    getCacheStats(): { size: number; maxSize: number; hitRate: number } {
        return {
            size: this.cache.size,
            maxSize: this.maxCacheSize,
            hitRate: 0 // TODO: Implement hit rate tracking
        };
    }

    /**
     * Ensure templates directory exists
     */
    private async ensureTemplatesDirectory(): Promise<void> {
        try {
            await fs.mkdir(this.templatesPath, { recursive: true });
        } catch (error) {
            throw new Error(`Failed to create templates directory: ${error}`);
        }
    }

    /**
     * Load all templates into cache
     */
    private async loadAllTemplates(): Promise<void> {
        try {
            const architectures = await this.getArchitectures();

            for (const architecture of architectures) {
                const templates = await this.getTemplatesForArchitecture(architecture);

                for (const template of templates) {
                    const key = this.getTemplateKey(template.metadata.name, architecture);
                    this.cache.set(key, template);
                }
            }

            this.enforceCacheLimit();
        } catch (error) {
            console.warn('Failed to load all templates:', error);
        }
    }

    /**
     * Load a template from file
     */
    private async loadTemplate(templatePath: string): Promise<TemplateInfo | null> {
        try {
            const content = await fs.readFile(templatePath, 'utf-8');
            const metadata = await this.loadTemplateMetadata(templatePath);

            return {
                metadata,
                content,
                path: templatePath
            };
        } catch (error) {
            console.warn(`Failed to load template from ${templatePath}:`, error);
            return null;
        }
    }

    /**
     * Load template metadata
     */
    private async loadTemplateMetadata(templatePath: string): Promise<TemplateMetadata> {
        const metadataPath = this.getMetadataPathFromTemplatePath(templatePath);

        try {
            const metadataContent = await fs.readFile(metadataPath, 'utf-8');
            const metadata = JSON.parse(metadataContent);

            // Ensure required fields have defaults
            return {
                name: metadata.name || path.basename(templatePath, '.hbs'),
                version: metadata.version || '1.0.0',
                description: metadata.description || '',
                author: metadata.author || '',
                created: new Date(metadata.created || Date.now()),
                modified: new Date(metadata.modified || Date.now()),
                tags: metadata.tags || [],
                dependencies: metadata.dependencies || [],
                architecture: metadata.architecture || 'default',
                component: metadata.component || 'unknown',
                category: metadata.category || 'general'
            };
        } catch (error) {
            // Return default metadata if file doesn't exist or is invalid
            const fileName = path.basename(templatePath, '.hbs');
            const architecture = path.basename(path.dirname(templatePath));

            return {
                name: fileName,
                version: '1.0.0',
                description: '',
                author: '',
                created: new Date(),
                modified: new Date(),
                tags: [],
                dependencies: [],
                architecture,
                component: 'unknown',
                category: 'general'
            };
        }
    }

    /**
     * Save template metadata
     */
    private async saveTemplateMetadata(templateInfo: TemplateInfo): Promise<void> {
        const metadataPath = this.getMetadataPath(templateInfo.metadata.name, templateInfo.metadata.architecture);

        // Update modified date
        templateInfo.metadata.modified = new Date();

        const metadataContent = JSON.stringify(templateInfo.metadata, null, 2);
        await fs.writeFile(metadataPath, metadataContent, 'utf-8');
    }

    /**
     * Set up file watchers for auto-reload
     */
    private async setupFileWatchers(): Promise<void> {
        // TODO: Implement file watching for auto-reload
        // This would require additional dependencies like chokidar
        console.log('File watchers not implemented yet');
    }

    /**
     * Get template key for cache
     */
    private getTemplateKey(name: string, architecture: string): string {
        return `${architecture}:${name}`;
    }

    /**
     * Get template file path
     */
    private getTemplatePath(name: string, architecture: string): string {
        return path.join(this.templatesPath, architecture, `${name}.hbs`);
    }

    /**
     * Get metadata file path
     */
    private getMetadataPath(name: string, architecture: string): string {
        return path.join(this.templatesPath, architecture, `${name}.json`);
    }

    /**
     * Get metadata path from template path
     */
    private getMetadataPathFromTemplatePath(templatePath: string): string {
        return templatePath.replace('.hbs', '.json');
    }

    /**
     * Enforce cache size limit
     */
    private enforceCacheLimit(): void {
        if (this.cache.size > this.maxCacheSize) {
            // Remove oldest entries (simple LRU would be better)
            const entries = Array.from(this.cache.entries());
            const toRemove = entries.slice(0, this.cache.size - this.maxCacheSize);

            for (const [key] of toRemove) {
                this.cache.delete(key);
            }
        }
    }
}

/**
 * Template manager that combines storage with engine
 */
export class Manager {
    private storage: Storage;
    private engine: DefaultTemplateEngine;

    constructor(storage: Storage, engine: DefaultTemplateEngine) {
        this.storage = storage;
        this.engine = engine;
    }

    /**
     * Render a template by name
     */
    async renderTemplate(name: string, context: TemplateContext, architecture: string = 'default'): Promise<string> {
        const template = await this.storage.getTemplate(name, architecture);
        if (!template) {
            throw new Error(`Template '${name}' not found for architecture '${architecture}'`);
        }

        return this.engine.render(template.content, context);
    }

    /**
     * Render a template with inheritance
     */
    async renderTemplateWithInheritance(name: string, context: TemplateContext, architecture: string = 'default'): Promise<string> {
        const inheritanceChain = await this.storage.resolveTemplateInheritance(name, architecture);

        if (inheritanceChain.length === 0) {
            throw new Error(`Template '${name}' not found for architecture '${architecture}'`);
        }

        // Start with the base template and apply inheritance
        let result = inheritanceChain[0].content;

        for (let i = 1; i < inheritanceChain.length; i++) {
            const parentTemplate = inheritanceChain[i];
            // Simple inheritance: replace {{> content}} with the child template
            result = result.replace(/\{\{>\s*content\s*\}\}/g, result);
            result = parentTemplate.content;
        }

        return this.engine.render(result, context);
    }

    /**
     * Get template manager instance
     */
    static async create(templatesPath: string, options: Partial<TemplateStorageOptions> = {}): Promise<Manager> {
        const storageOptions: TemplateStorageOptions = {
            templatesPath,
            cacheEnabled: true,
            autoReload: false,
            maxCacheSize: 1000,
            ...options
        };

        const storage = new Storage(storageOptions);
        const engine = new DefaultTemplateEngine();

        await storage.initialize();

        return new Manager(storage, engine);
    }
}
