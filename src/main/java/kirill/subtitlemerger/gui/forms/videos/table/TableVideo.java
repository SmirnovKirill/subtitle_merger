package kirill.subtitlemerger.gui.forms.videos.table;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import kirill.subtitlemerger.logic.utils.entities.ActionResult;
import lombok.Getter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.joda.time.LocalDateTime;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static kirill.subtitlemerger.gui.forms.videos.table.TableSubtitleOption.UNKNOWN_SIZE;

@CommonsLog
public class TableVideo {
    @Getter
    private String id;

    private TableWithVideos table;

    private BooleanProperty selected;

    @Getter
    private String filePath;

    private ReadOnlyLongWrapper size;

    private ReadOnlyObjectWrapper<LocalDateTime> lastModified;

    @Getter
    private String notValidReason;

    @Getter
    private String format;

    private ObservableList<TableSubtitleOption> subtitleOptions;

    private ObservableList<TableSubtitleOption> unmodifiableSubtitleOptions;

    private ReadOnlyIntegerWrapper externalOptionCount;

    private ReadOnlyIntegerWrapper hideableOptionCount;

    private BooleanProperty someOptionsHidden;

    private ReadOnlyIntegerWrapper notLoadedOptionCount;

    private ReadOnlyObjectWrapper<TableSubtitleOption> upperOption;

    private ReadOnlyObjectWrapper<TableSubtitleOption> lowerOption;

    private ObjectProperty<ActionResult> actionResult;

    public TableVideo(
            String id,
            TableWithVideos table,
            boolean selected,
            String filePath,
            long size,
            LocalDateTime lastModified,
            String notValidReason,
            String format,
            ActionResult actionResult
    ) {
        this.id = id;
        this.table = table;
        this.selected = new SimpleBooleanProperty(selected);
        this.filePath = filePath;
        this.size = new ReadOnlyLongWrapper(size);
        this.lastModified = new ReadOnlyObjectWrapper<>(lastModified);
        this.notValidReason = notValidReason;
        this.format = format;
        subtitleOptions = FXCollections.observableArrayList();
        unmodifiableSubtitleOptions = FXCollections.unmodifiableObservableList(subtitleOptions);
        externalOptionCount = new ReadOnlyIntegerWrapper();
        hideableOptionCount = new ReadOnlyIntegerWrapper();
        someOptionsHidden = new SimpleBooleanProperty();
        notLoadedOptionCount = new ReadOnlyIntegerWrapper();
        upperOption = new ReadOnlyObjectWrapper<>();
        lowerOption = new ReadOnlyObjectWrapper<>();
        this.actionResult = new SimpleObjectProperty<>(actionResult);

        /*
         * All the selection's extra logic is implemented with a listener because there is no other way to ensure that
         * this code is invoked since the properties are exposed and can be modified from the outside.
         */
        this.selected.addListener((observable, oldValue, newValue) -> handleSelected(newValue, oldValue));
    }

    private void handleSelected(boolean selected, boolean previousValue) {
        if (selected == previousValue) {
            return;
        }

        table.handleVideoSelected(selected, StringUtils.isBlank(notValidReason));
    }

    public void setSubtitleOptions(List<TableSubtitleOption> subtitleOptions) {
        this.subtitleOptions.setAll(subtitleOptions);
        externalOptionCount.set(getExternalOptionCount(subtitleOptions));
        if (getExternalOptionCount() > 2) {
            log.error("there can't be more than 2 external subtitle options for a video, most likely a bug");
            throw new IllegalStateException();
        }
        hideableOptionCount.set(getHideableOptionCount(subtitleOptions));
        /* Hide hideable options by default. */
        setSomeOptionsHidden(subtitleOptions.stream().anyMatch(TableSubtitleOption::isHideable));
        notLoadedOptionCount.set(getNotLoadedOptionCount(subtitleOptions));
        upperOption.set(getUpperOption(subtitleOptions));
        lowerOption.set(getLowerOption(subtitleOptions));
    }

    private static int getExternalOptionCount(List<TableSubtitleOption> subtitleOptions) {
        return (int) subtitleOptions.stream()
                .filter(option -> option.getType() == TableSubtitleOptionType.EXTERNAL)
                .count();
    }

    private static int getHideableOptionCount(List<TableSubtitleOption> subtitleOptions) {
        return (int) subtitleOptions.stream().filter(TableSubtitleOption::isHideable).count();
    }

    private static int getNotLoadedOptionCount(List<TableSubtitleOption> subtitleOptions) {
        return (int) subtitleOptions.stream()
                .filter(option -> option.getType() == TableSubtitleOptionType.BUILT_IN)
                .filter(option -> option.getSize() == TableSubtitleOption.UNKNOWN_SIZE)
                .count();
    }

    @Nullable
    private static TableSubtitleOption getUpperOption(List<TableSubtitleOption> subtitleOptions) {
        return subtitleOptions.stream().filter(TableSubtitleOption::isSelectedAsUpper).findFirst().orElse(null);
    }

    @Nullable
    private static TableSubtitleOption getLowerOption(List<TableSubtitleOption> subtitleOptions) {
        return subtitleOptions.stream().filter(TableSubtitleOption::isSelectedAsLower).findFirst().orElse(null);
    }

