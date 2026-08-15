package pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver;

import java.util.List;
import java.util.Set;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.FunctionalityId;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.StepEffect;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.utils.StringUtils;

/**
 * A pair of functionalities the planner deems worth testing concurrently,
 * together with the evidence of why: the shared identities they conflict on and
 * what each member does to them.
 * <p>
 * Members are held in canonical order ({@code first <= second} by id), so an
 * unordered pair has exactly one representation: structural equality and set
 * membership then behave as pair equality, and the planner's output is
 * comparable across runs.
 * <p>
 * A self-pair — {@code first} and {@code second} being the same catalog
 * functionality — is intentional and means "run two instances of this
 * functionality, with the same arguments, against each other":
 * duplicate-request races (e.g. the same user joining the same tournament twice
 * concurrently) are a real anomaly class.
 *
 * @param first     the member that sorts first by id (equal to {@code second}
 *                  in a self-pair)
 * @param second    the member that sorts second by id
 * @param conflicts the per-identity evidence this group was built from
 */
public record FunctionalityGroup(
        FunctionalityId first, FunctionalityId second, Set<Conflict> conflicts) {

    // TODO group-level emergent effects: effects observed only when a group
    // * runs concurrently (extra compensations, event cascades) could be fed back
    // * so that e.g. A+B jointly conflicting with C is discovered even though
    // * neither A nor B alone conflicts with C.

    // TODO explore larger groups mechanisms (conflict-graph components or cliques)

    public FunctionalityGroup {
        if (first.toString().compareTo(second.toString()) > 0) {
            throw new IllegalArgumentException(
                    "Members must be in canonical order, got ('%s', '%s')".formatted(first, second));
        }
        conflicts = Set.copyOf(conflicts);
    }

    /**
     * The conflict evidence on one shared identity (aggregate handle or type):
     * what {@code first} and {@code second} do to it. At least one side always
     * contains a WRITE — read-read overlaps are not conflicts
     * (see {@link FunctionalityGroupPlanner}).
     */
    public record Conflict(
            String identity,
            Set<StepEffect.EffectKind> firstMemberKinds,
            Set<StepEffect.EffectKind> secondMemberKinds) {

        public Conflict {
            firstMemberKinds = Set.copyOf(firstMemberKinds);
            secondMemberKinds = Set.copyOf(secondMemberKinds);
        }
    }

    /**
     * The functionalities to run concurrently, in canonical order
     * (a self-pair repeats the same id twice).
     */
    public List<FunctionalityId> members() {
        return List.of(first, second);
    }

    public boolean isSelfPair() {
        return first.equals(second);
    }

    /**
     * A stable, filesystem-safe name for this group, derived from its members in
     * canonical order.
     */
    public String label() {
        return StringUtils.toFileNameSafe(first + "__" + second);
    }

    /** Whether this group is exactly the (unordered) pair {@code a}, {@code b}. */
    public boolean isPairOf(FunctionalityId a, FunctionalityId b) {
        return (first.equals(a) && second.equals(b))
                || (first.equals(b) && second.equals(a));
    }
}
