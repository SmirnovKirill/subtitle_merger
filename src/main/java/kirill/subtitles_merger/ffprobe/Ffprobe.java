package kirill.subtitles_merger.ffprobe;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import kirill.subtitles_merger.ffprobe.json.JsonFfprobeFileInfo;

import java.io.IOException;
import java.util.Arrays;

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
    }

    public JsonFfprobeFileInfo getFileInfo(String filePath) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(
                Arrays.asList(
                        path,
                        "-v",
                        "quiet",
                        "-show_streams",
                        "-print_format",
                        "json",
                        filePath

                )
        );

        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        try {
            return JSON_OBJECT_MAPPER.readValue(process.getInputStream(), JsonFfprobeFileInfo.class);
        } finally {
            process.destroy();
        }
    }
}
