package ru.byprogminer.Lab7_Programming.renderers;

import ru.byprogminer.Lab7_Programming.Renderer;
import ru.byprogminer.Lab7_Programming.View;
import ru.byprogminer.Lab8_Programming.gui.MainWindow;

public class GuiRenderer implements Renderer {

    private final MainWindow mainWindow;

    public GuiRenderer(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
    }

    @Override
    public void render(View view) {
        // TODO views handling
    }
}
