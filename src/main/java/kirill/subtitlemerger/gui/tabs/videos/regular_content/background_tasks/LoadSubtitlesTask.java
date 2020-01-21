package kirill.subtitlemerger.gui.tabs.videos.regular_content.background_tasks;

import javafx.application.Platform;
import javafx.scene.control.ProgressIndicator;
import kirill.subtitlemerger.gui.GuiSettings;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.RegularContentController;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files.GuiFileInfo;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files.GuiSubtitleStreamInfo;
import kirill.subtitlemerger.logic.core.Parser;
import kirill.subtitlemerger.logic.core.Writer;
import kirill.subtitlemerger.logic.work_with_files.entities.FileInfo;
import kirill.subtitlemerger.logic.work_with_files.entities.SubtitleStreamInfo;
import kirill.subtitlemerger.logic.work_with_files.ffmpeg.Ffmpeg;
import kirill.subtitlemerger.logic.work_with_files.ffmpeg.FfmpegException;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class LoadSubtitlesTask extends BackgroundTask<Void> {
    private Integer subtitleId;

    private List<FileInfo> unsortedFilesInfo;

    private List<GuiFileInfo> guiFilesInfo;

    private Ffmpeg ffmpeg;

    private GuiSettings.SortBy sortBy;

    private GuiSettings.SortDirection sortDirection;

    @Getter
    private boolean cancelled;

    @Getter
    private int allSubtitleCount;

    @Getter
    private int processedCount;

    @Getter
    private int loadedSuccessfullyCount;

    @Getter
    private int loadedBeforeCount;

    @Getter
    private int failedToLoadCount;

    public LoadSubtitlesTask(
            List<FileInfo> filesInfo,
            List<GuiFileInfo> guiFilesInfo,
            GuiSettings.SortBy sortBy,
            GuiSettings.SortDirection sortDirection,
            Ffmpeg ffmpeg
    ) {
        super();

        this.unsortedFilesInfo = filesInfo;
        this.guiFilesInfo = guiFilesInfo;
        this.sortBy = sortBy;
        this.sortDirection = sortDirection;
        this.ffmpeg = ffmpeg;
    }

    public LoadSubtitlesTask(
            FileInfo fileInfo,
            GuiFileInfo guiFileInfo,
            GuiSettings.SortBy sortBy,
            GuiSettings.SortDirection sortDirection,
            Ffmpeg ffmpeg
    ) {
        super();

        this.unsortedFilesInfo = Collections.singletonList(fileInfo);
        this.guiFilesInfo = Collections.singletonList(guiFileInfo);
        this.sortBy = sortBy;
        this.sortDirection = sortDirection;
        this.ffmpeg = ffmpeg;
    }

    public LoadSubtitlesTask(
            int subtitleId,
            FileInfo fileInfo,
            GuiFileInfo guiFileInfo,
            GuiSettings.SortBy sortBy,
            GuiSettings.SortDirection sortDirection,
            Ffmpeg ffmpeg
    ) {
        super();

        this.subtitleId = subtitleId;
        this.unsortedFilesInfo = Collections.singletonList(fileInfo);
        this.guiFilesInfo = Collections.singletonList(guiFileInfo);
        this.sortBy = sortBy;
        this.sortDirection = sortDirection;
        this.ffmpeg = ffmpeg;
    }

    @Override
    protected Void call() {
        updateProgress(ProgressIndicator.INDETERMINATE_PROGRESS, ProgressIndicator.INDETERMINATE_PROGRESS);
        updateMessage("calculating number of subtitles to load...");
        initializeCounters();

        /*
         * We should process files in order the user sees them because the scene is updated during the process
         * and it would look strange for the user if the order won't meet his or her expectations.
         * We will sort the copy of the original list of course.
         */
        updateProgress(ProgressIndicator.INDETERMINATE_PROGRESS, ProgressIndicator.INDETERMINATE_PROGRESS);
        updateMessage("sorting files...");
        List<FileInfo> sortedFilesInfo = getSortedFilesInfo(unsortedFilesInfo, sortBy, sortDirection);

        mainLoop: for (FileInfo fileInfo : sortedFilesInfo) {
            GuiFileInfo guiFileInfo = RegularContentController.findMatchingGuiFileInfo(fileInfo, guiFilesInfo);
            if (!CollectionUtils.isEmpty(fileInfo.getSubtitleStreamsInfo())) {
                int index = 0;
                for (SubtitleStreamInfo subtitleStream : fileInfo.getSubtitleStreamsInfo()) {
                    if (isCancelled()) {
                        cancelled = true;
                        break mainLoop;
                    }

                    if (subtitleStream.getUnavailabilityReason() != null) {
                        index++;
                        continue;
                    }

                    if (subtitleId != null && subtitleId != subtitleStream.getId()) {
                        index++;
                        continue;
                    }

                    if (subtitleStream.getSubtitles() != null) {
                        processedCount++;
                        index++;
                        continue;
                    }

                    try {
                        updateMessage(
                                getUpdateMessage(
                                        processedCount,
                                        allSubtitleCount,
                                        subtitleStream,
                                        fileInfo.getFile()
                                )
                        );

                        String subtitleText = ffmpeg.getSubtitlesText(subtitleStream.getId(), fileInfo.getFile());
                        subtitleStream.setSubtitles(
                                Parser.fromSubRipText(
                                        subtitleText,
                                        subtitleStream.getTitle(),
                                        subtitleStream.getLanguage()
                                )
                        );

                        GuiSubtitleStreamInfo guiSubtitleStreamInfo = guiFileInfo.getSubtitleStreamsInfo().get(index);
                        int subtitleSize = Writer.toSubRipText(subtitleStream.getSubtitles()).getBytes().length;

                        /*
                         * Have to call this in the JavaFX thread because this change can lead to updates on the screen.
                         */
                        Platform.runLater(() -> guiSubtitleStreamInfo.setSize(subtitleSize));

                        loadedSuccessfullyCount++;
                    } catch (FfmpegException e) {
                        if (e.getCode() == FfmpegException.Code.INTERRUPTED) {
                            cancelled = true;
                            break mainLoop;
                        } else {
                            //todo save reason
                            failedToLoadCount++;
                        }
                    } catch (Parser.IncorrectFormatException e) {
                        //todo save reason
                        failedToLoadCount++;
                    }

                    processedCount++;
                    index++;
                }
            }

            guiFileInfo.setHaveSubtitleSizesToLoad(RegularContentController.haveSubtitlesToLoad(fileInfo));
        }

        return null;
    }

    private void initializeCounters() {
        for (FileInfo fileInfo : unsortedFilesInfo) {
            if (!CollectionUtils.isEmpty(fileInfo.getSubtitleStreamsInfo())) {
                for (SubtitleStreamInfo subtitleStream : fileInfo.getSubtitleStreamsInfo()) {
                    if (subtitleStream.getUnavailabilityReason() != null) {
                        continue;
                    }

                    if (subtitleId != null && subtitleId != subtitleStream.getId()) {
                        continue;
                    }

                    allSubtitleCount++;

                    if (subtitleStream.getSubtitles() != null) {
                        loadedBeforeCount++;
                    }
                }
            }
        }
    }

    private static List<FileInfo> getSortedFilesInfo(
            List<FileInfo> unsortedFilesInfo,
            GuiSettings.SortBy sortBy,
            GuiSettings.SortDirection sortDirection
    ) {
        List<FileInfo> result = new ArrayList<>(unsortedFilesInfo);

        Comparator<FileInfo> comparator;
        switch (sortBy) {
            case NAME:
                comparator = Comparator.comparing(fileInfo -> fileInfo.getFile().getAbsolutePath());
                break;
            case MODIFICATION_TIME:
                comparator = Comparator.comparing(FileInfo::getLastModified);
                break;
            case SIZE:
                comparator = Comparator.comparing(FileInfo::getSize);
                break;
            default:
                throw new IllegalStateException();
        }

        if (sortDirection == GuiSettings.SortDirection.DESCENDING) {
            comparator = comparator.reversed();
        }

        result.sort(comparator);

        return result;
    }

    private static String getUpdateMessage(
            int processedCount,
            int subtitleToLoadCount,
            SubtitleStreamInfo subtitleStream,
            File file
    ) {
        String language = subtitleStream.getLanguage() != null
                ? subtitleStream.getLanguage().toString().toUpperCase()
                : "UNKNOWN LANGUAGE";
        return (processedCount + 1) + "/" + subtitleToLoadCount + " getting subtitle "
                + language
                + (StringUtils.isBlank(subtitleStream.getTitle()) ? "" : " " + subtitleStream.getTitle())
                + " in " + file.getName();
    }
}
