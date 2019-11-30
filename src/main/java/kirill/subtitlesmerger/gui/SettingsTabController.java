package kirill.subtitlesmerger.gui;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import kirill.subtitlesmerger.logic.data.Config;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@CommonsLog
class SettingsTabController implements TabController{
    private SettingsTabView tabView;

    private Config config;

    private List<LanguageAlpha3Code> allLanguageCodes;

    SettingsTabController(SettingsTabView tabView, Config config) {
        this.tabView = tabView;
        this.config = config;
        this.allLanguageCodes = getAllLanguageCodes();
    }

    private static List<LanguageAlpha3Code> getAllLanguageCodes() {
        return Arrays.stream(LanguageAlpha3Code.values())
                .filter(code -> code != LanguageAlpha3Code.undefined)
                .filter(code -> code.getUsage() != LanguageAlpha3Code.Usage.TERMINOLOGY)
                .sorted(Comparator.comparing(LanguageAlpha3Code::getName))
                .collect(Collectors.toList());
    }

    @Override
    public void initialize() {
        tabView.setLanguageCodesForComboBoxes(allLanguageCodes);
        tabView.setFfprobeSetButtonHandler(this::ffprobeFileButtonClicked);
        tabView.setFfmpegSetButtonHandler(this::ffmpegFileButtonClicked);
        tabView.setUpperLanguageListener(this::upperLanguageListener);
        tabView.setSwapLanguagesButtonHandler(this::swapLanguagesButtonClicked);
        tabView.setLowerLanguageListener(this::lowerLanguageListener);

        updateFileChoosersAndFields();
    }

    private void ffprobeFileButtonClicked(ActionEvent event) {
        File ffprobeFile = tabView.getSelectedFfprobeFile().orElse(null);
        if (ffprobeFile == null) {
            tabView.clearResult();
            return;
        }

        if (Objects.equals(ffprobeFile, config.getFfprobeFile())) {
            tabView.showSuccessMessage("path to ffprobe has stayed the same");
            return;
        }

        boolean hadValueBefore = config.getFfmpegFile() != null;

        try {
            config.saveFfprobeFile(ffprobeFile.getAbsolutePath());
            updateFileChoosersAndFields();

            if (hadValueBefore) {
                tabView.showSuccessMessage("path to ffprobe has been updated successfully");
            } else {
                tabView.showSuccessMessage("path to ffprobe has been saved successfully");
            }
        } catch (Config.ConfigException e) {
            tabView.showErrorMessage("incorrect path to ffprobe");
        }
    }

    private void updateFileChoosersAndFields() {
        File ffprobeFile = config.getFfprobeFile();
        File ffmpegFile = config.getFfmpegFile();

        if (ffprobeFile != null) {
            tabView.updateFfprobeInfo(
                    ffprobeFile.getAbsolutePath(),
                    "update path to ffprobe",
                    "update path to ffprobe",
                    ffprobeFile.getParentFile()
            );
        } else {
            tabView.updateFfprobeInfo(
                    "",
                    "choose path to ffprobe",
                    "choose path to ffprobe",
                    ffmpegFile != null ? ffmpegFile.getParentFile() : null
            );
        }

        if (ffmpegFile != null) {
            tabView.updateFfmpegInfo(
                    ffmpegFile.getAbsolutePath(),
                    "update path to ffmpeg",
                    "update path to ffmpeg",
                    ffmpegFile.getParentFile()
            );
        } else {
            tabView.updateFfmpegInfo(
                    "",
                    "choose path to ffmpeg",
                    "choose path to ffmpeg",
                    ffprobeFile != null ? ffprobeFile.getParentFile() : null
            );
        }

        LanguageAlpha3Code upperLanguage = config.getUpperLanguage();
        if (upperLanguage != null) {
            tabView.setSelectedUpperLanguage(upperLanguage);
        }

        LanguageAlpha3Code lowerLanguage = config.getLowerLanguage();
        if (lowerLanguage != null) {
            tabView.setSelectedLowerLanguage(lowerLanguage);
        }

        tabView.setSwapLanguagesButtonDisable(config.getUpperLanguage() == null || config.getLowerLanguage() == null);
    }

