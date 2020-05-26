package kirill.subtitlemerger.logic.subtitles;

import kirill.subtitlemerger.logic.subtitles.entities.Subtitle;
import kirill.subtitlemerger.logic.subtitles.entities.Subtitles;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.Nullable;
import org.joda.time.LocalTime;

import java.util.*;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

@CommonsLog
public class SubtitleMerger {
    public static Subtitles mergeSubtitles(
            Subtitles upperSubtitles,
            Subtitles lowerSubtitles
    ) throws InterruptedException {
        List<MergerSubtitle> result = makeInitialMerge(upperSubtitles, lowerSubtitles);
        result = getExpandedSubtitles(result);
        sortSubtitleLines(result);
        result = getCombinedSubtitles(result);

        return convert(result);
    }

    /**
     * The first and the simplest stage of the merge - we make a list of all seen points of time and for each segment we
     * see whether there are subtitles from any source and if there are we add this segment and its lines.
     */
    private static List<MergerSubtitle> makeInitialMerge(
            Subtitles upperSubtitles,
            Subtitles lowerSubtitles
    ) throws InterruptedException {
        List<MergerSubtitle> result = new ArrayList<>();

        List<LocalTime> uniqueSortedPointsOfTime = getUniqueSortedPointsOfTime(upperSubtitles, lowerSubtitles);

        int upperIndex = 0;
        int lowerIndex = 0;
        for (int i = 0; i < uniqueSortedPointsOfTime.size() - 1; i++) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

            LocalTime from = uniqueSortedPointsOfTime.get(i);
            LocalTime to = uniqueSortedPointsOfTime.get(i + 1);

            Subtitle upperSubtitle = null;
            Integer matchingUpperIndex = getIndexMatchingTime(upperIndex, upperSubtitles, from, to);
            if (matchingUpperIndex != null) {
                upperIndex = matchingUpperIndex;
                upperSubtitle = upperSubtitles.getSubtitles().get(upperIndex);
            }

            Subtitle lowerSubtitle = null;
            Integer matchingLowerIndex = getIndexMatchingTime(lowerIndex, lowerSubtitles, from, to);
            if (matchingLowerIndex != null) {
                lowerIndex = matchingLowerIndex;
                lowerSubtitle = lowerSubtitles.getSubtitles().get(lowerIndex);
            }

            if (upperSubtitle != null || lowerSubtitle != null) {
                List<MergerSubtitleLine> subtitleLines = new ArrayList<>();

                if (upperSubtitle != null) {
                    subtitleLines.addAll(
                            upperSubtitle.getLines().stream()
                                    .map(line -> new MergerSubtitleLine(line, Source.UPPER_SUBTITLES))
                                    .collect(toList())
                    );
                }

                if (lowerSubtitle != null) {
                    subtitleLines.addAll(
                            lowerSubtitle.getLines().stream()
                                    .map(line -> new MergerSubtitleLine(line, Source.LOWER_SUBTITLES))
                                    .collect(toList())
                    );
                }

                result.add(new MergerSubtitle(from, to, subtitleLines));
            }
        }

