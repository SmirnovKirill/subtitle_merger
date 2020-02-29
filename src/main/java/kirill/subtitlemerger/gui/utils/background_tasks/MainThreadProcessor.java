package kirill.subtitlemerger.gui.utils.background_tasks;

@FunctionalInterface
public interface MainThreadProcessor<T> {
    void process(T backgroundThreadResult);
}
