package kirill.subtitlesmerger.gui.tabs.videos.regular_content;

import javafx.beans.Observable;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import kirill.subtitlesmerger.gui.GuiContext;
import kirill.subtitlesmerger.gui.GuiSettings;
import kirill.subtitlesmerger.gui.tabs.videos.regular_content.table_with_files.GuiFileInfo;
import kirill.subtitlesmerger.gui.tabs.videos.regular_content.table_with_files.TableWithFiles;
import kirill.subtitlesmerger.logic.LogicConstants;
import kirill.subtitlesmerger.logic.work_with_files.FileInfoGetter;
import kirill.subtitlesmerger.logic.work_with_files.entities.FileInfo;
import kirill.subtitlesmerger.logic.work_with_files.ffmpeg.Ffprobe;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.joda.time.LocalDateTime;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;

@CommonsLog
public class RegularContentController {
    private static final String SORT_BY_NAME_TEXT = "By _Name";

    private static final String SORT_BY_MODIFICATION_TIME_TEXT = "By _Modification Time";

    private static final String SORT_BY_SIZE_TEXT = "By _Size";

    private static final String SORT_ASCENDING_TEXT = "_Ascending";

    private static final String SORT_DESCENDING_TEXT = "_Descending";

    private Stage stage;

    private GuiContext guiContext;

    @FXML
    private Pane pane;

    @FXML
    private Pane choicePane;

    @FXML
    private Pane resultPane;

    @FXML
    private Pane chosenDirectoryPane;

    @FXML
    private TextField chosenDirectoryField;

    @FXML
    private CheckBox hideUnavailableCheckbox;

    @FXML
    private TableWithFiles tableWithFiles;

    private ToggleGroup sortByGroup;

    private ToggleGroup sortDirectionGroup;

    private Mode mode;

    private File directory;

    private List<GuiFileInfo> allGuiFilesInfo;

    public void initialize(Stage stage, GuiContext guiContext) {
        this.stage = stage;
        this.guiContext = guiContext;
        saveDefaultSortSettingsIfNotSet(guiContext.getSettings());
        this.sortByGroup = new ToggleGroup();
        this.sortByGroup.selectedToggleProperty().addListener(this::sortByChanged);
        this.sortDirectionGroup = new ToggleGroup();
        this.sortDirectionGroup.selectedToggleProperty().addListener(this::sortDirectionChanged);
        this.tableWithFiles.initialize();
        this.allGuiFilesInfo = new ArrayList<>();
        this.tableWithFiles.setContextMenu(
                generateContextMenu(
                        this.sortByGroup,
                        this.sortDirectionGroup,
                        guiContext.getSettings()
                )
        );
    }

    private static void saveDefaultSortSettingsIfNotSet(GuiSettings settings) {
        try {
            if (settings.getSortBy() == null) {
                settings.saveSortBy(GuiSettings.SortBy.MODIFICATION_TIME.toString());
            }

            if (settings.getSortDirection() == null) {
                settings.saveSortDirection(GuiSettings.SortDirection.ASCENDING.toString());
            }
        } catch (GuiSettings.ConfigException e) {
            log.error("failed to save sort parameters, should not happen: " + ExceptionUtils.getStackTrace(e));
        }
    }

    private void sortByChanged(Observable observable) {
        RadioMenuItem radioMenuItem = (RadioMenuItem) sortByGroup.getSelectedToggle();

        try {
            switch (radioMenuItem.getText()) {
                case SORT_BY_NAME_TEXT:
                    guiContext.getSettings().saveSortBy(GuiSettings.SortBy.NAME.toString());
                    break;
                case SORT_BY_MODIFICATION_TIME_TEXT:
                    guiContext.getSettings().saveSortBy(GuiSettings.SortBy.MODIFICATION_TIME.toString());
                    break;
                case SORT_BY_SIZE_TEXT:
                    guiContext.getSettings().saveSortBy(GuiSettings.SortBy.SIZE.toString());
                    break;
                default:
                    throw new IllegalStateException();
            }
        } catch (GuiSettings.ConfigException e) {
            log.error("failed to save sort by, should not happen: " + ExceptionUtils.getStackTrace(e));
        }

        tableWithFiles.getItems().setAll(
                getGuiFilesInfoToShow(
                        allGuiFilesInfo,
                        hideUnavailableCheckbox.isSelected(),
                        guiContext.getSettings().getSortBy(),
                        guiContext.getSettings().getSortDirection()
                )
        );
    }

