package kirill.subtitlemerger.logic.utils.process;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@CommonsLog
public class ProcessRunner {
    /**
     * This is a helper method to run native processes. The main feature of this class is work with the console output.
     * Output is handled in a separate thread so the process can be properly interrupted. If output is being read in
     * the main thread, interruption will have no effect because Reader::read method ignores interruptions.
     *
     * @param arguments command line arguments to start the process
     * @return string containing the console output (standard and error streams are combined, it's convenient
     * for out goals)
     * @throws ProcessException with different codes inside when errors happen
     */
    public static String run(List<String> arguments) throws ProcessException, InterruptedException {
        Process process = startProcess(arguments);
        String consoleOutput = readAllConsoleOutput(process);
        waitForProcessTermination(process, consoleOutput);

        if (process.exitValue() != 0) {
            throw new ProcessException(ProcessException.Code.EXIT_VALUE_NOT_ZERO, consoleOutput);
        }

        if (consoleOutput == null) {
            log.error("console output is null, that can't happen");
            throw new IllegalStateException();
        }

        return consoleOutput;
    }

    private static Process startProcess(List<String> arguments) throws ProcessException {
        ProcessBuilder processBuilder = new ProcessBuilder(arguments);
        processBuilder.redirectErrorStream(true);

        try {
            return processBuilder.start();
        } catch (IOException e) {
            throw new ProcessException(ProcessException.Code.FAILED_TO_START, null);
        }
    }

    private static String readAllConsoleOutput(Process process) throws ProcessException, InterruptedException {
        ReadAllConsoleOutputTask task = new ReadAllConsoleOutputTask(process);

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();

        String result = null;
        try (
                InputStream ignored1 = process.getInputStream();
                InputStream ignored2 = process.getErrorStream();
                OutputStream ignored3 = process.getOutputStream()
        ) {
            result = task.get();
            return result;
        } catch (InterruptedException e) {
            log.debug("interrupted when waiting for the console output");

            closeQuietly(process.getInputStream());
            closeQuietly(process.getErrorStream());
            process.destroyForcibly();
            log.debug("destroyForcibly has been called on a process");

            try {
                task.get(500, TimeUnit.MILLISECONDS);
            } catch (TimeoutException timeoutException) {
                log.error("failed to wait for the thread after closing the streams, something is wrong");
            } catch (InterruptedException ignored) {
                //We can ignore this exception because we already handle InterruptedException.
            } catch (ExecutionException executionException) {
                log.warn("failed to read console output: " + ExceptionUtils.getStackTrace(executionException));
            }

            log.debug("interruption has been handled successfully");
            throw e;
        } catch (ExecutionException e) {
            process.destroyForcibly();
            log.debug("destroyForcibly has been called on a process");

            Throwable cause = e.getCause();
            if (cause instanceof ProcessException) {
                throw (ProcessException) cause;
            }

            log.error("unknown execution exception: " + ExceptionUtils.getStackTrace(cause));
            throw new ProcessException(ProcessException.Code.FAILED_TO_READ_OUTPUT, null);
        } catch (IOException e) {
            log.warn("failed to close the streams: " + ExceptionUtils.getStackTrace(e));
            return result;
        }
    }

    private static void closeQuietly(InputStream inputStream) {
        try {
            inputStream.close();
        } catch (IOException e) {
            log.warn("failed to close stream: " + ExceptionUtils.getStackTrace(e));
        }
    }

    private static void waitForProcessTermination(
            Process process,
            String consoleOutput
    ) throws ProcessException, InterruptedException {
        try {
            if (!process.waitFor(5000, TimeUnit.MILLISECONDS)) {
                log.error("process has not been finished in 5 seconds, that's weird");
                process.destroyForcibly();
                log.debug("destroyForcibly has been called on a process");
                throw new ProcessException(ProcessException.Code.PROCESS_KILLED, consoleOutput);
            }
        } catch (InterruptedException e) {
            log.debug("interrupted when waiting for process termination");
            throw e;
        }
    }

    private static class ReadAllConsoleOutputTask extends FutureTask<String> {
        ReadAllConsoleOutputTask(Process process) {
            super(() -> {
                /*
                 * Code below is basically copied from the IOUtils::toString. I decided to make my own implementation
                 * instead of using the existing one because if some exception occurs I want to see what output was
                 * generated before that exception for better diagnostics.
                 */
                StringBuilderWriter result = new StringBuilderWriter();

                try {
                    InputStreamReader in = new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8);
                    char[] buffer = new char[1024 * 4];

                    int n;
                    while ((n = in.read(buffer)) != IOUtils.EOF) {
                        result.write(buffer, 0, n);
                    }

                    return result.toString();
                } catch (IOException e) {
                    throw new ProcessException(ProcessException.Code.FAILED_TO_READ_OUTPUT, result.toString());
                }
            });
        }
    }
}
