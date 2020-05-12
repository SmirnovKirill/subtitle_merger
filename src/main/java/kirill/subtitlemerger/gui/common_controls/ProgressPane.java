package kirill.subtitlemerger.gui.common_controls;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.*;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import kirill.subtitlemerger.gui.utils.GuiUtils;
import org.apache.commons.lang3.StringUtils;

public class ProgressPane extends VBox {
    @SuppressWarnings("unused")
    @FXML
    private ProgressIndicator progressIndicator;

    @SuppressWarnings("unused")
    @FXML
    private Label progressLabel;

    @SuppressWarnings("unused")
    @FXML
    private Pane cancelPane;

    @SuppressWarnings("unused")
    @FXML
    private Label cancelDescriptionLabel;

    @SuppressWarnings("unused")
    @FXML
    private Hyperlink cancelLink;

    private BooleanProperty cancelPossible;

    private StringProperty cancelDescription;

    public ProgressPane() {
        GuiUtils.initializeControl(this, "/gui/javafx/common_controls/progress_pane.fxml");

        cancelPossible = new SimpleBooleanProperty();
        cancelDescription = new SimpleStringProperty();

        GuiUtils.bindVisibleAndManaged(cancelPane, cancelPossible);

        /*
         * Besides the cancellation description text that can be set through the property there is always a text after
         * this description offering to click the link to cancel the task. So if the description is set there should be
         * a space after that description so that the texts won't be too close.
         */
        StringBinding cancelDescriptionBinding = Bindings.createStringBinding(
                () -> {
                    String description = cancelDescription.get();
                    return StringUtils.isBlank(description) ? "" : description + " ";
                },
                cancelDescription
        );
        cancelDescriptionLabel.textProperty().bind(cancelDescriptionBinding);
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public double getProgress() {
        return progressIndicator.getProgress();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public DoubleProperty progressProperty() {
        return progressIndicator.progressProperty();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public void setProgress(double progress) {
        progressIndicator.setProgress(progress);
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public String getMessage() {
        return progressLabel.getText();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public StringProperty messageProperty() {
        return progressLabel.textProperty();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public void setMessage(String message) {
        progressLabel.setText(message);
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public boolean getCancelPossible() {
        return cancelPossible.get();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public BooleanProperty cancelPossibleProperty() {
        return cancelPossible;
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public void setCancelPossible(boolean cancelPossible) {
        this.cancelPossible.set(cancelPossible);
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public String getCancelDescription() {
        return cancelDescription.get();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public StringProperty cancelDescriptionProperty() {
        return cancelDescription;
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public void setCancelDescription(String cancelDescription) {
        this.cancelDescription.set(cancelDescription);
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public EventHandler<ActionEvent> getOnCancelAction() {
        return cancelLink.getOnAction();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public ObjectProperty<EventHandler<ActionEvent>> onCancelActionProperty() {
        return cancelLink.onActionProperty();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public void setOnCancelAction(EventHandler<ActionEvent> onCancelAction) {
        cancelLink.setOnAction(onCancelAction);
    }
}
