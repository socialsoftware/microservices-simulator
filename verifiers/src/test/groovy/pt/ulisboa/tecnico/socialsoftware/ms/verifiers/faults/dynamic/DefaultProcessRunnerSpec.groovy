package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic

import spock.lang.Specification
import spock.lang.TempDir

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class DefaultProcessRunnerSpec extends Specification {

    @TempDir
    Path tempDir

    def 'interrupting a running Maven process destroys it and closes the output gobblers'() {
        given:
        def workingDir = tempDir.resolve('working-dir')
        Files.createDirectories(workingDir)
        def process = new ControlledProcess()
        def runner = new DefaultProcessRunner({ ProcessRunner.ProcessCommand command ->
            assert command.workingDirectory() == workingDir
            assert command.arguments() == ['mvn', 'test']
            process
        })
        def command = new ProcessRunner.ProcessCommand(workingDir, ['mvn', 'test'], Duration.ofSeconds(30))
        def failure = new AtomicReference<Throwable>()
        def runnerThread = new Thread({
            try {
                runner.run(command)
            } catch (Throwable t) {
                failure.set(t)
            }
        }, 'default-process-runner-spec')

        when:
        runnerThread.start()
        assert process.waitForStarted.await(5, TimeUnit.SECONDS)
        assert process.stdout.readStarted.await(5, TimeUnit.SECONDS)
        assert process.stderr.readStarted.await(5, TimeUnit.SECONDS)
        runnerThread.interrupt()
        runnerThread.join(5000)

        then:
        !runnerThread.isAlive()
        failure.get() instanceof InterruptedException
        process.destroyCalled.get()
        process.destroyForciblyCalled.get()
        process.stdout.closed.get()
        process.stderr.closed.get()
        !process.isAlive()
    }

    private static final class ControlledProcess extends Process {
        final BlockingInputStream stdout = new BlockingInputStream()
        final BlockingInputStream stderr = new BlockingInputStream()
        final CountDownLatch waitForStarted = new CountDownLatch(1)
        final AtomicBoolean alive = new AtomicBoolean(true)
        final AtomicBoolean destroyCalled = new AtomicBoolean(false)
        final AtomicBoolean destroyForciblyCalled = new AtomicBoolean(false)

        @Override
        OutputStream getOutputStream() {
            new ByteArrayOutputStream()
        }

        @Override
        InputStream getInputStream() {
            stdout
        }

        @Override
        InputStream getErrorStream() {
            stderr
        }

        @Override
        int waitFor() throws InterruptedException {
            waitForStarted.countDown()
            while (alive.get()) {
                TimeUnit.DAYS.sleep(1)
            }
            return 0
        }

        @Override
        boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException {
            waitForStarted.countDown()
            unit.sleep(timeout)
            return false
        }

        @Override
        int exitValue() {
            if (alive.get()) {
                throw new IllegalThreadStateException('process still running')
            }
            return 0
        }

        @Override
        void destroy() {
            destroyCalled.set(true)
            alive.set(false)
            stdout.close()
            stderr.close()
        }

        @Override
        Process destroyForcibly() {
            destroyForciblyCalled.set(true)
            alive.set(false)
            stdout.close()
            stderr.close()
            return this
        }

        @Override
        boolean isAlive() {
            alive.get()
        }
    }

    private static final class BlockingInputStream extends InputStream {
        final CountDownLatch readStarted = new CountDownLatch(1)
        final AtomicBoolean closed = new AtomicBoolean(false)

        @Override
        int read() throws IOException {
            readStarted.countDown()
            synchronized (this) {
                while (!closed.get()) {
                    try {
                        wait()
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt()
                        throw new IOException('Interrupted while waiting for the stream to close', e)
                    }
                }
            }
            return -1
        }

        @Override
        void close() throws IOException {
            closed.set(true)
            synchronized (this) {
                notifyAll()
            }
        }
    }
}
