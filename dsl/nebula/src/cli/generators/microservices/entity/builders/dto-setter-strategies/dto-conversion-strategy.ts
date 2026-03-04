import { EntityExt } from "../../../../../types/ast-extensions.js";
import { DtoFieldSchema } from "../../../../../services/dto-schema-service.js";
import { DtoSetterStrategy } from "./dto-setter-strategy.js";



export class DtoConversionStrategy implements DtoSetterStrategy {
    canHandle(
        field: DtoFieldSchema,
        entity: EntityExt,
        prop: any | null,
        override?: { property: any; extractField?: string }
    ): boolean {
        
        const effectiveExtractField = override?.extractField || field.extractField;
        if (effectiveExtractField) {
            return false;
        }

        return !!field.requiresConversion;
    }

    build(
        field: DtoFieldSchema,
        entity: EntityExt,
        prop: any,
        override: { property: any; extractField?: string } | undefined,
        getterCall: string
    ): string | null {
        const capName = field.name.charAt(0).toUpperCase() + field.name.slice(1);

        
        if (field.isCollection && field.referencedEntityName) {
            const collector = field.javaType.startsWith('Set<') ? 'Collectors.toSet()' : 'Collectors.toList()';
            if (field.referencedEntityIsRoot || !field.referencedEntityHasGenerateDto) {
                return `        dto.set${capName}(${getterCall} != null ? ${getterCall}.stream().map(${field.referencedEntityName}::buildDto).collect(${collector}) : null);`;
            } else {
                return `        dto.set${capName}(${getterCall} != null ? ${getterCall}.stream().map(${field.referencedDtoName}::new).collect(${collector}) : null);`;
            }
        }

        
        if (!field.isCollection) {
            if (field.referencedEntityIsRoot || !field.referencedEntityHasGenerateDto) {
                return `        dto.set${capName}(${getterCall} != null ? ${getterCall}.buildDto() : null);`;
            } else {
                return `        dto.set${capName}(${getterCall} != null ? new ${field.referencedDtoName}(${getterCall}) : null);`;
            }
        }

        return null;
    }
}
