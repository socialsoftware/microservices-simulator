/**
 * Batch Processing System
 * 
 * This module provides high-performance batch operations for file writing,
 * template rendering, and directory creation to optimize generation performance
 * for large projects with many files.
 */

import * as fs from 'node:fs/promises';
import * as path from 'node:path';
import { TemplateManager } from './template-manager.js';

/**
 * File operation definition
 */
export interface FileOperation {
    filePath: string;
    content: string;
    description: string;
    metadata?: Record<string, any>;
}

/**
 * Template rendering operation
 */
export interface TemplateOperation {
    templatePath: string;
    context: any;
    outputPath: string;
    description: string;
}

/**
 * Directory creation operation
 */
export interface DirectoryOperation {
    path: string;
    recursive: boolean;
}

/**
 * Batch operation result
 */
export interface BatchResult {
    successful: number;
    failed: number;
    total: number;
    errors: string[];
    duration: number;
}

/**
 * Progress tracking callback
 */
export type ProgressCallback = (completed: number, total: number, current?: string) => void;

/**
 * Batch processor for high-performance file operations
 */
export class BatchProcessor {
    private concurrencyLimit: number;
    private enableProgressTracking: boolean;

    constructor(concurrencyLimit: number = 10, enableProgressTracking: boolean = true) {
        this.concurrencyLimit = concurrencyLimit;
        this.enableProgressTracking = enableProgressTracking;
    }

    /**
     * Write multiple files in parallel with concurrency control
     */
    async writeFiles(
        operations: FileOperation[],
        progressCallback?: ProgressCallback
    ): Promise<BatchResult> {
        const startTime = Date.now();
        console.log(`📁 Starting batch write of ${operations.length} files...`);

        const results = await this.processBatch(
            operations,
            async (operation) => this.writeFile(operation),
            progressCallback,
            'file writing'
        );

        const duration = Date.now() - startTime;
        console.log(`✅ Batch write completed: ${results.successful}/${results.total} files in ${duration}ms`);

        return { ...results, duration };
    }

    /**
     * Render multiple templates in parallel
     */
    async renderTemplates(
        operations: TemplateOperation[],
        progressCallback?: ProgressCallback
    ): Promise<BatchResult> {
        const startTime = Date.now();
        console.log(`🎨 Starting batch template rendering of ${operations.length} templates...`);

        const results = await this.processBatch(
            operations,
            async (operation) => this.renderTemplate(operation),
            progressCallback,
            'template rendering'
        );

        const duration = Date.now() - startTime;
        console.log(`✅ Batch rendering completed: ${results.successful}/${results.total} templates in ${duration}ms`);

        return { ...results, duration };
    }

    /**
     * Create multiple directories in parallel
     */
    async createDirectories(
        operations: DirectoryOperation[],
        progressCallback?: ProgressCallback
    ): Promise<BatchResult> {
        const startTime = Date.now();
        console.log(`📂 Starting batch directory creation of ${operations.length} directories...`);

        // Deduplicate directories to avoid conflicts
        const uniqueOperations = this.deduplicateDirectories(operations);

        const results = await this.processBatch(
            uniqueOperations,
            async (operation) => this.createDirectory(operation),
            progressCallback,
            'directory creation'
        );

        const duration = Date.now() - startTime;
        console.log(`✅ Batch directory creation completed: ${results.successful}/${results.total} directories in ${duration}ms`);

        return { ...results, duration };
    }

    /**
     * Combined operation: render templates and write files
     */
    async renderAndWriteTemplates(
        operations: TemplateOperation[],
        progressCallback?: ProgressCallback
    ): Promise<BatchResult> {
        const startTime = Date.now();
        console.log(`🎨📁 Starting batch render-and-write of ${operations.length} templates...`);

        const results = await this.processBatch(
            operations,
            async (operation) => this.renderAndWriteTemplate(operation),
            progressCallback,
            'render and write'
        );

        const duration = Date.now() - startTime;
        console.log(`✅ Batch render-and-write completed: ${results.successful}/${results.total} templates in ${duration}ms`);

        return { ...results, duration };
    }

