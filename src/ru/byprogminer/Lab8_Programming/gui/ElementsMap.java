package ru.byprogminer.Lab8_Programming.gui;

import ru.byprogminer.Lab3_Programming.LivingObject;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static ru.byprogminer.Lab5_Programming.LabUtils.cloneBufferedImage;

public class ElementsMap extends JPanel implements MouseListener, MouseMotionListener, MouseWheelListener {

    public interface Listener {

        void elementSelected(Event e);
        void elementDeselected(Event e);
    }

    public static class Event {

        public final ElementsMap elementsMap;
        public final LivingObject selectedElement;

        public Event(ElementsMap elementsMap, LivingObject selectedElement) {
            this.elementsMap = elementsMap;
            this.selectedElement = selectedElement;
        }
    }

    private static final String DEFAULT_ICON_PATH = "/resources/defaultIcon.png";

    private static final int ICON_WIDTH = 32;
    private static final int ICON_HEIGHT = 32;

    private static final BufferedImage defaultIcon;

    static {
        try {
            defaultIcon = ImageIO.read(Objects.requireNonNull(ElementsMap.class.getResourceAsStream(DEFAULT_ICON_PATH)));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public final Set<Listener> listeners = new HashSet<>();
    private final List<LivingObject> elements = new ArrayList<>();
    private LivingObject selectedElement = null;

    private BufferedImage buffer = null;

    private int previousMouseX = 0;
    private int previousMouseY = 0;

    private int frameX = 0;
    private int frameY = 0;
    private double zoom = 1;

    public ElementsMap() {
        addMouseListener(this);
        addMouseWheelListener(this);
        addMouseMotionListener(this);
        addComponentListener(new ComponentListener() {

            @Override
            public void componentResized(ComponentEvent componentEvent) {
                invalidateBuffer();
            }

            @Override
            public void componentMoved(ComponentEvent componentEvent) {
                invalidateBuffer();
            }

            @Override
            public void componentShown(ComponentEvent componentEvent) {
                invalidateBuffer();
            }

            @Override
            public void componentHidden(ComponentEvent componentEvent) {
                invalidateBuffer();
            }
        });
    }

    public void setElements(Set<LivingObject> elements) {
        this.elements.clear();

        this.elements.addAll(elements.stream()
                .sorted(Comparator.comparingDouble(LivingObject::getZ))
                .collect(Collectors.toList()));
        invalidateBuffer();
        repaint();
    }

    public void selectElement(LivingObject element) {
        selectedElement = element;
        invalidateBuffer();
        repaint();
    }

    public void deselectElement() {
        selectedElement = null;

        invalidateBuffer();
        repaint();
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        if (buffer == null) {
            validateBuffer();
        }

        graphics.drawImage(buffer, 0, 0, getWidth(), getHeight(), null);
    }

    private void invalidateBuffer() {
        buffer = null;
    }

    private void validateBuffer() {
        buffer = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);

        final Graphics graphics = buffer.getGraphics();
        graphics.setColor(Color.BLACK);
        graphics.setFont(getFont());

        final Point offset = getOffset();
        graphics.drawLine(offset.x, 0, offset.x, buffer.getHeight());
        graphics.drawLine(0, offset.y, buffer.getWidth(), offset.y);

        for (LivingObject element : elements) {
            final BufferedImage icon = cloneBufferedImage(element.getImage() == null ? defaultIcon : element.getImage());
            final Rectangle location = getElementLocation(element, offset);

            if (!element.isLives()) {
                final BufferedImage newIcon = new BufferedImage(icon.getWidth(), icon.getHeight(), BufferedImage.TYPE_USHORT_GRAY);
                newIcon.getGraphics().drawImage(icon, 0, 0, null);

                final Graphics iconGraphics = icon.getGraphics();
                iconGraphics.clearRect(0, 0, icon.getWidth(), icon.getHeight());
                iconGraphics.drawImage(newIcon, 0, 0, null);
            }

            graphics.drawImage(icon, location.x, location.y, location.width, location.height, null);

            final FontMetrics fontMetrics = graphics.getFontMetrics();
            graphics.drawString(element.getName(),
                    location.x + location.width / 2 - fontMetrics.stringWidth(element.getName()) / 2,
                    location.y + location.height + fontMetrics.getAscent());

            if (element == selectedElement) {
                final Color oldColor = graphics.getColor();

                graphics.setColor(Color.RED);
                graphics.drawLine(location.x, location.y, location.x + location.width - 1, location.y);
                graphics.drawLine(location.x + location.width - 1, location.y, location.x + location.width - 1, location.y + location.height - 1);
                graphics.drawLine(location.x + location.width - 1, location.y + location.height - 1, location.x, location.y + location.height - 1);
                graphics.drawLine(location.x, location.y + location.height - 1, location.x, location.y);
                graphics.setColor(oldColor);
            }
        }
    }

    private Point getOffset() {
        if (buffer == null) {
            validateBuffer();
        }

        return new Point(buffer.getWidth() / 2 - (int) (frameX * zoom),
                buffer.getHeight() / 2 - (int) (frameY * zoom));
    }

    private Rectangle getElementLocation(LivingObject element, Point offset) {
        return new Rectangle(
                (int) (element.getX() * zoom) + offset.x - ICON_WIDTH / 2,
                (int) (element.getY() * zoom) + offset.y - ICON_HEIGHT / 2,
                ICON_WIDTH, ICON_HEIGHT
        );
    }

    @Override
    public void mouseClicked(MouseEvent mouseEvent) {
        final Point offset = getOffset();

        LivingObject element = null;
        for (int i = elements.size() - 1; i >= 0; --i) {
            final Rectangle location = getElementLocation(elements.get(i), offset);

            if (location.contains(mouseEvent.getX(), mouseEvent.getY())) {
                element = elements.get(i);
                break;
            }
        }

        selectElement(element);
        if (element != null) {
            sendEvent(Listener::elementSelected);
        } else {
            sendEvent(Listener::elementDeselected);
        }
    }

    @Override
    public void mousePressed(MouseEvent mouseEvent) {}

    @Override
    public void mouseReleased(MouseEvent mouseEvent) {}

    @Override
    public void mouseEntered(MouseEvent mouseEvent) {}

    @Override
    public void mouseExited(MouseEvent mouseEvent) {}

    @Override
    public void mouseDragged(MouseEvent mouseEvent) {
        frameX -= (mouseEvent.getX() - previousMouseX) / zoom;
        frameY -= (mouseEvent.getY() - previousMouseY) / zoom;
        invalidateBuffer();
        repaint();

        previousMouseX = mouseEvent.getX();
        previousMouseY = mouseEvent.getY();
    }

    @Override
    public void mouseMoved(MouseEvent mouseEvent) {
        previousMouseX = mouseEvent.getX();
        previousMouseY = mouseEvent.getY();
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent mouseWheelEvent) {
        zoom += mouseWheelEvent.getPreciseWheelRotation() / 10;

        if (zoom <= 0) {
            zoom = 0.1;
        }

        invalidateBuffer();
        repaint();
    }

    private void sendEvent(BiConsumer<Listener, Event> handler) {
        sendEvent(handler, new Event(this, selectedElement));
    }

    private void sendEvent(BiConsumer<Listener, Event> handler, Event event) {
        listeners.forEach(listener -> handler.accept(listener, event));
    }
}
