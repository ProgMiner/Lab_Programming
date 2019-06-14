package ru.byprogminer.Lab8_Programming.gui;

import ru.byprogminer.Lab3_Programming.LivingObject;
import ru.byprogminer.Lab3_Programming.Object;
import ru.byprogminer.Lab7_Programming.logging.Loggers;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static ru.byprogminer.Lab5_Programming.LabUtils.*;

@SuppressWarnings("FieldCanBeLocal")
public class MainWindow extends JFrame {

    public interface Listener {

        void mainFileLoadMenuItemClicked(Event event);
        void mainFileSaveMenuItemClicked(Event event);
        void mainFileImportMenuItemClicked(Event event);
        void mainFileUsersMenuItemClicked(Event event);
        void mainFileExitMenuItemClicked(Event event);
        void mainLanguageMenuItemClicked(Event event);
        void mainAboutMenuItemClicked(Event event);

        void elementChanged(Event event);

        void userNotLoggedLoginButtonClicked(Event event);
        void userLoggedInCurrentUserLogoutButtonClicked(Event event);
        void infoButtonClicked(Event event);
        void addButtonClicked(Event event);
        void removeButtonClicked(Event event);
        void removeLowerButtonClicked(Event event);
        void removeGreaterButtonClicked(Event event);
    }

    public static abstract class Adapter implements Listener {

        @Override
        public void mainFileLoadMenuItemClicked(Event event) {}

        @Override
        public void mainFileSaveMenuItemClicked(Event event) {}

        @Override
        public void mainFileImportMenuItemClicked(Event event) {}

        @Override
        public void mainFileUsersMenuItemClicked(Event event) {}

        @Override
        public void mainFileExitMenuItemClicked(Event event) {}

        @Override
        public void mainLanguageMenuItemClicked(Event event) {}

        @Override
        public void mainAboutMenuItemClicked(Event event) {}

        @Override
        public void elementChanged(Event event) {}

        @Override
        public void userNotLoggedLoginButtonClicked(Event event) {}

        @Override
        public void userLoggedInCurrentUserLogoutButtonClicked(Event event) {}

        @Override
        public void infoButtonClicked(Event event) {}

        @Override
        public void addButtonClicked(Event event) {}

        @Override
        public void removeButtonClicked(Event event) {}

        @Override
        public void removeLowerButtonClicked(Event event) {}

        @Override
        public void removeGreaterButtonClicked(Event event) {}
    }

    public static class Event {

        public final MainWindow window;
        public final String language;
        public final LivingObject selectedElement;
        public final LivingObject newElement;

        private Event(MainWindow window, String language, LivingObject selectedElement, LivingObject newElement) {
            this.window = window;
            this.language = language;
            this.selectedElement = selectedElement;
            this.newElement = newElement;
        }
    }

    private static class ItemsColumnContents {

        public final Set<Object> items;

        public ItemsColumnContents(Set<Object> items) {
            this.items = Collections.unmodifiableSet(Objects.requireNonNull(items));
        }

        @Override
        public String toString() {
            return "Items...";
        }
    }

    private static class ImageColumnContents implements Icon {

        public final BufferedImage image;
        public final ImageIcon icon;

        public ImageColumnContents(BufferedImage image) {
            this.image = Objects.requireNonNull(image);

            icon = new ImageIcon(makeThumbnail(image, MAP_LIST_LIST_MAX_IMAGE_WIDTH, MAP_LIST_LIST_MAX_IMAGE_HEIGHT));
        }

        public static ImageColumnContents get(BufferedImage image) {
            if (image == null) {
                return null;
            }

            return new ImageColumnContents(image);
        }

        @Override
        public void paintIcon(Component component, Graphics graphics, int i, int i1) {
            icon.paintIcon(component, graphics, i, i1);
        }

        @Override
        public int getIconWidth() {
            return icon.getIconWidth();
        }

        @Override
        public int getIconHeight() {
            return icon.getIconHeight();
        }
    }

    private static final int MARGIN = 5;

