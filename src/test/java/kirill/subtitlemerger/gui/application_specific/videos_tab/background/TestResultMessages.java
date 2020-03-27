package kirill.subtitlemerger.gui.application_specific.videos_tab.background;

import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;

public class TestResultMessages {
    @Test
    public void testAddFiles() {
      /*  assertThat(
                AddFilesTask.generateMultiPartResult(getMockedAddFilesResult(1, 0))
        ).isEqualTo(new ActionResult("File has been added already", null, null));

        assertThat(
                AddFilesTask.generateMultiPartResult(getMockedAddFilesResult(2, 0))
        ).isEqualTo(new ActionResult("All 2 files have been added already", null, null));

        assertThat(
                AddFilesTask.generateMultiPartResult(getMockedAddFilesResult(1, 1))
        ).isEqualTo(new ActionResult("File has been added successfully", null, null));

        assertThat(
                AddFilesTask.generateMultiPartResult(getMockedAddFilesResult(2, 2))
        ).isEqualTo(new ActionResult("All 2 files have been added successfully", null, null));

        assertThat(
                AddFilesTask.generateMultiPartResult(getMockedAddFilesResult(2, 1))
        ).isEqualTo(
                new ActionResult(
                        "1/2 files has been added successfully, 1/2 added before",
                        null,
                        null
                )
        );

        assertThat(
                AddFilesTask.generateMultiPartResult(getMockedAddFilesResult(3, 2))
        ).isEqualTo(
                new ActionResult(
                        "2/3 files have been added successfully, 1/3 added before",
                        null,
                        null
                )
        );*/ //todo restore!!!
    }

