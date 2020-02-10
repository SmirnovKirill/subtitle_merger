package kirill.subtitlemerger.gui.tabs.videos.regular_content.background_tasks;

import kirill.subtitlemerger.gui.core.entities.MultiPartResult;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestResultMessages {
    @Test
    public void testAddFilesTask() {
        assertThat(
                AddFilesTask.generateMultiPartResult(getMockedAddFilesResult(1, 0))
        ).isEqualTo(new MultiPartResult("File has been added already", null, null));

        assertThat(
                AddFilesTask.generateMultiPartResult(getMockedAddFilesResult(2, 0))
        ).isEqualTo(new MultiPartResult("All 2 files have been added already", null, null));

        assertThat(
                AddFilesTask.generateMultiPartResult(getMockedAddFilesResult(1, 1))
        ).isEqualTo(new MultiPartResult("File has been added successfully", null, null));

        assertThat(
                AddFilesTask.generateMultiPartResult(getMockedAddFilesResult(2, 2))
        ).isEqualTo(new MultiPartResult("All 2 files have been added successfully", null, null));

        assertThat(
                AddFilesTask.generateMultiPartResult(getMockedAddFilesResult(2, 1))
        ).isEqualTo(
                new MultiPartResult(
                        "1/2 files has been added successfully, 1/2 added before",
                        null,
                        null
                )
        );

        assertThat(
                AddFilesTask.generateMultiPartResult(getMockedAddFilesResult(3, 2))
        ).isEqualTo(
                new MultiPartResult(
                        "2/3 files have been added successfully, 1/3 added before",
                        null,
                        null
                )
        );
    }

    private static AddFilesTask.Result getMockedAddFilesResult(int filesToAddCount, int actuallyAddedCount) {
        AddFilesTask.Result result = mock(AddFilesTask.Result.class);

        when(result.getFilesToAddCount()).thenReturn(filesToAddCount);
        when(result.getActuallyAddedCount()).thenReturn(actuallyAddedCount);

        return result;
    }
}
