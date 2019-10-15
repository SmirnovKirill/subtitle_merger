package kirill.subtitles_merger;

import kirill.subtitles_merger.logic.Subtitles;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@AllArgsConstructor
@Getter
public class Video {
    private File file;

    private List<VideoSubtitles> subtitles;

    /**
     * Пытается подобрать пару субтитров согласно настройкам по предпочитаемым языкам. Если есть более одного
     * файла с субтитрами для языка выбирается самый большой.
     */
    public Optional<List<Subtitles>> getSubtitlesPair(Config config) {
        List<VideoSubtitles> subtitlesMatchingUpperLanguage = subtitles.stream()
                .filter(subtitles -> subtitles.getLanguage() == config.getUpperLanguage())
                .collect(Collectors.toList());

        List<VideoSubtitles> subtitlesMatchingLowerLanguage = subtitles.stream()
                .filter(subtitles -> subtitles.getLanguage() == config.getLowerLanguage())
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(subtitlesMatchingUpperLanguage) || CollectionUtils.isEmpty(subtitlesMatchingLowerLanguage)) {
            return Optional.empty();
        }

        subtitlesMatchingUpperLanguage.sort(
                Comparator.comparing((VideoSubtitles subtitles) -> subtitles.getSubtitles().toString().length()).reversed()
        );

        subtitlesMatchingLowerLanguage.sort(
                Comparator.comparing((VideoSubtitles subtitles) -> subtitles.getSubtitles().toString().length()).reversed()
        );

        return Optional.of(
                Arrays.asList(
                        subtitlesMatchingUpperLanguage.get(0).getSubtitles(),
                        subtitlesMatchingLowerLanguage.get(0).getSubtitles()
                )
        );
    }
}
