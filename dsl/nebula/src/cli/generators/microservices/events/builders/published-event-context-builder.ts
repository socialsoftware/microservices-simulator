import { PublishedEvent, EventField } from "../../../../../language/generated/ast.js";
import { AggregateExt } from "../../../../types/ast-extensions.js";
import { GeneratorCapabilities } from "../../../common/generator-capabilities.js";
import { UnifiedTypeResolver as TypeResolver } from "../../../common/unified-type-resolver.js";
import { StringUtils } from '../../../../utils/string-utils.js';

/**
 * Published Event Context Builder
 *
 * Responsible for building context objects for published event generation.
 * Handles published event field mapping, imports, and package structure.
 */
export class PublishedEventContextBuilder {
    constructor(private capabilities: GeneratorCapabilities) {}

    /**
     * Build context for a published event.
     *
     * Generates all necessary metadata for rendering a published event template:
     * - Package name based on aggregate
     * - Event fields with Java types
     * - Required imports
     *
     * @param event The published event definition from DSL
     * @param aggregate The containing aggregate
     * @param options Generation options including project name
     * @returns Context object for template rendering
     */
    buildPublishedEventContext(event: PublishedEvent, aggregate: AggregateExt, options: { projectName: string }): any {
        const eventName = event.name;
        const lowerAggregate = aggregate.name.toLowerCase();

        const fields = event.fields.map((field: EventField) => ({
            type: TypeResolver.resolveJavaType(field.type),
            name: field.name,
            capitalizedName: StringUtils.capitalize(field.name)
        }));

        const imports = this.generatePublishedEventImports(fields);

        return {
            packageName: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${lowerAggregate}.events.publish`,
            eventName,
            fields,
            imports
        };
    }

    /**
     * Generate Java imports for published event fields.
     *
     * Analyzes field types and generates appropriate import statements:
     * - LocalDateTime → java.time.LocalDateTime
     * - BigDecimal → java.math.BigDecimal
     * - Set<T> → java.util.Set
     * - List<T> → java.util.List
     *
     * @param fields Array of event fields with Java types
     * @returns Newline-separated import statements
     */
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

    /**
     * Get base package name from capabilities.
     *
     * Extracts the base package (e.g., "pt.ulisboa.tecnico.socialsoftware")
     * from the package builder configuration.
     *
     * @returns Base package name without trailing segments
     */
    private getBasePackage(): string {
        return this.capabilities.packageBuilder.buildCustomPackage('').split('.').slice(0, -1).join('.');
    }
}
