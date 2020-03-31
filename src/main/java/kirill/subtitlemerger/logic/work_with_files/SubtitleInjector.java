package kirill.subtitlemerger.logic.work_with_files;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import kirill.subtitlemerger.logic.core.SubtitleMerger;
import kirill.subtitlemerger.logic.core.entities.Subtitles;
import kirill.subtitlemerger.logic.work_with_files.entities.FileInfo;
import kirill.subtitlemerger.logic.work_with_files.entities.FileWithSubtitles;
import kirill.subtitlemerger.logic.work_with_files.entities.MergedSubtitleInfo;
import kirill.subtitlemerger.logic.work_with_files.entities.SubtitleOption;
import kirill.subtitlemerger.logic.work_with_files.ffmpeg.Ffmpeg;
import kirill.subtitlemerger.logic.work_with_files.ffmpeg.FfmpegException;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.ObjectUtils;

@CommonsLog
public class SubtitleInjector {
    public static void mergeAndInjectSubtitlesToFile(
            FileInfo fileInfo,
            boolean makeDefault,
            Ffmpeg ffmpeg
    ) throws FfmpegException, InterruptedException {
        SubtitleOption upperSubtitleOption = fileInfo.getSubtitleOptions().stream()
                .filter(SubtitleOption::isSelectedAsUpper)
                .findFirst().orElseThrow(IllegalStateException::new);
        SubtitleOption lowerSubtitleOption = fileInfo.getSubtitleOptions().stream()
                .filter(SubtitleOption::isSelectedAsLower)
                .findFirst().orElseThrow(IllegalStateException::new);

        Subtitles mergedSubtitles = SubtitleMerger.mergeSubtitles(
                upperSubtitleOption.getSubtitles(),
                lowerSubtitleOption.getSubtitles()
        );

        LanguageAlpha3Code mainLanguage = ObjectUtils.firstNonNull(
                upperSubtitleOption.getSubtitles().getLanguage(),
                lowerSubtitleOption.getSubtitles().getLanguage()
        );
        String title = getMergedSubtitlesTitle(upperSubtitleOption, lowerSubtitleOption);

        ffmpeg.injectSubtitlesToFile(
                mergedSubtitles,
                title,
                mainLanguage,
                makeDefault,
                fileInfo
        );

        fileInfo.setMergedSubtitleInfo(
                new MergedSubtitleInfo(
                        mergedSubtitles,
                        upperSubtitleOption.getId(),
                        upperSubtitleOption.getEncoding(),
                        lowerSubtitleOption.getId(),
                        lowerSubtitleOption.getEncoding()
                )
        );
    }

    private static String getMergedSubtitlesTitle(
            SubtitleOption upperSubtitleOption,
            SubtitleOption lowerSubtitleOption
    ) {
        String result = "Merged subtitles ";

        if (upperSubtitleOption.getSubtitles().getLanguage() != null) {
            result += upperSubtitleOption.getSubtitles().getLanguage().toString();
        } else if (upperSubtitleOption instanceof FileWithSubtitles) {
            result += "file";
        } else {
            result += "unknown";
        }

        result += '-';

        if (lowerSubtitleOption.getSubtitles().getLanguage() != null) {
            result += lowerSubtitleOption.getSubtitles().getLanguage().toString();
        } else if (lowerSubtitleOption instanceof FileWithSubtitles) {
            result += "file";
        } else {
            result += "unknown";
        }

        return result;
    }
}
