import { PublishedEvent, EventField } from "../../../../../language/generated/ast.js";
import { AggregateExt } from "../../../../types/ast-extensions.js";
import { GeneratorCapabilities } from "../../../common/generator-capabilities.js";
import { UnifiedTypeResolver as TypeResolver } from "../../../common/unified-type-resolver.js";
import { StringUtils } from '../../../../utils/string-utils.js';



export class PublishedEventContextBuilder {
    constructor(private capabilities: GeneratorCapabilities) {}

    

    buildPublishedEventContext(event: PublishedEvent, aggregate: AggregateExt, options: { projectName: string }): any {
        const eventName = event.name;

        const fields = event.fields.map((field: EventField) => ({
            type: TypeResolver.resolveJavaType(field.type),
            name: field.name,
            capitalizedName: StringUtils.capitalize(field.name)
        }));

        const imports = this.generatePublishedEventImports(fields);

        return {
            packageName: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.events`,
            eventName,
            fields,
            imports
        };
    }

    

    private generatePublishedEventImports(fields: any[]): string {
        const imports = new Set<string>();

        fields.forEach(field => {
            if (field.type === 'LocalDateTime') {
                imports.add('import java.time.LocalDateTime;');
            } else if (field.type === 'BigDecimal') {
                imports.add('import java.math.BigDecimal;');
            } else if (field.type.startsWith('Set<')) {
                imports.add('import java.util.Set;');
            } else if (field.type.startsWith('List<')) {
                imports.add('import java.util.List;');
            }
        });

        return Array.from(imports).join('\n');
    }

    

    private getBasePackage(): string {
        return this.capabilities.packageBuilder.buildCustomPackage('').split('.').slice(0, -1).join('.');
    }
}
