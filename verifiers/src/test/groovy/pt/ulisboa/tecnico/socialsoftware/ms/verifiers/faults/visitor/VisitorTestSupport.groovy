package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ParserConfiguration
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.ApplicationsFileTreeParser
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path

abstract class VisitorTestSupport extends Specification {

    protected static Path resolveProjectPath(String first, String... more) {
        def direct = Path.of(first, *more)
        if (Files.isDirectory(direct)) return direct
        return Path.of('..').resolve(Path.of(first, *more)).normalize()
    }

    /**
     * Configures StaticJavaParser with Java 21 language level and a symbol solver
     * that resolves dummyapp source + full runtime classpath (simulator, Spring, Jakarta).
     * Call this from setupSpec() in each concrete spec.
     */
    protected static void configureParser() {
        def srcRoot = resolveProjectPath('applications', 'dummyapp', 'src', 'main', 'java')
        def solver = new CombinedTypeSolver(
            new ReflectionTypeSolver(false),
            new JavaParserTypeSolver(srcRoot.toFile())
        )
        StaticJavaParser.getParserConfiguration()
            .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21)
            .setSymbolResolver(new JavaSymbolSolver(solver))
    }

    /**
     * Parses all .java files discovered under applications/dummyapp/src/main/java/.
     * Requires configureParser() to have been called first.
     */
    protected static List<CompilationUnit> parseAllDummyappFiles() {
        def dummyappRoot = resolveProjectPath('applications', 'dummyapp')
        def parser = new ApplicationsFileTreeParser()
        parser.parse(dummyappRoot)
        return parser.javaFilePaths.values().collect { path ->
            StaticJavaParser.parse(path.toFile())
        }
    }
}
