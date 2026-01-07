import { Entity } from "../../../../../language/generated/ast.js";
import { capitalize } from "../../../../utils/generator-utils.js";
import { TypeResolver } from "../../../common/resolvers/type-resolver.js";

export class ServiceCrudGenerator {
    static generateCrudMethods(aggregateName: string, rootEntity: Entity, projectName: string): string {
        const capitalizedAggregate = capitalize(aggregateName);
        const lowerAggregate = aggregateName.toLowerCase();
        const rootEntityName = rootEntity.name;

        return `    // CRUD Operations
    public ${rootEntityName}Dto create${capitalizedAggregate}(${this.generateConstructorParams(rootEntity)}) {
        try {
            ${rootEntityName} ${lowerAggregate} = new ${rootEntityName}(${this.getConstructorArgs(rootEntity)});
            ${lowerAggregate} = ${lowerAggregate}Repository.save(${lowerAggregate});
            return new ${rootEntityName}Dto(${lowerAggregate});
        } catch (Exception e) {
            throw new ${capitalize(projectName)}Exception("Error creating ${lowerAggregate}: " + e.getMessage());
        }
    }

    public ${rootEntityName}Dto get${capitalizedAggregate}ById(Integer id) {
        try {
            ${rootEntityName} ${lowerAggregate} = (${rootEntityName}) ${lowerAggregate}Repository.findById(id)
                .orElseThrow(() -> new ${capitalize(projectName)}Exception("${capitalizedAggregate} not found with id: " + id));
            return new ${rootEntityName}Dto(${lowerAggregate});
        } catch (${capitalize(projectName)}Exception e) {
            throw e;
        } catch (Exception e) {
            throw new ${capitalize(projectName)}Exception("Error retrieving ${lowerAggregate}: " + e.getMessage());
        }
    }

    public List<${rootEntityName}Dto> getAll${capitalizedAggregate}s() {
        try {
            return ${lowerAggregate}Repository.findAll().stream()
                .map(entity -> new ${rootEntityName}Dto((${rootEntityName}) entity))
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new ${capitalize(projectName)}Exception("Error retrieving all ${lowerAggregate}s: " + e.getMessage());
        }
    }

    public ${rootEntityName}Dto update${capitalizedAggregate}(${rootEntityName}Dto ${lowerAggregate}Dto) {
        try {
            Integer id = ${lowerAggregate}Dto.getAggregateId();
            ${rootEntityName} ${lowerAggregate} = (${rootEntityName}) ${lowerAggregate}Repository.findById(id)
                .orElseThrow(() -> new ${capitalize(projectName)}Exception("${capitalizedAggregate} not found with id: " + id));
            
            ${this.generateUpdateLogic(rootEntity, aggregateName)}
            
            ${lowerAggregate} = ${lowerAggregate}Repository.save(${lowerAggregate});
            return new ${rootEntityName}Dto(${lowerAggregate});
        } catch (${capitalize(projectName)}Exception e) {
            throw e;
        } catch (Exception e) {
            throw new ${capitalize(projectName)}Exception("Error updating ${lowerAggregate}: " + e.getMessage());
        }
    }

    public void delete${capitalizedAggregate}(Integer id) {
        try {
            if (!${lowerAggregate}Repository.existsById(id)) {
                throw new ${capitalize(projectName)}Exception("${capitalizedAggregate} not found with id: " + id);
            }
            ${lowerAggregate}Repository.deleteById(id);
        } catch (${capitalize(projectName)}Exception e) {
            throw e;
        } catch (Exception e) {
            throw new ${capitalize(projectName)}Exception("Error deleting ${lowerAggregate}: " + e.getMessage());
        }
    }`;
    }

    private static generateConstructorParams(rootEntity: Entity): string {
        if (!rootEntity.properties) return '';

        return rootEntity.properties
            .filter(prop => prop.name.toLowerCase() !== 'id')
            .map(prop => `${TypeResolver.resolveJavaType(prop.type)} ${prop.name}`)
            .join(', ');
    }

    private static getConstructorArgs(rootEntity: Entity): string {
        if (!rootEntity.properties) return '';

        return rootEntity.properties
            .filter(prop => prop.name.toLowerCase() !== 'id')
            .map(prop => prop.name)
            .join(', ');
    }

    private static generateUpdateLogic(rootEntity: Entity, aggregateName: string): string {
        if (!rootEntity.properties) return '';

        const lowerAggregate = aggregateName.toLowerCase();
        const updates = rootEntity.properties
            .filter(prop => prop.name.toLowerCase() !== 'id')
            .map(prop => {
                const setterName = `set${capitalize(prop.name)}`;
                const getterName = this.getGetterMethodName(prop);
                const isBoolean = this.isBooleanProperty(prop);

                if (isBoolean) {
                    return `            ${lowerAggregate}.${setterName}(${lowerAggregate}Dto.${getterName}());`;
                } else {
                    return `            if (${lowerAggregate}Dto.${getterName}() != null) {
                ${lowerAggregate}.${setterName}(${lowerAggregate}Dto.${getterName}());
            }`;
                }
            });

        return updates.join('\n');
    }

    private static getGetterMethodName(property: any): string {
        const capitalizedName = capitalize(property.name);
        const isBoolean = this.isBooleanProperty(property);

        if (isBoolean) {
            return `is${capitalizedName}`;
        }
        return `get${capitalizedName}`;
    }

    private static isBooleanProperty(property: any): boolean {
        if (!property.type) return false;
        if (property.type.$type === 'PrimitiveType') {
            return property.type.typeName?.toLowerCase() === 'boolean';
        }

        if (typeof property.type === 'string') {
            return property.type.toLowerCase() === 'boolean';
        }

        return false;
    }
}
