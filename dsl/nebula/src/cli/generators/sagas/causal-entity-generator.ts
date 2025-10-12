import { Aggregate, Entity } from "../../../language/generated/ast.js";
import { getGlobalConfig } from "../shared/config.js";

export interface CausalEntityGenerationOptions {
    architecture?: string;
    features?: string[];
    projectName: string;
}

export class CausalEntityGenerator {
    async generateCausal(aggregate: Aggregate, options: CausalEntityGenerationOptions): Promise<{ [key: string]: string }> {
        const rootEntity = aggregate.entities.find((e: any) => e.isRoot);
        if (!rootEntity) {
            throw new Error(`No root entity found in aggregate ${aggregate.name}`);
        }

        const results: { [key: string]: string } = {};

        results["entity"] = this.generateCausalEntityClass(aggregate, rootEntity, options);
        results["factory"] = this.generateCausalFactoryClass(aggregate, rootEntity, options);

        return results;
    }

    private generateCausalEntityClass(aggregate: Aggregate, rootEntity: Entity, options: CausalEntityGenerationOptions): string {
        const aggregateName = this.capitalize(aggregate.name);
        const packageName = `${getGlobalConfig().buildPackageName(options.projectName, 'sagas', 'aggregates', 'causal')}`;

        const properties = (rootEntity.properties || []).map((prop: any) => ({
            name: prop.name,
            type: this.resolveJavaType(prop.type),
        }));

        const imports = this.generateImports(aggregate, rootEntity, options);

        const fields = properties.map(p => `    private ${p.type} ${p.name};`).join("\n");
        const gettersSetters = properties.map(p => `    public ${p.type} get${this.capitalize(p.name)}() {\n        return ${p.name};\n    }\n\n    public void set${this.capitalize(p.name)}(${p.type} ${p.name}) {\n        this.${p.name} = ${p.name};\n    }`).join("\n\n");

        return `package ${packageName};

${imports}

public class Causal${aggregateName} {
${fields}

    public Causal${aggregateName}() {}

${gettersSetters}
}`;
    }

    private generateCausalFactoryClass(aggregate: Aggregate, rootEntity: Entity, options: CausalEntityGenerationOptions): string {
        const aggregateName = this.capitalize(aggregate.name);
        const packageName = `${getGlobalConfig().buildPackageName(options.projectName, 'sagas', 'aggregates', 'causal')}`;

        const imports = this.generateImports(aggregate, rootEntity, options);

        const properties = (rootEntity.properties || []).map((prop: any) => ({
            name: prop.name,
            type: this.resolveJavaType(prop.type),
        }));
        const params = properties.map(p => `${p.type} ${p.name}`).join(", ");
        const setters = properties.map(p => `        causal.set${this.capitalize(p.name)}(${p.name});`).join("\n");

        return `package ${packageName};

${imports}

public class Causal${aggregateName}Factory {
    public static Causal${aggregateName} create(${params}) {
        Causal${aggregateName} causal = new Causal${aggregateName}();
${setters}
        return causal;
    }
}`;
    }

    private generateImports(aggregate: Aggregate, rootEntity: Entity, options: CausalEntityGenerationOptions): string {
        const imports = new Set<string>();

        imports.add("import java.time.LocalDateTime;");
        imports.add("import java.util.List;");
        imports.add("import java.util.ArrayList;");

        aggregate.entities.forEach((entity: any) => {
            if (entity.name !== rootEntity.name) {
                imports.add(`import ${getGlobalConfig().buildPackageName(options.projectName, 'microservices', aggregate.name.toLowerCase(), 'aggregate')}.${entity.name};`);
            }
        });

        return Array.from(imports).join("\n");
    }

    private resolveJavaType(type: any): string {
        if (!type) return "Object";
        const t = (type as any);
        if (t.$type === 'BuiltinType') return t.name;
        if (t.$type === 'PrimitiveType') return t.typeName;
        if (t.$type === 'EntityType' && t.type && t.type.ref) return (t.type.ref.name || 'Object');
        if (t.$type === 'ListType') return `List<${t.elementType}>`;
        return 'Object';
    }

    private capitalize(str: string): string {
        return str.charAt(0).toUpperCase() + str.slice(1);
    }
}


