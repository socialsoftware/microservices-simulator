package qualification.copiedupdate;

import com.github.javaparser.StaticJavaParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.ApplicationAnalysisState;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor.ConstructorCopyVisitor;

/** Runs the production inference independently for controlled-history qualification. */
public final class ExtractContracts {
    public static void main(String[] args) throws Exception {
        StaticJavaParser.getParserConfiguration().setLanguageLevel(com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_21);
        var visitor = new ConstructorCopyVisitor();
        var state = new ApplicationAnalysisState();
        try (var files = Files.walk(Path.of(args[0]))) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).sorted().toList())
                visitor.visit(StaticJavaParser.parse(file), state);
        }
        visitor.finish(state);
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(Path.of(args[1]).toFile(), state.copyContractArtifact);
    }
}
