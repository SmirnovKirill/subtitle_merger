package kirill.subtitlemerger.gui.util.background;

@FunctionalInterface
public interface BackgroundRunner<T> {
    T run(BackgroundRunnerManager manager);
}
