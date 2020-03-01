package kirill.subtitlemerger.gui.utils.background;

@FunctionalInterface
public interface BackgroundRunnerCallback<T> {
    void run(T backgroundRunnerResult);
}
