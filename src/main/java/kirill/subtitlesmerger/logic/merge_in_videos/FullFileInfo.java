package kirill.subtitlesmerger.logic.merge_in_videos;

import kirill.subtitlesmerger.logic.core.Merger;
import kirill.subtitlesmerger.logic.core.entities.Subtitles;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@AllArgsConstructor
@Getter
public
class FullFileInfo {
    private BriefFileInfo briefInfo;

    private UnavailabilityReason unavailabilityReason;

    private List<FullSubtitlesStreamInfo> subtitlesStreams;

    public Optional<Subtitles> getMerged(Config config) {
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
                Comparator.comparing((FullSubtitlesStreamInfo stream) -> stream.getSubtitles().toString().length())
                        .reversed()
        );

        streamsLowerLanguage.sort(
                Comparator.comparing((FullSubtitlesStreamInfo stream) -> stream.getSubtitles().toString().length())
                        .reversed()
        );

        return Optional.of(
                Merger.mergeSubtitles(
                        streamsUpperLanguage.get(0).getSubtitles(),
                        streamsLowerLanguage.get(0).getSubtitles()
                )
        );
    }

    public enum UnavailabilityReason {
        FAILED_BEFORE, // means that an error has happened before, at previous stage when we were obtaining brief info
        FFMPEG_FAILED
    }
}
