import {
    ImportRequirements
} from "./entity-generator.js";


export function generateImports(importReqs: ImportRequirements, projectName: string, isRoot: boolean): string {
    const imports = new Set<string>();

    // For persistence annotations, use a wildcard import
    if (importReqs.usesPersistence) {
        imports.add('import jakarta.persistence.*;');
    }

    // Add collection imports if needed
    if (importReqs.usesSet) {
        imports.add('import java.util.Set;');
        imports.add('import java.util.HashSet;');
    }

    if (importReqs.usesList) {
        imports.add('import java.util.List;');
        imports.add('import java.util.ArrayList;');
    }

    // Add streams if needed
    if (importReqs.usesStreams) {
        imports.add('import java.util.stream.Collectors;');
    }

    // Add temporal types if needed
    if (importReqs.usesLocalDateTime) {
        imports.add('import java.time.LocalDateTime;');
    }

    if (importReqs.usesBigDecimal) {
        imports.add('import java.math.BigDecimal;');
    }

    // Add domain imports for root entities
    if (isRoot || importReqs.usesAggregate) {
        imports.add('import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;');
    }

    // Add exception imports if needed
    if (isRoot) {
        imports.add(`import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;`);
        imports.add(`import static pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage.INVARIANT_BREAK;`);
    }

    // Add any custom imports
    if (importReqs.customImports) {
        importReqs.customImports.forEach(importStatement => {
            imports.add(importStatement);
        });
    }

    // Sort imports for consistency and group them
    const sortedImports = Array.from(imports).sort((a, b) => {
        // Group imports by package
        const aPackage = a.split('.')[1]; // jakarta, java, pt, etc.
        const bPackage = b.split('.')[1];

        // Jakarta imports first
        if (aPackage === 'jakarta' && bPackage !== 'jakarta') return -1;
        if (aPackage !== 'jakarta' && bPackage === 'jakarta') return 1;

        // Java imports second
        if (aPackage === 'java' && bPackage !== 'java' && bPackage !== 'jakarta') return -1;
        if (aPackage !== 'java' && bPackage === 'java' && aPackage !== 'jakarta') return 1;

        // Then alphabetical order
        return a.localeCompare(b);
    });

    return sortedImports.join('\n') + '\n';
} 