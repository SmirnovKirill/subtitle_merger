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
class FullFileInfo {
    private BriefFileInfo briefInfo;

    private FullFileUnavailabilityReason unavailabilityReason;

    private List<FullSingleSubtitlesInfo> allSubtitles;

    Optional<Subtitles> getMerged(Config config) {
        if (unavailabilityReason != null || CollectionUtils.isEmpty(allSubtitles)) {
            return Optional.empty();
        }

        List<FullSingleSubtitlesInfo> subtitlesUpperLanguage = allSubtitles.stream()
                .filter(subtitles -> subtitles.getUnavailabilityReason() == null)
                .filter(subtitles -> subtitles.getBriefInfo().getLanguage() == config.getUpperLanguage())
                .collect(Collectors.toList());

        List<FullSingleSubtitlesInfo> subtitlesLowerLanguage = allSubtitles.stream()
                .filter(subtitles -> subtitles.getUnavailabilityReason() == null)
                .filter(subtitles -> subtitles.getBriefInfo().getLanguage() == config.getLowerLanguage())
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(subtitlesUpperLanguage) || CollectionUtils.isEmpty(subtitlesLowerLanguage)) {
            return Optional.empty();
        }

        subtitlesUpperLanguage.sort(
                Comparator.comparing((FullSingleSubtitlesInfo subtitles) -> subtitles.getContent().toString().length())
                        .reversed()
        );

        subtitlesLowerLanguage.sort(
                Comparator.comparing((FullSingleSubtitlesInfo subtitles) -> subtitles.getContent().toString().length())
                        .reversed()
        );

        return Optional.of(
                Merger.mergeSubtitles(
                        subtitlesUpperLanguage.get(0).getContent(),
                        subtitlesLowerLanguage.get(0).getContent()
                )
        );
    }
}
