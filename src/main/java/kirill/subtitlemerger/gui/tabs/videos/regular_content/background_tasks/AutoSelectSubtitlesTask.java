package kirill.subtitlemerger.gui.tabs.videos.regular_content.background_tasks;

import javafx.application.Platform;
import kirill.subtitlemerger.gui.GuiSettings;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.RegularContentController;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files.GuiFileInfo;
import kirill.subtitlemerger.gui.tabs.videos.regular_content.table_with_files.GuiSubtitleStreamInfo;
import kirill.subtitlemerger.logic.core.Parser;
import kirill.subtitlemerger.logic.work_with_files.entities.FileInfo;
import kirill.subtitlemerger.logic.work_with_files.entities.SubtitleStreamInfo;
import kirill.subtitlemerger.logic.work_with_files.ffmpeg.Ffmpeg;
import kirill.subtitlemerger.logic.work_with_files.ffmpeg.FfmpegException;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class AutoSelectSubtitlesTask extends BackgroundTask<Void> {
    private List<FileInfo> allFilesInfo;

    private List<GuiFileInfo> displayedGuiFilesInfo;

    private Ffmpeg ffmpeg;

    private GuiSettings guiSettings;

    @Getter
    private boolean cancelled;

    @Getter
    private int allFileCount;

    @Getter
    private int processedCount;

    @Getter
    private int finishedSuccessfullyCount;

    @Getter
    private int failedCount;

    public AutoSelectSubtitlesTask(
            List<FileInfo> allFilesInfo,
            List<GuiFileInfo> displayedGuiFilesInfo,
            Ffmpeg ffmpeg,
            GuiSettings guiSettings
    ) {
        super();

        this.allFilesInfo = allFilesInfo;
        this.displayedGuiFilesInfo = displayedGuiFilesInfo;
        this.ffmpeg = ffmpeg;
        this.guiSettings = guiSettings;
    }

    @Override
    protected Void call() {
        updateMessage("getting list of files to work with...");
        List<GuiFileInfo> guiFilesInfoToWorkWith = getGuiFilesInfoToWorkWith(displayedGuiFilesInfo);

        allFileCount = guiFilesInfoToWorkWith.size();

        for (GuiFileInfo guiFileInfo : guiFilesInfoToWorkWith) {
            if (super.isCancelled()) {
                cancelled = true;
                break;
            }

            FileInfo fileInfo = RegularContentController.findMatchingFileInfo(guiFileInfo, allFilesInfo);
            if (CollectionUtils.isEmpty(fileInfo.getSubtitleStreamsInfo())) {
                processedCount++;
                continue;
            }

            List<SubtitleStreamInfo> matchingUpperSubtitles = getMatchingUpperSubtitles(fileInfo, guiSettings);
            List<SubtitleStreamInfo> matchingLowerSubtitles = getMatchingLowerSubtitles(fileInfo, guiSettings);
            if (CollectionUtils.isEmpty(matchingUpperSubtitles) || CollectionUtils.isEmpty(matchingLowerSubtitles)) {
                processedCount++;
                continue;
            }

            try {
                if (matchingUpperSubtitles.size() != 1) {
                    loadSizesForGroupIfNecessary(
                            fileInfo.getFile(),
                            matchingUpperSubtitles,
                            guiFileInfo.getSubtitleStreamsInfo()
                    );
                    matchingUpperSubtitles.sort(Comparator.comparing(SubtitleStreamInfo::getSubtitleSize).reversed());
                }
                GuiSubtitleStreamInfo guiAutoSelectedUpperSubtitles = RegularContentController.findMatchingGuiStreamInfo(
                        matchingUpperSubtitles.get(0).getId(),
                        guiFileInfo.getSubtitleStreamsInfo()
                );
                guiAutoSelectedUpperSubtitles.setSelectedAsUpper(true);

                if (matchingLowerSubtitles.size() != 1) {
                    loadSizesForGroupIfNecessary(
                            fileInfo.getFile(),
                            matchingLowerSubtitles,
                            guiFileInfo.getSubtitleStreamsInfo()
                    );
                    matchingLowerSubtitles.sort(Comparator.comparing(SubtitleStreamInfo::getSubtitleSize).reversed());
                }
                GuiSubtitleStreamInfo guiAutoSelectedLowerSubtitles = RegularContentController.findMatchingGuiStreamInfo(
                        matchingLowerSubtitles.get(0).getId(),
                        guiFileInfo.getSubtitleStreamsInfo()
                );
                guiAutoSelectedLowerSubtitles.setSelectedAsLower(true);

                guiFileInfo.setHaveSubtitleSizesToLoad(RegularContentController.haveSubtitlesToLoad(fileInfo));

                finishedSuccessfullyCount++;
            } catch (FfmpegException e) {
                if (e.getCode() == FfmpegException.Code.INTERRUPTED) {
                    cancelled = true;
                    break;
                } else {
                    //todo save reason
                    failedCount++;
                }
            } catch (Parser.IncorrectFormatException e) {
                //todo save reason
                failedCount++;
            }

            processedCount++;
        }

        return null;
    }

    private static List<GuiFileInfo> getGuiFilesInfoToWorkWith(List<GuiFileInfo> displayedGuiFilesInfo) {
        return displayedGuiFilesInfo.stream().filter(GuiFileInfo::isSelected).collect(Collectors.toList());
    }

    private static List<SubtitleStreamInfo> getMatchingUpperSubtitles(FileInfo fileInfo, GuiSettings guiSettings) {
        return fileInfo.getSubtitleStreamsInfo().stream()
                .filter(stream -> stream.getUnavailabilityReason() == null)
                .filter(stream -> stream.getLanguage() == guiSettings.getUpperLanguage())
                .collect(Collectors.toList());
    }

    private static List<SubtitleStreamInfo> getMatchingLowerSubtitles(FileInfo fileInfo, GuiSettings guiSettings) {
        return fileInfo.getSubtitleStreamsInfo().stream()
                .filter(stream -> stream.getUnavailabilityReason() == null)
                .filter(stream -> stream.getLanguage() == guiSettings.getLowerLanguage())
                .collect(Collectors.toList());
    }

    private void loadSizesForGroupIfNecessary(
            File file,
            List<SubtitleStreamInfo> subtitleStreams,
            List<GuiSubtitleStreamInfo> guiSubtitleStreams
    ) throws Parser.IncorrectFormatException, FfmpegException {
        for (SubtitleStreamInfo subtitleStream : subtitleStreams) {
            updateMessage(
                    getUpdateMessage(
                            processedCount,
                            allFileCount,
                            subtitleStream,
                            file
                    )
            );

            if (subtitleStream.getSubtitles() != null) {
                continue;
            }

            String subtitleText = ffmpeg.getSubtitlesText(subtitleStream.getId(), file);
            subtitleStream.setSubtitlesAndSize(
                    Parser.fromSubRipText(
                            subtitleText,
                            subtitleStream.getTitle(),
                            subtitleStream.getLanguage()
                    )
            );

            GuiSubtitleStreamInfo guiSubtitleStreamInfo = RegularContentController.findMatchingGuiStreamInfo(
                    subtitleStream.getId(),
                    guiSubtitleStreams
            );

            /*
             * Have to call this in the JavaFX thread because this change can lead to updates on the screen.
             */
            Platform.runLater(() -> guiSubtitleStreamInfo.setSize(subtitleStream.getSubtitleSize()));
        }
    }

    private static String getUpdateMessage(
            int processedCount,
            int allSubtitleCount,
            SubtitleStreamInfo subtitleStream,
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
}
