import { Entity } from "../../../../language/generated/ast.js";

export interface QueryParseResult {
    query: string;
    parameterMappings: Array<{
        placeholder: string;  // e.g., ":price", ":priceStart"
        propertyName: string;  // e.g., "price"
        paramIndex: number;    // Index in method parameters
    }>;
}

/**
 * Parses Spring Data method naming conventions and generates JPQL queries.
 *
 * Supported patterns:
 * - findBy<Property>
 * - findBy<Property1>And<Property2>
 * - findBy<Property1>Or<Property2>
 * - findBy<Property>OrderBy<Property>Asc/Desc
 * - existsBy<Property>
 * - countBy<Property>
 * - deleteBy<Property>
 * - findBy<Property><Operator> (e.g., GreaterThan, LessThan, Like, Contains)
 *
 * @example
 * findByName → SELECT e FROM Entity e WHERE e.name = :name
 * findByAgeGreaterThan → SELECT e FROM Entity e WHERE e.age > :age
 * existsByEmail → SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM Entity e WHERE e.email = :email
 */
export class SpringDataQueryParser {

    private static readonly OPERATORS: { [key: string]: string } = {
        'GreaterThan': '>',
        'LessThan': '<',
        'GreaterThanEqual': '>=',
        'LessThanEqual': '<=',
        'Between': 'BETWEEN',
        'Like': 'LIKE',
        'NotLike': 'NOT LIKE',
        'StartingWith': 'LIKE',  // Special handling: append %
        'EndingWith': 'LIKE',     // Special handling: prepend %
        'Containing': 'LIKE',     // Special handling: wrap with %%
        'Contains': 'MEMBER OF',  // Collection: element MEMBER OF collection
        'NotContains': 'NOT MEMBER OF',
        'In': 'IN',
        'NotIn': 'NOT IN',
        'Not': '!=',
        'IsNull': 'IS NULL',
        'IsNotNull': 'IS NOT NULL',
        'IsEmpty': 'IS EMPTY',
        'IsNotEmpty': 'IS NOT EMPTY',
        'True': '= true',
        'False': '= false'
    };

    /**
     * Attempts to parse a method name using Spring Data conventions.
     * Returns generated JPQL query with parameter mappings if pattern matches, null otherwise.
     */
    static parseMethodName(methodName: string, entityName: string, rootEntity: Entity): QueryParseResult | null {
        // Extract operation prefix
        if (methodName.startsWith('findBy')) {
            return this.parseFindByQuery(methodName, entityName, rootEntity);
        } else if (methodName.startsWith('existsBy')) {
            return this.parseExistsByQuery(methodName, entityName, rootEntity);
        } else if (methodName.startsWith('countBy')) {
            return this.parseCountByQuery(methodName, entityName, rootEntity);
        } else if (methodName.startsWith('deleteBy')) {
            return this.parseDeleteByQuery(methodName, entityName, rootEntity);
        } else if (methodName.startsWith('findAll')) {
            return this.parseFindAllQuery(methodName, entityName, rootEntity);
        }

        // No matching pattern
        return null;
    }

    /**
     * Legacy method for backwards compatibility.
     * Returns only the query string without parameter mappings.
     */
    static parseMethodNameLegacy(methodName: string, entityName: string, rootEntity: Entity): string | null {
        const result = this.parseMethodName(methodName, entityName, rootEntity);
        return result ? result.query : null;
    }

    private static parseFindByQuery(methodName: string, entityName: string, rootEntity: Entity): QueryParseResult {
        const afterFindBy = methodName.substring('findBy'.length);

        // Check for findAll variants
        if (afterFindBy === '') {
            return {
                query: `SELECT e FROM ${entityName} e`,
                parameterMappings: []
            };
        }

        // Parse conditions and ordering
        const { conditions, orderBy } = this.parseConditionsAndOrdering(afterFindBy, rootEntity);

        const whereClause = conditions.map(cond => cond.jpql).join(' ');
        const queryBase = `SELECT e FROM ${entityName} e WHERE ${whereClause}`;

        const query = orderBy ? `${queryBase} ${orderBy}` : queryBase;

        // Build parameter mappings
        const parameterMappings = this.buildParameterMappings(conditions);

        return { query, parameterMappings };
    }

