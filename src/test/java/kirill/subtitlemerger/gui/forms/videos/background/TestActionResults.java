package kirill.subtitlemerger.gui.forms.videos.background;

import kirill.subtitlemerger.logic.utils.entities.MultiPartActionResult;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class TestActionResults {
    @Test
    public void testLoadSubtitlesActionResult() {
        assertThat(AllSubtitlesLoader.getLoadSubtitlesActionResult(1, 0, 0))
                .isEqualTo(MultiPartActionResult.EMPTY);
        assertThat(AllSubtitlesLoader.getLoadSubtitlesActionResult(1, 1, 0))
                .isEqualTo(
                        MultiPartActionResult.onlyWarning(
                                "The subtitles have been loaded but have an incorrect format"
                        )
                );
        assertThat(AllSubtitlesLoader.getLoadSubtitlesActionResult(1, 0, 1))
                .isEqualTo(MultiPartActionResult.onlyError("Failed to load the subtitles"));

        assertThat(AllSubtitlesLoader.getLoadSubtitlesActionResult(2, 1, 0))
                .isEqualTo(
                        MultiPartActionResult.onlyWarning(
                                "1/2 subtitles have been loaded but have an incorrect format"
                        )
                );
        assertThat(AllSubtitlesLoader.getLoadSubtitlesActionResult(2, 2, 0))
                .isEqualTo(
                        MultiPartActionResult.onlyWarning(
                                "2/2 subtitles have been loaded but have incorrect formats"
                        )
                );

        assertThat(AllSubtitlesLoader.getLoadSubtitlesActionResult(2, 0, 1))
                .isEqualTo(MultiPartActionResult.onlyError("Failed to load 1/2 subtitles"));
        assertThat(AllSubtitlesLoader.getLoadSubtitlesActionResult(2, 0, 2))
                .isEqualTo(MultiPartActionResult.onlyError("Failed to load 2/2 subtitles"));

        assertThat(AllSubtitlesLoader.getLoadSubtitlesActionResult(3, 2, 1))
                .isEqualTo(
                        new MultiPartActionResult(
                                null,
                                "2/3 subtitles have been loaded but have incorrect formats",
                                "failed to load 1/3"
                        )
                );
    }

    @Test
    public void testLoadSubtitlesError() {
        assertThat(VideosBackgroundUtils.getLoadSubtitlesError(1, 0, 0))
                .isEqualTo("");
        assertThat(VideosBackgroundUtils.getLoadSubtitlesError(1, 1, 0))
                .isEqualTo("Failed to load the subtitles");
        assertThat(VideosBackgroundUtils.getLoadSubtitlesError(1, 0, 1))
                .isEqualTo("The subtitles have an incorrect format");

        assertThat(VideosBackgroundUtils.getLoadSubtitlesError(2, 1, 0))
                .isEqualTo("Failed to load 1/2 subtitles");
        assertThat(VideosBackgroundUtils.getLoadSubtitlesError(2, 2, 0))
                .isEqualTo("Failed to load 2/2 subtitles");

        assertThat(VideosBackgroundUtils.getLoadSubtitlesError(2, 0, 1))
                .isEqualTo("1/2 subtitles have an incorrect format");
        assertThat(VideosBackgroundUtils.getLoadSubtitlesError(2, 0, 2))
                .isEqualTo("2/2 subtitles have incorrect formats");

        assertThat(VideosBackgroundUtils.getLoadSubtitlesError(3, 1, 2))
                .isEqualTo("Failed to load 1/3 subtitles, 2/3 have incorrect formats");
    }

    @Test
    public void testLoadSubtitles() {
        assertThat(
                VideosBackgroundUtils.getLoadSubtitlesResult(
                        0,
                        0,
                        0,
                        0,
                        0
                )
        ).isEqualTo(MultiPartActionResult.onlyWarning("There are no subtitles to load"));

        assertThat(
                VideosBackgroundUtils.getLoadSubtitlesResult(
                        11,
                        0,
                        0,
                        0,
                        0
                )
        ).isEqualTo(MultiPartActionResult.onlyWarning("The task has been canceled, nothing was loaded"));

        assertThat(
                VideosBackgroundUtils.getLoadSubtitlesResult(
                        1,
                        1,
                        1,
                        0,
                        0
                )
        ).isEqualTo(MultiPartActionResult.onlySuccess("The subtitles have been loaded successfully"));

        assertThat(
                VideosBackgroundUtils.getLoadSubtitlesResult(
                        2,
                        2,
                        2,
                        0,
                        0
                )
        ).isEqualTo(MultiPartActionResult.onlySuccess("All 2 subtitles have been loaded successfully"));

        assertThat(
                VideosBackgroundUtils.getLoadSubtitlesResult(
                        1,
                        1,
                        0,
                        1,
                        0
                )
        ).isEqualTo(MultiPartActionResult.onlyWarning("The subtitles have been loaded but have an incorrect format"));

        assertThat(
                VideosBackgroundUtils.getLoadSubtitlesResult(
                        2,
                        2,
                        0,
                        2,
                        0
                )
        ).isEqualTo(MultiPartActionResult.onlyWarning("All 2 subtitles have been loaded but have incorrect formats"));

        assertThat(
                VideosBackgroundUtils.getLoadSubtitlesResult(
                        1,
                        1,
                        0,
                        0,
                        1
                )
        ).isEqualTo(MultiPartActionResult.onlyError("Failed to load the subtitles"));

        assertThat(
                VideosBackgroundUtils.getLoadSubtitlesResult(
                        2,
                        2,
                        0,
                        0,
                        2
                )
        ).isEqualTo(MultiPartActionResult.onlyError("Failed to load all 2 subtitles"));

        assertThat(
                VideosBackgroundUtils.getLoadSubtitlesResult(
                        4,
                        3,
                        1,
                        1,
                        1
                )
        ).isEqualTo(
                new MultiPartActionResult(
                        "1/4 subtitles have been loaded successfully",
                        "1/4 loaded but have an incorrect format, 1/4 not loaded because of the cancellation",
                        "failed to load 1/4"
                )
        );

        assertThat(
                VideosBackgroundUtils.getLoadSubtitlesResult(
                        4,
                        3,
                        1,
                        0,
                        2
                )
        ).isEqualTo(
                new MultiPartActionResult(
                        "1/4 subtitles have been loaded successfully",
                        "1/4 not loaded because of the cancellation",
                        "failed to load 2/4"
                )
        );

        assertThat(
                VideosBackgroundUtils.getLoadSubtitlesResult(
                        4,
                        3,
                        0,
                        2,
                        1
                )
        ).isEqualTo(
                new MultiPartActionResult(
                        null,
                        "2/4 subtitles have been loaded but have incorrect formats, 1/4 not loaded because of "
                                + "the cancellation",
                        "failed to load 1/4"
                )
        );

        assertThat(
                VideosBackgroundUtils.getLoadSubtitlesResult(
                        3,
                        1,
                        0,
                        0,
                        1
                )
        ).isEqualTo(
                new MultiPartActionResult(
                        null,
                        "2/3 subtitles have not been loaded because of the cancellation",
                        "failed to load 1/3"
                )
        );
    }

    @Test
    public void testAutoSelection() {
        assertThat(
                AutoSelectRunner.getActionResult(
                        11,
                        0,
                        0,
                        0,
                        0
                )
        ).isEqualTo(MultiPartActionResult.onlyWarning("The task has been canceled, nothing was done"));

        assertThat(
                AutoSelectRunner.getActionResult(
                        1,
                        1,
                        1,
                        0,
                        0
                )
        ).isEqualTo(MultiPartActionResult.onlySuccess("Auto-selecting has finished successfully for the video"));

        assertThat(
                AutoSelectRunner.getActionResult(
                        2,
                        2,
                        2,
                        0,
                        0
                )
        ).isEqualTo(MultiPartActionResult.onlySuccess("Auto-selecting has finished successfully for all 2 videos"));

        assertThat(
                AutoSelectRunner.getActionResult(
                        1,
                        1,
                        0,
                        1,
                        0
                )
        ).isEqualTo(MultiPartActionResult.onlyWarning("Auto-selecting is not possible for the video"));

        assertThat(
                AutoSelectRunner.getActionResult(
                        2,
                        2,
                        0,
                        2,
                        0
                )
        ).isEqualTo(MultiPartActionResult.onlyWarning("Auto-selecting is not possible for all 2 videos"));

        assertThat(
                AutoSelectRunner.getActionResult(
                        1,
                        1,
                        0,
                        0,
                        1
                )
        ).isEqualTo(MultiPartActionResult.onlyError("Auto-selecting has failed for the video"));

        assertThat(
                AutoSelectRunner.getActionResult(
                        2,
                        2,
                        0,
                        0,
                        2
                )
        ).isEqualTo(MultiPartActionResult.onlyError("Auto-selecting has failed for all 2 videos"));

        assertThat(
                AutoSelectRunner.getActionResult(
                        11,
                        9,
                        2,
                        3,
                        4
                )
        ).isEqualTo(
                new MultiPartActionResult(
                        "Auto-selecting has finished for 2/11 videos successfully",
                        "is not possible for 3/11, canceled for 2/11",
                        "failed for 4/11"
                )
        );

        assertThat(
                AutoSelectRunner.getActionResult(
                        11,
                        7,
                        0,
                        3,
                        4
                )
        ).isEqualTo(
                new MultiPartActionResult(
                        null,
                        "Auto-selecting is not possible for 3/11 videos, has been canceled for 4/11",
                        "failed for 4/11"
                )
        );

        assertThat(
                AutoSelectRunner.getActionResult(
                        11,
                        11,
                        0,
                        3,
                        8
                )
        ).isEqualTo(
                new MultiPartActionResult(
                        null,
                        "Auto-selecting is not possible for 3/11 videos",
                        "has failed for 8/11"
                )
        );

        assertThat(
                AutoSelectRunner.getActionResult(
                        11,
                        5,
                        0,
                        0,
                        5
                )
        ).isEqualTo(
                new MultiPartActionResult(
                        null,
                        "Auto-selecting has been canceled for 6/11 videos",
                        "failed for 5/11"
                )
        );
    }

    @Test
    public void testMerge() {
        assertThrows(
                IllegalArgumentException.class,
                () -> MergeRunner.getActionResult(
                        11,
                        5,
                        0,
                        2,
                        3,
                        0
                )
        );

        assertThat(
                MergeRunner.getActionResult(
                        11,
                        0,
                        0,
                        0,
                        0,
                        0
                )
        ).isEqualTo(MultiPartActionResult.onlyWarning("The task has been canceled, nothing was done"));

        assertThat(
                MergeRunner.getActionResult(
                        1,
                        1,
                        1,
                        0,
                        0,
                        0
                )
        ).isEqualTo(MultiPartActionResult.onlySuccess("Merging has finished successfully for the video"));

        assertThat(
                MergeRunner.getActionResult(
                        2,
                        2,
                        2,
                        0,
                        0,
                        0
                )
        ).isEqualTo(MultiPartActionResult.onlySuccess("Merging has finished successfully for all 2 videos"));

        assertThat(
                MergeRunner.getActionResult(
                        1,
                        1,
                        0,
                        1,
                        0,
                        0
                )
        ).isEqualTo(
                MultiPartActionResult.onlyWarning(
                        "Merging is not possible because you haven't confirmed file overwriting"
                )
        );

        assertThat(
                MergeRunner.getActionResult(
                        2,
                        2,
                        0,
                        2,
                        0,
                        0
                )
        ).isEqualTo(
                MultiPartActionResult.onlyWarning(
                        "Merging is not possible because you haven't confirmed file overwriting for all 2 videos"
                )
        );

        assertThat(
                MergeRunner.getActionResult(
                        1,
                        1,
                        0,
                        0,
                        1,
                        0
                )
        ).isEqualTo(
                MultiPartActionResult.onlyWarning(
                        "Selected subtitles have already been merged"
                )
        );

        assertThat(
                MergeRunner.getActionResult(
                        2,
                        2,
                        0,
                        0,
                        2,
                        0
                )
        ).isEqualTo(
                MultiPartActionResult.onlyWarning(
                        "Selected subtitles have already been merged for all 2 videos"
                )
        );

        assertThat(
                MergeRunner.getActionResult(
                        1,
                        1,
                        0,
                        0,
                        0,
                        1
                )
        ).isEqualTo(MultiPartActionResult.onlyError("Merging has failed for the video"));

        assertThat(
                MergeRunner.getActionResult(
                        2,
                        2,
                        0,
                        0,
                        0,
                        2
                )
        ).isEqualTo(MultiPartActionResult.onlyError("Merging has failed for all 2 videos"));

        assertThat(
                MergeRunner.getActionResult(
                        11,
                        9,
                        2,
                        3,
                        0,
                        4
                )
        ).isEqualTo(
                new MultiPartActionResult(
                        "Merging has finished for 2/11 videos successfully",
                        "is not possible for 3/11 (no confirmation to file overwriting), canceled for 2/11",
                        "failed for 4/11"
                )
        );

        assertThat(
                MergeRunner.getActionResult(
                        11,
                        9,
                        2,
                        0,
                        3,
                        4
                )
        ).isEqualTo(
                new MultiPartActionResult(
                        "Merging has finished for 2/11 videos successfully",
                        "is not possible for 3/11 (subtitles were already merged), canceled for 2/11",
                        "failed for 4/11"
                )
        );

        assertThat(
                MergeRunner.getActionResult(
                        11,
                        7,
                        0,
                        3,
                        0,
                        4
                )
        ).isEqualTo(
                new MultiPartActionResult(
                        null,
                        "Merging is not possible for 3/11 videos (no confirmation to file overwriting), has "
                                + "been canceled for 4/11",
                        "failed for 4/11"
                )
        );

        assertThat(
                MergeRunner.getActionResult(
                        11,
                        7,
                        0,
                        0,
                        3,
                        4
                )
        ).isEqualTo(
                new MultiPartActionResult(
                        null,
                        "Merging is not possible for 3/11 videos (subtitles were already merged), has "
                                + "been canceled for 4/11",
                        "failed for 4/11"
                )
        );

        assertThat(
                MergeRunner.getActionResult(
                        11,
                        11,
                        0,
                        3,
                        0,
                        8
                )
        ).isEqualTo(
                new MultiPartActionResult(
                        null,
                        "Merging is not possible for 3/11 videos (no confirmation to file overwriting)",
                        "has failed for 8/11"
                )
        );

        assertThat(
                MergeRunner.getActionResult(
                        11,
                        11,
                        0,
                        0,
                        3,
                        8
                )
        ).isEqualTo(
                new MultiPartActionResult(
                        null,
                        "Merging is not possible for 3/11 videos (subtitles were already merged)",
                        "has failed for 8/11"
                )
        );

        assertThat(
                MergeRunner.getActionResult(
                        11,
                        5,
                        0,
                        0,
                        0,
                        5
                )
        ).isEqualTo(
                new MultiPartActionResult(
                        null,
                        "Merging has been canceled for 6/11 videos",
                        "failed for 5/11"
                )
        );
    }
}
