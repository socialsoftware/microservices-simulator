/**
 * Input Validation Utilities
 * 
 * Centralized input validation to prevent security issues and ensure data integrity.
 * Includes path traversal protection, project name validation, and input sanitization.
 */

import * as path from 'node:path';

export interface ValidationResult {
    isValid: boolean;
    error?: string;
    sanitized?: string;
}

/**
 * Centralized input validator with security-focused validation rules
 */
export class InputValidator {

    /**
     * Validate file path to prevent path traversal attacks
     * @param filePath - The file path to validate
     * @param baseDir - The base directory that paths should be within (optional)
     * @returns Validation result with sanitized path
     */
    static validateFilePath(filePath: string, baseDir?: string): ValidationResult {
        if (!filePath || typeof filePath !== 'string') {
            return { isValid: false, error: 'File path is required and must be a string' };
        }

        // Check for path traversal attempts
        if (filePath.includes('..') || filePath.includes('~')) {
            return { isValid: false, error: 'Path traversal attempts are not allowed' };
        }

        // Check for absolute paths outside project (if baseDir provided)
        if (baseDir && path.isAbsolute(filePath)) {
            const resolvedPath = path.resolve(filePath);
            const resolvedBase = path.resolve(baseDir);

            if (!resolvedPath.startsWith(resolvedBase)) {
                return { isValid: false, error: 'Absolute paths outside project directory are not allowed' };
            }
        }

        // Normalize the path
        const sanitized = path.normalize(filePath);

        // Additional security checks
        if (sanitized.includes('\0')) {
            return { isValid: false, error: 'Null bytes in path are not allowed' };
        }

        return { isValid: true, sanitized };
    }

    /**
     * Validate project name according to Java/Maven conventions
     * @param projectName - The project name to validate
     * @returns Validation result with sanitized name
     */
    static validateProjectName(projectName: string): ValidationResult {
        if (!projectName || typeof projectName !== 'string') {
            return { isValid: false, error: 'Project name is required and must be a string' };
        }

        // Trim whitespace
        const trimmed = projectName.trim();

        if (trimmed.length === 0) {
            return { isValid: false, error: 'Project name cannot be empty' };
        }

        if (trimmed.length > 50) {
            return { isValid: false, error: 'Project name must be 50 characters or less' };
        }

        // Check for valid characters (alphanumeric, hyphens, underscores)
        const validPattern = /^[a-zA-Z][a-zA-Z0-9_-]*$/;
        if (!validPattern.test(trimmed)) {
            return { isValid: false, error: 'Project name must start with a letter and contain only letters, numbers, hyphens, and underscores' };
        }

        // Check for reserved names
        const reservedNames = ['con', 'prn', 'aux', 'nul', 'com1', 'com2', 'com3', 'com4', 'com5', 'com6', 'com7', 'com8', 'com9', 'lpt1', 'lpt2', 'lpt3', 'lpt4', 'lpt5', 'lpt6', 'lpt7', 'lpt8', 'lpt9'];
        if (reservedNames.includes(trimmed.toLowerCase())) {
            return { isValid: false, error: 'Project name cannot be a reserved system name' };
        }

        return { isValid: true, sanitized: trimmed };
    }

    /**
     * Validate Java package name
     * @param packageName - The package name to validate
     * @returns Validation result with sanitized package name
     */
    static validatePackageName(packageName: string): ValidationResult {
        if (!packageName || typeof packageName !== 'string') {
            return { isValid: false, error: 'Package name is required and must be a string' };
        }

        const trimmed = packageName.trim();

        if (trimmed.length === 0) {
            return { isValid: false, error: 'Package name cannot be empty' };
        }

        // Check overall pattern (segments separated by dots)
        const packagePattern = /^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)*$/;
        if (!packagePattern.test(trimmed)) {
            return { isValid: false, error: 'Package name must follow Java naming conventions (lowercase, dots as separators, no leading numbers)' };
        }

