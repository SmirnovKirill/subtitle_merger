package kirill.subtitlesmerger.gui;

import javafx.event.ActionEvent;
import kirill.subtitlesmerger.logic.data.Config;

import java.io.File;

 class SettingsTabController {
    private SettingsTab tab;

    private Config config;

    SettingsTabController(SettingsTab tab, Config config) {
        this.tab = tab;
        this.config = config;
    }

    void initialize() {
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

        try {
            config.saveFfprobeFile(ffprobeFile.getAbsolutePath());
            updateFileChoosersAndFields();

            tab.showSuccessMessage("path to ffprobe has been saved successfully");
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

        try {
            config.saveFfmpegFile(ffmpegFile.getAbsolutePath());
            updateFileChoosersAndFields();

            tab.showSuccessMessage("path to ffmpeg has been saved successfully");
        } catch (Config.ConfigException e) {
            tab.showErrorMessage("incorrect path to ffmpeg");
        }
    }
}
