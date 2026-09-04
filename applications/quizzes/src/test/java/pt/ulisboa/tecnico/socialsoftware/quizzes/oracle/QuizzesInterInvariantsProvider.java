package pt.ulisboa.tecnico.socialsoftware.quizzes.oracle;

import java.util.Set;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.InterInvariant;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.InterInvariantsProvider;

@Component
@Profile("oracle")
public class QuizzesInterInvariantsProvider implements InterInvariantsProvider {

    @Override
    public Set<InterInvariant> getInterInvariants() {
        return Set.of();
    }
}