    private static parseExistsByQuery(methodName: string, entityName: string, rootEntity: Entity): QueryParseResult {
        const afterExistsBy = methodName.substring('existsBy'.length);

        if (afterExistsBy === '') {
            // Generic exists check
            return {
                query: `SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM ${entityName} e`,
                parameterMappings: []
            };
        }

        const { conditions } = this.parseConditionsAndOrdering(afterExistsBy, rootEntity);
        const whereClause = conditions.map(cond => cond.jpql).join(' ');
        const query = `SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM ${entityName} e WHERE ${whereClause}`;
        const parameterMappings = this.buildParameterMappings(conditions);

        return { query, parameterMappings };
    }

    private static parseCountByQuery(methodName: string, entityName: string, rootEntity: Entity): QueryParseResult {
        const afterCountBy = methodName.substring('countBy'.length);

        if (afterCountBy === '') {
            return {
                query: `SELECT COUNT(e) FROM ${entityName} e`,
                parameterMappings: []
            };
        }

        const { conditions } = this.parseConditionsAndOrdering(afterCountBy, rootEntity);
        const whereClause = conditions.map(cond => cond.jpql).join(' ');
        const query = `SELECT COUNT(e) FROM ${entityName} e WHERE ${whereClause}`;
        const parameterMappings = this.buildParameterMappings(conditions);

        return { query, parameterMappings };
    }

    private static parseDeleteByQuery(methodName: string, entityName: string, rootEntity: Entity): QueryParseResult {
        const afterDeleteBy = methodName.substring('deleteBy'.length);

        if (afterDeleteBy === '') {
            throw new Error(`Invalid Spring Data method: ${methodName} - deleteBy requires a condition`);
        }

        const { conditions } = this.parseConditionsAndOrdering(afterDeleteBy, rootEntity);
        const whereClause = conditions.map(cond => cond.jpql).join(' ');
        const query = `DELETE FROM ${entityName} e WHERE ${whereClause}`;
        const parameterMappings = this.buildParameterMappings(conditions);

        return { query, parameterMappings };
    }

    private static parseFindAllQuery(methodName: string, entityName: string, rootEntity: Entity): QueryParseResult {
        const afterFindAll = methodName.substring('findAll'.length);

        if (afterFindAll === '' || afterFindAll === 'By') {
            return {
                query: `SELECT e FROM ${entityName} e`,
                parameterMappings: []
            };
        }

        // Handle findAllOrderBy...
        if (afterFindAll.startsWith('OrderBy')) {
            const orderByPart = afterFindAll.substring('OrderBy'.length);
            const orderBy = this.parseOrderBy(orderByPart, rootEntity);
            return {
                query: `SELECT e FROM ${entityName} e ${orderBy}`,
                parameterMappings: []
            };
        }

        return {
            query: `SELECT e FROM ${entityName} e`,
            parameterMappings: []
        };
    }

    private static parseConditionsAndOrdering(expression: string, rootEntity: Entity): {
        conditions: Array<{ property: string; operator: string; jpql: string; paramName: string }>;
        orderBy?: string;
    } {
        // Split by OrderBy to separate conditions from ordering
        const orderByIndex = expression.indexOf('OrderBy');
        let conditionsStr = expression;
        let orderBy: string | undefined;

        if (orderByIndex !== -1) {
            conditionsStr = expression.substring(0, orderByIndex);
            const orderByStr = expression.substring(orderByIndex + 'OrderBy'.length);
            orderBy = this.parseOrderBy(orderByStr, rootEntity);
        }

        // Parse conditions (split by And/Or)
        const conditions = this.parseConditions(conditionsStr, rootEntity);

        return { conditions, orderBy };
    }

