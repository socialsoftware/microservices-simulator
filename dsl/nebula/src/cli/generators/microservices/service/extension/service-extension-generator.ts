import { Aggregate } from "../../../../../language/generated/ast.js";
import { capitalize } from "../../../../utils/generator-utils.js";
import { getGlobalConfig } from "../../../common/config.js";

export class ServiceExtensionGenerator {

    static generateExtensionCode(aggregate: Aggregate, projectName: string): string {
        const aggregateName = aggregate.name;
        const lowerAggregate = aggregateName.toLowerCase();
        const capitalizedAggregate = capitalize(aggregateName);
        const packageName = getGlobalConfig().buildPackageName(
            projectName,
            'microservices',
            lowerAggregate,
            'service'
        );

        return `package ${packageName};

import org.springframework.stereotype.Component;

@Component
public class ${capitalizedAggregate}ServiceExtension {
}
`;
    }

    static getExtensionFileName(aggregate: Aggregate): string {
        return `${capitalize(aggregate.name)}ServiceExtension.java`;
    }
}
