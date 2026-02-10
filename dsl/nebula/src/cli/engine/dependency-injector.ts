import { Generator } from "../generators/base/generator-interface.js";

/**
 * Service definition for dependency injection
 */
export interface ServiceDefinition<T = any> {
    name: string;
    factory: () => T;
    singleton: boolean;
    dependencies?: string[];
}

/**
 * Generator factory function type
 */
export type GeneratorFactory<T extends Generator = Generator> = (dependencies: any[]) => T;

/**
 * Validation result for dependency resolution
 */
export interface ValidationResult {
    valid: boolean;
    errors: string[];
    warnings?: string[];
}

/**
 * Dependency Injection container for generators and services
 *
 * Provides:
 * - Service registration (singletons and transients)
 * - Generator factory registration
 * - Circular dependency detection
 * - Lazy initialization
 * - Dependency graph visualization
 */
export class DependencyInjector {
    private services = new Map<string, any>();
    private serviceDefinitions = new Map<string, ServiceDefinition>();
    private generatorFactories = new Map<string, GeneratorFactory>();
    private generatorInstances = new Map<string, Generator>();
    private generatorMetadata = new Map<string, { dependencies: string[] }>();

    /**
     * Register a service (singleton or transient)
     */
    registerService<T>(definition: ServiceDefinition<T>): void {
        this.serviceDefinitions.set(definition.name, definition);

        // If singleton, create instance immediately if no dependencies
        if (definition.singleton && (!definition.dependencies || definition.dependencies.length === 0)) {
            const instance = definition.factory();
            this.services.set(definition.name, instance);
        }
    }

    /**
     * Register a generator factory
     */
    registerGenerator<T extends Generator>(
        name: string,
        factory: GeneratorFactory<T>,
        dependencies: string[] = []
    ): void {
        this.generatorFactories.set(name, factory);
        this.generatorMetadata.set(name, { dependencies });
    }

    /**
     * Register a pre-created service instance
     */
    registerInstance<T>(name: string, instance: T): void {
        this.services.set(name, instance);
    }

    /**
     * Resolve a service by name
     */
    resolveService<T>(name: string): T {
        // Check if already instantiated
        if (this.services.has(name)) {
            return this.services.get(name) as T;
        }

        // Get service definition
        const definition = this.serviceDefinitions.get(name);
        if (!definition) {
            throw new Error(`Service '${name}' not registered`);
        }

        // Resolve dependencies first
        const dependencies: any[] = [];
        if (definition.dependencies) {
            for (const dep of definition.dependencies) {
                dependencies.push(this.resolveService(dep));
            }
        }

        // Create instance
        const instance = definition.factory();

        // Cache if singleton
        if (definition.singleton) {
            this.services.set(name, instance);
        }

        return instance as T;
    }

    /**
     * Resolve a generator by name
     */
    resolveGenerator<T extends Generator>(name: string): T {
        // Check if already instantiated
        if (this.generatorInstances.has(name)) {
            return this.generatorInstances.get(name) as T;
        }

        // Get generator factory
        const factory = this.generatorFactories.get(name);
        if (!factory) {
            throw new Error(`Generator '${name}' not registered`);
        }

        // Get metadata
        const metadata = this.generatorMetadata.get(name);
        if (!metadata) {
            throw new Error(`Generator metadata for '${name}' not found`);
        }

        // Resolve dependencies recursively
        const dependencies: any[] = [];
        for (const dep of metadata.dependencies) {
            // Check if it's a service or generator
            if (this.serviceDefinitions.has(dep) || this.services.has(dep)) {
                dependencies.push(this.resolveService(dep));
            } else if (this.generatorFactories.has(dep)) {
                dependencies.push(this.resolveGenerator(dep));
            } else {
                throw new Error(`Dependency '${dep}' for generator '${name}' not found`);
            }
        }

        // Create generator instance
        const generator = factory(dependencies);

        // Cache generator
        this.generatorInstances.set(name, generator);

        return generator as T;
    }