    private static final int MAP_LIST_LIST_NAME_COLUMN = 0;
    private static final int MAP_LIST_LIST_VOLUME_COLUMN = 1;
    private static final int MAP_LIST_LIST_CREATING_TIME_COLUMN = 2;
    private static final int MAP_LIST_LIST_X_COLUMN = 3;
    private static final int MAP_LIST_LIST_Y_COLUMN = 4;
    private static final int MAP_LIST_LIST_Z_COLUMN = 5;
    private static final int MAP_LIST_LIST_LIVES_COLUMN = 6;
    private static final int MAP_LIST_LIST_ITEMS_COLUMN = 7;
    private static final int MAP_LIST_LIST_IMAGE_COLUMN = 8;
    private static final int MAP_LIST_LIST_MAX_IMAGE_WIDTH = 50;
    private static final int MAP_LIST_LIST_MAX_IMAGE_HEIGHT = 20;

    private final String name;

    private final JMenuBar mainMenuBar = new JMenuBar();
    private final JMenu mainFileMenu = new JMenu("File");
    private final JMenuItem mainFileLoadMenuItem = new JMenuItem("Load");
    private final JMenuItem mainFileSaveMenuItem = new JMenuItem("Save");
    private final JMenuItem mainFileImportMenuItem = new JMenuItem("Import");
    private final JMenuItem mainFileUsersMenuItem = new JMenuItem("Users");
    private final JMenuItem mainFileExitMenuItem = new JMenuItem("Exit");
    private final ButtonGroup mainLanguageButtonGroup = new ButtonGroup();
    private final JMenu mainLanguageMenu = new JMenu("Language");
    private final JMenuItem mainAboutMenuItem = new JMenuItem("About");

    private final JPanel contentPanePanel = new JPanel(new GridBagLayout());
    private final JTabbedPane mapListTabbedPane = new JTabbedPane();
    private final ElementsMap mapListMapElementsMap = new ElementsMap();
    private final DefaultTableModel mapListListTableModel =
            new DefaultTableModel(arrayOf("Name", "Volume", "Creating time", "X", "Y", "Z", "Lives", "Items", "Image"), 0);
    private final JTable mapListListTable = new JTable(mapListListTableModel) {

        @Override
        public Class<?> getColumnClass(int col) {
            if (getRowCount() == 0) {
                return super.getColumnClass(col);
            }

            final java.lang.Object value = getValueAt(0, col);
            if (value == null) {
                return super.getColumnClass(col);
            } else {
                return value.getClass();
            }
        }
    };
    private final ButtonColumn mapListListItemsButtonColumn = new ButtonColumn(mapListListTable);
    private final ButtonColumn mapListListImageButtonColumn = new ButtonColumn(mapListListTable);
    private final JScrollPane mapListListScrollPane = new JScrollPane(mapListListTable);
    private final JPanel userPanel = new JPanel(new GridLayout(1, 1));
    private final JPanel userNotLoggedPanel = new JPanel(new GridBagLayout());
    private final JButton userNotLoggedLoginButton = new JButton("Login");
    private final JPanel userLoggedInPanel = new JPanel(new GridLayout(1, 1));
    private final JPanel userLoggedInCurrentUserPanel = new JPanel(new GridBagLayout());
    private final String userLoggedInCurrentUserUsernameText = "Current user: ";
    private final JLabel userLoggedInCurrentUserUsernameLabel = new JLabel(userLoggedInCurrentUserUsernameText);
    private final JButton userLoggedInCurrentUserLogoutButton = new JButton("Logout");
    private final JButton infoButton = new JButton("Collection info");
    private final JButton addButton = new JButton("Add element");
    private final JButton removeButton = new JButton("Remove element");
    private final JButton removeLowerButton = new JButton("Remove lower elements");
    private final JButton removeGreaterButton = new JButton("Remove greater elements");

    private final Set<Listener> listeners = new HashSet<>();
    private final AtomicBoolean programmaticallyMapListListTableChange = new AtomicBoolean();
    private final Map<LivingObject, Integer> elementsListIndexes = new HashMap<>();
    private final Map<Integer, LivingObject> listElements = new HashMap<>();
    private LivingObject selectedElement = null;
    private boolean userLoggedIn = false;

    private final Logger log = Loggers.getObjectLogger(this);

