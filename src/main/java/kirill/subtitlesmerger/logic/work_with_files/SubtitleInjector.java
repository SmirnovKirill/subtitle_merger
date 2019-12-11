package kirill.subtitlesmerger.logic.work_with_files;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import kirill.subtitlesmerger.logic.core.Merger;
import kirill.subtitlesmerger.logic.core.Writer;
import kirill.subtitlesmerger.logic.core.entities.Subtitles;
import kirill.subtitlesmerger.logic.work_with_files.entities.FileInfo;
import kirill.subtitlesmerger.logic.work_with_files.entities.SubtitleStream;
import kirill.subtitlesmerger.logic.work_with_files.ffmpeg.Ffmpeg;
import kirill.subtitlesmerger.logic.work_with_files.ffmpeg.FfmpegException;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

@CommonsLog
public class SubtitleInjector {
    public static void mergeAndInjectSubtitlesToFile(
            Subtitles upperSubtitles,
            Subtitles lowerSubtitles,
            FileInfo fileInfo,
            Ffmpeg ffmpeg
    ) throws SubtitlesAlreadyInjectedException, FfmpegException {
        Subtitles result = Merger.mergeSubtitles(upperSubtitles, lowerSubtitles);
        checkForDuplicates(result, fileInfo);

        LanguageAlpha3Code mainLanguage = getMergedSubtitlesMainLanguage(upperSubtitles, lowerSubtitles);
        String title = getMergedSubtitlesTitle(upperSubtitles, lowerSubtitles);

        ffmpeg.injectSubtitlesToFile(
                result,
                title,
                mainLanguage,
                fileInfo.getSubtitleStreams().size(),
                fileInfo.getFile()
        );
    }

    private static void checkForDuplicates(
            Subtitles result,
            FileInfo fileInfo
    ) throws SubtitlesAlreadyInjectedException {
        String resultText = Writer.toSubRipText(result);

        for (SubtitleStream streamInfo : fileInfo.getSubtitleStreams()) {
            if (streamInfo.getSubtitles() == null) {
                log.error("subtitles have to be initialized before injecting!");
                throw new IllegalArgumentException();
            }

            if (Objects.equals(Writer.toSubRipText(streamInfo.getSubtitles()), resultText)) {
                throw new SubtitlesAlreadyInjectedException();
            }
        }
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
