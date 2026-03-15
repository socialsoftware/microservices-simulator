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

    static String targetApplicationsDir
    static String targetApplicationBasePackage
    static String javaSourceDir
    static String basePackagePath

    def setupSpec() {
        def props = new Properties()
        new File("src/test/resources/application-test.properties").withInputStream {
            props.load(it)
        }
        targetApplicationsDir = props.getProperty("simulator.analysis.target-applications-dir")
        targetApplicationBasePackage = props.getProperty("simulator.analysis.target-application-base-package")
        javaSourceDir = "${targetApplicationsDir}/src/main/java"
        basePackagePath = targetApplicationBasePackage.replace('.', '/')

        def solver = new CombinedTypeSolver(
                new ReflectionTypeSolver(false),
                new JavaParserTypeSolver(new File(javaSourceDir))
        )
        StaticJavaParser.getParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21)
                .setSymbolResolver(new JavaSymbolSolver(solver))
    }

    protected CompilationUnit parseFile(Path path) {
        return StaticJavaParser.parse(path)
    }

    protected Path testAppPath(String relativePath) {
        Paths.get("${javaSourceDir}/${basePackagePath}/${relativePath}")
    }

    protected FaultAnalysisProperties testAppProperties() {
        new FaultAnalysisProperties(targetApplicationsDir, targetApplicationBasePackage)
    }
}
