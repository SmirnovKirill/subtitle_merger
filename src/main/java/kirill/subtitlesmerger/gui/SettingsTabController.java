package kirill.subtitlesmerger.gui;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import javafx.event.ActionEvent;
import kirill.subtitlesmerger.logic.data.Config;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

class SettingsTabController {
    private SettingsTab tab;

    private Config config;

    private List<LanguageAlpha3Code> allLanguageCodes;

    SettingsTabController(SettingsTab tab, Config config) {
        this.tab = tab;
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

    void initialize() {
        tab.setLanguageCodesForComboBoxes(allLanguageCodes);
        updateFileChoosersAndFields();
        tab.setFfprobeSetButtonHandler(this::ffprobeFileButtonClicked);
        tab.setFfmpegSetButtonHandler(this::ffmpegFileButtonClicked);
    }

    private void updateFileChoosersAndFields() {
        File ffprobeFile = config.getFfprobeFile();
        File ffmpegFile = config.getFfmpegFile();

        if (ffprobeFile != null) {
            tab.updateFfprobeInfo(
                    ffprobeFile.getAbsolutePath(),
                    "update path to ffprobe",
                    "update path to ffprobe",
                    ffprobeFile.getParentFile()
            );
        } else {
            tab.updateFfprobeInfo(
                    "",
                    "choose path to ffprobe",
                    "choose path to ffprobe",
                    ffmpegFile != null ? ffmpegFile.getParentFile() : null
            );
        }

        if (ffmpegFile != null) {
            tab.updateFfmpegInfo(
                    ffmpegFile.getAbsolutePath(),
                    "update path to ffmpeg",
                    "update path to ffmpeg",
                    ffmpegFile.getParentFile()
            );
        } else {
            tab.updateFfmpegInfo(
                    "",
                    "choose path to ffmpeg",
                    "choose path to ffmpeg",
                    ffprobeFile != null ? ffprobeFile.getParentFile() : null
            );
        }
    }

    private void ffprobeFileButtonClicked(ActionEvent event) {
        File ffprobeFile = tab.getSelectedFfprobeFile().orElse(null);
        if (ffprobeFile == null) {
            tab.clearResult();
            return;
        }

        if (Objects.equals(ffprobeFile, config.getFfprobeFile())) {
            tab.showSuccessMessage("path to ffprobe has stayed the same");
            return;
        }

        boolean hadValueBefore = config.getFfmpegFile() != null;

        try {
            config.saveFfprobeFile(ffprobeFile.getAbsolutePath());
            updateFileChoosersAndFields();

            if (hadValueBefore) {
                tab.showSuccessMessage("path to ffprobe has been updated successfully");
            } else {
                tab.showSuccessMessage("path to ffprobe has been saved successfully");
            }
        } catch (Config.ConfigException e) {
            tab.showErrorMessage("incorrect path to ffprobe");
        }
    }

    private void ffmpegFileButtonClicked(ActionEvent event) {
        File ffmpegFile = tab.getSelectedFfmpegFile().orElse(null);
        if (ffmpegFile == null) {
            tab.clearResult();
            return;
        }

        if (Objects.equals(ffmpegFile, config.getFfmpegFile())) {
            tab.showSuccessMessage("path to ffmpeg has stayed the same");
            return;
        }

        boolean hadValueBefore = config.getFfmpegFile() != null;

        try {
            config.saveFfmpegFile(ffmpegFile.getAbsolutePath());
            updateFileChoosersAndFields();

            if (hadValueBefore) {
                tab.showSuccessMessage("path to ffmpeg has been updated successfully");
            } else {
                tab.showSuccessMessage("path to ffmpeg has been saved successfully");
            }
        } catch (Config.ConfigException e) {
            tab.showErrorMessage("incorrect path to ffmpeg");
        }
    }
}