    private void sortDirectionChanged(Observable observable) {
        RadioMenuItem radioMenuItem = (RadioMenuItem) sortDirectionGroup.getSelectedToggle();

        try {
            switch (radioMenuItem.getText()) {
                case SORT_ASCENDING_TEXT:
                    guiContext.getSettings().saveSortDirection(GuiSettings.SortDirection.ASCENDING.toString());
                    break;
                case SORT_DESCENDING_TEXT:
                    guiContext.getSettings().saveSortDirection(GuiSettings.SortDirection.DESCENDING.toString());
                    break;
                default:
                    throw new IllegalStateException();
            }
        } catch (GuiSettings.ConfigException e) {
            log.error("failed to save sort direction, should not happen: " + ExceptionUtils.getStackTrace(e));
        }

        tableWithFiles.getItems().setAll(
                getGuiFilesInfoToShow(
                        allGuiFilesInfo,
                        hideUnavailableCheckbox.isSelected(),
                        guiContext.getSettings().getSortBy(),
                        guiContext.getSettings().getSortDirection()
                )
        );
    }

    private static ContextMenu generateContextMenu(
            ToggleGroup sortByGroup,
            ToggleGroup sortDirectionGroup,
            GuiSettings settings
    ) {
        ContextMenu result = new ContextMenu();

        Menu menu = new Menu("_Sort files");

        GuiSettings.SortBy sortBy = settings.getSortBy();
        GuiSettings.SortDirection sortDirection = settings.getSortDirection();

        RadioMenuItem byName = new RadioMenuItem(SORT_BY_NAME_TEXT);
        byName.setToggleGroup(sortByGroup);
        if (sortBy == GuiSettings.SortBy.NAME) {
            byName.setSelected(true);
        }

        RadioMenuItem byModificationTime = new RadioMenuItem(SORT_BY_MODIFICATION_TIME_TEXT);
        byModificationTime.setToggleGroup(sortByGroup);
        if (sortBy == GuiSettings.SortBy.MODIFICATION_TIME) {
            byModificationTime.setSelected(true);
        }

        RadioMenuItem bySize = new RadioMenuItem(SORT_BY_SIZE_TEXT);
        bySize.setToggleGroup(sortByGroup);
        if (sortBy == GuiSettings.SortBy.SIZE) {
            bySize.setSelected(true);
        }

        RadioMenuItem ascending = new RadioMenuItem(SORT_ASCENDING_TEXT);
        ascending.setToggleGroup(sortDirectionGroup);
        if (sortDirection == GuiSettings.SortDirection.ASCENDING) {
            ascending.setSelected(true);
        }

        RadioMenuItem descending = new RadioMenuItem(SORT_DESCENDING_TEXT);
        descending.setToggleGroup(sortDirectionGroup);
        if (sortDirection == GuiSettings.SortDirection.DESCENDING) {
            descending.setSelected(true);
        }

        menu.getItems().addAll(
                byName,
                byModificationTime,
                bySize,
                new SeparatorMenuItem(),
                ascending,
                descending
        );

        result.getItems().add(menu);

        return result;
    }

    public void show() {
        pane.setVisible(true);
    }

    public void hide() {
        pane.setVisible(false);
    }

