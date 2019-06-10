package ru.byprogminer.Lab8_Programming.gui;

import javax.swing.*;
import java.awt.*;

@SuppressWarnings("FieldCanBeLocal")
public class MainWindow extends JFrame {

    private final String name;
    private final int margin = 5;

    private JMenuBar mainMenuBar = new JMenuBar();
    private JMenu mainFileMenu = new JMenu("File");
    private JMenuItem mainFileLoadMenuItem = new JMenuItem("Load");
    private JMenuItem mainFileSaveMenuItem = new JMenuItem("Save");
    private JMenuItem mainFileImportMenuItem = new JMenuItem("Import");
    private JMenuItem mainFileUsersMenuItem = new JMenuItem("Users");
    private JMenuItem mainFileExitMenuItem = new JMenuItem("Exit");
    private JMenuItem mainAboutMenuItem = new JMenuItem("About");

    private JPanel contentPanePanel = new JPanel(new GridBagLayout());
    private JTabbedPane mapListTabbedPane = new JTabbedPane();
    private ElementsMap mapListMapElementsMap = new ElementsMap();
    private JTable mapListListTable = new JTable();
    private JPanel userPanel = new JPanel(new GridLayout(1, 1));
    private JPanel userNotLoggedPanel = new JPanel(new GridLayout(1, 1));
    private JButton userNotLoggedLoginButton = new JButton("Login");
    private JPanel userLoggedInPanel = new JPanel(new GridLayout(1, 1));
    private JPanel userLoggedInCurrentUserPanel = new JPanel(new GridLayout(3, 1, margin, margin));
    private String userLoggedInCurrentUserUsernameText = "Current user: ";
    private JLabel userLoggedInCurrentUserUsernameLabel = new JLabel(userLoggedInCurrentUserUsernameText);
    private JButton userLoggedInCurrentUserLogoutButton = new JButton("Logout");
    private JButton infoButton = new JButton("Collection info");
    private JButton addButton = new JButton("Add element");
    private JButton removeButton = new JButton("Remove element");
    private JButton removeLowerButton = new JButton("Remove lower elements");
    private JButton removeGreaterButton = new JButton("Remove greater elements");

    public MainWindow(String name) {
        super(name);

        this.name = name;

        // mainFileLoadMenuItem.addActionListener();
        mainFileLoadMenuItem.setFont(GuiUtils.DEFAULT_FONT);
        mainFileMenu.add(mainFileLoadMenuItem);

        // mainFileSaveMenuItem.addActionListener();
        mainFileSaveMenuItem.setFont(GuiUtils.DEFAULT_FONT);
        mainFileMenu.add(mainFileSaveMenuItem);

        // mainFileImportMenuItem.addActionListener();
        mainFileImportMenuItem.setFont(GuiUtils.DEFAULT_FONT);
        mainFileMenu.add(mainFileImportMenuItem);
        mainFileMenu.addSeparator();

        // mainFileUsersMenuItem.addActionListener();
        mainFileUsersMenuItem.setFont(GuiUtils.DEFAULT_FONT);
        mainFileMenu.add(mainFileUsersMenuItem);
        mainFileMenu.addSeparator();

        // mainFileExitMenuItem.addActionListener();
        mainFileExitMenuItem.setFont(GuiUtils.DEFAULT_FONT);
        mainFileMenu.add(mainFileExitMenuItem);
        mainFileMenu.setFont(GuiUtils.DEFAULT_FONT);
        mainMenuBar.add(mainFileMenu);

        // mainAboutMenuItem.addActionListener();
        mainAboutMenuItem.setFont(GuiUtils.DEFAULT_FONT);
        mainMenuBar.add(mainAboutMenuItem);
        setJMenuBar(mainMenuBar);

        mapListMapElementsMap.setFont(GuiUtils.DEFAULT_FONT);
        mapListTabbedPane.add(mapListMapElementsMap, "Map");

        mapListListTable.setFont(GuiUtils.DEFAULT_FONT);
        mapListTabbedPane.add(mapListListTable, "List");
        mapListTabbedPane.setFont(GuiUtils.DEFAULT_FONT);
        contentPanePanel.add(mapListTabbedPane, new GridBagConstraints(0, 0, 1, 8, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, margin), 0, 0));

