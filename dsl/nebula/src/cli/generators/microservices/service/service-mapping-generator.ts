import { Aggregate } from "../../../../language/generated/ast.js";
import { getGlobalConfig } from "../../common/config.js";

export class ServiceMappingGenerator {
    generate(aggregates: Aggregate[], options: { projectName: string; basePackage?: string }): string {
        const projectName = options.projectName.toLowerCase();
        const basePackage = options.basePackage || getGlobalConfig().getBasePackage();
        const packageName = `${basePackage}.${projectName}`;

        const entries = aggregates.map(aggregate => {
            const enumName = this.toEnumCase(aggregate.name);
            const serviceName = aggregate.name.charAt(0).toLowerCase() + aggregate.name.slice(1);
            return `    ${enumName}("${serviceName}")`;
        });

        return `package ${packageName};

public enum ServiceMapping {
${entries.join(',\n')};

    private final String serviceName;

    ServiceMapping(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }
}
`;
    }

    private toEnumCase(name: string): string {
        return name.replace(/([a-z])([A-Z])/g, '$1_$2').toUpperCase();
    }
}
