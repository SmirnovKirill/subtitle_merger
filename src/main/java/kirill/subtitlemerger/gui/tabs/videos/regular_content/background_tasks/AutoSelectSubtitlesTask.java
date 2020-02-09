package kirill.subtitlemerger.gui.tabs.videos.regular_content.background_tasks;

import javafx.application.Platform;
import kirill.subtitlemerger.gui.GuiSettings;
import kirill.subtitlemerger.gui.background_tasks.BackgroundTask;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.RegularContentController;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files.GuiFileInfo;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files.GuiSubtitleStream;
import kirill.subtitlemerger.logic.core.Parser;
import kirill.subtitlemerger.logic.work_with_files.entities.FileInfo;
import kirill.subtitlemerger.logic.work_with_files.entities.SubtitleStream;
import kirill.subtitlemerger.logic.work_with_files.ffmpeg.Ffmpeg;
import kirill.subtitlemerger.logic.work_with_files.ffmpeg.FfmpegException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class AutoSelectSubtitlesTask extends BackgroundTask<AutoSelectSubtitlesTask.Result> {
    private List<FileInfo> allFilesInfo;

    private List<GuiFileInfo> displayedGuiFilesInfo;

    private Ffmpeg ffmpeg;

    private GuiSettings guiSettings;

    private Consumer<Result> onFinish;

    public AutoSelectSubtitlesTask(
            List<FileInfo> allFilesInfo,
            List<GuiFileInfo> displayedGuiFilesInfo,
            Ffmpeg ffmpeg,
            GuiSettings guiSettings,
            Consumer<Result> onFinish
    ) {
        this.allFilesInfo = allFilesInfo;
        this.displayedGuiFilesInfo = displayedGuiFilesInfo;
        this.ffmpeg = ffmpeg;
        this.guiSettings = guiSettings;
        this.onFinish = onFinish;
    }

    @Override
    protected Result run() {
        LoadDirectoryFilesTask.clearMessages(displayedGuiFilesInfo, this);

        updateMessage("getting list of files to work with...");
        List<GuiFileInfo> guiFilesInfoToWorkWith = getGuiFilesInfoToWorkWith(displayedGuiFilesInfo);

        Result result = new Result(
                guiFilesInfoToWorkWith.size(),
                0,
                0,
                0,
                0
        );
        setCancellationPossible(true);

        for (GuiFileInfo guiFileInfo : guiFilesInfoToWorkWith) {
            if (super.isCancelled()) {
                return result;
            }

            FileInfo fileInfo = RegularContentController.findMatchingFileInfo(guiFileInfo, allFilesInfo);
            if (CollectionUtils.isEmpty(fileInfo.getSubtitleStreams())) {
                result.setNotEnoughStreamsCount(result.getNotEnoughStreamsCount() + 1);
                result.setProcessedCount(result.getProcessedCount() + 1);
                continue;
            }

            List<SubtitleStream> matchingUpperSubtitles = getMatchingUpperSubtitles(fileInfo, guiSettings);
            List<SubtitleStream> matchingLowerSubtitles = getMatchingLowerSubtitles(fileInfo, guiSettings);
            if (CollectionUtils.isEmpty(matchingUpperSubtitles) || CollectionUtils.isEmpty(matchingLowerSubtitles)) {
                result.setNotEnoughStreamsCount(result.getNotEnoughStreamsCount() + 1);
                result.setProcessedCount(result.getProcessedCount() + 1);
                continue;
            }

            try {
                boolean loadedSuccessfully = loadSizesIfNecessary(
                        fileInfo.getFile(),
                        matchingUpperSubtitles,
                        matchingLowerSubtitles,
                        guiFileInfo.getSubtitleStreams(),
                        result
                );
                if (!loadedSuccessfully) {
                    result.setFailedCount(result.getFailedCount() + 1);
                    result.setProcessedCount(result.getProcessedCount() + 1);
                    continue;
                }

                if (matchingUpperSubtitles.size() > 1) {
                    matchingUpperSubtitles.sort(Comparator.comparing(SubtitleStream::getSubtitleSize).reversed());
                }
                if (matchingLowerSubtitles.size() > 1) {
                    matchingLowerSubtitles.sort(Comparator.comparing(SubtitleStream::getSubtitleSize).reversed());
                }

                RegularContentController.findMatchingGuiStream(
                        matchingUpperSubtitles.get(0).getFfmpegIndex(),
                        guiFileInfo.getSubtitleStreams()
                ).setSelectedAsUpper(true);

                RegularContentController.findMatchingGuiStream(
                        matchingLowerSubtitles.get(0).getFfmpegIndex(),
                        guiFileInfo.getSubtitleStreams()
                ).setSelectedAsLower(true);

                guiFileInfo.setHaveSubtitleSizesToLoad(RegularContentController.haveSubtitlesToLoad(fileInfo));

                result.setFinishedSuccessfullyCount(result.getFinishedSuccessfullyCount() + 1);
                result.setProcessedCount(result.getProcessedCount() + 1);
            } catch (FfmpegException e) {
                if (e.getCode() == FfmpegException.Code.INTERRUPTED) {
                    return result;
                } else {
                    throw new IllegalStateException();
                }
            }
        }

        return result;
    }

    private static List<GuiFileInfo> getGuiFilesInfoToWorkWith(List<GuiFileInfo> displayedGuiFilesInfo) {
        return displayedGuiFilesInfo.stream().filter(GuiFileInfo::isSelected).collect(Collectors.toList());
    }

    private static List<SubtitleStream> getMatchingUpperSubtitles(FileInfo fileInfo, GuiSettings guiSettings) {
        return fileInfo.getSubtitleStreams().stream()
                .filter(stream -> stream.getUnavailabilityReason() == null)
                .filter(stream -> stream.getLanguage() == guiSettings.getUpperLanguage())
                .collect(Collectors.toList());
    }

    private static List<SubtitleStream> getMatchingLowerSubtitles(FileInfo fileInfo, GuiSettings guiSettings) {
        return fileInfo.getSubtitleStreams().stream()
                .filter(stream -> stream.getUnavailabilityReason() == null)
                .filter(stream -> stream.getLanguage() == guiSettings.getLowerLanguage())
                .collect(Collectors.toList());
    }

    private boolean loadSizesIfNecessary(
            File file,
            List<SubtitleStream> upperSubtitleStreams,
            List<SubtitleStream> lowerSubtitleStreams,
            List<GuiSubtitleStream> guiSubtitleStreams,
            Result taskResult
    ) throws FfmpegException {
        boolean result = true;

        List<SubtitleStream> subtitlesToLoad = new ArrayList<>();
        if (upperSubtitleStreams.size() > 1) {
            subtitlesToLoad.addAll(upperSubtitleStreams);
        }
        if (lowerSubtitleStreams.size() > 1) {
            subtitlesToLoad.addAll(lowerSubtitleStreams);
        }

        for (SubtitleStream subtitleStream : subtitlesToLoad) {
            updateMessage(
                    getUpdateMessage(
                            taskResult.getProcessedCount(),
                            taskResult.getAllFileCount(),
                            subtitleStream,
                            file
                    )
            );

            if (subtitleStream.getSubtitles() != null) {
                continue;
            }

            GuiSubtitleStream guiSubtitleStream = RegularContentController.findMatchingGuiStream(
                    subtitleStream.getFfmpegIndex(),
                    guiSubtitleStreams
            );

            try {
                String subtitleText = ffmpeg.getSubtitlesText(subtitleStream.getFfmpegIndex(), file);
                subtitleStream.setSubtitlesAndSize(
                        Parser.fromSubRipText(
                                subtitleText,
                                subtitleStream.getTitle(),
                                subtitleStream.getLanguage()
                        )
                );

                /*
                 * Have to call this in the JavaFX thread because this change can lead to updates on the screen.
                 */
                Platform.runLater(() -> guiSubtitleStream.setSize(subtitleStream.getSubtitleSize()));
            } catch (FfmpegException e) {
                if (e.getCode() == FfmpegException.Code.INTERRUPTED) {
                    throw e;
                }

                result = false;
                Platform.runLater(() -> {
                    guiSubtitleStream.setFailedToLoadReason(LoadSubtitlesTask.guiTextFrom(e));
                });
            } catch (Parser.IncorrectFormatException e) {
                result = false;
                Platform.runLater(() -> {
                    guiSubtitleStream.setFailedToLoadReason("subtitles seem to have incorrect format");
                });
            }
        }

        return result;
    }

    private static String getUpdateMessage(
            int processedCount,
            int allSubtitleCount,
            SubtitleStream subtitleStream,
            File file
    ) {
        String language = subtitleStream.getLanguage() != null
                ? subtitleStream.getLanguage().toString().toUpperCase()
                : "UNKNOWN LANGUAGE";

        String progressPrefix = allSubtitleCount > 1
                ? (processedCount + 1) + "/" + allSubtitleCount + " "
                : "";

        return progressPrefix + "getting subtitle "
                + language
                + (StringUtils.isBlank(subtitleStream.getTitle()) ? "" : " " + subtitleStream.getTitle())
                + " in " + file.getName();
    }

    @Override
    protected void onFinish(Result result) {
        onFinish.accept(result);
    }

    @AllArgsConstructor
    @Getter
    @Setter
    public static class Result {
        private int allFileCount;

        private int notEnoughStreamsCount;

        private int processedCount;

        private int finishedSuccessfullyCount;

        private int failedCount;
    }
}