    @FXML
    private void videosButtonClicked() {
        List<File> files = getFiles(stage, guiContext.getSettings());
        if (CollectionUtils.isEmpty(files)) {
            return;
        }

        try {
            guiContext.getSettings().saveLastDirectoryWithVideos(files.get(0).getParent());
        } catch (GuiSettings.ConfigException e) {
            log.error("failed to save last directory with videos, that shouldn't happen: " + getStackTrace(e));
        }

        mode = Mode.FILES;
        directory = null;
        //todo in background + progress
        allGuiFilesInfo = getGuiFilesInfo(files, mode, guiContext.getFfprobe());
        hideUnavailableCheckbox.setSelected(hideUnavailable(allGuiFilesInfo));
        tableWithFiles.getItems().setAll(
                getGuiFilesInfoToShow(
                        allGuiFilesInfo,
                        hideUnavailableCheckbox.isSelected(),
                        guiContext.getSettings().getSortBy(),
                        guiContext.getSettings().getSortDirection()
                )
        );

        choicePane.setVisible(false);
        resultPane.setVisible(true);
        chosenDirectoryPane.setVisible(false);
        chosenDirectoryPane.setManaged(false);
    }

    private static List<File> getFiles(Stage stage, GuiSettings settings) {
        FileChooser fileChooser = new FileChooser();

        fileChooser.setTitle("choose videos");
        fileChooser.setInitialDirectory(settings.getLastDirectoryWithVideos());
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("mkv files (*.mkv)", "*.mkv")
        );

