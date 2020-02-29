package kirill.subtitlemerger.gui.utils.background_tasks;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import lombok.Setter;

public class TaskManager {
    @Setter
    private BackgroundTask<?> task;

    private BooleanProperty canCancel;

    public TaskManager(boolean canCancel) {
        this.canCancel = new SimpleBooleanProperty(canCancel);
    }

    public boolean isCanCancel() {
        return canCancel.get();
    }

    public BooleanProperty canCancelProperty() {
        return canCancel;
    }

    public void setCanCancel(boolean canCancel) {
        this.canCancel.set(canCancel);
    }

    public void updateMessage(String message) {
        task.updateMessage(message);
    }

    public void updateProgress(long workDone, long max) {
        task.updateProgress(workDone, max);
    }

    public void updateProgress(double workDone, double max) {
        task.updateProgress(workDone, max);
    }
}

