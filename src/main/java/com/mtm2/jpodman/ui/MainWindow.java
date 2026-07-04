package com.mtm2.jpodman.ui;

import com.mtm2.jpodman.AppPreferences;
import com.mtm2.jpodman.GameInstall;
import com.mtm2.jpodman.MonsterVersionInfo;
import com.mtm2.jpodman.PodListItem;
import com.mtm2.jpodman.PodMountList;
import com.mtm2.jpodman.io.MonsterExeDetector;
import com.mtm2.jpodman.io.PodDiscoveryService;
import com.mtm2.jpodman.io.PodDisplayNameResolver;
import com.mtm2.jpodman.io.PodIniReader;
import com.mtm2.jpodman.io.PodIniWriter;
import com.mtm2.jpodman.io.PodListExporter;
import com.mtm2.jpodman.io.PodMetadataService;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Main Swing window for managing mounted and available POD files. */
public final class MainWindow extends JFrame {
    private static final Dimension LIST_PANEL_MINIMUM_SIZE = new Dimension(300, 360);
    private static final Dimension CONTROL_PANEL_SIZE = new Dimension(128, 260);

    private AppPreferences preferences = AppPreferences.load();
    private Path gameRoot = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
    private GameInstall gameInstall = MonsterExeDetector.detect(gameRoot);
    private PodMountList mountedPods = PodMountList.empty();
    private final PodMetadataService metadataService = new PodMetadataService();

    private final DefaultListModel<PodListItem> mountedModel = new DefaultListModel<>();
    private final DefaultListModel<PodListItem> availableModel = new DefaultListModel<>();
    private List<PodListItem> allMountedItems = List.of();
    private List<PodListItem> allAvailableItems = List.of();
    private final JList<PodListItem> mountedList = new JList<>(mountedModel);
    private final JList<PodListItem> availableList = new JList<>(availableModel);
    private final JTextField mountedFilterField = new JTextField();
    private final JTextField availableFilterField = new JTextField();
    private final JLabel statusLabel = new JLabel("Ready");
    private final JLabel mountedCountLabel = new JLabel();
    private final JLabel availableCountLabel = new JLabel();
    private final JLabel gameLabel = new JLabel();
    private JMenuItem fontsAndSettingsItem;

    public MainWindow() {
        super("JPodman");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(840, 520));
        setLocationByPlatform(true);
        setAlwaysOnTop(preferences.keepWindowOnTop());

