package kirill.subtitlemerger.gui.forms.videos.background;

import kirill.subtitlemerger.logic.utils.entities.ActionResult;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;


public class TestActionResults {
    @Test
    public void testAutoSelect() {
        assertThat(
                AutoSelectRunner.getActionResult(
                        11,
                        0,
                        0,
                        0,
                        0
                )
        ).isEqualTo(new ActionResult("", "The task has been cancelled, nothing was done", ""));

        assertThat(
                AutoSelectRunner.getActionResult(
                        1,
                        1,
                        1,
                        0,
                        0
                )
        ).isEqualTo(
                new ActionResult("Auto-selection has finished successfully for the video", "", "")
        );

        assertThat(
                AutoSelectRunner.getActionResult(
                        2,
                        2,
                        2,
                        0,
                        0
                )
        ).isEqualTo(
                new ActionResult(
                        "Auto-selection has finished successfully for all 2 videos",
                        "",
                        ""
                )
        );

        assertThat(
                AutoSelectRunner.getActionResult(
                        1,
                        1,
                        0,
                        1,
                        0
                )
        ).isEqualTo(new ActionResult("", "Auto-selection is not possible for the video", ""));

        assertThat(
                AutoSelectRunner.getActionResult(
                        2,
                        2,
                        0,
                        2,
                        0
                )
        ).isEqualTo(new ActionResult("", "Auto-selection is not possible for all 2 videos", ""));

        assertThat(
                AutoSelectRunner.getActionResult(
                        1,
                        1,
                        0,
                        0,
                        1
                )
        ).isEqualTo(new ActionResult("", "", "Auto-selection has failed for the video"));

        assertThat(
                AutoSelectRunner.getActionResult(
                        2,
                        2,
                        0,
                        0,
                        2
                )
        ).isEqualTo(new ActionResult("", "", "Auto-selection has failed for all 2 videos"));

        assertThat(
                AutoSelectRunner.getActionResult(
                        4,
                        3,
                        1,
                        1,
                        1
                )
        ).isEqualTo(
                new ActionResult(
                        "Auto-selection has finished for 1/4 videos successfully",
                        "cancelled for 1/4, not possible for 1/4",
                        "failed for 1/4"
                )
        );

        assertThat(
                AutoSelectRunner.getActionResult(
                        3,
                        2,
                        0,
                        1,
                        1
                )
        ).isEqualTo(
                new ActionResult(
                        "",
                        "Auto-selection has been cancelled for 1/3 videos, not possible for 1/3",
                        "failed for 1/3"
                )
        );

        assertThat(
                AutoSelectRunner.getActionResult(
                        3,
                        3,
                        0,
                        2,
                        1
                )
        ).isEqualTo(
                new ActionResult(
                        "",
                        "Auto-selection is not possible for 2/3 videos",
                        "failed for 1/3"
                )
        );
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
        ).isEqualTo(new ActionResult("", "There are no subtitles to load", ""));

        assertThat(
                VideosBackgroundUtils.getLoadSubtitlesResult(
                        11,
                        0,
                        0,
                        0,
                        0
                )
        ).isEqualTo(new ActionResult("", "The task has been cancelled, nothing was loaded", ""));

        assertThat(
                VideosBackgroundUtils.getLoadSubtitlesResult(
                        1,
                        1,
                        1,
                        0,
                        0
                )
        ).isEqualTo(new ActionResult("The subtitles have been loaded successfully", "", ""));

        assertThat(
                VideosBackgroundUtils.getLoadSubtitlesResult(
                        2,
                        2,
                        2,
                        0,
                        0
                )
        ).isEqualTo(new ActionResult("All 2 subtitles have been loaded successfully", "", ""));

        assertThat(
                VideosBackgroundUtils.getLoadSubtitlesResult(
                        1,
                        1,
                        0,
                        1,
                        0
                )
        ).isEqualTo(new ActionResult("", "", "Failed to load the subtitles"));

        assertThat(
                VideosBackgroundUtils.getLoadSubtitlesResult(
                        2,
                        2,
                        0,
                        2,
                        0
                )
        ).isEqualTo(new ActionResult("", "", "Failed to load all 2 subtitles"));

        assertThat(
                VideosBackgroundUtils.getLoadSubtitlesResult(
                        1,
                        1,
                        0,
                        0,
                        1
                )
        ).isEqualTo(new ActionResult("", "", "The subtitles have an incorrect format"));

        assertThat(
                VideosBackgroundUtils.getLoadSubtitlesResult(
                        2,
                        2,
                        0,
                        0,
                        2
                )
        ).isEqualTo(new ActionResult("", "", "All 2 subtitles have an incorrect format"));

        assertThat(
                VideosBackgroundUtils.getLoadSubtitlesResult(
                        4,
                        3,
                        1,
                        1,
                        1
                )
        ).isEqualTo(
                new ActionResult(
                        "1/4 subtitles have been loaded successfully",
                        "1/4 cancelled",
                        "failed to load 1/4, 1/4 have an incorrect format"
                )
        );

        assertThat(
                VideosBackgroundUtils.getLoadSubtitlesResult(
                        3,
                        2,
                        0,
                        1,
                        1
                )
        ).isEqualTo(
                new ActionResult(
                        "",
                        "1/3 subtitle loadings has been cancelled",
                        "failed to load 1/3, 1/3 have an incorrect format"
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
                new ActionResult(
                        "",
                        "2/3 subtitle loadings have been cancelled",
                        "1/3 have an incorrect format"
                )
        );
    }
}
