package ru.byprogminer.Lab7_Programming.renderers;

import ru.byprogminer.Lab7_Programming.Renderer;
import ru.byprogminer.Lab7_Programming.View;
import ru.byprogminer.Lab8_Programming.gui.MainWindow;

public class GuiRenderer implements Renderer {

    public final MainWindow mainWindow;

    public GuiRenderer(String name) {
        mainWindow = new MainWindow(name);
    }

    @Override
    public void render(View view) {
        // TODO views handling
    }
}