        // userNotLoggedLoginButton.addActionListener();
        userNotLoggedLoginButton.setFont(GuiUtils.DEFAULT_BUTTON_FONT);
        userNotLoggedLoginButton.setMargin(GuiUtils.DEFAULT_BUTTON_MARGIN);
        userNotLoggedPanel.add(userNotLoggedLoginButton);
        userPanel.add(userNotLoggedPanel);

        userLoggedInCurrentUserUsernameLabel.setFont(GuiUtils.DEFAULT_FONT);
        userLoggedInCurrentUserPanel.add(userLoggedInCurrentUserUsernameLabel);

        // userLoggedInCurrentUserLogoutButton.addActionListener();
        userLoggedInCurrentUserLogoutButton.setFont(GuiUtils.DEFAULT_BUTTON_FONT);
        userLoggedInCurrentUserLogoutButton.setMargin(GuiUtils.DEFAULT_BUTTON_MARGIN);
        userLoggedInCurrentUserPanel.add(userLoggedInCurrentUserLogoutButton);
        userLoggedInCurrentUserPanel.setBorder(BorderFactory.createLoweredSoftBevelBorder());
        userLoggedInPanel.add(userLoggedInCurrentUserPanel);
        contentPanePanel.add(userPanel, new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, margin, 0), 0, 0));
        contentPanePanel.add(new JSeparator(SwingConstants.HORIZONTAL), new GridBagConstraints(1, 1, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, margin, 0), 0, 0));

        // infoButton.addActionListener();
        infoButton.setFont(GuiUtils.DEFAULT_BUTTON_FONT);
        infoButton.setMargin(GuiUtils.DEFAULT_BUTTON_MARGIN);
        infoButton.setPreferredSize(infoButton.getMaximumSize());
        contentPanePanel.add(infoButton, new GridBagConstraints(1, 2, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, margin, 0), 0, 0));

        // addButton.addActionListener();
        addButton.setFont(GuiUtils.DEFAULT_BUTTON_FONT);
        addButton.setMargin(GuiUtils.DEFAULT_BUTTON_MARGIN);
        contentPanePanel.add(addButton, new GridBagConstraints(1, 3, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, margin, 0), 0, 0));

        // removeButton.addActionListener();
        removeButton.setFont(GuiUtils.DEFAULT_BUTTON_FONT);
        removeButton.setMargin(GuiUtils.DEFAULT_BUTTON_MARGIN);
        contentPanePanel.add(removeButton, new GridBagConstraints(1, 4, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, margin, 0), 0, 0));

        // removeLowerButton.addActionListener();
        removeLowerButton.setFont(GuiUtils.DEFAULT_BUTTON_FONT);
        removeLowerButton.setMargin(GuiUtils.DEFAULT_BUTTON_MARGIN);
        contentPanePanel.add(removeLowerButton, new GridBagConstraints(1, 5, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, margin, 0), 0, 0));

        // removeGreaterButton.addActionListener();
        removeGreaterButton.setFont(GuiUtils.DEFAULT_BUTTON_FONT);
        removeGreaterButton.setMargin(GuiUtils.DEFAULT_BUTTON_MARGIN);
        contentPanePanel.add(removeGreaterButton, new GridBagConstraints(1, 6, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        contentPanePanel.add(Box.createVerticalGlue(), new GridBagConstraints(1, 7, 1, 1, 0, 1, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        contentPanePanel.setBorder(BorderFactory.createEmptyBorder(margin, margin, margin, margin));
        setContentPane(contentPanePanel);
        setMinimumSize(new Dimension(850, 630));
        setLocationRelativeTo(null);
        pack();
    }
}