    private static parseConditions(conditionsStr: string, rootEntity: Entity): Array<{
        property: string;
        operator: string;
        jpql: string;
        paramName: string;
    }> {
        const results: Array<{ property: string; operator: string; jpql: string; paramName: string }> = [];

        // Split by And/Or while preserving logical operators
        const tokens = this.tokenizeConditions(conditionsStr);

        for (const token of tokens) {
            if (token.type === 'AND') {
                results.push({ property: '', operator: 'AND', jpql: 'AND', paramName: '' });
            } else if (token.type === 'OR') {
                results.push({ property: '', operator: 'OR', jpql: 'OR', paramName: '' });
            } else if (token.type === 'CONDITION') {
                const condition = this.parseCondition(token.value, rootEntity);
                results.push(condition);
            }
        }

        return results;
    }

    private static tokenizeConditions(conditionsStr: string): Array<{ type: 'CONDITION' | 'AND' | 'OR'; value: string }> {
        const tokens: Array<{ type: 'CONDITION' | 'AND' | 'OR'; value: string }> = [];
        let current = '';
        let i = 0;

        while (i < conditionsStr.length) {
            // Check for 'And'
            if (conditionsStr.substring(i, i + 3) === 'And') {
                if (current.trim()) {
                    tokens.push({ type: 'CONDITION', value: current.trim() });
                    current = '';
                }
                tokens.push({ type: 'AND', value: 'And' });
                i += 3;
            }
            // Check for 'Or'
            else if (conditionsStr.substring(i, i + 2) === 'Or') {
                if (current.trim()) {
                    tokens.push({ type: 'CONDITION', value: current.trim() });
                    current = '';
                }
                tokens.push({ type: 'OR', value: 'Or' });
                i += 2;
            }
            else {
                current += conditionsStr[i];
                i++;
            }
        }

        if (current.trim()) {
            tokens.push({ type: 'CONDITION', value: current.trim() });
        }

        return tokens;
    }

    private static parseCondition(conditionStr: string, rootEntity: Entity): {
        property: string;
        operator: string;
        jpql: string;
        paramName: string;
    } {
        // Try to match operator suffix
        // Sort operators by length (longest first) to match "NotContains" before "Contains"
        let operator = '=';  // default
        let propertyName = conditionStr;
        let operatorKeyword = '';

        const sortedOperators = Object.entries(this.OPERATORS).sort((a, b) => b[0].length - a[0].length);

        for (const [keyword, jpqlOp] of sortedOperators) {
            if (conditionStr.endsWith(keyword)) {
                operator = jpqlOp;
                propertyName = conditionStr.substring(0, conditionStr.length - keyword.length);
                operatorKeyword = keyword;
                break;
            }
        }

        // Lowercase first character of property name to match Java convention
        propertyName = this.lowercaseFirst(propertyName);
        const paramName = propertyName;

        // Handle special operators
        if (operatorKeyword === 'IsNull' || operatorKeyword === 'IsNotNull') {
            return {
                property: propertyName,
                operator: operatorKeyword,
                jpql: `e.${propertyName} ${operator}`,
                paramName: ''
            };
        } else if (operatorKeyword === 'IsEmpty' || operatorKeyword === 'IsNotEmpty') {
            // Collection emptiness check
            return {
                property: propertyName,
                operator: operatorKeyword,
                jpql: `e.${propertyName} ${operator}`,
                paramName: ''
            };
        } else if (operatorKeyword === 'StartingWith') {
            return {
                property: propertyName,
                operator: operatorKeyword,
                jpql: `e.${propertyName} LIKE CONCAT(:${paramName}, '%')`,
                paramName
            };
        } else if (operatorKeyword === 'EndingWith') {
            return {
                property: propertyName,
                operator: operatorKeyword,
                jpql: `e.${propertyName} LIKE CONCAT('%', :${paramName})`,
                paramName
            };
        } else if (operatorKeyword === 'Containing') {
            // String LIKE with wildcards
            return {
                property: propertyName,
                operator: operatorKeyword,
                jpql: `e.${propertyName} LIKE CONCAT('%', :${paramName}, '%')`,
                paramName
            };
        } else if (operatorKeyword === 'Contains') {
            // Collection membership: :element MEMBER OF e.collection
            return {
                property: propertyName,
                operator: operatorKeyword,
                jpql: `:${paramName} MEMBER OF e.${propertyName}`,
                paramName
            };
        } else if (operatorKeyword === 'NotContains') {
            // Collection membership negation
            return {
                property: propertyName,
                operator: operatorKeyword,
                jpql: `:${paramName} NOT MEMBER OF e.${propertyName}`,
                paramName
            };
        } else if (operatorKeyword === 'Between') {
            // Between needs two parameters
            return {
                property: propertyName,
                operator: operatorKeyword,
                jpql: `e.${propertyName} BETWEEN :${paramName}Start AND :${paramName}End`,
                paramName: `${paramName}Start, ${paramName}End`
            };
        } else if (operatorKeyword === 'In') {
            // IN operator: e.property IN :values
            return {
                property: propertyName,
                operator: operatorKeyword,
                jpql: `e.${propertyName} IN :${paramName}`,
                paramName
            };
        } else if (operatorKeyword === 'NotIn') {
            // NOT IN operator
            return {
                property: propertyName,
                operator: operatorKeyword,
                jpql: `e.${propertyName} NOT IN :${paramName}`,
                paramName
            };
        } else if (operator === '= true' || operator === '= false') {
            return {
                property: propertyName,
                operator: operatorKeyword,
                jpql: `e.${propertyName} ${operator}`,
                paramName: ''
            };
        } else {
            // Standard operator (including Not which becomes !=)
            return {
                property: propertyName,
                operator: operatorKeyword || 'equals',
                jpql: `e.${propertyName} ${operator} :${paramName}`,
                paramName
            };
        }
    }

