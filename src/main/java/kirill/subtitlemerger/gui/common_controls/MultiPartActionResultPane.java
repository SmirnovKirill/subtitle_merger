package kirill.subtitlemerger.gui.common_controls;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import kirill.subtitlemerger.gui.utils.GuiUtils;
import kirill.subtitlemerger.logic.utils.entities.MultiPartActionResult;
import org.apache.commons.lang3.StringUtils;

/**
 * A special pane to display data from the MultiPartActionResult class in three different colors - the part with success
 * in green, the part with warnings in orange and the error part in red. The pane is invisible and not managed (by
 * default) if there are no texts to display.
 * Note that you have to be careful and use only texts that can be displayed fully because JavaFX can't clip the Text
 * classes with ellipsis like the Label classes, and this pane can't use labels because there is no way to ensure the
 * priority of the labels (for example if there are three labels and there is not enough space all three will be
 * displayed but partly). And even if there was a way to ensure the priority, labels would have been treated like
 * objects that can't be cut in pieces so there could be line breaks between the labels.
 * So in general it's better to use the the ActionResultLabel whenever possible.
 *
 * @see MultiPartActionResult
 * @see ActionResultLabel
 */
public class MultiPartActionResultPane extends TextFlow {
    private Text successText;

    private Text warningText;

    private Text errorText;

    private BooleanProperty alwaysManaged;

    private ObjectProperty<MultiPartActionResult> actionResult;

    public MultiPartActionResultPane() {
        this(MultiPartActionResult.EMPTY);
    }

    @SuppressWarnings("WeakerAccess")
    public MultiPartActionResultPane(MultiPartActionResult actionResult) {
        successText = new Text();
        successText.setFill(Paint.valueOf("green"));
        getChildren().add(successText);

        warningText = new Text();
        warningText.setFill(Paint.valueOf("#ff8c00"));
        getChildren().add(warningText);

        errorText = new Text();
        errorText.setFill(Paint.valueOf("#e2574c"));
        getChildren().add(errorText);

        alwaysManaged = new SimpleBooleanProperty(false);
        this.actionResult = new SimpleObjectProperty<>(actionResult);

        this.actionResult.addListener(observable -> updateScene(getActionResult()));
        alwaysManaged.addListener(observable -> updateScene(getActionResult()));
        updateScene(actionResult);
    }

    private void updateScene(MultiPartActionResult actionResult) {
        String success = null;
        String warning = null;
        String error = null;
        boolean empty = true;

        if (!StringUtils.isBlank(actionResult.getSuccess())) {
            empty = false;

            success = actionResult.getSuccess();
            if (!StringUtils.isBlank(actionResult.getWarning()) || !StringUtils.isBlank(actionResult.getError())) {
                success += ", ";
            }
        }

        if (!StringUtils.isBlank(actionResult.getWarning())) {
            empty = false;

            warning = actionResult.getWarning();
            if (!StringUtils.isBlank(actionResult.getError())) {
                warning += ", ";
            }
        }

        if (!StringUtils.isBlank(actionResult.getError())) {
            empty = false;

            error = actionResult.getError();
        }

        successText.setText(success);
        GuiUtils.setVisibleAndManaged(successText, !StringUtils.isBlank(success));
        warningText.setText(warning);
        GuiUtils.setVisibleAndManaged(warningText, !StringUtils.isBlank(warning));
        errorText.setText(error);
        /*
         * A pretty tricking place. When texts are empty but visible it can lead to a strange behaviour - if there is
         * only an error part then the first letter is orange and the other letters are red. And it's not enough to make
         * empty texts invisible, they have to become not managed as well. But at the same time if all texts become not
         * managed then the alwaysManaged flag won't work so we have to keep at least one text managed no matter what.
         */
        errorText.setVisible(!StringUtils.isBlank(error));

        setVisible(!empty);
        setManaged(isAlwaysManaged() || !empty);
    }

    public void clear() {
        setActionResult(MultiPartActionResult.EMPTY);
    }

    public void setOnlySuccess(String text) {
        setActionResult(MultiPartActionResult.onlySuccess(text));
    }

    public void setOnlyWarning(String text) {
        setActionResult(MultiPartActionResult.onlyWarning(text));
    }

    public void setOnlyError(String text) {
        setActionResult(MultiPartActionResult.onlyError(text));
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public boolean isAlwaysManaged() {
        return alwaysManaged.get();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public BooleanProperty alwaysManagedProperty() {
        return alwaysManaged;
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public void setAlwaysManaged(boolean alwaysManaged) {
        this.alwaysManaged.set(alwaysManaged);
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public MultiPartActionResult getActionResult() {
        return actionResult.get();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public ObjectProperty<MultiPartActionResult> actionResultProperty() {
        return actionResult;
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public void setActionResult(MultiPartActionResult actionResult) {
        this.actionResult.set(actionResult);
    }
}
