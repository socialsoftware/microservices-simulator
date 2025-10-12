import { Aggregate } from "../../../../language/generated/ast.js";
// import { capitalize } from "../../../utils/generator-utils.js";

export class ServiceQueryGenerator {
    static generateQueryMethods(aggregateName: string, aggregate: Aggregate): string {
        return '    // Query methods disabled - repository methods not implemented';
    }

    // private static generateFindByPropertyMethods(aggregateName: string, aggregate: Aggregate): string {
    //     const capitalizedAggregate = capitalize(aggregateName);
    //     const lowerAggregate = aggregateName.toLowerCase();
    //     const rootEntity = aggregate.entities.find((e: any) => e.isRoot);

    //     if (!rootEntity || !rootEntity.properties) {
    //         return '';
    //     }

    //     const methods = rootEntity.properties
    //         .filter(prop => prop.name.toLowerCase() !== 'id')
    //         .map(prop => {
    //             const propName = prop.name;
    //             const capitalizedProp = capitalize(propName);
    //             const propType = this.resolvePropertyType(prop.type);

    //             return `    @Transactional(readOnly = true)
    // public List<${rootEntity.name}Dto> find${capitalizedAggregate}sBy${capitalizedProp}(${propType} ${propName}) {
    //     try {
    //         return ${lowerAggregate}Repository.findBy${capitalizedProp}(${propName}).stream()
    //             .map(${rootEntity.name}Dto::new)
    //             .collect(Collectors.toList());
    //     } catch (Exception e) {
    //         throw new ${capitalizedAggregate}sException("Error finding ${lowerAggregate}s by ${propName}: " + e.getMessage(), e);
    //     }
    // }`;
    //         });

    //     return methods.join('\n\n');
    // }

    // private static generateCountMethods(aggregateName: string): string {
    //     const capitalizedAggregate = capitalize(aggregateName);
    //     const lowerAggregate = aggregateName.toLowerCase();

    //     return `    @Transactional(readOnly = true)
    // public long count${capitalizedAggregate}s() {
    //     try {
    //         return ${lowerAggregate}Repository.count();
    //     } catch (Exception e) {
    //         throw new ${capitalizedAggregate}sException("Error counting ${lowerAggregate}s: " + e.getMessage(), e);
    //     }
    // }`;
    // }

    // private static generateExistsMethods(aggregateName: string): string {
    //     const capitalizedAggregate = capitalize(aggregateName);
    //     const lowerAggregate = aggregateName.toLowerCase();

    //     return `    @Transactional(readOnly = true)
    // public boolean ${lowerAggregate}Exists(Long id) {
    //     try {
    //         return ${lowerAggregate}Repository.existsById(id);
    //     } catch (Exception e) {
    //         throw new ${capitalizedAggregate}sException("Error checking ${lowerAggregate} existence: " + e.getMessage(), e);
    //     }
    // }`;
    // }

    // private static resolvePropertyType(type: any): string {
    //     if (!type) return 'String';

    //     if (typeof type === 'string') {
    //         return type;
    //     }

    //     if (type.$type) {
    //         switch (type.$type) {
    //             case 'PrimitiveType':
    //                 return this.mapPrimitiveType(type.name);
    //             case 'EntityType':
    //                 return type.type?.ref?.name || 'String';
    //             case 'BuiltinType':
    //                 return this.mapBuiltinType(type.name);
    //             default:
    //                 return 'String';
    //         }
    //     }

    //     if (type.name) {
    //         return type.name;
    //     }

    //     return 'String';
    // }

    // private static mapPrimitiveType(typeName: string): string {
    //     if (!typeName) {
    //         console.warn('mapPrimitiveType called with undefined typeName, defaulting to String');
    //         return 'String';
    //     }

    //     const typeMap: { [key: string]: string } = {
    //         'string': 'String',
    //         'int': 'Integer',
    //         'integer': 'Integer',
    //         'long': 'Long',
    //         'double': 'Double',
    //         'float': 'Float',
    //         'boolean': 'Boolean',
    //         'date': 'LocalDate',
    //         'datetime': 'LocalDateTime',
    //         'time': 'LocalTime'
    //     };

    //     return typeMap[typeName.toLowerCase()] || 'String';
    // }

    // private static mapBuiltinType(typeName: string): string {
    //     const typeMap: { [key: string]: string } = {
    //         'String': 'String',
    //         'Integer': 'Integer',
    //         'Long': 'Long',
    //         'Double': 'Double',
    //         'Float': 'Float',
    //         'Boolean': 'Boolean',
    //         'Date': 'LocalDate'
    //     };

    //     return typeMap[typeName] || 'String';
    // }
}