    /**
     * Validate all dependencies (detect circular dependencies and missing dependencies)
     */
    validateDependencies(): ValidationResult {
        const errors: string[] = [];
        const warnings: string[] = [];
        const visited = new Set<string>();
        const stack = new Set<string>();

        // Helper function for DFS
        const validateNode = (name: string, isService: boolean): void => {
            if (stack.has(name)) {
                // Circular dependency detected
                const cycle = Array.from(stack);
                cycle.push(name);
                errors.push(`Circular dependency: ${cycle.join(' -> ')}`);
                return;
            }

            if (visited.has(name)) {
                return;
            }

            visited.add(name);
            stack.add(name);

            // Get dependencies
            let dependencies: string[] = [];
            if (isService) {
                const def = this.serviceDefinitions.get(name);
                dependencies = def?.dependencies || [];
            } else {
                const meta = this.generatorMetadata.get(name);
                dependencies = meta?.dependencies || [];
            }

            // Validate each dependency
            for (const dep of dependencies) {
                // Check if dependency exists
                const depExists = this.serviceDefinitions.has(dep) ||
                                 this.services.has(dep) ||
                                 this.generatorFactories.has(dep);

                if (!depExists) {
                    errors.push(`Missing dependency '${dep}' required by '${name}'`);
                    continue;
                }

                // Recursively validate
                const depIsService = this.serviceDefinitions.has(dep) || this.services.has(dep);
                validateNode(dep, depIsService);
            }

            stack.delete(name);
        };

        // Validate all services
        for (const name of this.serviceDefinitions.keys()) {
            validateNode(name, true);
        }

        // Validate all generators
        for (const name of this.generatorFactories.keys()) {
            validateNode(name, false);
        }

        return {
            valid: errors.length === 0,
            errors,
            warnings
        };
    }

    /**
     * Get topological order for generator execution
     */
    getGenerationOrder(): string[] {
        const visited = new Set<string>();
        const order: string[] = [];

        const visit = (name: string): void => {
            if (visited.has(name)) return;

            const metadata = this.generatorMetadata.get(name);
            if (!metadata) return;

            visited.add(name);

            // Visit dependencies first
            for (const dep of metadata.dependencies) {
                if (this.generatorFactories.has(dep)) {
                    visit(dep);
                }
            }

            order.push(name);
        };

        // Visit all generators
        for (const name of this.generatorFactories.keys()) {
            visit(name);
        }

        return order;
    }

    /**
     * Get dependency graph for visualization
     */
    getDependencyGraph(): { nodes: string[]; edges: [string, string][] } {
        const nodes: string[] = [];
        const edges: [string, string][] = [];

        // Add all services
        for (const name of this.serviceDefinitions.keys()) {
            nodes.push(`service:${name}`);
        }
        for (const name of this.services.keys()) {
            if (!this.serviceDefinitions.has(name)) {
                nodes.push(`service:${name}`);
            }
        }

        // Add all generators
        for (const name of this.generatorFactories.keys()) {
            nodes.push(`generator:${name}`);
        }

        // Add edges for service dependencies
        for (const [name, def] of this.serviceDefinitions) {
            if (def.dependencies) {
                for (const dep of def.dependencies) {
                    edges.push([`service:${name}`, `service:${dep}`]);
                }
            }
        }

        // Add edges for generator dependencies
        for (const [name, meta] of this.generatorMetadata) {
            for (const dep of meta.dependencies) {
                const depType = this.generatorFactories.has(dep) ? 'generator' : 'service';
                edges.push([`generator:${name}`, `${depType}:${dep}`]);
            }
        }

        return { nodes, edges };
    }

    /**
     * Clear all cached instances (useful for testing)
     */
    clear(): void {
        this.services.clear();
        this.generatorInstances.clear();
        // Don't clear definitions/factories - those are configuration
    }

    /**
     * Get statistics about registered services and generators
     */
    getStatistics(): {
        services: number;
        generators: number;
        singletons: number;
        transients: number;
        cached: number;
    } {
        const singletons = Array.from(this.serviceDefinitions.values())
            .filter(def => def.singleton).length;
        const transients = this.serviceDefinitions.size - singletons;

        return {
            services: this.serviceDefinitions.size + this.services.size,
            generators: this.generatorFactories.size,
            singletons,
            transients,
            cached: this.services.size + this.generatorInstances.size
        };
    }
}

/**
 * Global DI container instance
 */
export const globalInjector = new DependencyInjector();
