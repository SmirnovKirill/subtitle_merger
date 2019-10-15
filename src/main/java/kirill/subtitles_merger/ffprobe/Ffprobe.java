package kirill.subtitles_merger.ffprobe;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neovisionaries.i18n.LanguageAlpha3Code;
import kirill.subtitles_merger.ffprobe.json.JsonFfprobeFileInfo;
import kirill.subtitles_merger.ffprobe.json.JsonStream;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

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

    public FfprobeSubtitlesInfo getSubtitlesInfo(File file) throws IOException {
        JsonFfprobeFileInfo rawJsonInfo = getRawJsonInfo(file);

        List<SubtitleStream> subtitleStreams = new ArrayList<>();

        for (JsonStream rawStream : rawJsonInfo.getStreams()) {
            if ("subtitle".equals(rawStream.getCodecType())) {
                subtitleStreams.add(
                        new SubtitleStream(
                                rawStream.getIndex(),
                                getLanguage(rawStream).orElse(null),
                                getTitle(rawStream).orElse(null)
                        )
                );
            }
        }

        return new FfprobeSubtitlesInfo(subtitleStreams);
    }

    private JsonFfprobeFileInfo getRawJsonInfo(File file) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(
                Arrays.asList(
                        path,
                        "-v",
                        "quiet",
                        "-show_streams",
                        "-print_format",
                        "json",
                        file.getAbsolutePath()

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

    private Optional<LanguageAlpha3Code> getLanguage(JsonStream stream) {
        if (MapUtils.isEmpty(stream.getTags())) {
            return Optional.empty();
        }

        String languageRaw = stream.getTags().get("language");
        if (StringUtils.isBlank(languageRaw)) {
            return Optional.empty();
        }

        return Optional.ofNullable(LanguageAlpha3Code.getByCodeIgnoreCase(languageRaw));
    }

    private Optional<String> getTitle(JsonStream stream) {
        if (MapUtils.isEmpty(stream.getTags())) {
            return Optional.empty();
        }

        return Optional.ofNullable(stream.getTags().get("title"));
    }
}
