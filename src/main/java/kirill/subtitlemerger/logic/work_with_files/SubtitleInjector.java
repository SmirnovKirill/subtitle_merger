package kirill.subtitlemerger.logic.work_with_files;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import kirill.subtitlemerger.logic.core.Merger;
import kirill.subtitlemerger.logic.core.entities.Subtitles;
import kirill.subtitlemerger.logic.work_with_files.entities.FileInfo;
import kirill.subtitlemerger.logic.work_with_files.ffmpeg.Ffmpeg;
import kirill.subtitlemerger.logic.work_with_files.ffmpeg.FfmpegException;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

@CommonsLog
public class SubtitleInjector {
    public static void mergeAndInjectSubtitlesToFile(
            Subtitles upperSubtitles,
            Subtitles lowerSubtitles,
            boolean makeDefault,
            FileInfo fileInfo,
            Ffmpeg ffmpeg
    ) throws FfmpegException {
        Subtitles result = Merger.mergeSubtitles(upperSubtitles, lowerSubtitles);

        LanguageAlpha3Code mainLanguage = getMergedSubtitlesMainLanguage(upperSubtitles, lowerSubtitles);
        String title = getMergedSubtitlesTitle(upperSubtitles, lowerSubtitles);

        ffmpeg.injectSubtitlesToFile(
                result,
                title,
                mainLanguage,
                makeDefault,
                fileInfo.getSubtitleStreamsInfo(),
                fileInfo.getFile()
        );
    }

    private static LanguageAlpha3Code getMergedSubtitlesMainLanguage(
            Subtitles upperSubtitles,
            Subtitles lowerSubtitles
    ) {
        if (!CollectionUtils.isEmpty(upperSubtitles.getLanguages())) {
           return upperSubtitles.getLanguages().get(0);
        } else if (!CollectionUtils.isEmpty(lowerSubtitles.getLanguages())) {
            return lowerSubtitles.getLanguages().get(0);
        } else {
            return null;
        }
    }

    private static String getMergedSubtitlesTitle(Subtitles upperSubtitles, Subtitles lowerSubtitles) {
        String result = "Merged subtitles ";

        if (!CollectionUtils.isEmpty(upperSubtitles.getLanguages())) {
            result += StringUtils.join(upperSubtitles.getLanguages(), '-');
        } else {
            result += "file";
        }

        result += '-';

        if (!CollectionUtils.isEmpty(lowerSubtitles.getLanguages())) {
            result += StringUtils.join(lowerSubtitles.getLanguages(), '-');
        } else {
            result += "file";
        }

        return result;
    }

    public static class SubtitlesAlreadyInjectedException extends Exception {
    }
}
