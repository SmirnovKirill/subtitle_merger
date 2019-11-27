package kirill.subtitlesmerger.gui;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import kirill.subtitlesmerger.logic.data.Config;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@CommonsLog
public class MergeInVideosTabController {
    private MergeInVideosTab tab;

    private Config config;

    MergeInVideosTabController(MergeInVideosTab tab, Config config) {
        this.tab = tab;
        this.config = config;
    }

    void initialize() {
        List<String> missingSettings = getMissingSettings(config);
        if (!CollectionUtils.isEmpty(missingSettings)) {
            tab.showMissingSettings(missingSettings);
        } else {
            tab.showRegularContent();
        }
    }

    private static List<String> getMissingSettings(Config config) {
        List<String> result = new ArrayList<>();

        if (config.getFfprobeFile() == null) {
            result.add("path to ffprobe");
        }

        if (config.getFfmpegFile() == null) {
            result.add("path to ffmpeg");
        }

        if (config.getUpperLanguage() == null) {
            result.add("preferred language for upper subtitles");
        }

        if (config.getLowerLanguage() == null) {
            result.add("preferred language for lower subtitles");
        }

        return result;
    }
}
