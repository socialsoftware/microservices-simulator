package pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis

import com.github.javaparser.ParserConfiguration
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver
import spock.lang.Specification

import java.nio.file.Path
import java.nio.file.Paths

abstract class AnalysisTestSupport extends Specification {

    static final String TEST_APP_BASE =
            "src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/faultanalysis/testapp"

    def setupSpec() {
        def solver = new CombinedTypeSolver(
                new ReflectionTypeSolver(false),
                new JavaParserTypeSolver(new File("src/test/java"))
        )
        StaticJavaParser.getParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21)
                .setSymbolResolver(new JavaSymbolSolver(solver))
    }

    protected CompilationUnit parseFile(Path path) {
        return StaticJavaParser.parse(path)
    }

    protected Path testAppPath(String relativePath) {
        Paths.get("${TEST_APP_BASE}/${relativePath}")
    }
}
