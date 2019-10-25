package kirill.subtitles_merger.ffmpeg;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

class ProcessRunner {
    /**
     *
     * @param arguments аргументы командной строки для запуска
     * @return строку содержащую вывод из консоли (стандартный поток и поток с ошибками
     * объединены, это подходит для задач приложения).
     * @throws ProcessException с кодами внутри при разных ошибках выполнения.
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