    /**
     * Optimized directory creation that groups by parent directories
     */
    async createDirectoriesOptimized(filePaths: string[]): Promise<BatchResult> {
        const startTime = Date.now();

        // Extract unique directories from file paths
        const directories = new Set<string>();
        filePaths.forEach(filePath => {
            const dir = path.dirname(filePath);
            directories.add(dir);
        });

        // Sort directories by depth (create parent directories first)
        const sortedDirectories = Array.from(directories).sort((a, b) => {
            const depthA = a.split(path.sep).length;
            const depthB = b.split(path.sep).length;
            return depthA - depthB;
        });

        const operations: DirectoryOperation[] = sortedDirectories.map(dir => ({
            path: dir,
            recursive: true
        }));

        const results = await this.createDirectories(operations);

        const duration = Date.now() - startTime;
        console.log(`🚀 Optimized directory creation: ${results.successful} directories in ${duration}ms`);

        return { ...results, duration };
    }

    /**
     * Generic batch processor with concurrency control
     */
    private async processBatch<T>(
        operations: T[],
        processor: (operation: T) => Promise<void>,
        progressCallback?: ProgressCallback,
        operationType: string = 'operation'
    ): Promise<Omit<BatchResult, 'duration'>> {
        const total = operations.length;
        let completed = 0;
        let successful = 0;
        const errors: string[] = [];

        // Process operations in batches with concurrency limit
        for (let i = 0; i < operations.length; i += this.concurrencyLimit) {
            const batch = operations.slice(i, i + this.concurrencyLimit);

            const batchPromises = batch.map(async (operation, batchIndex) => {
                try {
                    await processor(operation);
                    successful++;

                    if (this.enableProgressTracking && progressCallback) {
                        const currentDescription = this.getOperationDescription(operation);
                        progressCallback(completed + batchIndex + 1, total, currentDescription);
                    }
                } catch (error) {
                    const errorMessage = `${operationType} failed: ${error instanceof Error ? error.message : String(error)}`;
                    errors.push(errorMessage);
                }
            });

            await Promise.all(batchPromises);
            completed += batch.length;

            // Progress logging for large batches
            if (total > 20 && i + this.concurrencyLimit < total) {
                console.log(`  📊 Progress: ${completed}/${total} ${operationType}s completed`);
            }
        }

        return {
            successful,
            failed: total - successful,
            total,
            errors
        };
    }

    /**
     * Individual operation processors
     */
    private async writeFile(operation: FileOperation): Promise<void> {
        await fs.mkdir(path.dirname(operation.filePath), { recursive: true });
        await fs.writeFile(operation.filePath, operation.content, 'utf-8');

        if (this.enableProgressTracking) {
            console.log(`\t- Generated ${operation.description}`);
        }
    }

    private async renderTemplate(operation: TemplateOperation): Promise<void> {
        const templateManager = TemplateManager.getInstance();
        const rendered = templateManager.renderTemplate(operation.templatePath, operation.context);

        // Store rendered content (this would typically be used with writeFiles)
        (operation as any).renderedContent = rendered;
    }

    private async renderAndWriteTemplate(operation: TemplateOperation): Promise<void> {
        const templateManager = TemplateManager.getInstance();
        const rendered = templateManager.renderTemplate(operation.templatePath, operation.context);

        await fs.mkdir(path.dirname(operation.outputPath), { recursive: true });
        await fs.writeFile(operation.outputPath, rendered, 'utf-8');

        if (this.enableProgressTracking) {
            console.log(`\t- Generated ${operation.description}`);
        }
    }

    private async createDirectory(operation: DirectoryOperation): Promise<void> {
        await fs.mkdir(operation.path, { recursive: operation.recursive });
    }

