# Langium Project

This project is a Langium-based project, which is a framework for developing domain-specific languages (DSLs). The project includes a variety of files that contribute to the functionality of the language being developed.

## File Structure

- `generator.ts`: This file is responsible for generating output based on the abstract syntax tree (AST) of the language. It takes an `Aggregate` object, which represents a collection of domain events, and returns the length of these events as a string.

- `ast.js`: This file contains the generated AST for the language. The AST is a tree representation of the abstract syntactic structure of the language.

## Usage

The usage should use the following commands:

1. `npm run langium:generate`: This command generates the necessary artifacts based on the Langium language specification. It generates code for the language parser, lexer, and other language-specific components.

2. `npm run build`: This command builds the project by compiling the source code and generating the executable files or artifacts.

3. `npm run langium:generate && npm run build`: This command combines the previous two commands. It first generates the artifacts using Langium and then builds the project.

4. `./bin/cli.js generate examples/aggregate.nebula`: This command runs the CLI (Command Line Interface) tool located in the `bin` directory. It generates the examples based on the `aggregate.nebula` file.
