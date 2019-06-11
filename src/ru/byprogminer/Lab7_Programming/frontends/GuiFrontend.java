package ru.byprogminer.Lab7_Programming.frontends;

import ru.byprogminer.Lab7_Programming.Frontend;
import ru.byprogminer.Lab7_Programming.Renderer;
import ru.byprogminer.Lab7_Programming.renderers.GuiRenderer;
import ru.byprogminer.Lab8_Programming.gui.MainWindow;

import javax.swing.*;

public class GuiFrontend implements Frontend {

    private final MainWindow mainWindow;
    private final Renderer renderer;

    public GuiFrontend(MainWindow mainWindow) {
        this.mainWindow = mainWindow;

        this.renderer = new GuiRenderer(mainWindow);
    }

    @Override
    public void exec() throws IllegalStateException {
        SwingUtilities.invokeLater(() -> mainWindow.setVisible(true));

        while (!mainWindow.isVisible()) {
            Thread.yield();
        }

        while (mainWindow.isVisible()) {
            Thread.yield();
        }
    }

    @Override
    public void stop() {
        //
    }
}