    private static parseOrderBy(orderByStr: string, rootEntity: Entity): string {
        // Split by And to handle multiple order clauses
        const parts = orderByStr.split('And').map(p => p.trim());
        const orderClauses: string[] = [];

        for (const part of parts) {
            let propertyName = part;
            let direction = 'ASC';

            if (part.endsWith('Desc')) {
                propertyName = part.substring(0, part.length - 'Desc'.length);
                direction = 'DESC';
            } else if (part.endsWith('Asc')) {
                propertyName = part.substring(0, part.length - 'Asc'.length);
                direction = 'ASC';
            }

            propertyName = this.lowercaseFirst(propertyName);
            orderClauses.push(`e.${propertyName} ${direction}`);
        }

        return `ORDER BY ${orderClauses.join(', ')}`;
    }

    private static lowercaseFirst(str: string): string {
        if (!str) return str;
        return str.charAt(0).toLowerCase() + str.slice(1);
    }

    /**
     * Builds parameter mappings from parsed conditions.
     * Extracts parameter placeholders and maps them to expected parameter indices.
     */
    private static buildParameterMappings(
        conditions: Array<{ property: string; operator: string; jpql: string; paramName: string }>
    ): Array<{ placeholder: string; propertyName: string; paramIndex: number }> {
        const mappings: Array<{ placeholder: string; propertyName: string; paramIndex: number }> = [];
        let paramIndex = 0;

        for (const condition of conditions) {
            // Skip logical operators (AND/OR)
            if (condition.operator === 'AND' || condition.operator === 'OR') {
                continue;
            }

            // Skip conditions without parameters (IsNull, IsNotNull, True, False, IsEmpty, IsNotEmpty)
            if (!condition.paramName || condition.paramName === '') {
                continue;
            }

            // Handle Between which has two parameters (propertyStart, propertyEnd)
            if (condition.paramName.includes(',')) {
                const params = condition.paramName.split(',').map(p => p.trim());
                for (const param of params) {
                    mappings.push({
                        placeholder: `:${param}`,
                        propertyName: condition.property,
                        paramIndex: paramIndex++
                    });
                }
            } else {
                // Standard single parameter
                mappings.push({
                    placeholder: `:${condition.paramName}`,
                    propertyName: condition.property,
                    paramIndex: paramIndex++
                });
            }
        }

        return mappings;
    }

    /**
     * Validates that the property exists in the entity.
     * Returns true if valid, false otherwise.
     */
    static validateProperty(propertyName: string, rootEntity: Entity): boolean {
        if (!rootEntity.properties) {
            return false;
        }
        return rootEntity.properties.some(prop => prop.name === propertyName);
    }
}
