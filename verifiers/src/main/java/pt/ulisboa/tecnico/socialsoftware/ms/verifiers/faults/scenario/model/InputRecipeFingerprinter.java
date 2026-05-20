package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.function.Consumer;

final class InputRecipeFingerprinter {

    private static final HexFormat HEX = HexFormat.of();

    private InputRecipeFingerprinter() {
    }

    static String fingerprint(String schemaVersion,
                              boolean executorReady,
                              List<String> blockers,
                              List<InputRecipeArgument> arguments) {
        return hash(digest -> {
            updateString(digest, "input-recipe");
            updateString(digest, schemaVersion);
            updateBoolean(digest, executorReady);
            updateStrings(digest, blockers);
            updateArguments(digest, arguments);
        });
    }

    private static void updateArguments(MessageDigest digest, List<InputRecipeArgument> arguments) {
        List<InputRecipeArgument> safeArguments = arguments == null ? List.of() : arguments;
        updateInt(digest, safeArguments.size());
        for (InputRecipeArgument argument : safeArguments) {
            updateInt(digest, argument == null ? -1 : argument.index());
            updateString(digest, argument == null ? null : argument.expectedTypeFqn());
            updateString(digest, argument == null || argument.resolutionStatus() == null ? null : argument.resolutionStatus().name());
            updateBoolean(digest, argument != null && argument.executorReady());
            updateStrings(digest, argument == null ? List.of() : argument.blockers());
            updateString(digest, argument == null ? null : argument.provenanceText());
            updateNode(digest, argument == null ? null : argument.recipe());
        }
    }

    private static void updateAssignments(MessageDigest digest, List<InputRecipeAssignment> assignments) {
        List<InputRecipeAssignment> safeAssignments = assignments == null ? List.of() : assignments;
        updateInt(digest, safeAssignments.size());
        for (InputRecipeAssignment assignment : safeAssignments) {
            updateString(digest, assignment == null ? null : assignment.assignmentKind());
            updateString(digest, assignment == null ? null : assignment.propertyName());
            updateString(digest, assignment == null ? null : assignment.sourceName());
            updateInt(digest, assignment == null ? -1 : assignment.orderIndex());
            updateString(digest, assignment == null ? null : assignment.sourceText());
            updateBoolean(digest, assignment != null && assignment.executorReady());
            updateStrings(digest, assignment == null ? List.of() : assignment.blockers());
            updateNode(digest, assignment == null ? null : assignment.valueRecipe());
        }
    }

    private static void updateMapEntries(MessageDigest digest, List<InputRecipeMapEntry> entries) {
        List<InputRecipeMapEntry> safeEntries = entries == null ? List.of() : entries;
        updateInt(digest, safeEntries.size());
        for (InputRecipeMapEntry entry : safeEntries) {
            updateInt(digest, entry == null ? -1 : entry.index());
            updateNode(digest, entry == null ? null : entry.keyRecipe());
            updateNode(digest, entry == null ? null : entry.valueRecipe());
        }
    }

    private static void updateNodes(MessageDigest digest, List<InputRecipeNode> nodes) {
        List<InputRecipeNode> safeNodes = nodes == null ? List.of() : nodes;
        updateInt(digest, safeNodes.size());
        for (InputRecipeNode node : safeNodes) {
            updateNode(digest, node);
        }
    }

    private static void updateNode(MessageDigest digest, InputRecipeNode node) {
        updateBoolean(digest, node != null);
        if (node == null) {
            return;
        }

        updateString(digest, node.kind());
        updateString(digest, node.sourceText());
        updateString(digest, node.provenanceText());
        updateBoolean(digest, node.executorReady());
        updateStrings(digest, node.blockers());
        updateString(digest, node.literalKind());
        updateValue(digest, node.value());
        updateString(digest, node.targetTypeFqn());
        updateString(digest, node.targetTypeText());
        updateArguments(digest, node.arguments());
        updateAssignments(digest, node.assignments());
        updateString(digest, node.collectionKind());
        updateNodes(digest, node.elements());
        updateMapEntries(digest, node.entries());
        updateString(digest, node.transformName());
        updateString(digest, node.targetType());
        updateNode(digest, node.receiver());
        updateString(digest, node.receiverReference());
        updateString(digest, node.methodName());
        updateArguments(digest, node.callArguments());
        updateString(digest, node.expectedReturnTypeFqn());
        updateString(digest, node.propertyName());
        updateString(digest, node.placeholderId());
        updateString(digest, node.placeholderPurpose());
        updateString(digest, node.expectedTypeFqn());
        updateString(digest, node.helperName());
        updateNode(digest, node.resultRecipe());
        updateString(digest, node.internalCategory());
    }

    private static void updateStrings(MessageDigest digest, List<String> values) {
        List<String> safeValues = values == null ? List.of() : values;
        updateInt(digest, safeValues.size());
        for (String value : safeValues) {
            updateString(digest, value);
        }
    }

    private static void updateValue(MessageDigest digest, Object value) {
        if (value == null) {
            updateString(digest, null);
            return;
        }
        if (value instanceof BigDecimal decimal) {
            updateString(digest, "bigdecimal");
            updateString(digest, decimal.stripTrailingZeros().toPlainString());
            return;
        }
        updateString(digest, value.getClass().getName());
        updateString(digest, String.valueOf(value));
    }

    private static String hash(Consumer<MessageDigest> updateAction) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            updateAction.accept(digest);
            return HEX.formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Missing SHA-256 support", exception);
        }
    }

    private static void updateString(MessageDigest digest, String value) {
        if (value == null) {
            updateInt(digest, -1);
            return;
        }
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        updateInt(digest, bytes.length);
        digest.update(bytes);
    }

    private static void updateBoolean(MessageDigest digest, boolean value) {
        digest.update((byte) (value ? 1 : 0));
    }

    private static void updateInt(MessageDigest digest, int value) {
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(value).array());
    }
}
