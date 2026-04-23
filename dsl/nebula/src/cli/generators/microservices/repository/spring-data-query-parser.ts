import { Entity } from "../../../../language/generated/ast.js";

export interface QueryParseResult {
    query: string;
    parameterMappings: Array<{
        placeholder: string;  
        propertyName: string;  
        paramIndex: number;    
    }>;
}



export class SpringDataQueryParser {

    private static readonly OPERATORS: { [key: string]: string } = {
        'GreaterThan': '>',
        'LessThan': '<',
        'GreaterThanEqual': '>=',
        'LessThanEqual': '<=',
        'Between': 'BETWEEN',
        'Like': 'LIKE',
        'NotLike': 'NOT LIKE',
        'StartingWith': 'LIKE',  
        'EndingWith': 'LIKE',     
        'Containing': 'LIKE',     
        'Contains': 'MEMBER OF',  
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

    

    static parseMethodName(methodName: string, entityName: string, rootEntity: Entity): QueryParseResult | null {
        
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

        
        return null;
    }

    

    static parseMethodNameLegacy(methodName: string, entityName: string, rootEntity: Entity): string | null {
        const result = this.parseMethodName(methodName, entityName, rootEntity);
        return result ? result.query : null;
    }

    private static parseFindByQuery(methodName: string, entityName: string, rootEntity: Entity): QueryParseResult {
        const afterFindBy = methodName.substring('findBy'.length);

        
        if (afterFindBy === '') {
            return {
                query: `SELECT e FROM ${entityName} e`,
                parameterMappings: []
            };
        }

        
        const { conditions, orderBy } = this.parseConditionsAndOrdering(afterFindBy, rootEntity);

        const whereClause = conditions.map(cond => cond.jpql).join(' ');
        const queryBase = `SELECT e FROM ${entityName} e WHERE ${whereClause}`;

        const query = orderBy ? `${queryBase} ${orderBy}` : queryBase;

        
        const parameterMappings = this.buildParameterMappings(conditions);

        return { query, parameterMappings };
    }

    private static parseExistsByQuery(methodName: string, entityName: string, rootEntity: Entity): QueryParseResult {
        const afterExistsBy = methodName.substring('existsBy'.length);

        if (afterExistsBy === '') {
            
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
        
        const orderByIndex = expression.indexOf('OrderBy');
        let conditionsStr = expression;
        let orderBy: string | undefined;

        if (orderByIndex !== -1) {
            conditionsStr = expression.substring(0, orderByIndex);
            const orderByStr = expression.substring(orderByIndex + 'OrderBy'.length);
            orderBy = this.parseOrderBy(orderByStr, rootEntity);
        }

        
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
            
            if (conditionsStr.substring(i, i + 3) === 'And') {
                if (current.trim()) {
                    tokens.push({ type: 'CONDITION', value: current.trim() });
                    current = '';
                }
                tokens.push({ type: 'AND', value: 'And' });
                i += 3;
            }
            
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
        
        
        let operator = '=';  
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

        
        propertyName = this.lowercaseFirst(propertyName);
        const paramName = propertyName;

        
        if (operatorKeyword === 'IsNull' || operatorKeyword === 'IsNotNull') {
            return {
                property: propertyName,
                operator: operatorKeyword,
                jpql: `e.${propertyName} ${operator}`,
                paramName: ''
            };
        } else if (operatorKeyword === 'IsEmpty' || operatorKeyword === 'IsNotEmpty') {
            
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
            
            return {
                property: propertyName,
                operator: operatorKeyword,
                jpql: `e.${propertyName} LIKE CONCAT('%', :${paramName}, '%')`,
                paramName
            };
        } else if (operatorKeyword === 'Contains') {
            
            return {
                property: propertyName,
                operator: operatorKeyword,
                jpql: `:${paramName} MEMBER OF e.${propertyName}`,
                paramName
            };
        } else if (operatorKeyword === 'NotContains') {
            
            return {
                property: propertyName,
                operator: operatorKeyword,
                jpql: `:${paramName} NOT MEMBER OF e.${propertyName}`,
                paramName
            };
        } else if (operatorKeyword === 'Between') {
            
            return {
                property: propertyName,
                operator: operatorKeyword,
                jpql: `e.${propertyName} BETWEEN :${paramName}Start AND :${paramName}End`,
                paramName: `${paramName}Start, ${paramName}End`
            };
        } else if (operatorKeyword === 'In') {
            
            return {
                property: propertyName,
                operator: operatorKeyword,
                jpql: `e.${propertyName} IN :${paramName}`,
                paramName
            };
        } else if (operatorKeyword === 'NotIn') {
            
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
            
            return {
                property: propertyName,
                operator: operatorKeyword || 'equals',
                jpql: `e.${propertyName} ${operator} :${paramName}`,
                paramName
            };
        }
    }

    private static parseOrderBy(orderByStr: string, rootEntity: Entity): string {
        
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

    

    private static buildParameterMappings(
        conditions: Array<{ property: string; operator: string; jpql: string; paramName: string }>
    ): Array<{ placeholder: string; propertyName: string; paramIndex: number }> {
        const mappings: Array<{ placeholder: string; propertyName: string; paramIndex: number }> = [];
        let paramIndex = 0;

        for (const condition of conditions) {
            
            if (condition.operator === 'AND' || condition.operator === 'OR') {
                continue;
            }

            
            if (!condition.paramName || condition.paramName === '') {
                continue;
            }

            
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
                
                mappings.push({
                    placeholder: `:${condition.paramName}`,
                    propertyName: condition.property,
                    paramIndex: paramIndex++
                });
            }
        }

        return mappings;
    }

    

    static validateProperty(propertyName: string, rootEntity: Entity): boolean {
        if (!rootEntity.properties) {
            return false;
        }
        return rootEntity.properties.some(prop => prop.name === propertyName);
    }
}