    public static TableVideo getById(String id, Collection<TableVideo> videos) {
        TableVideo result = videos.stream().filter(video -> Objects.equals(video.getId(), id)).findFirst().orElse(null);

        if (result == null) {
            log.error("no table video for id " + id + ", most likely a bug");
            throw new IllegalStateException();
        }

        return result;
    }

    public void addOption(TableSubtitleOption option, ActionResult actionResult) {
        subtitleOptions.add(option);

        if (option.getType() == TableSubtitleOptionType.EXTERNAL) {
            if (getExternalOptionCount() >= 2) {
                log.error("there are already 2 external subtitle options for a video, most likely a bug");
                throw new IllegalStateException();
            }
            externalOptionCount.set(getExternalOptionCount() + 1);
        } else if (option.getType() == TableSubtitleOptionType.BUILT_IN){
            if (option.isHideable()) {
                hideableOptionCount.set(getHideableOptionCount() + 1);
                if (getHideableOptionCount() == 1) {
                    setSomeOptionsHidden(true);
                }
            }

            if (option.getSize() == UNKNOWN_SIZE) {
                notLoadedOptionCount.set(getNotLoadedOptionCount() + 1);
            }
        } else {
            log.error("unexpected subtitle option type " + option.getType() + ", most likely a bug");
            throw new IllegalStateException();
        }

        if (option.isSelectedAsUpper()) {
            upperOption.set(option);
        }
        if (option.isSelectedAsLower()) {
            lowerOption.set(option);
        }

        setActionResult(actionResult);
    }

    public void removeOption(TableSubtitleOption option, ActionResult actionResult) {
        if (option.getType() != TableSubtitleOptionType.EXTERNAL) {
            log.error("can't remove a not external option, most likely a bug");
            throw new IllegalStateException();
        }

        boolean removed = subtitleOptions.removeIf(
                currentOption -> Objects.equals(currentOption.getId(), option.getId())
        );
        if (!removed) {
            log.error("option with id " + option.getId() + " has not been removed, most likely a bug");
            throw new IllegalStateException();
        }

        externalOptionCount.set(getExternalOptionCount() - 1);

        if (option.isSelectedAsUpper()) {
            upperOption.set(null);
        }
        if (option.isSelectedAsLower()) {
            lowerOption.set(null);
        }

        setActionResult(actionResult);
    }

    void optionLoadedSuccessfully() {
        notLoadedOptionCount.set(getNotLoadedOptionCount() - 1);
    }

    void setUpperOption(TableSubtitleOption option) {
        upperOption.set(option);
    }

    void setLowerOption(TableSubtitleOption option) {
        lowerOption.set(option);
    }

    public void setSizeAndLastModified(long size, LocalDateTime lastModified) {
        this.size.set(size);
        this.lastModified.set(lastModified);
    }

    public void clearActionResult() {
        setActionResult(ActionResult.NO_RESULT);
    }

    @SuppressWarnings("unused")
    public void setOnlySuccess(String text) {
        setActionResult(ActionResult.onlySuccess(text));
    }

    public void setOnlyWarn(String text) {
        setActionResult(ActionResult.onlyWarn(text));
    }

    public void setOnlyError(String text) {
        setActionResult(ActionResult.onlyError(text));
    }

    public ObservableList<TableSubtitleOption> getSubtitleOptions() {
        return unmodifiableSubtitleOptions;
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public boolean isSelected() {
        return selected.get();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public BooleanProperty selectedProperty() {
        return selected;
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public void setSelected(boolean selected) {
        this.selected.set(selected);
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public long getSize() {
        return size.get();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public ReadOnlyLongProperty sizeProperty() {
        return size.getReadOnlyProperty();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public LocalDateTime getLastModified() {
        return lastModified.get();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public ReadOnlyObjectProperty<LocalDateTime> lastModifiedProperty() {
        return lastModified.getReadOnlyProperty();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public int getExternalOptionCount() {
        return externalOptionCount.get();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public ReadOnlyIntegerProperty externalOptionCountProperty() {
        return externalOptionCount.getReadOnlyProperty();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public int getHideableOptionCount() {
        return hideableOptionCount.get();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public ReadOnlyIntegerProperty hideableOptionCountProperty() {
        return hideableOptionCount.getReadOnlyProperty();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public boolean isSomeOptionsHidden() {
        return someOptionsHidden.get();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public BooleanProperty someOptionsHiddenProperty() {
        return someOptionsHidden;
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public void setSomeOptionsHidden(boolean someOptionsHidden) {
        this.someOptionsHidden.set(someOptionsHidden);
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public int getNotLoadedOptionCount() {
        return notLoadedOptionCount.get();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public ReadOnlyIntegerProperty notLoadedOptionCountProperty() {
        return notLoadedOptionCount.getReadOnlyProperty();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public TableSubtitleOption getUpperOption() {
        return upperOption.get();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public ReadOnlyObjectProperty<TableSubtitleOption> upperOptionProperty() {
        return upperOption.getReadOnlyProperty();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public TableSubtitleOption getLowerOption() {
        return lowerOption.get();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public ReadOnlyObjectProperty<TableSubtitleOption> lowerOptionProperty() {
        return lowerOption.getReadOnlyProperty();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public ActionResult getActionResult() {
        return actionResult.get();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public ObjectProperty<ActionResult> actionResultProperty() {
        return actionResult;
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public void setActionResult(ActionResult actionResult) {
        this.actionResult.set(actionResult);
    }
}