    public MainWindow(String name) {
        super(name);

        this.name = name;

        mainFileLoadMenuItem.setMnemonic(KeyEvent.VK_L);
        mainFileLoadMenuItem.setFont(GuiUtils.DEFAULT_FONT);
        mainFileLoadMenuItem.addActionListener(actionEvent -> sendEvent(Listener::mainFileLoadMenuItemClicked));
        mainFileMenu.add(mainFileLoadMenuItem);

        mainFileSaveMenuItem.setMnemonic(KeyEvent.VK_S);
        mainFileSaveMenuItem.setFont(GuiUtils.DEFAULT_FONT);
        mainFileSaveMenuItem.addActionListener(actionEvent -> sendEvent(Listener::mainFileSaveMenuItemClicked));
        mainFileMenu.add(mainFileSaveMenuItem);

        mainFileImportMenuItem.setMnemonic(KeyEvent.VK_I);
        mainFileImportMenuItem.setFont(GuiUtils.DEFAULT_FONT);
        mainFileImportMenuItem.addActionListener(actionEvent -> sendEvent(Listener::mainFileImportMenuItemClicked));
        mainFileMenu.add(mainFileImportMenuItem);
        mainFileMenu.addSeparator();

        mainFileUsersMenuItem.setMnemonic(KeyEvent.VK_U);
        mainFileUsersMenuItem.setFont(GuiUtils.DEFAULT_FONT);
        mainFileUsersMenuItem.addActionListener(actionEvent -> sendEvent(Listener::mainFileUsersMenuItemClicked));
        mainFileMenu.add(mainFileUsersMenuItem);
        mainFileMenu.addSeparator();

        mainFileExitMenuItem.setMnemonic(KeyEvent.VK_E);
        mainFileExitMenuItem.setFont(GuiUtils.DEFAULT_FONT);
        mainFileExitMenuItem.addActionListener(actionEvent -> sendEvent(Listener::mainFileExitMenuItemClicked));
        mainFileMenu.add(mainFileExitMenuItem);
        mainFileMenu.setMnemonic(KeyEvent.VK_F);
        mainFileMenu.setFont(GuiUtils.DEFAULT_FONT);
        mainMenuBar.add(mainFileMenu);

        mainLanguageMenu.setMnemonic(KeyEvent.VK_L);
        mainLanguageMenu.setFont(GuiUtils.DEFAULT_FONT);
        mainMenuBar.add(mainLanguageMenu);

        mainAboutMenuItem.setMnemonic(KeyEvent.VK_A);
        mainAboutMenuItem.setFont(GuiUtils.DEFAULT_FONT);
        mainAboutMenuItem.addActionListener(actionEvent -> sendEvent(Listener::mainAboutMenuItemClicked));
        mainMenuBar.add(mainAboutMenuItem);
        setJMenuBar(mainMenuBar);

        mapListMapElementsMap.setFont(GuiUtils.DEFAULT_FONT);
        mapListTabbedPane.add(mapListMapElementsMap, "Map");

        programmaticallyMapListListTableChange.set(true);
        GuiUtils.configureDefaultJTable(mapListListTable, mapListListScrollPane);
        mapListListTableModel.addTableModelListener(this::mapListListTableModelTableChanged);

        final TableColumnModel mapListListTableColumnModel = mapListListTable.getColumnModel();
        mapListListTableColumnModel.getColumn(MAP_LIST_LIST_NAME_COLUMN).setMinWidth(100);
        mapListListTableColumnModel.getColumn(MAP_LIST_LIST_NAME_COLUMN).setPreferredWidth(120);
        mapListListTableColumnModel.getColumn(MAP_LIST_LIST_VOLUME_COLUMN).setMinWidth(60);
        mapListListTableColumnModel.getColumn(MAP_LIST_LIST_CREATING_TIME_COLUMN).setMinWidth(145);
        mapListListTableColumnModel.getColumn(MAP_LIST_LIST_X_COLUMN).setMinWidth(25);
        mapListListTableColumnModel.getColumn(MAP_LIST_LIST_Y_COLUMN).setMinWidth(25);
        mapListListTableColumnModel.getColumn(MAP_LIST_LIST_Z_COLUMN).setMinWidth(25);
        mapListListTableColumnModel.getColumn(MAP_LIST_LIST_LIVES_COLUMN).setMinWidth(45);
        mapListListTableColumnModel.getColumn(MAP_LIST_LIST_LIVES_COLUMN).setMaxWidth(45);
        mapListListTableColumnModel.getColumn(MAP_LIST_LIST_ITEMS_COLUMN).setMinWidth(75);
        mapListListTableColumnModel.getColumn(MAP_LIST_LIST_ITEMS_COLUMN).setMaxWidth(75);
        mapListListTableColumnModel.getColumn(MAP_LIST_LIST_IMAGE_COLUMN).setMinWidth(50);

        mapListListItemsButtonColumn.getEditorButton().setMargin(GuiUtils.DEFAULT_MARGIN);
        mapListListItemsButtonColumn.getRendererButton().setMargin(GuiUtils.DEFAULT_MARGIN);
        mapListListItemsButtonColumn.getEditorButton().setFont(GuiUtils.DEFAULT_BUTTON_FONT);
        mapListListItemsButtonColumn.getRendererButton().setFont(GuiUtils.DEFAULT_BUTTON_FONT);
        mapListListItemsButtonColumn.addActionListener(actionEvent ->
                mapListListTableItemsButtonColumnClicked(Integer.parseInt(actionEvent.getActionCommand())));
        mapListListItemsButtonColumn.setup(MAP_LIST_LIST_ITEMS_COLUMN);

        mapListListImageButtonColumn.getEditorButton().setMargin(GuiUtils.DEFAULT_MARGIN);
        mapListListImageButtonColumn.getRendererButton().setMargin(GuiUtils.DEFAULT_MARGIN);
        mapListListImageButtonColumn.getEditorButton().setFont(GuiUtils.DEFAULT_BUTTON_FONT);
        mapListListImageButtonColumn.getRendererButton().setFont(GuiUtils.DEFAULT_BUTTON_FONT);
        mapListListImageButtonColumn.addActionListener(actionEvent ->
                mapListListTableImageButtonColumnClicked(Integer.parseInt(actionEvent.getActionCommand())));
        mapListListImageButtonColumn.setup(MAP_LIST_LIST_IMAGE_COLUMN);
        mapListListTable.getSelectionModel().addListSelectionListener(listSelectionEvent -> mapListListTableSelectionModelValueChanged());
        mapListListTable.doLayout();
        programmaticallyMapListListTableChange.set(false);
        mapListTabbedPane.add(mapListListScrollPane, "List");
        mapListTabbedPane.setFont(GuiUtils.DEFAULT_FONT);
        contentPanePanel.add(mapListTabbedPane, new GridBagConstraints(0, 0, 1, 7, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, MARGIN), 0, 0));

