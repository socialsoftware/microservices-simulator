package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class DefaultProcessRunner implements ProcessRunner {
    private static final int MAX_CAPTURED_OUTPUT_BYTES = 1_048_576;

    private final ProcessFactory processFactory;

    public DefaultProcessRunner() {
        this(command -> {
            ProcessBuilder builder = new ProcessBuilder(command.arguments());
            builder.directory(command.workingDirectory().toFile());
            return builder.start();
        });
    }

    DefaultProcessRunner(ProcessFactory processFactory) {
        this.processFactory = Objects.requireNonNull(processFactory, "processFactory cannot be null");
    }

    @Override
    public ProcessResult run(ProcessCommand command) throws IOException, InterruptedException {
        Objects.requireNonNull(command, "command cannot be null");
        Process process = processFactory.start(command);
        ExecutorService gobblerExecutor = Executors.newFixedThreadPool(2, runnable -> {
            Thread thread = new Thread(runnable, "default-process-runner-gobbler");
            thread.setDaemon(true);
            return thread;
        });
        Future<String> stdout = gobblerExecutor.submit(() -> readUtf8(process.getInputStream()));
        Future<String> stderr = gobblerExecutor.submit(() -> readUtf8(process.getErrorStream()));
        try {
            boolean timedOut = waitForProcess(process, command.timeout());
            String stdoutText = awaitOutput(stdout);
            String stderrText = awaitOutput(stderr);
            return new ProcessResult(timedOut ? -1 : process.exitValue(), stdoutText, stderrText, timedOut);
        } catch (InterruptedException e) {
            terminateProcess(process);
            closeProcessStreams(process);
            cancelFuture(stdout);
            cancelFuture(stderr);
            throw e;
        } catch (ExecutionException e) {
            throw new IOException("Failed to capture process output", e.getCause());
        } finally {
            shutdownGobblerExecutor(gobblerExecutor);
        }
    }

    private boolean waitForProcess(Process process, Duration timeout) throws InterruptedException {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            process.waitFor();
            return false;
        }
        boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (!finished) {
            terminateProcess(process);
            process.waitFor();
            return true;
        }
        return false;
    }

    private String awaitOutput(Future<String> future) throws InterruptedException, ExecutionException {
        try {
            return future.get();
        } catch (CancellationException e) {
            return "";
        }
    }

    private void cancelFuture(Future<String> future) {
        if (future != null) {
            future.cancel(true);
        }
    }

    private void terminateProcess(Process process) {
        if (process == null || !process.isAlive()) {
            return;
        }
        process.destroy();
        process.destroyForcibly();
    }

    private void closeProcessStreams(Process process) {
        if (process == null) {
            return;
        }
        closeQuietly(process.getInputStream());
        closeQuietly(process.getErrorStream());
        closeQuietly(process.getOutputStream());
    }

    private void closeQuietly(java.io.Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException ignored) {
            // Best-effort cleanup only.
        }
    }

    private void shutdownGobblerExecutor(ExecutorService executor) {
        if (executor == null) {
            return;
        }
        executor.shutdownNow();
        try {
            if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                executor.awaitTermination(1, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String readUtf8(InputStream inputStream) {
        try (inputStream) {
            ByteArrayOutputStream captured = new ByteArrayOutputStream(Math.min(8192, MAX_CAPTURED_OUTPUT_BYTES));
            byte[] buffer = new byte[8192];
            int bytesRead;
            int remaining = MAX_CAPTURED_OUTPUT_BYTES;
            boolean truncated = false;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                int bytesToCapture = 0;
                if (remaining > 0) {
                    bytesToCapture = Math.min(bytesRead, remaining);
                    captured.write(buffer, 0, bytesToCapture);
                    remaining -= bytesToCapture;
                }
                if (bytesToCapture < bytesRead) {
                    truncated = true;
                }
            }
            String text = captured.toString(StandardCharsets.UTF_8);
            return truncated ? text + System.lineSeparator() + "<process output truncated after " + MAX_CAPTURED_OUTPUT_BYTES + " bytes>" : text;
        } catch (IOException e) {
            return "<failed to capture process output: " + e.getMessage() + ">";
        }
    }
}
