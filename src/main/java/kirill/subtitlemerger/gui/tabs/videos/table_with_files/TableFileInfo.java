package kirill.subtitlemerger.gui.tabs.videos.table_with_files;

import javafx.beans.property.*;
import kirill.subtitlemerger.gui.utils.entities.ActionResult;
import lombok.Getter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.LocalDateTime;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@CommonsLog
public class TableFileInfo {
    @Getter
    private String id;

    private BooleanProperty selected;

    @Getter
    private String filePath;

    @Getter
    private long size;

    @Getter
    private LocalDateTime lastModified;

    @Getter
    private UnavailabilityReason unavailabilityReason;

    @Getter
    private List<TableSubtitleOption> subtitleOptions;

    @Getter
    private int hideableOptionCount;

    private BooleanProperty someOptionsHidden;

    private IntegerProperty optionsWithUnknownSizeCount;

    private IntegerProperty visibleOptionCount;

    private ObjectProperty<TableSubtitleOption> upperOption;

    private ObjectProperty<TableSubtitleOption> lowerOption;

    private ObjectProperty<ActionResult> actionResult;

    public TableFileInfo(
            String id,
            boolean selected,
            String filePath,
            long size,
            LocalDateTime lastModified,
            UnavailabilityReason unavailabilityReason,
            List<TableSubtitleOption> subtitleOptions,
            boolean someOptionsHidden,
            ActionResult actionResult
    ) {
        validateSubtitleOptions(subtitleOptions);

        this.id = id;
        this.selected = new SimpleBooleanProperty(selected);
        this.filePath = filePath;
        this.size = size;
        this.lastModified = lastModified;
        this.unavailabilityReason = unavailabilityReason;
        this.subtitleOptions = subtitleOptions;
        hideableOptionCount = calculateHideableOptionCount(subtitleOptions);
        this.someOptionsHidden = new SimpleBooleanProperty(someOptionsHidden);
        optionsWithUnknownSizeCount = new SimpleIntegerProperty(
                calculateOptionsWithUnknownSizeCount(subtitleOptions)
        );
        visibleOptionCount = new SimpleIntegerProperty(calculateVisibleOptionCount(subtitleOptions));
        upperOption = new SimpleObjectProperty<>(getUpperOption(subtitleOptions).orElse(null));
        lowerOption = new SimpleObjectProperty<>(getLowerOption(subtitleOptions).orElse(null));
        this.actionResult = new SimpleObjectProperty<>(actionResult);

        addSubtitleOptionListeners();
    }

    private static void validateSubtitleOptions(List<TableSubtitleOption> subtitleOptions) {
        if (subtitleOptions.size() < 2) {
            log.error("number of options has to be two or more");
            throw new IllegalStateException();
        }

        for (int i = 0; i < subtitleOptions.size() - 2; i++) {
            if (subtitleOptions.get(i).isRemovable()) {
                log.error("only last two options can be removable");
                throw new IllegalStateException();
            }
        }

        for (int i = subtitleOptions.size() - 2; i < subtitleOptions.size(); i++) {
            if (!subtitleOptions.get(i).isRemovable()) {
                log.error("two last options has to be removable");
                throw new IllegalStateException();
            }
        }
    }

    private static int calculateHideableOptionCount(List<TableSubtitleOption> subtitleOptions) {
        return (int) subtitleOptions.stream().filter(TableSubtitleOption::isHideable).count();
    }

    private static int calculateOptionsWithUnknownSizeCount(List<TableSubtitleOption> subtitleOptions) {
        return (int) subtitleOptions.stream()
                .filter(option -> !option.isRemovable())
                .filter(option -> option.getSize() == TableSubtitleOption.UNKNOWN_SIZE)
                .count();
    }

    private static int calculateVisibleOptionCount(List<TableSubtitleOption> subtitleOptions) {
        return (int) subtitleOptions.stream()
                .filter(option -> !option.isRemovable() || !StringUtils.isBlank(option.getTitle()))
                .count();
    }

    private static Optional<TableSubtitleOption> getUpperOption(List<TableSubtitleOption> subtitleOptions) {
        return subtitleOptions.stream().filter(TableSubtitleOption::isSelectedAsUpper).findFirst();
    }

    private static Optional<TableSubtitleOption> getLowerOption(List<TableSubtitleOption> subtitleOptions) {
        return subtitleOptions.stream().filter(TableSubtitleOption::isSelectedAsLower).findFirst();
    }

