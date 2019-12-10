package kirill.subtitlesmerger.logic.work_with_files;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import kirill.subtitlesmerger.logic.core.Merger;
import kirill.subtitlesmerger.logic.core.Writer;
import kirill.subtitlesmerger.logic.core.entities.Subtitles;
import kirill.subtitlesmerger.logic.work_with_files.entities.FullFileInfo;
import kirill.subtitlesmerger.logic.work_with_files.entities.FullSubtitlesStreamInfo;
import kirill.subtitlesmerger.logic.work_with_files.ffmpeg.Ffmpeg;
import kirill.subtitlesmerger.logic.work_with_files.ffmpeg.FfmpegException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

public class SubtitleInjector {
    public static void mergeAndInjectSubtitlesToFile(
            Subtitles upperSubtitles,
            Subtitles lowerSubtitles,
            FullFileInfo fullFileInfo,
            Ffmpeg ffmpeg
    ) throws SubtitlesAlreadyInjectedException, FfmpegException {
        Subtitles result = Merger.mergeSubtitles(upperSubtitles, lowerSubtitles);
        checkForDuplicates(result, fullFileInfo);

        LanguageAlpha3Code mainLanguage = getMergedSubtitlesMainLanguage(upperSubtitles, lowerSubtitles);
        String title = getMergedSubtitlesTitle(upperSubtitles, lowerSubtitles);

        ffmpeg.injectSubtitlesToFile(
                result,
                title,
                mainLanguage,
                fullFileInfo.getSubtitlesStreams().size(),
                fullFileInfo.getBriefInfo().getFile()
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

    private static void checkForDuplicates(
            Subtitles result,
            FullFileInfo fullFileInfo
    ) throws SubtitlesAlreadyInjectedException {
        String resultText = Writer.toSubRipText(result);

        if (!CollectionUtils.isEmpty(fullFileInfo.getSubtitlesStreams())) {
            for (FullSubtitlesStreamInfo streamInfo : fullFileInfo.getSubtitlesStreams()) {
                if (Objects.equals(Writer.toSubRipText(streamInfo.getSubtitles()), resultText)) {
                    throw new SubtitlesAlreadyInjectedException();
                }
            }
        }
    }

    public static class SubtitlesAlreadyInjectedException extends Exception {
    }
}
