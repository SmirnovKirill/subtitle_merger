package kirill.subtitlemerger.gui.utils;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import kirill.subtitlemerger.gui.forms.common.ErrorPopupFormController;
import kirill.subtitlemerger.gui.forms.common.agreement_popup.AgreementPopupFormController;
import kirill.subtitlemerger.gui.forms.common.agreement_popup.AgreementResult;
import kirill.subtitlemerger.gui.forms.common.subtitle_preview.EncodingPreviewFormController;
import kirill.subtitlemerger.gui.forms.common.subtitle_preview.ReadOnlyPreviewFormController;
import kirill.subtitlemerger.gui.utils.entities.FormInfo;
import kirill.subtitlemerger.logic.subtitles.entities.SubtitlesAndInput;

public class Popups {
    public static void showError(String message, Stage ownerStage) {
        FormInfo nodeInfo = GuiUtils.loadForm("/gui/javafx/forms/common/error_popup_form.fxml");

        Stage popupStage = getPopupStage("Error!", nodeInfo.getRootNode(), ownerStage);

        ErrorPopupFormController controller = nodeInfo.getController();
        controller.initialize(message, popupStage);

        popupStage.showAndWait();
    }

    private static Stage getPopupStage(String title, Parent rootNode, Stage ownerStage) {
        Stage result = new Stage();

        result.initOwner(ownerStage);
        result.initModality(Modality.APPLICATION_MODAL);
        result.setTitle(title);
        result.setResizable(false);

        Scene scene = new Scene(rootNode);
        scene.getStylesheets().add("/gui/javafx/main.css");
        result.setScene(scene);

        return result;
    }

    public static AgreementResult askAgreement(
            String message,
            String applyToAllText,
            String yesText,
            String noText,
            Stage ownerStage
    ) {
        FormInfo nodeInfo = GuiUtils.loadForm("/gui/javafx/forms/common/agreement_popup_form.fxml");

        Stage popupStage = getPopupStage("Please confirm", nodeInfo.getRootNode(), ownerStage);

        AgreementPopupFormController controller = nodeInfo.getController();
        controller.initialize(message, applyToAllText, yesText, noText, popupStage);

        popupStage.showAndWait();

        return controller.getResult();
    }

    /**
     * A brief version of the agreement popup without an "apply to all" option.
     */
    public static boolean askAgreement(String message, String yesText, String noText, Stage ownerStage) {
        AgreementResult result = askAgreement(message, null, yesText, noText, ownerStage);
        return result == AgreementResult.YES;
    }

    public static void showSimpleSubtitlesPreview(String title, String text, Stage ownerStage) {
        FormInfo formInfo = GuiUtils.loadForm(
                "/gui/javafx/forms/common/subtitle_preview/read_only_preview_form.fxml"
        );

        Stage previewStage = Popups.getPopupStage("Subtitle preview", formInfo.getRootNode(), ownerStage);

        ReadOnlyPreviewFormController controller = formInfo.getController();
        controller.initializeSimple(title, text, previewStage);

        previewStage.showAndWait();
    }

    public static void showMergedSubtitlesPreview(
            String upperSubtitlesTitle,
            String lowerSubtitlesTitle,
            String text,
            Stage ownerStage
    ) {
        FormInfo formInfo = GuiUtils.loadForm(
                "/gui/javafx/forms/common/subtitle_preview/read_only_preview_form.fxml"
        );

        Stage previewStage = Popups.getPopupStage("Subtitle preview", formInfo.getRootNode(), ownerStage);

        ReadOnlyPreviewFormController controller = formInfo.getController();
        controller.initializeMerged(upperSubtitlesTitle, lowerSubtitlesTitle, text, previewStage);

        previewStage.showAndWait();
    }

    public static SubtitlesAndInput showEncodingPreview(
            String title,
            SubtitlesAndInput subtitlesAndInput,
            Stage ownerStage
    ) {
        FormInfo formInfo = GuiUtils.loadForm(
                "/gui/javafx/forms/common/subtitle_preview/encoding_preview_form.fxml"
        );

        Stage previewStage = Popups.getPopupStage("Subtitle preview", formInfo.getRootNode(), ownerStage);

        EncodingPreviewFormController controller = formInfo.getController();
        controller.initialize(title, subtitlesAndInput, previewStage);

        previewStage.showAndWait();

        return controller.getSelection();
    }
}
