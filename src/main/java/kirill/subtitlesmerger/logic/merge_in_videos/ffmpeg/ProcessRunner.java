package kirill.subtitlesmerger.logic.merge_in_videos.ffmpeg;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

class ProcessRunner {
    /**
     *
     * @param arguments command line arguments to start the process
     * @return string containing the console output (standard and error streams are combined, it's convenient
     * for out goals)
     * @throws ProcessException with different codes inside when errors happen
     */
    static String run(List<String> arguments) throws ProcessException {
        ProcessBuilder processBuilder = new ProcessBuilder(arguments);
        processBuilder.redirectErrorStream(true);

        Process process;
        try {
            process = processBuilder.start();
        } catch (IOException e) {
            throw new ProcessException(ProcessException.Code.FAILED_TO_START, null);
        }

        try {
            String result;
            try {
                result = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new ProcessException(ProcessException.Code.FAILED_TO_READ_OUTPUT, null);
            }

            if (!process.waitFor(10000, TimeUnit.MILLISECONDS)) {
                process.destroy();
                throw new ProcessException(ProcessException.Code.PROCESS_KILLED, result);
            }

            if (process.exitValue() != 0) {
                throw new ProcessException(ProcessException.Code.EXIT_VALUE_NOT_ZERO, result);
            }

            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProcessException(ProcessException.Code.INTERRUPTED, null);
        } finally {
            IOUtils.closeQuietly(process.getOutputStream());
            IOUtils.closeQuietly(process.getErrorStream());
        }
    }
}
