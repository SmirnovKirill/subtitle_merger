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
        tab.getFfprobeSetButton().setOnAction(this::ffprobeFileButtonClicked);
        tab.getFfmpegSetButton().setOnAction(this::ffmpegFileButtonClicked);
    }

    private void updateFileChoosersAndFields() {
        File ffprobeFile = config.getFfprobeFile();
        File ffmpegFile = config.getFfmpegFile();

        if (ffprobeFile != null) {
            tab.getFfprobeField().setText(ffprobeFile.getAbsolutePath());

            tab.getFfprobeSetButton().setText("update path to ffprobe");

            tab.getFfprobeFileChooser().setInitialDirectory(ffprobeFile.getParentFile());
            tab.getFfprobeFileChooser().setTitle("update path to ffprobe");
        } else {
            tab.getFfprobeSetButton().setText("choose path to ffprobe");

            if (ffmpegFile != null) {
                tab.getFfprobeFileChooser().setInitialDirectory(ffmpegFile.getParentFile());
            }
            tab.getFfprobeFileChooser().setTitle("choose path to ffprobe");
        }

        if (ffmpegFile != null) {
            tab.getFfmpegField().setText(ffmpegFile.getAbsolutePath());

            tab.getFfmpegSetButton().setText("update path to ffmpeg");

            tab.getFfmpegFileChooser().setInitialDirectory(ffmpegFile.getParentFile());
            tab.getFfmpegFileChooser().setTitle("update path to ffmpeg");
        } else {
            tab.getFfmpegSetButton().setText("choose path to ffmpeg");

            if (ffprobeFile != null) {
                tab.getFfmpegFileChooser().setInitialDirectory(ffprobeFile.getParentFile());
            }
            tab.getFfmpegFileChooser().setTitle("choose path to ffmpeg");
        }
    }

    private void ffprobeFileButtonClicked(ActionEvent event) {
        File ffprobeFile = tab.getFfprobeFileChooser().showOpenDialog(tab.getStage());
        if (ffprobeFile == null) {
            tab.clearResult();
            return;
        }

        try {
            config.saveFfprobeFile(ffprobeFile.getAbsolutePath());
            updateFileChoosersAndFields();

            tab.showSuccessMessage("ffprobe path has been saved successfully");
        } catch (Config.ConfigException e) {
            tab.showErrorMessage("incorrect path to ffprobe");
        }
    }

    private void ffmpegFileButtonClicked(ActionEvent event) {
        File ffmpegFile = tab.getFfmpegFileChooser().showOpenDialog(tab.getStage());
        if (ffmpegFile == null) {
            tab.clearResult();
            return;
        }

        try {
            config.saveFfmpegFile(ffmpegFile.getAbsolutePath());
            updateFileChoosersAndFields();

            tab.showSuccessMessage("ffmpeg path has been saved successfully");
        } catch (Config.ConfigException e) {
            tab.showErrorMessage("incorrect path to ffmpeg");
        }
    }
}