        mountedList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        availableList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        ListCellRenderer<? super PodListItem> renderer = new PodListItemRenderer();
        mountedList.setCellRenderer(renderer);
        availableList.setCellRenderer(renderer);
        installFilterListener(mountedFilterField, this::applyMountedFilter);
        installFilterListener(availableFilterField, this::applyAvailableFilter);
        mountedList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && selectClickedRow(mountedList, e)) {
                    removeSelectedMounted();
                }
            }
        });
        availableList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && selectClickedRow(availableList, e)) {
                    addSelectedAvailable();
                }
            }
        });

        buildMenuBar();
        add(buildContent(), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);

        updateStatus("Ready");
        pack();
    }

    public void initializeAfterStartup(Runnable onLoaded) {
        updateStatus("Loading POD lists...");
        new SwingWorker<StartupLoadResult, Void>() {
            @Override
            protected StartupLoadResult doInBackground() {
                return loadStartupData();
            }

            @Override
            protected void done() {
                try {
                    StartupLoadResult result = get();
                    mountedPods = result.mountedPods();
                    setMountedItemsFromPaths(mountedPods.entries());
                    setAvailableItemsFromPaths(result.availablePods());
                    updateStatus(result.statusMessage());
                    if (result.error() != null) {
                        JOptionPane.showMessageDialog(
                                MainWindow.this,
                                "pod.ini could not be read. Starting with an empty list:\n" + result.error().getMessage(),
                                "JPodman",
                                JOptionPane.WARNING_MESSAGE);
                    }
                } catch (Exception ex) {
                    mountedPods = PodMountList.empty();
                    setMountedItemsFromPaths(List.of());
                    setAvailableItemsFromPaths(List.of());
                    updateStatus("Started with empty POD lists.");
                } finally {
                    if (onLoaded != null) {
                        onLoaded.run();
                    }
                }
            }
        }.execute();
    }

    private StartupLoadResult loadStartupData() {
        PodMountList loaded = PodMountList.empty();
        IOException error = null;
        String status = Files.exists(gameInstall.podIniPath())
                ? "Opened " + gameInstall.podIniPath()
                : "No pod.ini found; a new one will be created on save.";
        try {
            loaded = PodIniReader.read(gameInstall.podIniPath(), preferences.podLimit());
        } catch (IOException ex) {
            error = ex;
            status = "Started with an empty POD list.";
        }
        List<String> available = PodDiscoveryService.discover(gameRoot, preferences.extraPodFolders(), preferences.folderDepth(), loaded);
        return new StartupLoadResult(loaded, available, status, error);
    }

    private JPanel buildContent() {
        JPanel mountedPanel = listPanel("Mounted PODs", mountedList, mountedCountLabel, mountedFilterField);
        JPanel availablePanel = listPanel("Available PODs", availableList, availableCountLabel, availableFilterField);

        JButton add = new JButton("<< Add");
        add.addActionListener(e -> addSelectedAvailable());
        JButton remove = new JButton("Remove >>");
        remove.addActionListener(e -> removeSelectedMounted());
        JButton up = new JButton("Up");
        up.addActionListener(e -> moveMounted(-1));
        JButton down = new JButton("Down");
        down.addActionListener(e -> moveMounted(1));
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> refreshAvailablePods());
        JButton podLists = new JButton("POD Lists");
        podLists.addActionListener(e -> showPodListManager());
        JPanel buttons = new JPanel(new GridLayout(0, 1, 4, 4));
        buttons.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        buttons.setPreferredSize(CONTROL_PANEL_SIZE);
        buttons.setMinimumSize(CONTROL_PANEL_SIZE);
        buttons.add(add);
        buttons.add(remove);
        buttons.add(up);
        buttons.add(down);
        buttons.add(refresh);
        buttons.add(podLists);

        JPanel buttonColumn = new JPanel(new GridBagLayout());
        buttonColumn.setPreferredSize(new Dimension(CONTROL_PANEL_SIZE.width, CONTROL_PANEL_SIZE.height));
        buttonColumn.setMinimumSize(new Dimension(CONTROL_PANEL_SIZE.width, 0));
        GridBagConstraints bc = new GridBagConstraints();
        bc.gridx = 0;
        bc.gridy = 0;
        bc.anchor = GridBagConstraints.CENTER;
        buttonColumn.add(buttons, bc);

        JPanel content = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.insets = new Insets(0, 0, 0, 0);

        c.gridx = 0;
        c.weightx = 0.5;
        content.add(mountedPanel, c);
        c.gridx = 1;
        c.weightx = 0;
        c.fill = GridBagConstraints.BOTH;
        content.add(buttonColumn, c);
        c.gridx = 2;
        c.weightx = 0.5;
        c.fill = GridBagConstraints.BOTH;
        content.add(availablePanel, c);
        return content;
    }

    private JPanel listPanel(String title, JList<PodListItem> list, JLabel countLabel, JTextField filterField) {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createTitledBorder(title));
        panel.setMinimumSize(LIST_PANEL_MINIMUM_SIZE);
        panel.setPreferredSize(LIST_PANEL_MINIMUM_SIZE);
        JPanel filterPanel = new JPanel(new BorderLayout(4, 0));
        filterPanel.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        filterPanel.add(new JLabel("Filter:"), BorderLayout.WEST);
        filterPanel.add(filterField, BorderLayout.CENTER);
        panel.add(filterPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(list), BorderLayout.CENTER);
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT));
        footer.add(countLabel);
        panel.add(footer, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildStatusBar() {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        panel.add(statusLabel, BorderLayout.CENTER);
        panel.add(gameLabel, BorderLayout.EAST);
        updateCounts();
        return panel;
    }

    private void buildMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu file = new JMenu("File");
        file.add(menuItem("Open Game Folder...", this::chooseGameFolder));
        file.add(menuItem("Open pod.ini...", this::openPodIni));
        file.add(menuItem("Save pod.ini", this::savePodIni));
        file.add(menuItem("Save pod.ini As...", this::savePodIniAs));
        file.addSeparator();
        file.add(menuItem("Export POD List...", this::exportPodList));
        file.addSeparator();
        file.add(menuItem("Save and Launch", () -> {
            if (savePodIni()) {
                launchGame();
            }
        }));
        file.add(menuItem("Save and Exit", () -> {
            if (savePodIni()) {
                dispose();
            }
        }));
        file.add(menuItem("Exit", this::dispose));

        JMenu tools = new JMenu("Tools");
        tools.add(menuItem("Refresh", this::refreshAvailablePods));
        tools.add(menuItem("Restore Stock PODs", () -> restoreStockPods(false)));
        tools.add(menuItem("Restore Minimal Stock PODs", () -> restoreStockPods(true)));
        tools.add(menuItem("Sort Mounted PODs", this::sortMountedPods));
        tools.addSeparator();
        tools.add(menuItem("Pod List Manager...", this::showPodListManager));
        JMenuItem registryItem = menuItem("Registry Info...", this::showRegistryInfo);
        registryItem.setEnabled(isWindows());
        registryItem.setToolTipText(isWindows() ? "View/reset MTM registry keys" : "Registry reset is only available on Windows");
        tools.add(registryItem);
        fontsAndSettingsItem = menuItem("Fonts & Settings...", this::showFontsAndSettings);
        tools.add(fontsAndSettingsItem);
        tools.addSeparator();
        tools.add(menuItem("Preferences...", this::showPreferences));
        updateMenuAvailability();

        JMenu help = new JMenu("Help");
        help.add(menuItem("About JPodman...", this::showAbout));

        bar.add(file);
        bar.add(tools);
        bar.add(help);
        setJMenuBar(bar);
    }

    private static JMenuItem menuItem(String label, Runnable action) {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(e -> action.run());
        return item;
    }

    private void chooseGameFolder() {
        JFileChooser chooser = new JFileChooser(gameRoot.toFile());
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Choose MTM Folder");
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        gameRoot = chooser.getSelectedFile().toPath().toAbsolutePath().normalize();
        gameInstall = MonsterExeDetector.detect(gameRoot);
        updateMenuAvailability();
        metadataService.clear();
        loadPodIni(gameInstall.podIniPath(), false);
        refreshAvailablePods();
        updateStatus("Opened " + gameRoot);
    }

    private void openPodIni() {
        JFileChooser chooser = iniChooser("Open pod.ini");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            Path selected = chooser.getSelectedFile().toPath().toAbsolutePath().normalize();
            gameRoot = selected.getParent() == null ? gameRoot : selected.getParent();
            gameInstall = MonsterExeDetector.detect(gameRoot);
            updateMenuAvailability();
            metadataService.clear();
            loadPodIni(selected, true);
            refreshAvailablePods();
        }
    }

    private boolean savePodIni() {
        try {
            syncModelToMounted();
            PodIniWriter.write(gameInstall.podIniPath(), mountedPods);
            updateStatus("Saved " + gameInstall.podIniPath());
            return true;
        } catch (IOException ex) {
            warnNotSaved(ex);
            return false;
        }
    }

    private void savePodIniAs() {
        JFileChooser chooser = iniChooser("Save pod.ini As");
        chooser.setSelectedFile(gameInstall.podIniPath().toFile());
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                syncModelToMounted();
                PodIniWriter.write(ensureExtension(chooser.getSelectedFile().toPath(), ".ini"), mountedPods);
                updateStatus("Saved " + chooser.getSelectedFile());
            } catch (IOException ex) {
                warnNotSaved(ex);
            }
        }
    }

    private void exportPodList() {
        JFileChooser chooser = new JFileChooser(gameRoot.toFile());
        chooser.setDialogTitle("Export POD List");
        chooser.setFileFilter(new FileNameExtensionFilter("Text Files (*.txt)", "txt"));
        chooser.setSelectedFile(gameRoot.resolve("podlist.txt").toFile());
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                syncModelToMounted();
                PodListExporter.export(ensureExtension(chooser.getSelectedFile().toPath(), ".txt"), mountedPods);
                updateStatus("Exported POD list");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "The POD list could not be exported:\n" + ex.getMessage(), "JPodman", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void launchGame() {
        if (gameInstall.executable().isEmpty()) {
            JOptionPane.showMessageDialog(this, "No monster.exe or monsterx.exe was found in the current folder.", "JPodman", JOptionPane.WARNING_MESSAGE);
            return;
        }
        syncModelToMounted();
        if (!confirmVersionSuggestedLimit()) {
            return;
        }
        try {
            Desktop.getDesktop().open(gameInstall.executable().get().toFile());
        } catch (IOException | UnsupportedOperationException ex) {
            JOptionPane.showMessageDialog(this, "The game could not be launched:\n" + ex.getMessage(), "JPodman", JOptionPane.WARNING_MESSAGE);
        }
    }

    private boolean confirmVersionSuggestedLimit() {
        Optional<Integer> warningLimit = gameInstall.warningPodLimit();
        if (warningLimit.isEmpty() || mountedPods.size() <= warningLimit.get()) {
            return true;
        }
        String source = gameInstall.monsterIniPodLimit().isPresent()
                ? "system/monster.ini podLimit"
                : gameInstall.versionInfo().map(MonsterVersionInfo::displayLabel).orElse("detected game version");
        int result = JOptionPane.showConfirmDialog(
                this,
                "Detected " + source + ".\n"
                        + "This setup reports about " + warningLimit.get() + " mounted PODs, "
                        + "but your current list has " + mountedPods.size() + ".\n\n"
                        + "JPodman will not change your configured limit. Launch anyway?",
                "POD Limit Warning",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);
        return result == JOptionPane.OK_OPTION;
    }

    private void showPreferences() {
        PreferencesDialog dialog = new PreferencesDialog(this, preferences);
        dialog.setVisible(true);
        if (!dialog.wasConfirmed()) {
            return;
        }
        preferences = dialog.preferences();
        try {
            preferences.save();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Preferences could not be saved:\n" + ex.getMessage(), "JPodman", JOptionPane.WARNING_MESSAGE);
        }
        setAlwaysOnTop(preferences.keepWindowOnTop());
        refreshDisplayedPodLabels();
        trimMountedToLimit();
        if (preferences.sortMountedPods()) {
            sortMountedPods();
        }
        refreshAvailablePods();
    }

    private void showPodListManager() {
        PodListManagerDialog dialog = new PodListManagerDialog(this);
        dialog.setVisible(true);
    }

    private void showAbout() {
        new AboutDialog(this).setVisible(true);
    }

    Path gameRootPath() {
        return gameRoot;
    }

    AppPreferences preferencesSnapshot() {
        return preferences;
    }

    List<PodListItem> knownPodItems() {
        List<PodListItem> combined = new ArrayList<>();
        combined.addAll(allMountedItems);
        combined.addAll(allAvailableItems);
        return List.copyOf(combined);
    }

    List<PodListItem> mountedPodItems() {
        return refreshDisplayLabels(allMountedItems);
    }

    List<PodListItem> availablePodItems() {
        return refreshDisplayLabels(allAvailableItems);
    }

    PodListItem podListItemFor(String mountPath) {
        for (PodListItem item : knownPodItems()) {
            if (normalizeMountPath(item.mountPath()).equals(normalizeMountPath(mountPath))) {
                return new PodListItem(item.mountPath(), displayLabelForPath(item.mountPath()));
            }
        }
        return new PodListItem(mountPath, PodDisplayNameResolver.missingLabel(mountPath));
    }

    void updatePreferencesFromDialog(AppPreferences updated) throws IOException {
        preferences = updated;
        preferences.save();
    }

    void reloadGamePodIniFromDialog() {
        loadPodIni(gameInstall.podIniPath(), false);
        refreshAvailablePods();
    }

    private void showRegistryInfo() {
        if (!isWindows()) {
            JOptionPane.showMessageDialog(this, "Registry reset is only available on Windows.", "JPodman", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try {
            Class<?> dialogClass = Class.forName("com.mtm2.jpodman.ui.RegistryDialog");
            JDialog dialog = (JDialog) dialogClass
                    .getConstructor(java.awt.Frame.class, GameInstall.class)
                    .newInstance(this, gameInstall);
            dialog.setVisible(true);
        } catch (NoClassDefFoundError ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Registry support requires the JNA runtime libraries.\n"
                            + "Run the packaged jar from target/jpodman.jar, or include Maven runtime dependencies on the classpath.",
                    "JPodman",
                    JOptionPane.WARNING_MESSAGE);
        } catch (ReflectiveOperationException ex) {
            JOptionPane.showMessageDialog(this, "Registry dialog could not be opened:\n" + ex.getMessage(), "JPodman", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void showFontsAndSettings() {
        if (!Files.isRegularFile(gameInstall.monsterIniPath())) {
            JOptionPane.showMessageDialog(this, "system/monster.ini was not found in the current game folder.", "JPodman", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        FontsAndSettingsDialog dialog = new FontsAndSettingsDialog(this, gameInstall, () -> {
            gameInstall = MonsterExeDetector.detect(gameRoot);
            updateCounts();
        });
        dialog.setVisible(true);
    }

    private void updateMenuAvailability() {
        if (fontsAndSettingsItem != null) {
            boolean exists = Files.isRegularFile(gameInstall.monsterIniPath());
            fontsAndSettingsItem.setEnabled(exists);
            fontsAndSettingsItem.setToolTipText(exists ? "Edit system/monster.ini fonts and settings" : "system/monster.ini was not found");
        }
    }

    private void loadPodIni(Path path, boolean imported) {
        try {
            mountedPods = PodIniReader.read(path, preferences.podLimit());
            reloadMountedModel();
            if (Files.exists(path)) {
                updateStatus((imported ? "Loaded " : "Opened ") + path);
            } else {
                updateStatus("No pod.ini found; a new one will be created on save.");
            }
        } catch (IOException ex) {
            mountedPods = PodMountList.empty();
            reloadMountedModel();
            JOptionPane.showMessageDialog(this, "pod.ini could not be read. Starting with an empty list:\n" + ex.getMessage(), "JPodman", JOptionPane.WARNING_MESSAGE);
        }
    }

    void refreshAvailablePods() {
        syncModelToMounted();
        List<String> discovered = PodDiscoveryService.discover(gameRoot, preferences.extraPodFolders(), preferences.folderDepth(), mountedPods);
        setAvailableItemsFromPaths(discovered);
        updateCounts();
        updateStatus("Refreshed available PODs");
    }

    private void addSelectedAvailable() {
        List<PodListItem> selected = availableList.getSelectedValuesList();
        if (selected.isEmpty()) {
            return;
        }
        syncModelToMounted();
        for (PodListItem item : selected) {
            String entry = item.mountPath();
            if (mountedPods.isFull(preferences.podLimit())) {
                JOptionPane.showMessageDialog(this, "The configured POD limit is " + preferences.podLimit() + ".", "JPodman", JOptionPane.INFORMATION_MESSAGE);
                break;
            }
            Path actualPath = PodDiscoveryService.resolveMountedPath(gameRoot, entry);
            if (!Files.isRegularFile(actualPath) || !PodDiscoveryService.isAcceptablePodPath(entry)) {
                continue;
            }
            mountedPods.add(entry, preferences.podLimit());
        }
        reloadMountedModel();
        refreshAvailablePods();
    }

    private void removeSelectedMounted() {
        List<PodListItem> selected = mountedList.getSelectedValuesList();
        if (selected.isEmpty()) {
            return;
        }
        syncModelToMounted();
        selected.forEach(item -> mountedPods.remove(item.mountPath()));
        reloadMountedModel();
        refreshAvailablePods();
    }

    private void restoreStockPods(boolean minimal) {
        mountedPods = PodMountList.empty();
        for (String entry : PodMountList.stockEntries(minimal)) {
            String stock = entry;
            if (!Files.isRegularFile(PodDiscoveryService.resolveMountedPath(gameRoot, stock))) {
                stock = entry.replace("Fixes/", "Stock/");
            }
            if (!Files.isRegularFile(PodDiscoveryService.resolveMountedPath(gameRoot, stock))) {
                stock = Path.of(entry).getFileName().toString();
            }
            mountedPods.add(stock, preferences.podLimit());
        }
        reloadMountedModel();
        refreshAvailablePods();
    }

    private void sortMountedPods() {
        syncModelToMounted();
        mountedPods.sortCaseInsensitive();
        reloadMountedModel();
    }

    private void moveMounted(int direction) {
        PodListItem selected = mountedList.getSelectedValue();
        if (selected == null) {
            return;
        }
        int index = indexOfMountPath(allMountedItems, selected.mountPath());
        int target = index + direction;
        if (index < 0 || target < 0 || target >= allMountedItems.size()) {
            return;
        }
        List<PodListItem> reordered = new ArrayList<>(allMountedItems);
        PodListItem value = reordered.remove(index);
        reordered.add(target, value);
        allMountedItems = List.copyOf(reordered);
        applyMountedFilter();
        selectMountPath(selected.mountPath());
        syncModelToMounted();
        updateCounts();
    }

    private void reloadMountedModel() {
        setMountedItemsFromPaths(mountedPods.entries());
        updateCounts();
    }

    private void syncModelToMounted() {
        PodMountList updated = PodMountList.empty();
        for (PodListItem item : allMountedItems) {
            updated.add(item.mountPath(), preferences.podLimit());
        }
        mountedPods = updated;
    }

    private void setMountedItemsFromPaths(List<String> paths) {
        allMountedItems = plainItems(paths);
        applyMountedFilter();
        decorateItems(allMountedItems, items -> {
            allMountedItems = items;
            applyMountedFilter();
        });
    }

    private void setAvailableItemsFromPaths(List<String> paths) {
        allAvailableItems = plainItems(paths);
        applyAvailableFilter();
        decorateItems(allAvailableItems, items -> {
            allAvailableItems = items;
            applyAvailableFilter();
        });
    }

    private List<PodListItem> plainItems(List<String> paths) {
        List<PodListItem> items = new ArrayList<>();
        for (String path : paths) {
            items.add(PodListItem.plain(path));
        }
        return List.copyOf(items);
    }

    private void decorateItems(List<PodListItem> source, java.util.function.Consumer<List<PodListItem>> onDone) {
        List<PodListItem> snapshot = List.copyOf(source);
        new SwingWorker<List<PodListItem>, Void>() {
            @Override
            protected List<PodListItem> doInBackground() {
                List<PodListItem> decorated = new ArrayList<>(snapshot.size());
                for (PodListItem item : snapshot) {
                    decorated.add(new PodListItem(item.mountPath(), displayLabelForPath(item.mountPath())));
                }
                return decorated;
            }

            @Override
            protected void done() {
                try {
                    List<PodListItem> decorated = get();
                    onDone.accept(List.copyOf(decorated));
                } catch (Exception ignored) {
                    // Plain labels are already visible; metadata is best effort.
                }
            }
        }.execute();
    }

    private void refreshDisplayedPodLabels() {
        allMountedItems = refreshDisplayLabels(allMountedItems);
        allAvailableItems = refreshDisplayLabels(allAvailableItems);
        applyMountedFilter();
        applyAvailableFilter();
    }

    private List<PodListItem> refreshDisplayLabels(List<PodListItem> items) {
        List<PodListItem> refreshed = new ArrayList<>();
        for (PodListItem item : items) {
            String existingLabel = item.displayLabel();
            if (existingLabel.endsWith(" [missing]")) {
                refreshed.add(item);
            } else {
                refreshed.add(new PodListItem(item.mountPath(), displayLabelForPath(item.mountPath())));
            }
        }
        return List.copyOf(refreshed);
    }

    private String displayLabelForPath(String mountPath) {
        Path podPath = PodDiscoveryService.resolveMountedPath(gameRoot, mountPath);
        return PodDisplayNameResolver.displayLabel(
                mountPath,
                metadataService.metadataFor(podPath),
                isSystemPod(mountPath));
    }

    private boolean isSystemPod(String mountPath) {
        String normalized = normalizeMountPath(mountPath);
        String fileName = normalizeFileName(mountPath);
        for (String systemPod : preferences.systemPodFiles()) {
            if (normalized.equals(normalizeMountPath(systemPod)) || fileName.equals(normalizeFileName(systemPod))) {
                return true;
            }
        }
        return false;
    }

    private void applyMountedFilter() {
        applyFilter(allMountedItems, mountedModel, mountedFilterField.getText());
        updateCounts();
    }

    private void applyAvailableFilter() {
        applyFilter(allAvailableItems, availableModel, availableFilterField.getText());
        updateCounts();
    }

    private void applyFilter(List<PodListItem> source, DefaultListModel<PodListItem> model, String filterText) {
        String filter = filterText == null ? "" : filterText.trim().toLowerCase(Locale.ROOT);
        model.clear();
        for (PodListItem item : source) {
            if (filter.isEmpty()
                    || item.mountPath().toLowerCase(Locale.ROOT).contains(filter)
                    || item.displayLabel().toLowerCase(Locale.ROOT).contains(filter)) {
                model.addElement(item);
            }
        }
    }

    private void installFilterListener(JTextField field, Runnable action) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                action.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                action.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                action.run();
            }
        });
    }

    private void trimMountedToLimit() {
        syncModelToMounted();
        if (mountedPods.size() <= preferences.podLimit()) {
            return;
        }
        PodMountList trimmed = PodMountList.empty();
        for (String entry : mountedPods.entries()) {
            trimmed.add(entry, preferences.podLimit());
        }
        mountedPods = trimmed;
        reloadMountedModel();
    }

    private void updateCounts() {
        mountedCountLabel.setText(allMountedItems.size() + " of " + preferences.podLimit()
                + filteredSuffix(mountedModel.size(), allMountedItems.size()));
        availableCountLabel.setText(Integer.toString(allAvailableItems.size())
                + filteredSuffix(availableModel.size(), allAvailableItems.size()));
        gameLabel.setText(gameInstall.versionLabel() + warningPodLimitSuffix());
    }

    private void updateStatus(String message) {
        statusLabel.setText(message);
        updateCounts();
    }

    private int indexOfMountPath(List<PodListItem> items, String mountPath) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).mountPath().equals(mountPath)) {
                return i;
            }
        }
        return -1;
    }

    private void selectMountPath(String mountPath) {
        for (int i = 0; i < mountedModel.size(); i++) {
            if (mountedModel.get(i).mountPath().equals(mountPath)) {
                mountedList.setSelectedIndex(i);
                mountedList.ensureIndexIsVisible(i);
                return;
            }
        }
    }

    private String filteredSuffix(int visible, int total) {
        return visible == total ? "" : " (" + visible + " shown)";
    }

    private String warningPodLimitSuffix() {
        return gameInstall.warningPodLimit()
                .map(limit -> " (pod limit: " + limit + ")")
                .orElse("");
    }

    private void warnNotSaved(IOException ex) {
        JOptionPane.showMessageDialog(
                this,
                "pod.ini could not be saved, so no data was persisted.\n" + ex.getMessage(),
                "JPodman",
                JOptionPane.WARNING_MESSAGE);
    }

    private JFileChooser iniChooser(String title) {
        JFileChooser chooser = new JFileChooser(gameRoot.toFile());
        chooser.setDialogTitle(title);
        chooser.setFileFilter(new FileNameExtensionFilter("POD INI Files (*.ini)", "ini"));
        return chooser;
    }

    private static Path ensureExtension(Path path, String extension) {
        String text = path.toString();
        return text.toLowerCase().endsWith(extension) ? path : Path.of(text + extension);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private static boolean selectClickedRow(JList<?> list, java.awt.event.MouseEvent event) {
        int index = list.locationToIndex(event.getPoint());
        if (index < 0) {
            return false;
        }
        java.awt.Rectangle bounds = list.getCellBounds(index, index);
        if (bounds == null || !bounds.contains(event.getPoint())) {
            return false;
        }
        list.setSelectedIndex(index);
        return true;
    }

    private static String normalizeMountPath(String value) {
        return value == null ? "" : value.trim().replace('\\', '/').toLowerCase(Locale.ROOT);
    }

    private static String normalizeFileName(String value) {
        String normalized = normalizeMountPath(value);
        int slash = normalized.lastIndexOf('/');
        return slash < 0 ? normalized : normalized.substring(slash + 1);
    }

    private record StartupLoadResult(
            PodMountList mountedPods,
            List<String> availablePods,
            String statusMessage,
            IOException error) {}

}
