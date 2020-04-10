package kirill.subtitlemerger.logic.work_with_files.ffmpeg;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import kirill.subtitlemerger.logic.utils.process.ProcessException;
import kirill.subtitlemerger.logic.utils.process.ProcessRunner;
import kirill.subtitlemerger.logic.work_with_files.ffmpeg.json.JsonFfprobeFileInfo;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@CommonsLog
public class Ffprobe {
    private static final ObjectMapper JSON_OBJECT_MAPPER;

    private File ffprobeFile;

    static {
        JSON_OBJECT_MAPPER = new ObjectMapper();
        JSON_OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        JSON_OBJECT_MAPPER.configure(MapperFeature.AUTO_DETECT_GETTERS, false);
        JSON_OBJECT_MAPPER.configure(MapperFeature.AUTO_DETECT_IS_GETTERS, false);
        JSON_OBJECT_MAPPER.configure(MapperFeature.AUTO_DETECT_SETTERS, false);
        JSON_OBJECT_MAPPER.configure(MapperFeature.AUTO_DETECT_FIELDS, false);
        JSON_OBJECT_MAPPER.configure(MapperFeature.AUTO_DETECT_CREATORS, false);
    }

    public Ffprobe(File ffprobeFile) throws FfmpegException {
        validate(ffprobeFile);

        this.ffprobeFile = ffprobeFile;
    }

    public static void validate(File ffprobeFile) throws FfmpegException {
        try {
            List<String> arguments = Arrays.asList(
                    ffprobeFile.getAbsolutePath(),
                    "-version"
            );

            log.debug("run ffprobe " + StringUtils.join(arguments, " "));
            String consoleOutput = ProcessRunner.run(arguments);
            log.debug("ffprobe console output: " + consoleOutput);

            if (!consoleOutput.startsWith("ffprobe version")) {
                log.info("console output doesn't start with the ffprobe version");
                throw new FfmpegException(FfmpegException.Code.INCORRECT_FFPROBE_PATH, consoleOutput);
            }
        } catch (ProcessException e) {
            if (e.getCode() == ProcessException.Code.INTERRUPTED) {
                throw  new FfmpegException(FfmpegException.Code.INTERRUPTED, e.getConsoleOutput());
            }

            log.warn("failed to check ffprobe: " + e.getCode());
            throw new FfmpegException(FfmpegException.Code.INCORRECT_FFPROBE_PATH, e.getConsoleOutput());
        }
    }

    public JsonFfprobeFileInfo getFileInfo(File file) throws FfmpegException {
        String consoleOutput;
        try {
            List<String> arguments = Arrays.asList(
                    ffprobeFile.getAbsolutePath(),
                    "-v",
                    "quiet",
                    "-show_format",
                    "-show_streams",
                    "-print_format",
                    "json",
                    file.getAbsolutePath()
            );

            log.debug("run ffprobe " + StringUtils.join(arguments, " "));
            consoleOutput = ProcessRunner.run(arguments);
            log.debug("ffprobe console output: " + consoleOutput);
        } catch (ProcessException e) {
            if (e.getCode() == ProcessException.Code.INTERRUPTED) {
                throw  new FfmpegException(FfmpegException.Code.INTERRUPTED, e.getConsoleOutput());
            }

            log.warn("failed to get file info with ffprobe: " + e.getCode());
            throw new FfmpegException(FfmpegException.Code.GENERAL_ERROR, e.getConsoleOutput());
        }

        try {
            return JSON_OBJECT_MAPPER.readValue(consoleOutput, JsonFfprobeFileInfo.class);
        } catch (JsonProcessingException e) {
            log.error("failed to convert console output to json: "
                    + ExceptionUtils.getStackTrace(e)
                    + ", output is "
                    + consoleOutput
            );
            throw new FfmpegException(FfmpegException.Code.GENERAL_ERROR, consoleOutput);
        }
    }
}
