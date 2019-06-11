package ru.byprogminer.Lab8_Programming.gui;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.*;
import java.util.function.BiConsumer;

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

        void userNotLoggedLoginButtonClicked(Event event);
        void userLoggedInCurrentUserLogoutButtonClicked(Event event);
        void infoButtonClicked(Event event);
        void addButtonClicked(Event event);
        void removeButtonClicked(Event event);
        void removeLowerButtonClicked(Event event);
        void removeGreaterButtonClicked(Event event);
    }

    public static class Event {

        public final MainWindow window;
        public final String language;

        private Event(MainWindow window, String language) {
            this.window = window;
            this.language = language;
        }
    }

    private final String name;
    private final int margin = 5;

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
    private final JTable mapListListTable = new JTable();
    private final JPanel userPanel = new JPanel(new GridLayout(1, 1));
    private final JPanel userNotLoggedPanel = new JPanel(new GridBagLayout());
    private final JButton userNotLoggedLoginButton = new JButton("Login");
    private final JPanel userLoggedInPanel = new JPanel(new GridLayout(1, 1));
    private final JPanel userLoggedInCurrentUserPanel = new JPanel(new GridLayout(2, 1, margin, margin));
    private final String userLoggedInCurrentUserUsernameText = "Current user: ";
    private final JLabel userLoggedInCurrentUserUsernameLabel = new JLabel(userLoggedInCurrentUserUsernameText);
    private final JButton userLoggedInCurrentUserLogoutButton = new JButton("Logout");
    private final JButton infoButton = new JButton("Collection info");
    private final JButton addButton = new JButton("Add element");
    private final JButton removeButton = new JButton("Remove element");
    private final JButton removeLowerButton = new JButton("Remove lower elements");
    private final JButton removeGreaterButton = new JButton("Remove greater elements");

    private final Set<Listener> listeners = new HashSet<>();
    private boolean userLoggedIn = false;

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

        mapListListTable.setFont(GuiUtils.DEFAULT_FONT);
        mapListTabbedPane.add(mapListListTable, "List");
        mapListTabbedPane.setFont(GuiUtils.DEFAULT_FONT);
        contentPanePanel.add(mapListTabbedPane, new GridBagConstraints(0, 0, 1, 7, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, margin), 0, 0));

        userNotLoggedLoginButton.addActionListener(actionEvent -> sendEvent(Listener::userNotLoggedLoginButtonClicked));
        userNotLoggedLoginButton.setFont(GuiUtils.DEFAULT_BUTTON_FONT);
        userNotLoggedLoginButton.setMargin(GuiUtils.DEFAULT_BUTTON_MARGIN);
        userNotLoggedPanel.add(userNotLoggedLoginButton, new GridBagConstraints(0, 0, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, margin, 0), 0, 0));
        userNotLoggedPanel.add(new JSeparator(SwingConstants.HORIZONTAL), new GridBagConstraints(0, 1, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        userPanel.add(userNotLoggedPanel);

        userLoggedInCurrentUserUsernameLabel.setFont(GuiUtils.DEFAULT_FONT);
        userLoggedInCurrentUserUsernameLabel.setBorder(GuiUtils.DEFAULT_MARGIN_BORDER);
        userLoggedInCurrentUserPanel.add(userLoggedInCurrentUserUsernameLabel);

        userLoggedInCurrentUserLogoutButton.addActionListener(actionEvent -> sendEvent(Listener::userLoggedInCurrentUserLogoutButtonClicked));
        userLoggedInCurrentUserLogoutButton.setFont(GuiUtils.DEFAULT_BUTTON_FONT);
        userLoggedInCurrentUserLogoutButton.setMargin(GuiUtils.DEFAULT_BUTTON_MARGIN);
        userLoggedInCurrentUserPanel.add(userLoggedInCurrentUserLogoutButton);
        userLoggedInCurrentUserPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.
                createEtchedBorder(EtchedBorder.LOWERED), BorderFactory.createEmptyBorder(0, margin, margin, margin)));
        userLoggedInPanel.add(userLoggedInCurrentUserPanel);
        contentPanePanel.add(userPanel, new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, margin, 0), 0, 0));

        infoButton.addActionListener(actionEvent -> sendEvent(Listener::infoButtonClicked));
        infoButton.setFont(GuiUtils.DEFAULT_BUTTON_FONT);
        infoButton.setMargin(GuiUtils.DEFAULT_BUTTON_MARGIN);
        infoButton.setPreferredSize(infoButton.getMaximumSize());
        contentPanePanel.add(infoButton, new GridBagConstraints(1, 1, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, margin, 0), 0, 0));

        addButton.addActionListener(actionEvent -> sendEvent(Listener::addButtonClicked));
        addButton.setFont(GuiUtils.DEFAULT_BUTTON_FONT);
        addButton.setMargin(GuiUtils.DEFAULT_BUTTON_MARGIN);
        contentPanePanel.add(addButton, new GridBagConstraints(1, 2, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, margin, 0), 0, 0));

        removeButton.addActionListener(actionEvent -> sendEvent(Listener::removeButtonClicked));
        removeButton.setFont(GuiUtils.DEFAULT_BUTTON_FONT);
        removeButton.setMargin(GuiUtils.DEFAULT_BUTTON_MARGIN);
        contentPanePanel.add(removeButton, new GridBagConstraints(1, 3, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, margin, 0), 0, 0));

        removeLowerButton.addActionListener(actionEvent -> sendEvent(Listener::removeLowerButtonClicked));
        removeLowerButton.setFont(GuiUtils.DEFAULT_BUTTON_FONT);
        removeLowerButton.setMargin(GuiUtils.DEFAULT_BUTTON_MARGIN);
        contentPanePanel.add(removeLowerButton, new GridBagConstraints(1, 4, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, margin, 0), 0, 0));

        removeGreaterButton.addActionListener(actionEvent -> sendEvent(Listener::removeGreaterButtonClicked));
        removeGreaterButton.setFont(GuiUtils.DEFAULT_BUTTON_FONT);
        removeGreaterButton.setMargin(GuiUtils.DEFAULT_BUTTON_MARGIN);
        contentPanePanel.add(removeGreaterButton, new GridBagConstraints(1, 5, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        contentPanePanel.add(Box.createVerticalGlue(), new GridBagConstraints(1, 6, 1, 1, 0, 1, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        contentPanePanel.setBorder(BorderFactory.createEmptyBorder(margin, margin, margin, margin));
        setContentPane(contentPanePanel);
        setMinimumSize(new Dimension(850, 630));
        setLocationRelativeTo(null);
        pack();
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

    private void sendEvent(BiConsumer<Listener, Event> handler) {
        final ButtonModel selection = mainLanguageButtonGroup.getSelection();

        sendEvent(handler, new Event(this, selection == null ? "" : selection.getActionCommand()));
    }

    private void sendEvent(BiConsumer<Listener, Event> handler, Event event) {
        listeners.forEach(listener -> handler.accept(listener, event));
    }
}