        // Check individual segments
        const segments = trimmed.split('.');
        for (const segment of segments) {
            if (segment.length === 0) {
                return { isValid: false, error: 'Package name cannot have empty segments' };
            }

            if (segment.length > 50) {
                return { isValid: false, error: 'Package name segments must be 50 characters or less' };
            }

            // Check for Java reserved words
            const javaKeywords = ['abstract', 'assert', 'boolean', 'break', 'byte', 'case', 'catch', 'char', 'class', 'const', 'continue', 'default', 'do', 'double', 'else', 'enum', 'extends', 'final', 'finally', 'float', 'for', 'goto', 'if', 'implements', 'import', 'instanceof', 'int', 'interface', 'long', 'native', 'new', 'package', 'private', 'protected', 'public', 'return', 'short', 'static', 'strictfp', 'super', 'switch', 'synchronized', 'this', 'throw', 'throws', 'transient', 'try', 'void', 'volatile', 'while'];
            if (javaKeywords.includes(segment)) {
                return { isValid: false, error: `Package segment '${segment}' is a Java reserved word` };
            }
        }

        return { isValid: true, sanitized: trimmed };
    }

    /**
     * Sanitize general string input by removing dangerous characters
     * @param input - The input string to sanitize
     * @param maxLength - Maximum allowed length (default: 255)
     * @returns Validation result with sanitized input
     */
    static sanitizeInput(input: string, maxLength: number = 255): ValidationResult {
        if (!input || typeof input !== 'string') {
            return { isValid: false, error: 'Input must be a string' };
        }

        // Remove null bytes and control characters
        let sanitized = input.replace(/[\x00-\x1F\x7F]/g, '');

        // Trim whitespace
        sanitized = sanitized.trim();

        // Check length
        if (sanitized.length > maxLength) {
            return { isValid: false, error: `Input must be ${maxLength} characters or less` };
        }

        return { isValid: true, sanitized };
    }

    /**
     * Validate database name
     * @param dbName - The database name to validate
     * @returns Validation result with sanitized name
     */
    static validateDatabaseName(dbName: string): ValidationResult {
        if (!dbName || typeof dbName !== 'string') {
            return { isValid: false, error: 'Database name is required and must be a string' };
        }

        const trimmed = dbName.trim();

        if (trimmed.length === 0) {
            return { isValid: false, error: 'Database name cannot be empty' };
        }

        if (trimmed.length > 63) {
            return { isValid: false, error: 'Database name must be 63 characters or less' };
        }

        // Check for valid characters (alphanumeric and underscores, no hyphens for DB names)
        const validPattern = /^[a-zA-Z][a-zA-Z0-9_]*$/;
        if (!validPattern.test(trimmed)) {
            return { isValid: false, error: 'Database name must start with a letter and contain only letters, numbers, and underscores' };
        }

        return { isValid: true, sanitized: trimmed };
    }

    /**
     * Validate port number
     * @param port - The port number to validate (string or number)
     * @returns Validation result with sanitized port number
     */
    static validatePort(port: string | number): ValidationResult {
        let portNum: number;

        if (typeof port === 'string') {
            portNum = parseInt(port, 10);
            if (isNaN(portNum)) {
                return { isValid: false, error: 'Port must be a valid number' };
            }
        } else if (typeof port === 'number') {
            portNum = port;
        } else {
            return { isValid: false, error: 'Port must be a number or string' };
        }

        if (portNum < 1 || portNum > 65535) {
            return { isValid: false, error: 'Port must be between 1 and 65535' };
        }

        // Warn about privileged ports
        if (portNum < 1024) {
            console.warn(`Warning: Port ${portNum} is a privileged port and may require elevated permissions`);
        }

        return { isValid: true, sanitized: portNum.toString() };
    }

    /**
     * Validate hostname or IP address
     * @param host - The hostname or IP to validate
     * @returns Validation result with sanitized host
     */
    static validateHost(host: string): ValidationResult {
        if (!host || typeof host !== 'string') {
            return { isValid: false, error: 'Host is required and must be a string' };
        }

        const trimmed = host.trim().toLowerCase();

        if (trimmed.length === 0) {
            return { isValid: false, error: 'Host cannot be empty' };
        }

        // Check for localhost variations
        const localhostVariations = ['localhost', '127.0.0.1', '::1', '0.0.0.0'];
        if (localhostVariations.includes(trimmed)) {
            return { isValid: true, sanitized: trimmed };
        }

        // Basic hostname validation (simplified)
        const hostnamePattern = /^[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?(\.[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?)*$/;
        if (hostnamePattern.test(trimmed)) {
            return { isValid: true, sanitized: trimmed };
        }

        // Basic IPv4 validation
        const ipv4Pattern = /^(\d{1,3}\.){3}\d{1,3}$/;
        if (ipv4Pattern.test(trimmed)) {
            const parts = trimmed.split('.');
            for (const part of parts) {
                const num = parseInt(part, 10);
                if (num < 0 || num > 255) {
                    return { isValid: false, error: 'Invalid IPv4 address' };
                }
            }
            return { isValid: true, sanitized: trimmed };
        }

        return { isValid: false, error: 'Host must be a valid hostname or IP address' };
    }

    /**
     * Validate file extension
     * @param filename - The filename to validate
     * @param allowedExtensions - Array of allowed extensions (e.g., ['.nebula', '.java'])
     * @returns Validation result
     */
    static validateFileExtension(filename: string, allowedExtensions: string[]): ValidationResult {
        if (!filename || typeof filename !== 'string') {
            return { isValid: false, error: 'Filename is required and must be a string' };
        }

        const ext = path.extname(filename).toLowerCase();

        if (allowedExtensions.length > 0 && !allowedExtensions.includes(ext)) {
            return { isValid: false, error: `File extension must be one of: ${allowedExtensions.join(', ')}` };
        }

        return { isValid: true, sanitized: filename };
    }
}

/**
 * Security utilities for additional protection
 */
export class SecurityUtils {

    /**
     * Check if a path is within the allowed base directory
     * @param targetPath - The path to check
     * @param baseDir - The base directory
     * @returns True if path is safe, false otherwise
     */
    static isPathWithinBase(targetPath: string, baseDir: string): boolean {
        const resolvedTarget = path.resolve(targetPath);
        const resolvedBase = path.resolve(baseDir);

        return resolvedTarget.startsWith(resolvedBase + path.sep) || resolvedTarget === resolvedBase;
    }

    /**
     * Sanitize filename by removing dangerous characters
     * @param filename - The filename to sanitize
     * @returns Sanitized filename
     */
    static sanitizeFilename(filename: string): string {
        if (!filename) return '';

        // Remove path separators and dangerous characters
        return filename
            .replace(/[<>:"/\\|?*\x00-\x1f]/g, '')
            .replace(/^\.+/, '') // Remove leading dots
            .trim();
    }

    /**
     * Generate a safe directory name from user input
     * @param input - User input
     * @returns Safe directory name
     */
    static generateSafeDirectoryName(input: string): string {
        if (!input) return 'untitled';

        return input
            .toLowerCase()
            .replace(/[^a-z0-9]/g, '-')
            .replace(/-+/g, '-')
            .replace(/^-|-$/g, '')
            .substring(0, 50) || 'untitled';
    }
}

/**
 * Validation error class for better error handling
 */
export class ValidationError extends Error {
    constructor(message: string, public field?: string, public value?: any) {
        super(message);
        this.name = 'ValidationError';
    }
}

/**
 * Utility function to throw validation error if validation fails
 * @param result - Validation result
 * @param field - Field name for error context
 */
export function assertValid(result: ValidationResult, field?: string): string {
    if (!result.isValid) {
        throw new ValidationError(result.error || 'Validation failed', field);
    }
    return result.sanitized || '';
}
