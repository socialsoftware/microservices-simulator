import { Generator } from "../generators/base/generator-interface.js";



export interface ServiceDefinition<T = any> {
    name: string;
    factory: () => T;
    singleton: boolean;
    dependencies?: string[];
}



export type GeneratorFactory<T extends Generator = Generator> = (dependencies: any[]) => T;



export interface ValidationResult {
    valid: boolean;
    errors: string[];
    warnings?: string[];
}



export class DependencyInjector {
    private services = new Map<string, any>();
    private serviceDefinitions = new Map<string, ServiceDefinition>();
    private generatorFactories = new Map<string, GeneratorFactory>();
    private generatorInstances = new Map<string, Generator>();
    private generatorMetadata = new Map<string, { dependencies: string[] }>();

    

    registerService<T>(definition: ServiceDefinition<T>): void {
        this.serviceDefinitions.set(definition.name, definition);

        
        if (definition.singleton && (!definition.dependencies || definition.dependencies.length === 0)) {
            const instance = definition.factory();
            this.services.set(definition.name, instance);
        }
    }

    

    registerGenerator<T extends Generator>(
        name: string,
        factory: GeneratorFactory<T>,
        dependencies: string[] = []
    ): void {
        this.generatorFactories.set(name, factory);
        this.generatorMetadata.set(name, { dependencies });
    }

    

    registerInstance<T>(name: string, instance: T): void {
        this.services.set(name, instance);
    }

    

    resolveService<T>(name: string): T {
        
        if (this.services.has(name)) {
            return this.services.get(name) as T;
        }

        
        const definition = this.serviceDefinitions.get(name);
        if (!definition) {
            throw new Error(`Service '${name}' not registered`);
        }

        
        const dependencies: any[] = [];
        if (definition.dependencies) {
            for (const dep of definition.dependencies) {
                dependencies.push(this.resolveService(dep));
            }
        }

        
        const instance = definition.factory();

        
        if (definition.singleton) {
            this.services.set(name, instance);
        }

        return instance as T;
    }

    

    resolveGenerator<T extends Generator>(name: string): T {
        
        if (this.generatorInstances.has(name)) {
            return this.generatorInstances.get(name) as T;
        }

        
        const factory = this.generatorFactories.get(name);
        if (!factory) {
            throw new Error(`Generator '${name}' not registered`);
        }

        
        const metadata = this.generatorMetadata.get(name);
        if (!metadata) {
            throw new Error(`Generator metadata for '${name}' not found`);
        }

        
        const dependencies: any[] = [];
        for (const dep of metadata.dependencies) {
            
            if (this.serviceDefinitions.has(dep) || this.services.has(dep)) {
                dependencies.push(this.resolveService(dep));
            } else if (this.generatorFactories.has(dep)) {
                dependencies.push(this.resolveGenerator(dep));
            } else {
                throw new Error(`Dependency '${dep}' for generator '${name}' not found`);
            }
        }

        
        const generator = factory(dependencies);

        
        this.generatorInstances.set(name, generator);

        return generator as T;
    }

    

    validateDependencies(): ValidationResult {
        const errors: string[] = [];
        const warnings: string[] = [];
        const visited = new Set<string>();
        const stack = new Set<string>();

        
        const validateNode = (name: string, isService: boolean): void => {
            if (stack.has(name)) {
                
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

            
            let dependencies: string[] = [];
            if (isService) {
                const def = this.serviceDefinitions.get(name);
                dependencies = def?.dependencies || [];
            } else {
                const meta = this.generatorMetadata.get(name);
                dependencies = meta?.dependencies || [];
            }

            
            for (const dep of dependencies) {
                
                const depExists = this.serviceDefinitions.has(dep) ||
                                 this.services.has(dep) ||
                                 this.generatorFactories.has(dep);

                if (!depExists) {
                    errors.push(`Missing dependency '${dep}' required by '${name}'`);
                    continue;
                }

                
                const depIsService = this.serviceDefinitions.has(dep) || this.services.has(dep);
                validateNode(dep, depIsService);
            }

            stack.delete(name);
        };

        
        for (const name of this.serviceDefinitions.keys()) {
            validateNode(name, true);
        }

        
        for (const name of this.generatorFactories.keys()) {
            validateNode(name, false);
        }

        return {
            valid: errors.length === 0,
            errors,
            warnings
        };
    }

    

    getGenerationOrder(): string[] {
        const visited = new Set<string>();
        const order: string[] = [];

        const visit = (name: string): void => {
            if (visited.has(name)) return;

            const metadata = this.generatorMetadata.get(name);
            if (!metadata) return;

            visited.add(name);

            
            for (const dep of metadata.dependencies) {
                if (this.generatorFactories.has(dep)) {
                    visit(dep);
                }
            }

            order.push(name);
        };

        
        for (const name of this.generatorFactories.keys()) {
            visit(name);
        }

        return order;
    }

    

    getDependencyGraph(): { nodes: string[]; edges: [string, string][] } {
        const nodes: string[] = [];
        const edges: [string, string][] = [];

        
        for (const name of this.serviceDefinitions.keys()) {
            nodes.push(`service:${name}`);
        }
        for (const name of this.services.keys()) {
            if (!this.serviceDefinitions.has(name)) {
                nodes.push(`service:${name}`);
            }
        }

        
        for (const name of this.generatorFactories.keys()) {
            nodes.push(`generator:${name}`);
        }

        
        for (const [name, def] of this.serviceDefinitions) {
            if (def.dependencies) {
                for (const dep of def.dependencies) {
                    edges.push([`service:${name}`, `service:${dep}`]);
                }
            }
        }

        
        for (const [name, meta] of this.generatorMetadata) {
            for (const dep of meta.dependencies) {
                const depType = this.generatorFactories.has(dep) ? 'generator' : 'service';
                edges.push([`generator:${name}`, `${depType}:${dep}`]);
            }
        }

        return { nodes, edges };
    }

    

    clear(): void {
        this.services.clear();
        this.generatorInstances.clear();
        
    }

    

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



export const globalInjector = new DependencyInjector();
