package kirill.subtitles_merger;

import kirill.subtitles_merger.logic.Merger;
import kirill.subtitles_merger.logic.Subtitles;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@AllArgsConstructor
@Getter
public class FullFileInfo {
    private BriefFileInfo briefInfo;

    private FullFileUnavailabilityReason unavailabilityReason;

    private List<FullSingleSubtitlesInfo> allSubtitles;

    public Optional<Subtitles> getMerged(Config config) {
        if (unavailabilityReason != null || CollectionUtils.isEmpty(allSubtitles)) {
            return Optional.empty();
        }

        List<FullSingleSubtitlesInfo> subtitlesMatchingUpperLanguage = allSubtitles.stream()
                .filter(subtitles -> subtitles.getUnavailabilityReason() == null)
                .filter(subtitles -> subtitles.getBriefInfo().getLanguage() == config.getUpperLanguage())
                .collect(Collectors.toList());

        List<FullSingleSubtitlesInfo> subtitlesMatchingLowerLanguage = allSubtitles.stream()
                .filter(subtitles -> subtitles.getUnavailabilityReason() == null)
                .filter(subtitles -> subtitles.getBriefInfo().getLanguage() == config.getLowerLanguage())
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(subtitlesMatchingUpperLanguage) || CollectionUtils.isEmpty(subtitlesMatchingLowerLanguage)) {
            return Optional.empty();
        }

        subtitlesMatchingUpperLanguage.sort(
                Comparator.comparing((FullSingleSubtitlesInfo subtitles) -> subtitles.getContent().toString().length()).reversed()
        );

        subtitlesMatchingLowerLanguage.sort(
                Comparator.comparing((FullSingleSubtitlesInfo subtitles) -> subtitles.getContent().toString().length()).reversed()
        );

        return Optional.of(
                Merger.mergeSubtitles(
                        subtitlesMatchingUpperLanguage.get(0).getContent(),
                        subtitlesMatchingLowerLanguage.get(0).getContent()
                )
        );
    }
}