        return fileChooser.showOpenMultipleDialog(stage);
    }

    private static List<GuiFileInfo> getGuiFilesInfo(List<File> files, Mode mode, Ffprobe ffprobe) {
        List<FileInfo> result = new ArrayList<>();

        for (File file : files) {
            if (file.isDirectory() || !file.exists()) {
                continue;
            }

            result.add(
                    FileInfoGetter.getFileInfoWithoutSubtitles(
                            file,
                            LogicConstants.ALLOWED_VIDEO_EXTENSIONS,
                            LogicConstants.ALLOWED_VIDEO_MIME_TYPES,
                            ffprobe
                    )
            );
        }

        return result.stream().map(fileInfo -> guiFileInfoFrom(fileInfo, mode)).collect(Collectors.toList());
    }

    private static GuiFileInfo guiFileInfoFrom(FileInfo fileInfo, Mode mode) {
        String path;
        if (mode == Mode.FILES) {
            path = fileInfo.getFile().getAbsolutePath();
        } else if (mode == Mode.DIRECTORY) {
            path = fileInfo.getFile().getName();
        } else {
            throw new IllegalStateException();
        }

        return new GuiFileInfo(
                path,
                fileInfo.getLastModified(),
                LocalDateTime.now(),
                fileInfo.getSize(),
                convert(fileInfo.getUnavailabilityReason()),
                ""
        );
    }

    private static String convert(FileInfo.UnavailabilityReason unavailabilityReason) {
        if (unavailabilityReason == null) {
            return "";
        }

        return unavailabilityReason.toString(); //todo
    }

    /*
     * Set "hide unavailable" checkbox by default if there is at least one available video. Otherwise it should
     * not be checked because the user will see just an empty file list which isn't user friendly.
     */
    private static boolean hideUnavailable(List<GuiFileInfo> files) {
        return files.stream().anyMatch(fileInfo -> StringUtils.isBlank(fileInfo.getUnavailabilityReason()));
    }

    private static List<GuiFileInfo> getGuiFilesInfoToShow(
            List<GuiFileInfo> allFilesInfo,
            boolean hideUnavailable,
            GuiSettings.SortBy sortBy,
            GuiSettings.SortDirection sortDirection
    ) {
        List<GuiFileInfo> result = getSortedFilesInfo(allFilesInfo, sortBy, sortDirection);

        if (!hideUnavailable) {
            return result;
        }

        return result.stream()
                .filter(file -> StringUtils.isBlank(file.getUnavailabilityReason()))
                .collect(toList());
    }

    private static List<GuiFileInfo> getSortedFilesInfo(
            List<GuiFileInfo> filesInfo,
            GuiSettings.SortBy sortBy,
            GuiSettings.SortDirection sortDirection
    ) {
        List<GuiFileInfo> result = new ArrayList<>(filesInfo);

        Comparator<GuiFileInfo> comparator;
        switch (sortBy) {
            case NAME:
                comparator = Comparator.comparing(GuiFileInfo::getPath);
                break;
            case MODIFICATION_TIME:
                comparator = Comparator.comparing(GuiFileInfo::getLastModified);
                break;
            case SIZE:
                comparator = Comparator.comparing(GuiFileInfo::getSize);
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

    @FXML
    private void directoryButtonClicked() {
        File directory = getDirectory(stage, guiContext.getSettings()).orElse(null);
        if (directory == null) {
            return;
        }
        File[] files = directory.listFiles();
        if (files == null) {
            log.error("failed to get directory files, directory " + directory.getAbsolutePath());
            files = new File[]{};
        }

        try {
            guiContext.getSettings().saveLastDirectoryWithVideos(directory.getAbsolutePath());
        } catch (GuiSettings.ConfigException e) {
            log.error("failed to save last directory with videos, that shouldn't happen: " + getStackTrace(e));
        }

        mode = Mode.DIRECTORY;
        this.directory = directory;
        allGuiFilesInfo = getGuiFilesInfo(Arrays.asList(files), mode, guiContext.getFfprobe());
        //todo in background + progress
        chosenDirectoryField.setText(directory.getAbsolutePath());
        hideUnavailableCheckbox.setSelected(hideUnavailable(allGuiFilesInfo));
        tableWithFiles.getItems().setAll(
                getGuiFilesInfoToShow(
                        allGuiFilesInfo,
                        hideUnavailableCheckbox.isSelected(),
                        guiContext.getSettings().getSortBy(),
                        guiContext.getSettings().getSortDirection()
                )
        );
        choicePane.setVisible(false);
        resultPane.setVisible(true);
        chosenDirectoryPane.setVisible(true);
        chosenDirectoryPane.setManaged(true);
    }

    private static Optional<File> getDirectory(Stage stage, GuiSettings settings) {
        DirectoryChooser directoryChooser = new DirectoryChooser();

        directoryChooser.setTitle("choose directory with videos");
        directoryChooser.setInitialDirectory(settings.getLastDirectoryWithVideos());

        return Optional.ofNullable(directoryChooser.showDialog(stage));
    }

    @FXML
    private void backToSelectionClicked() {
        choicePane.setVisible(true);
        resultPane.setVisible(false);
    }

    @FXML
    private void refreshButtonClicked() {
        File[] files = directory.listFiles();
        if (files == null) {
            log.error("failed to get directory files, directory " + directory.getAbsolutePath());
            files = new File[]{};
        }

        allGuiFilesInfo = getGuiFilesInfo(Arrays.asList(files), mode, guiContext.getFfprobe());
        //todo in background + progress
        hideUnavailableCheckbox.setSelected(hideUnavailable(allGuiFilesInfo));
        tableWithFiles.getItems().setAll(
                getGuiFilesInfoToShow(
                        allGuiFilesInfo,
                        hideUnavailableCheckbox.isSelected(),
                        guiContext.getSettings().getSortBy(),
                        guiContext.getSettings().getSortDirection()
                )
        );
    }

    @FXML
    private void hideUnavailableClicked() {
        tableWithFiles.getItems().setAll(
                getGuiFilesInfoToShow(
                        allGuiFilesInfo,
                        hideUnavailableCheckbox.isSelected(),
                        guiContext.getSettings().getSortBy(),
                        guiContext.getSettings().getSortDirection()
                )
        );
    }

    private enum Mode {
        FILES,
        DIRECTORY
    }
}
