package pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.model;

/**
 * Maps a Java method to the saga class it instantiates.
 * Discovered by scanning all Java methods for {@code new *Saga()} constructor calls.
 *
 * @param className     the class containing the method (e.g. "TournamentFunctionalities")
 * @param methodName    the method name (e.g. "createTournament")
 * @param sagaClassName the saga class instantiated (e.g. "CreateTournamentFunctionalitySagas")
 */
public record SagaCreationSite(String className, String methodName, String sagaClassName) {
}
