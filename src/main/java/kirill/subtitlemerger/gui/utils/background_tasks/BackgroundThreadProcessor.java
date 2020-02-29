package kirill.subtitlemerger.gui.utils.background_tasks;

@FunctionalInterface
public interface BackgroundThreadProcessor<T> {
    T process(TaskManager taskManager);
}
