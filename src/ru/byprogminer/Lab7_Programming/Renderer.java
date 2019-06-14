package ru.byprogminer.Lab7_Programming;

public interface Renderer {

    interface Listener {

        void viewRendering(Event e);
        void viewRendered(Event e);
    }

    abstract class Adapter implements Listener {

        @Override
        public void viewRendering(Event e) {}

        @Override
        public void viewRendered(Event e) {}
    }

    class Event {

        public final Renderer renderer;
        public final View view;

        protected Event(Renderer renderer, View view) {
            this.renderer = renderer;
            this.view = view;
        }
    }

    void render(View view);

    void addListener(Listener listener);
    void removeListener(Listener listener);
}
