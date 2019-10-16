package kirill.subtitles_merger.ffmpeg;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import kirill.subtitles_merger.ffmpeg.json.JsonFfprobeFileInfo;
import lombok.extern.apachecommons.CommonsLog;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

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

    public Ffprobe(String path) {
        this.path = path;
        //todo валидация
    }

    public JsonFfprobeFileInfo getFileInfo(File file) throws IOException, InterruptedException {
        JsonFfprobeFileInfo result;

        ProcessBuilder processBuilder = new ProcessBuilder(
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

        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        try {
            result = JSON_OBJECT_MAPPER.readValue(process.getInputStream(), JsonFfprobeFileInfo.class);
        } finally {
            process.destroy();
        }

        if (!process.waitFor(1, TimeUnit.SECONDS)) {
            log.error("ffprobe exits for too long, that's weird");
            throw new IllegalStateException();
        }

        if (process.exitValue() != 0) {
            log.error("ffprobe has exited with code " + process.exitValue());
            throw new IllegalStateException();
        }

        return result;
    }
}
