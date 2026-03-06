package pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.scenariogenerator.visitor;

import org.codehaus.groovy.ast.ClassCodeVisitorSupport;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.control.SourceUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Visits Groovy/Spock test files to extract test inputs for scenario generation.
 * <p>
 * Currently prints the name of each class found in analyzed Groovy files.
 */
public class SpockTestVisitor extends ClassCodeVisitorSupport {
    private static final Logger logger = LoggerFactory.getLogger(SpockTestVisitor.class);

    @Override
    public void visitClass(ClassNode node) {
        logger.info("Spock spec: {}", node.getName());
    }

    @Override
    protected SourceUnit getSourceUnit() {
        return null;
    }
}
