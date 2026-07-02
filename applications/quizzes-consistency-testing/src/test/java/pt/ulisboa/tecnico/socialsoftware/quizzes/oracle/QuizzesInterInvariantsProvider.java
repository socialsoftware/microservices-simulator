package pt.ulisboa.tecnico.socialsoftware.quizzes.oracle;

import java.util.Set;

import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.InterInvariant;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.InterInvariantsProvider;

@Component
@Profile("oracle")
public class QuizzesInterInvariantsProvider implements InterInvariantsProvider {

    @Override
    public @NonNull Set<InterInvariant> getInterInvariants() {
        // ! TODO Implement with the real inter-invariants set
        return Set.of();
    }
}