 /*   private static AddFilesTask.Result getMockedAddFilesResult(int filesToAddCount, int actuallyAddedCount) {
        AddFilesTask.Result result = mock(AddFilesTask.Result.class);

        when(result.getFilesToAddCount()).thenReturn(filesToAddCount);
        when(result.getActuallyAddedCount()).thenReturn(actuallyAddedCount);

        return result;
    }

    @Test
    public void testAutoSelectSubtitles() {
        assertThat(
                AutoSelectSubtitlesTask.generateMultiPartResult(
                        getMockedAutoSelectSubtitlesResult(
                                11,
                                0,
                                0,
                                0,
                                0
                        )
                )
        ).isEqualTo(new ActionResult(null, "Task has been cancelled, nothing was done", null));

        assertThat(
                AutoSelectSubtitlesTask.generateMultiPartResult(
                        getMockedAutoSelectSubtitlesResult(
                                1,
                                1,
                                1,
                                0,
                                0
                        )
                )
        ).isEqualTo(
                new ActionResult(
                        "Auto-selection has finished successfully for the file",
                        null,
                        null
                )
        );

        assertThat(
                AutoSelectSubtitlesTask.generateMultiPartResult(
                        getMockedAutoSelectSubtitlesResult(
                                2,
                                2,
                                2,
                                0,
                                0
                        )
                )
        ).isEqualTo(
                new ActionResult(
                        "Auto-selection has finished successfully for all 2 files",
                        null,
                        null
                )
        );

        assertThat(
                AutoSelectSubtitlesTask.generateMultiPartResult(
                        getMockedAutoSelectSubtitlesResult(
                                1,
                                1,
                                0,
                                1,
                                0
                        )
                )
        ).isEqualTo(new ActionResult(null, "Auto-selection is not possible for this file", null));

        assertThat(
                AutoSelectSubtitlesTask.generateMultiPartResult(
                        getMockedAutoSelectSubtitlesResult(
                                2,
                                2,
                                0,
                                2,
                                0
                        )
                )
        ).isEqualTo(
                new ActionResult(null, "Auto-selection is not possible for all 2 files", null)
        );

        assertThat(
                AutoSelectSubtitlesTask.generateMultiPartResult(
                        getMockedAutoSelectSubtitlesResult(
                                1,
                                1,
                                0,
                                0,
                                1
                        )
                )
        ).isEqualTo(
                new ActionResult(null, null, "Failed to perform auto-selection for the file")
        );

        assertThat(
                AutoSelectSubtitlesTask.generateMultiPartResult(
                        getMockedAutoSelectSubtitlesResult(
                                2,
                                2,
                                0,
                                0,
                                2
                        )
                )
        ).isEqualTo(
                new ActionResult(null, null, "Failed to perform auto-selection for all 2 files")
        );

        assertThat(
                AutoSelectSubtitlesTask.generateMultiPartResult(
                        getMockedAutoSelectSubtitlesResult(
                                4,
                                3,
                                1,
                                1,
                                1
                        )
                )
        ).isEqualTo(
                new ActionResult(
                        "Auto-selection has finished for 1/4 files successfully",
                        "cancelled for 1/4, not possible for 1/4",
                        "failed for 1/4"
                )
        );

        assertThat(
                AutoSelectSubtitlesTask.generateMultiPartResult(
                        getMockedAutoSelectSubtitlesResult(
                                5,
                                4,
                                2,
                                1,
                                1
                        )
                )
        ).isEqualTo(
                new ActionResult(
                        "Auto-selection has finished for 2/5 files successfully",
                        "cancelled for 1/5, not possible for 1/5",
                        "failed for 1/5"
                )
        );

        assertThat(
                AutoSelectSubtitlesTask.generateMultiPartResult(
                        getMockedAutoSelectSubtitlesResult(
                                3,
                                2,
                                0,
                                1,
                                1
                        )
                )
        ).isEqualTo(
                new ActionResult(
                        null,
                        "Auto-selection has been cancelled for 1/3 files, not possible for 1/3",
                        "failed for 1/3"
                )
        );

        assertThat(
                AutoSelectSubtitlesTask.generateMultiPartResult(
                        getMockedAutoSelectSubtitlesResult(
                                4,
                                2,
                                0,
                                1,
                                1
                        )
                )
        ).isEqualTo(
                new ActionResult(
                        null,
                        "Auto-selection has been cancelled for 2/4 files, not possible for 1/4",
                        "failed for 1/4"
                )
        );

        assertThat(
                AutoSelectSubtitlesTask.generateMultiPartResult(
                        getMockedAutoSelectSubtitlesResult(
                                2,
                                2,
                                0,
                                1,
                                1
                        )
                )
        ).isEqualTo(
                new ActionResult(
                        null,
                        "Auto-selection is not possible for 1/2 files",
                        "failed for 1/2"
                )
        );

        assertThat(
                AutoSelectSubtitlesTask.generateMultiPartResult(
                        getMockedAutoSelectSubtitlesResult(
                                3,
                                3,
                                0,
                                2,
                                1
                        )
                )
        ).isEqualTo(
                new ActionResult(
                        null,
                        "Auto-selection is not possible for 2/3 files",
                        "failed for 1/3"
                )
        );
    }

    private static AutoSelectSubtitlesTask.Result getMockedAutoSelectSubtitlesResult(
            int allFileCount,
            int processedCount,
            int finishedSuccessfullyCount,
            int notPossibleCount,
            int failedCount
    ) {
        AutoSelectSubtitlesTask.Result result = mock(AutoSelectSubtitlesTask.Result.class);

        when(result.getAllFileCount()).thenReturn(allFileCount);
        when(result.getProcessedCount()).thenReturn(processedCount);
        when(result.getFinishedSuccessfullyCount()).thenReturn(finishedSuccessfullyCount);
        when(result.getNotPossibleCount()).thenReturn(notPossibleCount);
        when(result.getFailedCount()).thenReturn(failedCount);

        return result;
    }

    @Test
    public void testLoadSubtitles() {
        assertThat(
                LoadFilesAllSubtitlesTask.generateMultiPartResult(
                        getMockedLoadSubtitlesResult(
                                0,
                                0,
                                0,
                                0
                        )
                )
        ).isEqualTo(new ActionResult(null, "There are no subtitles to load", null));

        assertThat(
                LoadFilesAllSubtitlesTask.generateMultiPartResult(
                        getMockedLoadSubtitlesResult(
                                11,
                                0,
                                0,
                                0
                        )
                )
        ).isEqualTo(new ActionResult(null, "Task has been cancelled, nothing was loaded", null));

        assertThat(
                LoadFilesAllSubtitlesTask.generateMultiPartResult(
                        getMockedLoadSubtitlesResult(
                                1,
                                1,
                                1,
                                0
                        )
                )
        ).isEqualTo(new ActionResult("Subtitles have been loaded successfully", null, null));

        assertThat(
                LoadFilesAllSubtitlesTask.generateMultiPartResult(
                        getMockedLoadSubtitlesResult(
                                2,
                                2,
                                2,
                                0
                        )
                )
        ).isEqualTo(
                new ActionResult(
                        "All 2 subtitles have been loaded successfully",
                        null,
                        null
                )
        );

        assertThat(
                LoadFilesAllSubtitlesTask.generateMultiPartResult(
                        getMockedLoadSubtitlesResult(
                                1,
                                1,
                                0,
                                1
                        )
                )
        ).isEqualTo(new ActionResult(null, null, "Failed to load subtitles"));

        assertThat(
                LoadFilesAllSubtitlesTask.generateMultiPartResult(
                        getMockedLoadSubtitlesResult(
                                2,
                                2,
                                0,
                                2
                        )
                )
        ).isEqualTo(new ActionResult(null, null, "Failed to load all 2 subtitles"));

        assertThat(
                LoadFilesAllSubtitlesTask.generateMultiPartResult(
                        getMockedLoadSubtitlesResult(
                                3,
                                2,
                                1,
                                1
                        )
                )
        ).isEqualTo(
                new ActionResult(
                        "1/3 subtitles have been loaded successfully",
                        "1/3 cancelled",
                        "1/3 failed"
                )
        );

        assertThat(
                LoadFilesAllSubtitlesTask.generateMultiPartResult(
                        getMockedLoadSubtitlesResult(
                                4,
                                3,
                                2,
                                1
                        )
                )
        ).isEqualTo(
                new ActionResult(
                        "2/4 subtitles have been loaded successfully",
                        "1/4 cancelled",
                        "1/4 failed"
                )
        );

        assertThat(
                LoadFilesAllSubtitlesTask.generateMultiPartResult(
                        getMockedLoadSubtitlesResult(
                                2,
                                1,
                                0,
                                1
                        )
                )
        ).isEqualTo(
                new ActionResult(
                        null,
                        "1/2 subtitles' loadings has been cancelled",
                        "1/2 failed"
                )
        );

        assertThat(
                LoadFilesAllSubtitlesTask.generateMultiPartResult(
                        getMockedLoadSubtitlesResult(
                                3,
                                1,
                                0,
                                1
                        )
                )
        ).isEqualTo(
                new ActionResult(
                        null,
                        "2/3 subtitles' loadings have been cancelled",
                        "1/3 failed"
                )
        );
    }

    private static LoadFilesAllSubtitlesTask.Result getMockedLoadSubtitlesResult(
            int streamToLoadCount,
            int processedCount,
            int loadedSuccessfullyCount,
            int failedToLoadCount
    ) {
        LoadFilesAllSubtitlesTask.Result result = mock(LoadFilesAllSubtitlesTask.Result.class);

        when(result.getStreamToLoadCount()).thenReturn(streamToLoadCount);
        when(result.getProcessedCount()).thenReturn(processedCount);
        when(result.getLoadedSuccessfullyCount()).thenReturn(loadedSuccessfullyCount);
        when(result.getFailedToLoadCount()).thenReturn(failedToLoadCount);

        return result;
    }*/
}
