import { getGlobalConfig } from "../../common/config.js";

export function scanCodeForImports(javaCode: string, projectName: string, isRoot: boolean, aggregateName?: string, entityName?: string): string[] {
    const imports: string[] = [];

    
    if (javaCode.includes('@Entity')) imports.push('import jakarta.persistence.Entity;');
    if (javaCode.includes('@Id')) imports.push('import jakarta.persistence.Id;');
    if (javaCode.includes('@GeneratedValue')) imports.push('import jakarta.persistence.GeneratedValue;');
    if (javaCode.includes('@OneToOne')) imports.push('import jakarta.persistence.OneToOne;');
    if (javaCode.includes('@ManyToOne')) imports.push('import jakarta.persistence.ManyToOne;');
    if (javaCode.includes('@OneToMany')) imports.push('import jakarta.persistence.OneToMany;');
    if (javaCode.includes('CascadeType')) imports.push('import jakarta.persistence.CascadeType;');
    if (javaCode.includes('FetchType')) imports.push('import jakarta.persistence.FetchType;');
    if (javaCode.includes('@Enumerated')) imports.push('import jakarta.persistence.Enumerated;\nimport jakarta.persistence.EnumType;');
    if (javaCode.includes('@Column')) imports.push('import jakarta.persistence.Column;');

    
    if (javaCode.includes('LocalDateTime')) imports.push('import java.time.LocalDateTime;');
    if (javaCode.includes('BigDecimal')) imports.push('import java.math.BigDecimal;');
    if (javaCode.includes('Set<') || javaCode.includes('HashSet')) imports.push('import java.util.Set;\nimport java.util.HashSet;');
    if (javaCode.includes('List<') || javaCode.includes('ArrayList')) imports.push('import java.util.List;\nimport java.util.ArrayList;');
    if (javaCode.includes('Collectors')) imports.push('import java.util.stream.Collectors;');

    
    if (isRoot) {
        if (aggregateName && entityName && aggregateName !== entityName) {
            
        } else {
            const fwk = getGlobalConfig().getFrameworkPackage();
            imports.push(`import ${fwk}.domain.aggregate.Aggregate;`);
        }
    }

    const fwk = getGlobalConfig().getFrameworkPackage();
    if (javaCode.includes('INVARIANT_BREAK')) {
        imports.push(`import static ${fwk}.exception.SimulatorErrorMessage.INVARIANT_BREAK;`);
    }
    if (javaCode.includes('SimulatorException')) {
        imports.push(`import ${fwk}.exception.SimulatorException;`);
    }

    
    const dtoPattern = /(\w+Dto)\s+\w+/g;
    let match;
    while ((match = dtoPattern.exec(javaCode)) !== null) {
        const dtoName = match[1];
        const dtoImport = `import ${getGlobalConfig().buildPackageName(projectName, 'shared', 'dtos')}.${dtoName};`;
        if (!imports.includes(dtoImport)) {
            imports.push(dtoImport);
        }
    }

    
    const enumPattern = /(\w+Type)(?:\.valueOf|\.STRING|\s+\w+)/g;
    while ((match = enumPattern.exec(javaCode)) !== null) {
        const enumName = match[1];
        
        const jpaTypes = ['CascadeType', 'FetchType', 'EnumType'];
        if (!jpaTypes.includes(enumName)) {
            const enumImport = `import ${getGlobalConfig().buildPackageName(projectName, 'shared', 'enums')}.${enumName};`;
            if (!imports.includes(enumImport)) {
                imports.push(enumImport);
            }
        }
    }

    
    if (javaCode.includes('AggregateState')) {
        imports.push(`import ${fwk}.domain.aggregate.Aggregate.AggregateState;`);
    }

    return imports;
}