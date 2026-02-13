/**
 * Exception Generator Utility
 *
 * Provides standardized exception handling code generation across all generators.
 * Ensures consistent error messages and exception handling patterns.
 *
 * Pattern:
 * 1. Catch project-specific exceptions and rethrow (preserves original exception)
 * 2. Catch generic exceptions and wrap with descriptive message
 */
export class ExceptionGenerator {

    /**
     * Generate standard catch block for CRUD operations.
     *
     * Pattern:
     * ```java
     * } catch (ProjectException e) {
     *     throw e;
     * } catch (Exception e) {
     *     throw new ProjectException("Error {action} {entity}: " + e.getMessage());
     * }
     * ```
     *
     * @param projectName Project name (capitalized)
     * @param action Action being performed (e.g., "creating", "updating", "deleting")
     * @param entityName Entity name (lowercase, e.g., "user", "product")
     * @returns Formatted catch block as string
     */
    static generateCatchBlock(projectName: string, action: string, entityName: string): string {
        const capitalizedProject = this.capitalize(projectName);
        return `        } catch (${capitalizedProject}Exception e) {
            throw e;
        } catch (Exception e) {
            throw new ${capitalizedProject}Exception("Error ${action} ${entityName}: " + e.getMessage());
        }`;
    }

    /**
     * Generate try-catch wrapper around method body.
     *
     * Pattern:
     * ```java
     * try {
     *     {body}
     * } catch (ProjectException e) {
     *     throw e;
     * } catch (Exception e) {
     *     throw new ProjectException("Error {action} {entity}: " + e.getMessage());
     * }
     * ```
     *
     * @param projectName Project name (capitalized)
     * @param action Action being performed
     * @param entityName Entity name (lowercase)
     * @param body Method body to wrap
     * @returns Complete try-catch block as string
     */
    static generateTryCatchWrapper(projectName: string, action: string, entityName: string, body: string): string {
        return `        try {
${body}
${this.generateCatchBlock(projectName, action, entityName)}`;
    }

    /**
     * Generate catch block with custom error message.
     *
     * Pattern:
     * ```java
     * } catch (ProjectException e) {
     *     throw e;
     * } catch (Exception e) {
     *     throw new ProjectException("{customMessage}: " + e.getMessage());
     * }
     * ```
     *
     * @param projectName Project name (capitalized)
     * @param customMessage Custom error message
     * @returns Formatted catch block as string
     */
    static generateCatchBlockWithMessage(projectName: string, customMessage: string): string {
        const capitalizedProject = this.capitalize(projectName);
        return `        } catch (${capitalizedProject}Exception e) {
            throw e;
        } catch (Exception e) {
            throw new ${capitalizedProject}Exception("${customMessage}: " + e.getMessage());
        }`;
    }

    /**
     * Generate catch block without rethrowing project exception.
     *
     * Used when you want to catch and transform all exceptions uniformly.
     *
     * Pattern:
     * ```java
     * } catch (Exception e) {
     *     throw new ProjectException("Error {action} {entity}: " + e.getMessage());
     * }
     * ```
     *
     * @param projectName Project name (capitalized)
     * @param action Action being performed
     * @param entityName Entity name (lowercase)
     * @returns Formatted catch block as string
     */
    static generateSimpleCatchBlock(projectName: string, action: string, entityName: string): string {
        const capitalizedProject = this.capitalize(projectName);
        return `        } catch (Exception e) {
            throw new ${capitalizedProject}Exception("Error ${action} ${entityName}: " + e.getMessage());
        }`;
    }

    /**
     * Generate opening try statement.
     *
     * Pattern:
     * ```java
     * try {
     * ```
     *
     * @returns Try statement as string
     */
    static generateTryBlock(): string {
        return `        try {`;
    }

    /**
     * Generate method wrapper with try-catch and return statement.
     *
     * Pattern:
     * ```java
     * public ReturnType methodName(...) {
     *     try {
     *         {body}
     *         return {returnExpression};
     *     } catch (ProjectException e) {
     *         throw e;
     *     } catch (Exception e) {
     *         throw new ProjectException("Error {action} {entity}: " + e.getMessage());
     *     }
     * }
     * ```
     *
     * @param signature Method signature
     * @param body Method body
     * @param returnExpression Expression to return
     * @param projectName Project name
     * @param action Action description
     * @param entityName Entity name
     * @returns Complete method as string
     */
    static generateMethodWithReturn(
        signature: string,
        body: string,
        returnExpression: string,
        projectName: string,
        action: string,
        entityName: string
    ): string {
        return `    ${signature} {
        try {
${body}
            return ${returnExpression};
${this.generateCatchBlock(projectName, action, entityName)}
    }`;
    }

    /**
     * Generate void method wrapper with try-catch.
     *
     * Pattern:
     * ```java
     * public void methodName(...) {
     *     try {
     *         {body}
     *     } catch (ProjectException e) {
     *         throw e;
     *     } catch (Exception e) {
     *         throw new ProjectException("Error {action} {entity}: " + e.getMessage());
     *     }
     * }
     * ```
     *
     * @param signature Method signature
     * @param body Method body
     * @param projectName Project name
     * @param action Action description
     * @param entityName Entity name
     * @returns Complete method as string
     */
    static generateVoidMethod(
        signature: string,
        body: string,
        projectName: string,
        action: string,
        entityName: string
    ): string {
        return `    ${signature} {
        try {
${body}
${this.generateCatchBlock(projectName, action, entityName)}
    }`;
    }

    /**
     * Capitalize first letter of string.
     *
     * @param str String to capitalize
     * @returns Capitalized string
     */
    private static capitalize(str: string): string {
        if (!str) return '';
        return str.charAt(0).toUpperCase() + str.slice(1);
    }
}