        userNotLoggedLoginButton.addActionListener(actionEvent -> sendEvent(Listener::userNotLoggedLoginButtonClicked));
        userNotLoggedLoginButton.setFont(GuiUtils.DEFAULT_BUTTON_FONT);
        userNotLoggedLoginButton.setMargin(GuiUtils.DEFAULT_BUTTON_MARGIN);
        userNotLoggedPanel.add(userNotLoggedLoginButton, new GridBagConstraints(0, 0, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, MARGIN, 0), 0, 0));
        userNotLoggedPanel.add(new JSeparator(SwingConstants.HORIZONTAL), new GridBagConstraints(0, 1, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        userPanel.add(userNotLoggedPanel);

        userLoggedInCurrentUserUsernameLabel.setFont(GuiUtils.DEFAULT_FONT);
        userLoggedInCurrentUserUsernameLabel.setBorder(GuiUtils.DEFAULT_MARGIN_BORDER);
        userLoggedInCurrentUserPanel.add(userLoggedInCurrentUserUsernameLabel, new GridBagConstraints(0, 0, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, MARGIN, 0), 0, 0));

        userLoggedInCurrentUserLogoutButton.addActionListener(actionEvent -> sendEvent(Listener::userLoggedInCurrentUserLogoutButtonClicked));
        userLoggedInCurrentUserLogoutButton.setFont(GuiUtils.DEFAULT_BUTTON_FONT);
        userLoggedInCurrentUserLogoutButton.setMargin(GuiUtils.DEFAULT_BUTTON_MARGIN);
        userLoggedInCurrentUserPanel.add(userLoggedInCurrentUserLogoutButton, new GridBagConstraints(0, 1, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        userLoggedInCurrentUserPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory
                .createEtchedBorder(EtchedBorder.LOWERED), BorderFactory.createEmptyBorder(0, MARGIN, MARGIN, MARGIN)));
        userLoggedInPanel.add(userLoggedInCurrentUserPanel);
        contentPanePanel.add(userPanel, new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, MARGIN, 0), 0, 0));

        infoButton.addActionListener(actionEvent -> sendEvent(Listener::infoButtonClicked));
        infoButton.setFont(GuiUtils.DEFAULT_BUTTON_FONT);
        infoButton.setMargin(GuiUtils.DEFAULT_BUTTON_MARGIN);
        infoButton.setPreferredSize(infoButton.getMaximumSize());
        contentPanePanel.add(infoButton, new GridBagConstraints(1, 1, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, MARGIN, 0), 0, 0));

        addButton.addActionListener(actionEvent -> sendEvent(Listener::addButtonClicked));
        addButton.setFont(GuiUtils.DEFAULT_BUTTON_FONT);
        addButton.setMargin(GuiUtils.DEFAULT_BUTTON_MARGIN);
        contentPanePanel.add(addButton, new GridBagConstraints(1, 2, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, MARGIN, 0), 0, 0));

        removeButton.addActionListener(actionEvent -> sendEvent(Listener::removeButtonClicked));
        removeButton.setFont(GuiUtils.DEFAULT_BUTTON_FONT);
        removeButton.setMargin(GuiUtils.DEFAULT_BUTTON_MARGIN);
        contentPanePanel.add(removeButton, new GridBagConstraints(1, 3, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, MARGIN, 0), 0, 0));

        removeLowerButton.addActionListener(actionEvent -> sendEvent(Listener::removeLowerButtonClicked));
        removeLowerButton.setFont(GuiUtils.DEFAULT_BUTTON_FONT);
        removeLowerButton.setMargin(GuiUtils.DEFAULT_BUTTON_MARGIN);
        contentPanePanel.add(removeLowerButton, new GridBagConstraints(1, 4, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, MARGIN, 0), 0, 0));

        removeGreaterButton.addActionListener(actionEvent -> sendEvent(Listener::removeGreaterButtonClicked));
        removeGreaterButton.setFont(GuiUtils.DEFAULT_BUTTON_FONT);
        removeGreaterButton.setMargin(GuiUtils.DEFAULT_BUTTON_MARGIN);
        contentPanePanel.add(removeGreaterButton, new GridBagConstraints(1, 5, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        contentPanePanel.add(Box.createVerticalGlue(), new GridBagConstraints(1, 6, 1, 1, 0, 1, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        contentPanePanel.setBorder(BorderFactory.createEmptyBorder(MARGIN, MARGIN, MARGIN, MARGIN));
        setContentPane(contentPanePanel);
        setMinimumSize(new Dimension(850, 630));
        setLocationRelativeTo(null);
        pack();

        deselectElement();
    }

    public void setCurrentUser(String currentUser) {
        final boolean newUserLoggedIn = currentUser != null;
        final boolean changeState = userLoggedIn != newUserLoggedIn;

        setTitle(GuiUtils.getWindowTitle(name, currentUser));
        userLoggedInCurrentUserUsernameLabel.setText(userLoggedInCurrentUserUsernameText + Objects.toString(currentUser, ""));
        if (changeState) {
            if (currentUser == null) {
                userPanel.remove(userLoggedInPanel);
                userPanel.add(userNotLoggedPanel);
            } else {
                userPanel.remove(userNotLoggedPanel);
                userPanel.add(userLoggedInPanel);
            }
        }

        userLoggedIn = newUserLoggedIn;
        pack();
    }

    public void setLanguages(List<String> languages) {
        final GuiDisabler<MainWindow> disabler = GuiDisabler.disable(this);
        final List<JMenuItem> menuItems = new ArrayList<>();

        for (String language : languages) {
            final JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(language);

            menuItem.setActionCommand(language);
            menuItem.addActionListener(actionEvent -> sendEvent(Listener::mainLanguageMenuItemClicked));
            menuItems.add(menuItem);
        }

        final Enumeration<AbstractButton> oldLanguageButtons = mainLanguageButtonGroup.getElements();
        while (oldLanguageButtons.hasMoreElements()) {
            mainLanguageButtonGroup.remove(oldLanguageButtons.nextElement());
        }

        mainLanguageMenu.removeAll();
        for (JMenuItem menuItem : menuItems) {
            mainLanguageButtonGroup.add(menuItem);
            mainLanguageMenu.add(menuItem);
        }

        disabler.revert();
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public void setElements(Set<LivingObject> elements) {
        deselectElement();

        // TODO update map

        listElements.clear();
        elementsListIndexes.clear();
        programmaticallyMapListListTableChange.set(true);
        mapListListTableModel.setRowCount(0);
        for (LivingObject element : elements) {
            addListRow(element);
        }
        programmaticallyMapListListTableChange.set(false);
    }

    private void mapListListTableSelectionModelValueChanged() {
        final int selection = mapListListTable.getSelectionModel().getMinSelectionIndex();

        if (selection != -1) {
            selectElement(selection);
        }
    }

    private void mapListListTableModelTableChanged(TableModelEvent tableModelEvent) {
        if (programmaticallyMapListListTableChange.get()) {
            return;
        }

        final int row = tableModelEvent.getFirstRow();
        final LivingObject element = listElements.get(row);
        if (tableModelEvent.getType() == TableModelEvent.UPDATE) {
            try {
                final LivingObject newElement = getElementFromRow(row);

                changeElement(element, newElement);
                sendEvent(Listener::elementChanged, element, newElement);
            } catch (Throwable e) {
                log.log(Level.SEVERE, "bad living object format", e);
                JOptionPane.showMessageDialog(MainWindow.this, arrayOf("Bad living object format", e.getLocalizedMessage()));
                changeElement(element, element);
            }
        }
    }

    private void mapListListTableItemsButtonColumnClicked(int row) {
        final ItemsDialog dialog = new ItemsDialog(this, "Items of " + mapListListTableModel.getValueAt(row, MAP_LIST_LIST_NAME_COLUMN));
        dialog.setItems(((ItemsColumnContents) mapListListTableModel.getValueAt(row, MAP_LIST_LIST_ITEMS_COLUMN)).items);

        dialog.addListener(new ItemsDialog.Listener() {
            @Override
            public void saveButtonClicked(ItemsDialog.Event event) {
                mapListListTableModel.setValueAt(new ItemsColumnContents(event.items), row, MAP_LIST_LIST_ITEMS_COLUMN);
                cancelButtonClicked(event);
            }

            @Override
            public void cancelButtonClicked(ItemsDialog.Event event) {
                event.dialog.setVisible(false);
                event.dialog.dispose();
            }
        });

        dialog.setVisible(true);
    }

    private void mapListListTableImageButtonColumnClicked(int row) {
        final JFileChooser fileChooser = new JFileChooser("Select image file");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        final int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            final File selectedFile = fileChooser.getSelectedFile();

            try {
                final BufferedImage image = ImageIO.read(selectedFile);

                if (image == null) {
                    throw new IllegalArgumentException("File is not an image");
                }

                mapListListTableModel.setValueAt(new ImageColumnContents(image), row, MAP_LIST_LIST_IMAGE_COLUMN);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, arrayOf("Bad file selected", e.getLocalizedMessage()));
            }
        }
    }

    private LivingObject getElementFromRow(int row) throws DateTimeParseException {
        final LivingObject element = new LivingObject(
                (String) mapListListTableModel.getValueAt(row, MAP_LIST_LIST_NAME_COLUMN),
                (Double) mapListListTableModel.getValueAt(row, MAP_LIST_LIST_VOLUME_COLUMN),
                parseLocalDateTime((String) mapListListTableModel.getValueAt(row, MAP_LIST_LIST_CREATING_TIME_COLUMN)),
                (Double) mapListListTableModel.getValueAt(row, MAP_LIST_LIST_X_COLUMN),
                (Double) mapListListTableModel.getValueAt(row, MAP_LIST_LIST_Y_COLUMN),
                (Double) mapListListTableModel.getValueAt(row, MAP_LIST_LIST_Z_COLUMN)
        );

        setLivingObjectLives(element, (Boolean) mapListListTableModel.getValueAt(row, MAP_LIST_LIST_LIVES_COLUMN));
        element.getItems().addAll(((ItemsColumnContents) mapListListTableModel.getValueAt(row, MAP_LIST_LIST_ITEMS_COLUMN)).items);
        callIfNotNull(mapListListTableModel.getValueAt(row, MAP_LIST_LIST_IMAGE_COLUMN), contents ->
                element.setImage(cloneBufferedImage(((ImageColumnContents) contents).image)));

        return element;
    }

    private void addListRow(LivingObject element) {
        elementsListIndexes.put(element, mapListListTableModel.getRowCount());
        listElements.put(mapListListTableModel.getRowCount(), element);

        final boolean pmlltc = programmaticallyMapListListTableChange.get();
        programmaticallyMapListListTableChange.set(true);
        mapListListTableModel.addRow(arrayOf(
                element.getName(),
                element.getVolume(),
                element.getCreatingTime().format(Object.DATE_TIME_FORMATTER),
                element.getX(),
                element.getY(),
                element.getZ(),
                element.isLives(),
                new ItemsColumnContents(element.getItems()),
                ImageColumnContents.get(element.getImage())
        ));
        programmaticallyMapListListTableChange.set(pmlltc);
    }

    private void deselectElement() {
        // TODO deselect on map

        final boolean pmlltc = programmaticallyMapListListTableChange.get();
        programmaticallyMapListListTableChange.set(true);
        mapListListTable.getSelectionModel().clearSelection();
        programmaticallyMapListListTableChange.set(pmlltc);

        selectedElement = null;
    }

    private void selectElement(LivingObject element) {
        // TODO select on map

        final boolean pmlltc = programmaticallyMapListListTableChange.get();
        programmaticallyMapListListTableChange.set(true);
        mapListListTable.getSelectionModel().setSelectionInterval(elementsListIndexes.get(element),
                elementsListIndexes.get(element));
        programmaticallyMapListListTableChange.set(pmlltc);

        selectedElement = element;
    }

    private void selectElement(int listIndex) {
        final LivingObject element = listElements.get(listIndex);

        // TODO select on map

        final boolean pmlltc = programmaticallyMapListListTableChange.get();
        programmaticallyMapListListTableChange.set(true);
        mapListListTable.getSelectionModel().setSelectionInterval(listIndex, listIndex);
        programmaticallyMapListListTableChange.set(pmlltc);

        selectedElement = element;
    }

    private void changeElement(LivingObject oldElement, LivingObject newElement) {
        final int listIndex = elementsListIndexes.remove(oldElement);
        elementsListIndexes.put(newElement, listIndex);
        listElements.put(listIndex, newElement);

        // TODO change on map

        final boolean pmlltc = programmaticallyMapListListTableChange.get();
        programmaticallyMapListListTableChange.set(true);
        mapListListTableModel.setValueAt(newElement.getName(), listIndex, MAP_LIST_LIST_NAME_COLUMN);
        mapListListTableModel.setValueAt(newElement.getVolume(), listIndex, MAP_LIST_LIST_VOLUME_COLUMN);
        mapListListTableModel.setValueAt(newElement.getCreatingTime().format(Object.DATE_TIME_FORMATTER), listIndex, MAP_LIST_LIST_CREATING_TIME_COLUMN);
        mapListListTableModel.setValueAt(newElement.getX(), listIndex, MAP_LIST_LIST_X_COLUMN);
        mapListListTableModel.setValueAt(newElement.getY(), listIndex, MAP_LIST_LIST_Y_COLUMN);
        mapListListTableModel.setValueAt(newElement.getZ(), listIndex, MAP_LIST_LIST_Z_COLUMN);
        mapListListTableModel.setValueAt(newElement.isLives(), listIndex, MAP_LIST_LIST_LIVES_COLUMN);
        mapListListTableModel.setValueAt(new ItemsColumnContents(newElement.getItems()), listIndex, MAP_LIST_LIST_ITEMS_COLUMN);
        mapListListTableModel.setValueAt(ImageColumnContents.get(newElement.getImage()), listIndex, MAP_LIST_LIST_IMAGE_COLUMN);
        programmaticallyMapListListTableChange.set(pmlltc);

        selectElement(newElement);
    }

    private void sendEvent(BiConsumer<Listener, Event> handler) {
        sendEvent(handler, selectedElement, selectedElement);
    }

    private void sendEvent(BiConsumer<Listener, Event> handler, LivingObject oldElement, LivingObject newElement) {
        final ButtonModel selection = mainLanguageButtonGroup.getSelection();

        sendEvent(handler, new Event(this,
                selection == null ? "" : selection.getActionCommand(),
                oldElement, newElement));
    }

    private void sendEvent(BiConsumer<Listener, Event> handler, Event event) {
        listeners.forEach(listener -> handler.accept(listener, event));
    }
}
