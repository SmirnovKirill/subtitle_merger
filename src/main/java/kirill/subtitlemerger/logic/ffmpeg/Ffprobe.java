package kirill.subtitlemerger.logic.ffmpeg;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import kirill.subtitlemerger.logic.ffmpeg.json.JsonFfprobeVideoInfo;
import kirill.subtitlemerger.logic.utils.process.ProcessException;
import kirill.subtitlemerger.logic.utils.process.ProcessRunner;
import lombok.extern.apachecommons.CommonsLog;
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

    public Ffprobe(File ffprobeFile) throws FfmpegException, InterruptedException {
        validate(ffprobeFile);

        this.ffprobeFile = ffprobeFile;
    }

    private static void validate(File ffprobeFile) throws FfmpegException, InterruptedException {
        try {
            List<String> arguments = Arrays.asList(
                    ffprobeFile.getAbsolutePath(),
                    "-version"
            );

            String consoleOutput = ProcessRunner.run(arguments);
            if (!consoleOutput.startsWith("ffprobe version")) {
                throw new FfmpegException(FfmpegException.Code.INCORRECT_FFPROBE_PATH, consoleOutput);
            }
        } catch (ProcessException e) {
            throw new FfmpegException(FfmpegException.Code.INCORRECT_FFPROBE_PATH, e.getConsoleOutput());
        }
    }

    public JsonFfprobeVideoInfo getVideoInfo(File videoFile) throws FfmpegException, InterruptedException {
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
                    videoFile.getAbsolutePath()
            );

            consoleOutput = ProcessRunner.run(arguments);
        } catch (ProcessException e) {
            throw new FfmpegException(FfmpegException.Code.GENERAL_ERROR, e.getConsoleOutput());
        }

        try {
            return JSON_OBJECT_MAPPER.readValue(consoleOutput, JsonFfprobeVideoInfo.class);
        } catch (JsonProcessingException e) {
            log.error(
                    "failed to convert the console output to json: " + ExceptionUtils.getStackTrace(e)
                            + ", console output " + consoleOutput
            );
            throw new FfmpegException(FfmpegException.Code.GENERAL_ERROR, consoleOutput);
        }
    }
}
