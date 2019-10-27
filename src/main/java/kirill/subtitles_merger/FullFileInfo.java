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

    private List<FullSubtitlesStreamInfo> subtitlesStreams;

    Optional<Subtitles> getMerged(Config config) {
        if (unavailabilityReason != null || CollectionUtils.isEmpty(subtitlesStreams)) {
            return Optional.empty();
        }

        List<FullSubtitlesStreamInfo> streamsUpperLanguage = subtitlesStreams.stream()
                .filter(subtitles -> subtitles.getUnavailabilityReason() == null)
                .filter(subtitles -> subtitles.getBriefInfo().getLanguage() == config.getUpperLanguage())
                .collect(Collectors.toList());

        List<FullSubtitlesStreamInfo> streamsLowerLanguage = subtitlesStreams.stream()
                .filter(subtitles -> subtitles.getUnavailabilityReason() == null)
                .filter(subtitles -> subtitles.getBriefInfo().getLanguage() == config.getLowerLanguage())
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(streamsUpperLanguage) || CollectionUtils.isEmpty(streamsLowerLanguage)) {
            return Optional.empty();
        }

        streamsUpperLanguage.sort(
                Comparator.comparing((FullSubtitlesStreamInfo stream) -> stream.getContent().toString().length())
                        .reversed()
        );

        streamsLowerLanguage.sort(
                Comparator.comparing((FullSubtitlesStreamInfo stream) -> stream.getContent().toString().length())
                        .reversed()
        );

        return Optional.of(
                Merger.mergeSubtitles(
                        streamsUpperLanguage.get(0).getContent(),
                        streamsLowerLanguage.get(0).getContent()
                )
        );
    }
}