    /**
     * Utility methods
     */
    private deduplicateDirectories(operations: DirectoryOperation[]): DirectoryOperation[] {
        const seen = new Set<string>();
        return operations.filter(op => {
            if (seen.has(op.path)) {
                return false;
            }
            seen.add(op.path);
            return true;
        });
    }

    private getOperationDescription(operation: any): string {
        if (operation.description) return operation.description;
        if (operation.filePath) return path.basename(operation.filePath);
        if (operation.outputPath) return path.basename(operation.outputPath);
        if (operation.path) return path.basename(operation.path);
        return 'unknown operation';
    }

    /**
     * Configuration methods
     */
    setConcurrencyLimit(limit: number): void {
        this.concurrencyLimit = Math.max(1, limit);
    }

    setProgressTracking(enabled: boolean): void {
        this.enableProgressTracking = enabled;
    }

    getConcurrencyLimit(): number {
        return this.concurrencyLimit;
    }
}

/**
 * Factory for creating batch processors with different configurations
 */
export class BatchProcessorFactory {
    /**
     * Create a high-performance batch processor
     */
    static createHighPerformance(): BatchProcessor {
        return new BatchProcessor(20, false); // High concurrency, no individual progress
    }

    /**
     * Create a standard batch processor
     */
    static createStandard(): BatchProcessor {
        return new BatchProcessor(10, true); // Balanced concurrency with progress
    }

    /**
     * Create a conservative batch processor (for limited resources)
     */
    static createConservative(): BatchProcessor {
        return new BatchProcessor(5, true); // Lower concurrency with progress
    }

    /**
     * Create a batch processor with custom settings
     */
    static createCustom(concurrencyLimit: number, enableProgressTracking: boolean): BatchProcessor {
        return new BatchProcessor(concurrencyLimit, enableProgressTracking);
    }
}

/**
 * Utility functions for batch operations
 */
export class BatchUtils {
    /**
     * Convert file paths to directory operations
     */
    static extractDirectoryOperations(filePaths: string[]): DirectoryOperation[] {
        const directories = new Set<string>();

        filePaths.forEach(filePath => {
            let currentDir = path.dirname(filePath);
            while (currentDir !== '.' && currentDir !== '/') {
                directories.add(currentDir);
                currentDir = path.dirname(currentDir);
            }
        });

        return Array.from(directories).map(dir => ({
            path: dir,
            recursive: true
        }));
    }

    /**
     * Group file operations by directory for optimized processing
     */
    static groupFileOperationsByDirectory(operations: FileOperation[]): Map<string, FileOperation[]> {
        const grouped = new Map<string, FileOperation[]>();

        operations.forEach(operation => {
            const dir = path.dirname(operation.filePath);
            const existing = grouped.get(dir) || [];
            existing.push(operation);
            grouped.set(dir, existing);
        });

        return grouped;
    }

    /**
     * Estimate batch processing time based on operation count and type
     */
    static estimateProcessingTime(
        operationCount: number,
        operationType: 'file' | 'template' | 'directory',
        concurrencyLimit: number = 10
    ): number {
        // Rough estimates in milliseconds
        const baseTimePerOperation = {
            'file': 50,      // File writing
            'template': 100, // Template rendering + writing
            'directory': 20  // Directory creation
        };

        const baseTime = baseTimePerOperation[operationType];
        const parallelBatches = Math.ceil(operationCount / concurrencyLimit);

        return parallelBatches * baseTime;
    }

    /**
     * Create progress callback that logs every N operations
     */
    static createProgressLogger(logInterval: number = 10): ProgressCallback {
        return (completed: number, total: number, current?: string) => {
            if (completed % logInterval === 0 || completed === total) {
                const percentage = Math.round((completed / total) * 100);
                const currentInfo = current ? ` (${current})` : '';
                console.log(`  📊 Progress: ${completed}/${total} (${percentage}%)${currentInfo}`);
            }
        };
    }
}