    private void addSubtitleOptionListeners() {
        for (TableSubtitleOption subtitleOption : subtitleOptions) {
            subtitleOption.selectedAsUpperProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue) {
                    for (TableSubtitleOption currentOption : subtitleOptions) {
                        if (Objects.equals(currentOption.getId(), subtitleOption.getId())) {
                            setUpperOption(currentOption);
                            currentOption.setSelectedAsLower(false);
                        } else {
                            currentOption.setSelectedAsUpper(false);
                        }
                    }
                } else {
                    TableSubtitleOption upperOption = getUpperOption();
                    if (upperOption != null && Objects.equals(upperOption.getId(), subtitleOption.getId())) {
                        setUpperOption(null);
                    }
                }
            });

            subtitleOption.selectedAsLowerProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue) {
                    for (TableSubtitleOption currentOption : subtitleOptions) {
                        if (Objects.equals(currentOption.getId(), subtitleOption.getId())) {
                            setLowerOption(currentOption);
                            currentOption.setSelectedAsUpper(false);
                        } else {
                            currentOption.setSelectedAsLower(false);
                        }
                    }
                } else {
                    TableSubtitleOption lowerOption = getLowerOption();
                    if (lowerOption != null && Objects.equals(lowerOption.getId(), subtitleOption.getId())) {
                        setLowerOption(null);
                    }
                }
            });
        }
    }

    public boolean isSelected() {
        return selected.get();
    }

    BooleanProperty selectedProperty() {
        return selected;
    }

    void setSelected(boolean selected) {
        this.selected.set(selected);
    }

    public boolean isSomeOptionsHidden() {
        return someOptionsHidden.get();
    }

    BooleanProperty someOptionsHiddenProperty() {
        return someOptionsHidden;
    }

    void setSomeOptionsHidden(boolean someOptionsHidden) {
        this.someOptionsHidden.set(someOptionsHidden);
    }

    public int getOptionsWithUnknownSizeCount() {
        return optionsWithUnknownSizeCount.get();
    }

    IntegerProperty optionsWithUnknownSizeCountProperty() {
        return optionsWithUnknownSizeCount;
    }

    void setOptionsWithUnknownSizeCount(int optionsWithUnknownSizeCount) {
        this.optionsWithUnknownSizeCount.set(optionsWithUnknownSizeCount);
    }

    public int getVisibleOptionCount() {
        return visibleOptionCount.get();
    }

    IntegerProperty visibleOptionCountProperty() {
        return visibleOptionCount;
    }

    void setVisibleOptionCount(int visibleOptionCount) {
        this.visibleOptionCount.set(visibleOptionCount);
    }

    public TableSubtitleOption getUpperOption() {
        return upperOption.get();
    }

    ObjectProperty<TableSubtitleOption> upperOptionProperty() {
        return upperOption;
    }

    void setUpperOption(TableSubtitleOption upperOption) {
        this.upperOption.set(upperOption);
    }

    public TableSubtitleOption getLowerOption() {
        return lowerOption.get();
    }

    ObjectProperty<TableSubtitleOption> lowerOptionProperty() {
        return lowerOption;
    }

    void setLowerOption(TableSubtitleOption lowerOption) {
        this.lowerOption.set(lowerOption);
    }

    public ActionResult getActionResult() {
        return actionResult.get();
    }

    ObjectProperty<ActionResult> actionResultProperty() {
        return actionResult;
    }

    void setActionResult(ActionResult actionResult) {
        this.actionResult.set(actionResult);
    }

    public static TableFileInfo getById(String id, Collection<TableFileInfo> filesInfo) {
        return filesInfo.stream()
                .filter(fileInfo -> Objects.equals(fileInfo.getId(), id))
                .findFirst().orElseThrow(IllegalStateException::new);
    }

    public void addSubtitleOption(TableSubtitleOption subtitleOption) {
        subtitleOptions.add(subtitleOption);

        hideableOptionCount = calculateHideableOptionCount(subtitleOptions);
        setOptionsWithUnknownSizeCount(calculateOptionsWithUnknownSizeCount(subtitleOptions));
        setVisibleOptionCount(calculateVisibleOptionCount(subtitleOptions));
    }

    public void updateSizeAndLastModified(long size, LocalDateTime lastModified) {
        this.size = size;
        this.lastModified = lastModified;
    }

    public void updateFileInfo(long size, LocalDateTime lastModified) {
        this.size = size;
        this.lastModified = lastModified;
    }

    public enum UnavailabilityReason {
        NO_EXTENSION,
        NOT_ALLOWED_EXTENSION,
        FAILED_TO_GET_MIME_TYPE,
        NOT_ALLOWED_MIME_TYPE,
        FAILED_TO_GET_FFPROBE_INFO,
        NOT_ALLOWED_CONTAINER
    }
}
