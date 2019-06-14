package ru.byprogminer.Lab7_Programming.renderers;

import ru.byprogminer.Lab7_Programming.Renderer;
import ru.byprogminer.Lab7_Programming.View;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

public abstract class AbstractRenderer implements Renderer {

    private final class AbstractEvent extends Renderer.Event {

        private AbstractEvent(Renderer renderer, View view) {
            super(renderer, view);
        }
    }

    private final Set<Listener> listeners = new HashSet<>();

    @Override
    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    @Override
    public final void render(View view) {
        sendEvent(Listener::viewRendering, view);

        doRender(view);

        sendEvent(Listener::viewRendered, view);
    }

    private void sendEvent(BiConsumer<Listener, Event> handler, View view) {
        listeners.forEach(listener -> handler.accept(listener, new AbstractEvent(this, view)));
    }

    private void sendEvent(BiConsumer<Listener, Event> handler, Event event) {
        listeners.forEach(listener -> handler.accept(listener, event));
    }

    protected abstract void doRender(View view);
}
