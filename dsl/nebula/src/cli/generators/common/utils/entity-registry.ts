import { Aggregate, Entity } from '../parsers/model-parser.js';

export class EntityRegistry {
    private entityToAggregateMap: Map<string, string> = new Map();
    private aggregateEntitiesMap: Map<string, string[]> = new Map();

    constructor(private aggregates: Aggregate[]) {
        this.buildMappings();
    }

    private buildMappings(): void {
        this.aggregates.forEach(aggregate => {
            const aggregateName = aggregate.name.toLowerCase();
            const entityNames: string[] = [];

            const rootEntity = aggregate.entities?.find((e: any) => e.isRoot);
            if (rootEntity) {
                this.entityToAggregateMap.set(rootEntity.name, aggregateName);
                entityNames.push(rootEntity.name);
            }

            if (aggregate.entities) {
                aggregate.entities.forEach((entity: Entity) => {
                    if (entity.name) {
                        this.entityToAggregateMap.set(entity.name, aggregateName);
                        entityNames.push(entity.name);
                    }
                });
            }

            this.aggregateEntitiesMap.set(aggregateName, entityNames);
        });
    }

    getAggregateForEntity(entityName: string): string | null {
        return this.entityToAggregateMap.get(entityName) || null;
    }

    getEntitiesForAggregate(aggregateName: string): string[] {
        return this.aggregateEntitiesMap.get(aggregateName.toLowerCase()) || [];
    }

    isEntityName(name: string): boolean {
        return this.entityToAggregateMap.has(name);
    }

    getAllEntityNames(): string[] {
        return Array.from(this.entityToAggregateMap.keys());
    }

    getAllAggregateNames(): string[] {
        return Array.from(this.aggregateEntitiesMap.keys());
    }

    static buildFromAggregates(aggregates: Aggregate[]): EntityRegistry {
        return new EntityRegistry(aggregates);
    }

    debugPrintMappings(): void {
        console.log('=== Entity Registry Mappings ===');
        console.log('Entity -> Aggregate:');
        this.entityToAggregateMap.forEach((aggregate, entity) => {
            console.log(`  ${entity} -> ${aggregate}`);
        });
        console.log('\nAggregate -> Entities:');
        this.aggregateEntitiesMap.forEach((entities, aggregate) => {
            console.log(`  ${aggregate} -> [${entities.join(', ')}]`);
        });
    }
}