    private void ffmpegFileButtonClicked(ActionEvent event) {
        File ffmpegFile = tabView.getSelectedFfmpegFile().orElse(null);
        if (ffmpegFile == null) {
            tabView.clearResult();
            return;
        }

        if (Objects.equals(ffmpegFile, config.getFfmpegFile())) {
            tabView.showSuccessMessage("path to ffmpeg has stayed the same");
            return;
        }

        boolean hadValueBefore = config.getFfmpegFile() != null;

        try {
            config.saveFfmpegFile(ffmpegFile.getAbsolutePath());
            updateFileChoosersAndFields();

            if (hadValueBefore) {
                tabView.showSuccessMessage("path to ffmpeg has been updated successfully");
            } else {
                tabView.showSuccessMessage("path to ffmpeg has been saved successfully");
            }
        } catch (Config.ConfigException e) {
            tabView.showErrorMessage("incorrect path to ffmpeg");
        }
    }

    private void upperLanguageListener(
            ObservableValue<? extends LanguageAlpha3Code> observable,
            LanguageAlpha3Code oldValue,
            LanguageAlpha3Code newValue
    ) {
        if (Objects.equals(newValue, config.getUpperLanguage())) {
            return;
        }

        if (Objects.equals(newValue, config.getLowerLanguage())) {
            updateFileChoosersAndFields();
            tabView.showErrorMessage("languages have to be different, please select another one");
            return;
        }

        boolean hadValueBefore = config.getUpperLanguage() != null;

        try {
            config.saveUpperLanguage(newValue.toString());
            updateFileChoosersAndFields();

            if (hadValueBefore) {
                tabView.showSuccessMessage("language for upper subtitles has been updated successfully");
            } else {
                tabView.showSuccessMessage("language for upper subtitles has been saved successfully");
            }
        } catch (Config.ConfigException e) {
            log.error("language for upper subtitles has not been saved: " + ExceptionUtils.getStackTrace(e));

            tabView.showErrorMessage("something bad has happened, language hasn't been saved");
        }
    }

    private void swapLanguagesButtonClicked(ActionEvent event) {
        LanguageAlpha3Code oldUpperLanguage = config.getUpperLanguage();
        LanguageAlpha3Code oldLowerLanguage = config.getLowerLanguage();

        try {
            config.saveUpperLanguage(oldLowerLanguage.toString());
            config.saveLowerLanguage(oldUpperLanguage.toString());
            updateFileChoosersAndFields();

            tabView.showSuccessMessage("languages have been swapped successfully");
        } catch (Config.ConfigException e) {
            log.error("languages haven't been swapped: " + ExceptionUtils.getStackTrace(e));

            tabView.showErrorMessage("something bad has happened, languages haven't been swapped");
        }
    }

    private void lowerLanguageListener(
            ObservableValue<? extends LanguageAlpha3Code> observable,
            LanguageAlpha3Code oldValue,
            LanguageAlpha3Code newValue
    ) {
        if (newValue == null) {
            throw new IllegalStateException();
        }

        if (Objects.equals(newValue, config.getLowerLanguage())) {
            return;
        }

        if (Objects.equals(newValue, config.getUpperLanguage())) {
            updateFileChoosersAndFields();
            tabView.showErrorMessage("languages have to be different, please select another one");
            return;
        }

        boolean hadValueBefore = config.getLowerLanguage() != null;

        try {
            config.saveLowerLanguage(newValue.toString());
            updateFileChoosersAndFields();

            if (hadValueBefore) {
                tabView.showSuccessMessage("language for lower subtitles has been updated successfully");
            } else {
                tabView.showSuccessMessage("language for lower subtitles has been saved successfully");
            }
        } catch (Config.ConfigException e) {
            log.error("language for lower subtitles has not been saved: " + ExceptionUtils.getStackTrace(e));

            tabView.showErrorMessage("something bad has happened, language hasn't been saved");
        }
    }

    @Override
    public TabView getTabView() {
        return tabView;
    }

    @Override
    public void tabClicked() {
        updateFileChoosersAndFields();
    }
}
