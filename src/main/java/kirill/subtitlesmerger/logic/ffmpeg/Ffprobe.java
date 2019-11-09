package kirill.subtitlesmerger.logic.ffmpeg;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import kirill.subtitlesmerger.logic.ffmpeg.json.JsonFfprobeFileInfo;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.File;
import java.util.Arrays;

@CommonsLog
public class Ffprobe {
    private static final ObjectMapper JSON_OBJECT_MAPPER;

    private String path;

    static {
        JSON_OBJECT_MAPPER = new ObjectMapper();
        JSON_OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        JSON_OBJECT_MAPPER.configure(MapperFeature.AUTO_DETECT_GETTERS, false);
        JSON_OBJECT_MAPPER.configure(MapperFeature.AUTO_DETECT_IS_GETTERS, false);
        JSON_OBJECT_MAPPER.configure(MapperFeature.AUTO_DETECT_SETTERS, false);
        JSON_OBJECT_MAPPER.configure(MapperFeature.AUTO_DETECT_FIELDS, false);
        JSON_OBJECT_MAPPER.configure(MapperFeature.AUTO_DETECT_CREATORS, false);
    }

    public Ffprobe(String path) throws FfmpegException {
        validate(path);

        this.path = path;
    }

    public static void validate(String ffprobePath) throws FfmpegException {
        try {
            String consoleOutput = ProcessRunner.run(
                    Arrays.asList(
                            ffprobePath,
                            "-version"
                    )
            );

            if (!consoleOutput.startsWith("ffprobe version")) {
                log.info("console output doesn't start with the ffprobe version");
                throw new FfmpegException(FfmpegException.Code.INCORRECT_FFPROBE_PATH);
            }
        } catch (ProcessException e) {
            log.info("failed to check ffprobe: " + e.getCode());
            throw new FfmpegException(FfmpegException.Code.INCORRECT_FFPROBE_PATH);
        }
    }

    public JsonFfprobeFileInfo getFileInfo(File file) throws FfmpegException {
        String consoleOutput;
        try {
            consoleOutput = ProcessRunner.run(
                    Arrays.asList(
                            path,
                            "-v",
                            "quiet",
                            "-show_format",
                            "-show_streams",
                            "-print_format",
                            "json",
                            file.getAbsolutePath()

                    )
            );
        } catch (ProcessException e) {
            log.warn("failed to get file info with ffprobe: " + e.getCode());
            throw new FfmpegException(FfmpegException.Code.GENERAL_ERROR);
        }

        try {
            return JSON_OBJECT_MAPPER.readValue(consoleOutput, JsonFfprobeFileInfo.class);
        } catch (JsonProcessingException e) {
            log.warn("failed to convert console output to json: "
                    + ExceptionUtils.getStackTrace(e)
                    + ", output is "
                    + consoleOutput
            );
            throw new FfmpegException(FfmpegException.Code.GENERAL_ERROR);
        }
    }
}
