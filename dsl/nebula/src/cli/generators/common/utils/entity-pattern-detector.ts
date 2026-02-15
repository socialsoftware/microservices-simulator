import { Aggregate } from "../../../../language/generated/ast.js";
import { getEntities } from "../../../utils/aggregate-helpers.js";



export class EntityPatternDetector {
    

    static buildEntityPattern(allAggregates: Aggregate[]): RegExp {
        const entityNames = allAggregates
            .flatMap(agg => getEntities(agg).map(e => e.name))
            .filter((name, index, self) => self.indexOf(name) === index); 

        if (entityNames.length === 0) {
            
            return /(?!.*)/g;
        }

        
        const namesPattern = entityNames.join('|');
        return new RegExp(`\\b([A-Z][a-zA-Z]*(?:${namesPattern}))\\b`, 'g');
    }

    

    static detectEntitiesInCode(code: string, allAggregates: Aggregate[]): string[] {
        const pattern = this.buildEntityPattern(allAggregates);
        const matches = code.matchAll(pattern);
        return Array.from(matches, m => m[1]);
    }

    

    static isEntityType(typeName: string, allAggregates: Aggregate[]): boolean {
        const allEntityNames = allAggregates
            .flatMap(agg => getEntities(agg).map(e => e.name));
        return allEntityNames.includes(typeName);
    }
}
