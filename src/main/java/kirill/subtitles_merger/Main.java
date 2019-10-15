package kirill.subtitles_merger;

import kirill.subtitles_merger.ffprobe.Ffprobe;
import kirill.subtitles_merger.ffprobe.FfprobeSubtitlesInfo;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.prefs.Preferences;

@CommonsLog
public class Main {
    private static final String PREFERENCES_ROOT_NODE = "subtitlesmetger";

    private static final List<String> ALLOWED_VIDEO_EXTENSIONS = Arrays.asList(
            "mp4",
            "mkv"
    );

    public static void main(String[] args) throws IOException {
        Preferences preferences = Preferences.userRoot().node(PREFERENCES_ROOT_NODE);
        String ffmpegPath = preferences.get("ffmpeg_path", "");
        if (StringUtils.isBlank(ffmpegPath)) {
            preferences.put("ffmpeg_path", "/usr/bin/ffmpeg");
        }

        String ffprobePath = preferences.get("ffprobe_path", "");
        if (StringUtils.isBlank(ffprobePath)) {
            preferences.put("ffprobe_path", "/usr/bin/ffprobe");
        }
        Ffprobe fFprobe = new Ffprobe(ffprobePath);


        List<File> videoFiles = getVideoFiles(new File("/home/user"));

        for (File file : videoFiles) {
            FfprobeSubtitlesInfo subtitlesInfo = fFprobe.getSubtitlesInfo(file);
            if (!CollectionUtils.isEmpty(subtitlesInfo.getSubtitleStreams())) {
                log.info("file " + file.getAbsolutePath() + " has " + subtitlesInfo.getSubtitleStreams().size() + " subs");
            }
        }
    }

    private static List<File> getVideoFiles(File folder) {
        List<File> result = new ArrayList<>();

        File[] allFiles = folder.listFiles();
        Objects.requireNonNull(allFiles);

        for (File file : allFiles) {
            if (file.isFile()) {
                String extension = FilenameUtils.getExtension(file.getName());
                if (ALLOWED_VIDEO_EXTENSIONS.contains(extension.toLowerCase())) {
                    result.add(file);
                }
            }
        }

        return result;
    }
}
