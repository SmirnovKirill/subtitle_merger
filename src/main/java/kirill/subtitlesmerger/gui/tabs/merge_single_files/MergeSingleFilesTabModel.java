package kirill.subtitlesmerger.gui.tabs.merge_single_files;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class MergeSingleFilesTabModel {
    static final String FILE_NOT_CHOSEN_PATH = "not selected";

    private ObservableList<String> upperSubtitlesChooseButtonClass;

    private StringProperty upperSubtitlesPath;

    private ObservableList<String> lowerSubtitlesChooseButtonClass;

    private StringProperty lowerSubtitlesPath;

    private ObservableList<String> mergedSubtitlesChooseButtonClass;

    private StringProperty mergedSubtitlesPath;

    private BooleanProperty mergeButtonDisable;

    private StringProperty resultText;

    private ObservableList<String> resultClass;

    public MergeSingleFilesTabModel() {
        this.upperSubtitlesChooseButtonClass = FXCollections.observableArrayList();

        this.upperSubtitlesPath = new SimpleStringProperty(
                this,
                "upperSubtitlesPath",
                FILE_NOT_CHOSEN_PATH
        );

        this.lowerSubtitlesChooseButtonClass = FXCollections.observableArrayList();

        this.lowerSubtitlesPath = new SimpleStringProperty(
                this,
                "lowerSubtitlesPath",
                FILE_NOT_CHOSEN_PATH
        );

        this.mergedSubtitlesChooseButtonClass = FXCollections.observableArrayList();

        this.mergedSubtitlesPath = new SimpleStringProperty(
                this,
                "mergedSubtitlesPath",
                FILE_NOT_CHOSEN_PATH
        );

        this.mergeButtonDisable = new SimpleBooleanProperty(
                this,
                "mergeButtonDisable",
                false
        );

        this.resultText = new SimpleStringProperty(
                this,
                "resultText",
                ""
        );

        this.resultClass = FXCollections.observableArrayList();
    }

    public ObservableList<String> getUpperSubtitlesChooseButtonClass() {
        return upperSubtitlesChooseButtonClass;
    }

    public void setUpperSubtitlesChooseButtonClass(ObservableList<String> upperSubtitlesChooseButtonClass) {
        this.upperSubtitlesChooseButtonClass = upperSubtitlesChooseButtonClass;
    }

    public String getUpperSubtitlesPath() {
        return upperSubtitlesPath.get();
    }

    public StringProperty upperSubtitlesPathProperty() {
        return upperSubtitlesPath;
    }

    public void setUpperSubtitlesPath(String upperSubtitlesPath) {
        this.upperSubtitlesPath.set(upperSubtitlesPath);
    }

    public ObservableList<String> getLowerSubtitlesChooseButtonClass() {
        return lowerSubtitlesChooseButtonClass;
    }

    public void setLowerSubtitlesChooseButtonClass(ObservableList<String> lowerSubtitlesChooseButtonClass) {
        this.lowerSubtitlesChooseButtonClass = lowerSubtitlesChooseButtonClass;
    }

    public String getLowerSubtitlesPath() {
        return lowerSubtitlesPath.get();
    }

    public StringProperty lowerSubtitlesPathProperty() {
        return lowerSubtitlesPath;
    }

    public void setLowerSubtitlesPath(String lowerSubtitlesPath) {
        this.lowerSubtitlesPath.set(lowerSubtitlesPath);
    }

    public ObservableList<String> getMergedSubtitlesChooseButtonClass() {
        return mergedSubtitlesChooseButtonClass;
    }

    public void setMergedSubtitlesChooseButtonClass(ObservableList<String> mergedSubtitlesChooseButtonClass) {
        this.mergedSubtitlesChooseButtonClass = mergedSubtitlesChooseButtonClass;
    }

    public String getMergedSubtitlesPath() {
        return mergedSubtitlesPath.get();
    }

    public StringProperty mergedSubtitlesPathProperty() {
        return mergedSubtitlesPath;
    }

    public void setMergedSubtitlesPath(String mergedSubtitlesPath) {
        this.mergedSubtitlesPath.set(mergedSubtitlesPath);
    }

    public boolean isMergeButtonDisable() {
        return mergeButtonDisable.get();
    }

    public BooleanProperty mergeButtonDisableProperty() {
        return mergeButtonDisable;
    }

    public void setMergeButtonDisable(boolean mergeButtonDisable) {
        this.mergeButtonDisable.set(mergeButtonDisable);
    }

    public String getResultText() {
        return resultText.get();
    }

    public StringProperty resultTextProperty() {
        return resultText;
    }

    public void setResultText(String resultText) {
        this.resultText.set(resultText);
    }

    public ObservableList<String> getResultClass() {
        return resultClass;
    }

    public void setResultClass(ObservableList<String> resultClass) {
        this.resultClass = resultClass;
    }
}
