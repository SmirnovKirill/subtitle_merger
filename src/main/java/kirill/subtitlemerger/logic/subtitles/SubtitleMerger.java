package kirill.subtitlemerger.logic.subtitles;

import kirill.subtitlemerger.logic.subtitles.entities.Subtitle;
import kirill.subtitlemerger.logic.subtitles.entities.Subtitles;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.ListUtils;
import org.jetbrains.annotations.Nullable;
import org.joda.time.LocalTime;

import java.util.*;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public class SubtitleMerger {
    public static Subtitles mergeSubtitles(
            Subtitles upperSubtitles,
            Subtitles lowerSubtitles
    ) throws InterruptedException {
        List<MergerSubtitle> result = makeInitialMerge(upperSubtitles, lowerSubtitles);
        fixJumps(result);
        orderSubtitleLines(result);
        result = getCombinedSubtitles(result);

        return convert(result);
    }

    /**
     * The first and the simplest stage of the merge - we make a list of all seen points of time and for each segment we
     * see whether there are subtitles from any source and if there are we create a subtitle for this segment.
     */
    private static List<MergerSubtitle> makeInitialMerge(
            Subtitles upperSubtitles,
            Subtitles lowerSubtitles
    ) throws InterruptedException {
        List<MergerSubtitle> result = new ArrayList<>();

        List<LocalTime> pointsOfTime = getUniqueSortedPointsOfTime(upperSubtitles, lowerSubtitles);

        boolean upperConsequential = consequentialSubtitles(upperSubtitles);
        boolean lowerConsequential = consequentialSubtitles(lowerSubtitles);
        int upperIndex = 0;
        int lowerIndex = 0;
        for (int i = 0; i < pointsOfTime.size() - 1; i++) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

            LocalTime from = pointsOfTime.get(i);
            LocalTime to = pointsOfTime.get(i + 1);

            Subtitle upperSubtitle = null;
            Integer matchingUpperIndex = getIndexMatchingTime(upperIndex, upperSubtitles, from, to, upperConsequential);
            if (matchingUpperIndex != null) {
                upperIndex = matchingUpperIndex;
                upperSubtitle = upperSubtitles.getSubtitles().get(upperIndex);
            }

            Subtitle lowerSubtitle = null;
            Integer matchingLowerIndex = getIndexMatchingTime(lowerIndex, lowerSubtitles, from, to, lowerConsequential);
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

        List<Subtitle> allSubtitles = ListUtils.union(upperSubtitles.getSubtitles(), lowerSubtitles.getSubtitles());
        for (Subtitle subtitle : allSubtitles) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

            result.add(subtitle.getFrom());
            result.add(subtitle.getTo());
        }

        return new ArrayList<>(result);
    }

    /**
     * Returns true if all the subtitles go consequentially meaning time points don't decrease. The result will be used
     * to increase merging performance later.
     */
    private static boolean consequentialSubtitles(Subtitles subtitles) {
        for (int i = 0; i < subtitles.getSubtitles().size(); i++) {
            Subtitle subtitle = subtitles.getSubtitles().get(i);
            if (subtitle.getTo().isBefore(subtitle.getFrom())) {
                return false;
            }

            if (i != subtitles.getSubtitles().size() - 1) {
                Subtitle nextSubtitle = subtitles.getSubtitles().get(i + 1);
                if (nextSubtitle.getFrom().isBefore(subtitle.getTo())) {
                    return false;
                }
            }
        }

        return true;
    }

    /*
     * For correctly formatted subtitles we can check only the current index and the next one (if there is one). But
     * sometimes there can be subtitles that are overlapped in time and to deal with them we have to check all indices
     * to the right. It's better to handle these situations differently because if we check only two indices instead of
     * all indices to the right performance will be increased significantly (dozens of times, for example for 10 mb
     * files initial merging would take around 100 ms instead of several seconds without this optimization).
     */
    @Nullable
    private static Integer getIndexMatchingTime(
            int currentIndex,
            Subtitles subtitles,
            LocalTime from,
            LocalTime to,
            boolean consequentialSubtitles
    ) {
        int topIndexToCheck;
        if (consequentialSubtitles) {
            topIndexToCheck = Integer.min(currentIndex + 1, subtitles.getSubtitles().size() - 1);
        } else {
            topIndexToCheck = subtitles.getSubtitles().size() - 1;
        }

        for (int i = currentIndex; i <= topIndexToCheck; i++) {
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
     * It may occur after the initial merge that several consecutive merged subtitles have the exact same lines from one
     * of the sources. If there is at least one subtitle among them who has only these lines (and thus just one source)
     * while there is also at least one subtitle that has these lines plus the lines from the other source, it may look
     * like a "jump" of the lines from the first source. Imagine that a subtitle with only these lines is displayed and
     * right after it goes another subtitle that has both these lines at the top and the other lines at the bottom.
     * Because the lines go alone and later they are displayed on top of the other lines it looks like a jump.
     * To fix this we should find all such subtitles with only one source and add appropriate lines from the other
     * source for them.
     */
    private static void fixJumps(List<MergerSubtitle> subtitles) throws InterruptedException {
        for (int i = 0; i < subtitles.size(); i++) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

            MergerSubtitle subtitle = subtitles.get(i);
            Set<Source> sources = subtitle.getLines().stream().map(MergerSubtitleLine::getSource).collect(toSet());
            if (sources.size() == 1) {
                Source otherSource = Arrays.stream(Source.values())
                        .filter(currentSource -> !sources.contains(currentSource))
                        .findFirst().orElseThrow(IllegalStateException::new);

                List<MergerSubtitleLine> linesToAdd = getLinesFromOtherSourceToAdd(i, otherSource, subtitles);
                if (linesToAdd != null) {
                    subtitle.getLines().addAll(linesToAdd);
                }
            }
        }
    }

    /**
     * Checks the subtitles around the subtitle with a given index which have the exact same lines for the source. If at
     * least one of them has lines from the other source as well then the lines from the other source are returned. The
     * method returns the closest lines and gives priority to subtitles to the left because it's better to show already
     * displayed subtitles longer than to show not yet displayed subtitles sooner (because of spoilers).
     */
    @Nullable
    private static List<MergerSubtitleLine> getLinesFromOtherSourceToAdd(
            int subtitleIndex,
            Source otherSource,
            List<MergerSubtitle> subtitles
    ) throws InterruptedException {
        MergerSubtitle subtitle = subtitles.get(subtitleIndex);
        Source source = subtitle.getLines().get(0).getSource();

        for (int i = subtitleIndex - 1; i >= 0; i--) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

            if (areConsecutive(subtitles.get(i), subtitles.get(i + 1), source)) {
                /* If the lines are different it means that this subtitle has lines from the other source. */
                if (!Objects.equals(subtitles.get(i).getLines(), subtitles.get(i + 1).getLines())) {
                    return subtitles.get(i).getLines().stream()
                            .filter(line -> line.getSource() == otherSource)
                            .collect(toList());
                }
            } else {
                break;
            }
        }

        for (int i = subtitleIndex + 1; i < subtitles.size(); i++) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

            if (areConsecutive(subtitles.get(i - 1), subtitles.get(i), source)) {
                /* If the lines are different it means that this subtitle has lines from the other source. */
                if (!Objects.equals(subtitles.get(i - 1).getLines(), subtitles.get(i).getLines())) {
                    return subtitles.get(i).getLines().stream()
                            .filter(line -> line.getSource() == otherSource)
                            .collect(toList());
                }
            } else {
                break;
            }
        }

        return null;
    }

    /**
     * Returns true if the subtitles are consecutive for the source meaning they go strictly one after the other and the
     * lines for the source are equal.
     */
    private static boolean areConsecutive(MergerSubtitle previous, MergerSubtitle next, Source source) {
        if (!Objects.equals(previous.getTo(), next.getFrom())) {
            return false;
        }

        return Objects.equals(
                previous.getLines().stream().filter(line -> line.getSource() == source).collect(toList()),
                next.getLines().stream().filter(line -> line.getSource() == source).collect(toList())
        );
    }

    /**
     * After the previous steps subtitle lines may be mixed up a little bit meaning lines from lower subtitles may go
     * higher than lines from upper subtitles. This method fixes that.
     */
    private static void orderSubtitleLines(List<MergerSubtitle> subtitles) throws InterruptedException {
        for (MergerSubtitle subtitle : subtitles) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

            List<MergerSubtitleLine> orderedLines = ListUtils.union(
                    subtitle.getLines().stream()
                            .filter(line -> line.getSource() == Source.UPPER_SUBTITLES)
                            .collect(toList()),
                    subtitle.getLines().stream()
                            .filter(line -> line.getSource() == Source.LOWER_SUBTITLES)
                            .collect(toList())
            );

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

            boolean addCurrentSubtitle = true;
            if (result.size() > 0) {
                MergerSubtitle lastAddedSubtitle = result.get(result.size() - 1);

                boolean canCombine = Objects.equals(lastAddedSubtitle.getLines(), currentSubtitle.getLines())
                        && Objects.equals(lastAddedSubtitle.getTo(), currentSubtitle.getFrom());
                if (canCombine) {
                    lastAddedSubtitle.setTo(currentSubtitle.getTo());
                    addCurrentSubtitle = false;
                }
            }

            if (addCurrentSubtitle) {
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