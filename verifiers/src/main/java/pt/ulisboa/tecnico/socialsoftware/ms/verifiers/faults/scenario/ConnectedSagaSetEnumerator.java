package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ConnectedSagaSetEnumerator {

    private ConnectedSagaSetEnumerator() {
    }

    public static Result enumerate(List<String> sagaFqns, Map<String, Set<String>> adjacency, int maxSagaSetSize) {
        List<String> sortedSagas = sagaFqns == null ? List.of() : sagaFqns.stream()
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();

        LinkedHashMap<String, Integer> counts = new LinkedHashMap<>();
        List<List<String>> connectedSets = new ArrayList<>();

        int upperBound = Math.min(Math.max(0, maxSagaSetSize), sortedSagas.size());
        int seen = 0;
        int accepted = 0;

        for (int size = 2; size <= upperBound; size++) {
            seen += enumerateCombinations(sortedSagas, adjacency, size, 0, new ArrayList<>(), connectedSets);
        }

        accepted = connectedSets.size();
        counts.put("connectedSagaSetsSeen", seen);
        counts.put("connectedSagaSetsEmitted", accepted);
        counts.put("connectedSagaSetsPruned", seen - accepted);

        return new Result(List.copyOf(connectedSets), Collections.unmodifiableMap(counts), List.of());
    }

    private static int enumerateCombinations(List<String> sagas,
                                             Map<String, Set<String>> adjacency,
                                             int targetSize,
                                             int startIndex,
                                             List<String> current,
                                             List<List<String>> connectedSets) {
        if (current.size() == targetSize) {
            if (isConnected(current, adjacency)) {
                connectedSets.add(List.copyOf(current));
            }
            return 1;
        }

        int seen = 0;
        for (int index = startIndex; index < sagas.size(); index++) {
            current.add(sagas.get(index));
            seen += enumerateCombinations(sagas, adjacency, targetSize, index + 1, current, connectedSets);
            current.remove(current.size() - 1);
        }
        return seen;
    }

    private static boolean isConnected(List<String> sagas, Map<String, Set<String>> adjacency) {
        if (sagas.size() <= 1) {
            return true;
        }

        Set<String> allowed = new LinkedHashSet<>(sagas);
        LinkedHashSet<String> visited = new LinkedHashSet<>();
        ArrayList<String> queue = new ArrayList<>();
        queue.add(sagas.get(0));
        visited.add(sagas.get(0));

        for (int index = 0; index < queue.size(); index++) {
            String current = queue.get(index);
            for (String neighbor : adjacency.getOrDefault(current, Set.of())) {
                if (allowed.contains(neighbor) && visited.add(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }

        return visited.size() == allowed.size();
    }

    public record Result(List<List<String>> connectedSagaSets,
                         Map<String, Integer> counts,
                         List<String> warnings) {
    }
}
