package kirill.subtitlemerger.gui.utils.background;

@FunctionalInterface
public interface BackgroundCallback<T> {
    void run(T backgroundRunnerResult);
}