        return result;
    }

    private static List<LocalTime> getUniqueSortedPointsOfTime(
            Subtitles upperSubtitles,
            Subtitles lowerSubtitles
    ) throws InterruptedException {
        Set<LocalTime> result = new TreeSet<>();

        Collection<Subtitle> allSubtitles = CollectionUtils.union(
                upperSubtitles.getSubtitles(),
                lowerSubtitles.getSubtitles()
        );

        for (Subtitle subtitle : allSubtitles) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

            result.add(subtitle.getFrom());
            result.add(subtitle.getTo());
        }

        return new ArrayList<>(result);
    }

    /*
     * For correctly formatted subtitles we should check only the current index and the next one (if there is one)
     * because subtitles should go consequentially. But unfortunately subtitles sometimes have errors in them and can be
     * overlapped in time. To deal with it it's better to check all further indices.
     */
    @Nullable
    private static Integer getIndexMatchingTime(
            int currentIndex,
            Subtitles subtitles,
            LocalTime from,
            LocalTime to
    ) {
        for (int i = currentIndex; i < subtitles.getSubtitles().size(); i++) {
            if (subtitleMatchesTime(subtitles.getSubtitles().get(i), from, to)) {
                return i;
            }
        }

        return null;
    }

    private static boolean subtitleMatchesTime(Subtitle subtitle, LocalTime from, LocalTime to) {
        boolean fromInside = !from.isBefore(subtitle.getFrom()) && !from.isAfter(subtitle.getTo());
        boolean toInside = !to.isBefore(subtitle.getFrom()) && !to.isAfter(subtitle.getTo());

        return fromInside && toInside;
    }

    /**
     * This method fixes "jumps" that appear after splitting into smaller segments. If for example for some segment
     * there are lines from only one source (upper) and on the next segment lines from the other source (lower)
     * are added it looks like the jump of the lines from the upper source because for some period of time they go alone
     * and later when the lines from the other source are added they are not alone anymore and are moved to the top.
     * This method kind of "expands" subtitles so they start and end together at the same time if they
     * appear together somewhere. If the lines go alone at all segments then no expanding happens - this is a common
     * case for english subtitles where there are descriptions of sounds that are usually not present for other
     * languages, so no expanding happens there.
     */
    private static List<MergerSubtitle> getExpandedSubtitles(
            List<MergerSubtitle> subtitles
    ) throws InterruptedException {
        List<MergerSubtitle> result = new ArrayList<>();

        for (int i = 0; i < subtitles.size(); i++) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

            MergerSubtitle subtitle = subtitles.get(i);

            Set<Source> sources = subtitle.getLines().stream().map(MergerSubtitleLine::getSource).collect(toSet());
            if (sources.size() != 1 || subtitleAlwaysHasOneSource(subtitle, i, subtitles)) {
                result.add(subtitle);
                continue;
            }

            Source otherSource = Arrays.stream(Source.values())
                    .filter(currentSource -> !sources.contains(currentSource))
                    .findFirst().orElseThrow(IllegalStateException::new);

            List<MergerSubtitleLine> subtitleLines = new ArrayList<>(subtitle.getLines());
            subtitleLines.addAll(getClosestLinesFromOtherSource(i, otherSource, subtitles));

            result.add(new MergerSubtitle(subtitle.getFrom(), subtitle.getTo(), subtitleLines));
        }

        return result;
    }

    /**
     * Checks whether the subtitle with a given index always has one source, without lines from the other source. To do
     * this we have to check subtitles both to the left and to the right from the given one.
     */
    private static boolean subtitleAlwaysHasOneSource(
            MergerSubtitle subtitle,
            int subtitleIndex,
            List<MergerSubtitle> subtitles
    ) throws InterruptedException {
        Source source = subtitle.getLines().get(0).getSource();

        for (int i = subtitleIndex - 1; i >= 0; i--) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

            if (subtitlesEqualForSource(subtitles.get(i), subtitles.get(i + 1), source)) {
                /*
                 * If we got here it means that subtitles have the same text for the source. Now if the lines are
                 * equal no matter what the source is it means that it's just another segment for the subtitle and we
                 * should keep going, but if the lines differ it means that there are lines from the other source
                 * and we should return false.
                 */
                if (!Objects.equals(subtitles.get(i).getLines(), subtitles.get(i + 1).getLines())) {
                    return false;
                }
            } else {
                break;
            }
        }

        for (int i = subtitleIndex + 1; i < subtitles.size(); i++) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

            if (subtitlesEqualForSource(subtitles.get(i - 1), subtitles.get(i), source)) {
                /*
                 * If we got here it means that subtitles have the same text for the source. Now if the lines are
                 * equal no matter what the source is it means that it's just another segment of for subtitle and we
                 * should keep going, but if the lines differ it means that there are lines from the other source
                 * and we should return false.
                 */
                if (!Objects.equals(subtitles.get(i - 1).getLines(), subtitles.get(i).getLines())) {
                    return false;
                }
            } else {
                break;
            }
        }

        return true;
    }

    /**
     * Subtitles are considered to be equal if they are strictly consequential (the next one starts right where the
     * previous ends) and the lines for the given source are the same.
     */
    private static boolean subtitlesEqualForSource(MergerSubtitle previous, MergerSubtitle next, Source source) {
        if (!Objects.equals(previous.getTo(), next.getFrom())) {
            return false;
        }

        return Objects.equals(
                previous.getLines().stream().filter(line -> line.getSource() == source).collect(toList()),
                next.getLines().stream().filter(line -> line.getSource() == source).collect(toList())
        );
    }

    private static List<MergerSubtitleLine> getClosestLinesFromOtherSource(
            int subtitleIndex,
            Source otherSource,
            List<MergerSubtitle> subtitles
    ) throws InterruptedException {
        List<MergerSubtitleLine> result;

        MergerSubtitle firstMatchingSubtitleForward = null;
        for (int i = subtitleIndex + 1; i < subtitles.size(); i++) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

            MergerSubtitle currentSubtitle = subtitles.get(i);
            if (currentSubtitle.getLines().stream().anyMatch(line -> line.getSource() == otherSource)) {
                firstMatchingSubtitleForward = currentSubtitle;
                break;
            }
        }

        MergerSubtitle firstMatchingSubtitleBackward = null;
        for (int i = subtitleIndex - 1; i >= 0; i--) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

            MergerSubtitle currentSubtitle = subtitles.get(i);
            if (currentSubtitle.getLines().stream().anyMatch(line -> line.getSource() == otherSource)) {
                firstMatchingSubtitleBackward = currentSubtitle;
                break;
            }
        }

        if (firstMatchingSubtitleForward == null && firstMatchingSubtitleBackward == null) {
            log.error("no matching subtitles, most likely a bug");
            throw new IllegalStateException();
        } else if (firstMatchingSubtitleForward != null && firstMatchingSubtitleBackward != null) {
            MergerSubtitle mainSubtitle = subtitles.get(subtitleIndex);

            int millisBeforeNext = firstMatchingSubtitleForward.getFrom().getMillisOfDay()
                    - mainSubtitle.getTo().getMillisOfDay();
            int millisAfterPrevious = mainSubtitle.getFrom().getMillisOfDay()
                    - firstMatchingSubtitleBackward.getTo().getMillisOfDay();
            if (millisBeforeNext < millisAfterPrevious) {
                result = firstMatchingSubtitleForward.getLines();
            } else {
                result = firstMatchingSubtitleBackward.getLines();
            }
        } else if (firstMatchingSubtitleForward != null) {
            result = firstMatchingSubtitleForward.getLines();
        } else {
            result = firstMatchingSubtitleBackward.getLines();
        }

        return result.stream()
                .filter(line -> Objects.equals(line.getSource(), otherSource))
                .collect(toList());
    }

    private static void sortSubtitleLines(List<MergerSubtitle> subtitles) throws InterruptedException {
        for (MergerSubtitle subtitle : subtitles) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

            List<MergerSubtitleLine> orderedLines = new ArrayList<>();

            for (Source source : Source.values()) {
                orderedLines.addAll(
                        subtitle.getLines().stream().filter(line -> line.getSource() == source).collect(toList())
                );
            }

            subtitle.setLines(orderedLines);
        }
    }

    /**
     * Combines consecutive subtitles that have the same lines (to simply make the result more compact).
     */
    private static List<MergerSubtitle> getCombinedSubtitles(
            List<MergerSubtitle> subtitles
    ) throws InterruptedException {
        List<MergerSubtitle> result = new ArrayList<>();

        for (MergerSubtitle currentSubtitle : subtitles) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

            boolean shouldAddCurrentSubtitle = true;

            if (result.size() > 0) {
                MergerSubtitle lastAddedSubtitle = result.get(result.size() - 1);

                boolean canCombine = Objects.equals(lastAddedSubtitle.getLines(), currentSubtitle.getLines())
                        && Objects.equals(lastAddedSubtitle.getTo(), currentSubtitle.getFrom());
                if (canCombine) {
                    lastAddedSubtitle.setTo(currentSubtitle.getTo());
                    shouldAddCurrentSubtitle = false;
                }
            }

            if (shouldAddCurrentSubtitle) {
                result.add(currentSubtitle);
            }
        }

        return result;
    }

    private static Subtitles convert(List<MergerSubtitle> mergedSubtitles) throws InterruptedException {
        List<Subtitle> result = new ArrayList<>();

        for (MergerSubtitle mergedSubtitle : mergedSubtitles) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

            result.add(
                    new Subtitle(
                            mergedSubtitle.getFrom(),
                            mergedSubtitle.getTo(),
                            mergedSubtitle.getLines().stream().map(MergerSubtitleLine::getText).collect(toList())
                    )
            );
        }

        return new Subtitles(result);
    }

    @AllArgsConstructor
    @Getter
    private static class MergerSubtitle {
        private LocalTime from;

        @Setter
        private LocalTime to;

        @Setter
        private List<MergerSubtitleLine> lines;
    }

    @AllArgsConstructor
    @Getter
    @EqualsAndHashCode
    private static class MergerSubtitleLine {
        private String text;

        private Source source;
    }

    private enum Source {
        UPPER_SUBTITLES,
        LOWER_SUBTITLES
    }
}
