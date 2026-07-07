package com.mtm2.jpodman.ui;

import com.mtm2.jpodman.AppPreferences;
import com.mtm2.jpodman.PodListItem;
import com.mtm2.jpodman.SavedPodList;
import com.mtm2.jpodman.io.SavedPodListService;
import com.mtm2.jpodman.io.SavedPodListService.ValidationResult;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Modal, resizable manager for saved POD lists. */
public final class PodListManagerDialog extends JDialog {
    private static final Dimension LIST_PANEL_SIZE = new Dimension(300, 360);
    private static final Dimension CONTROL_PANEL_SIZE = new Dimension(132, 260);

    private final MainWindow owner;
    private final DefaultListModel<SavedPodList> savedListsModel = new DefaultListModel<>();
    private final JList<SavedPodList> savedLists = new JList<>(savedListsModel);
    private final DefaultListModel<PodListItem> editorModel = new DefaultListModel<>();
    private final JList<PodListItem> editorList = new JList<>(editorModel);
    private final DefaultListModel<PodListItem> availableModel = new DefaultListModel<>();
    private final JList<PodListItem> availableList = new JList<>(availableModel);
    private final JTextField editorFilterField = new JTextField();
    private final JTextField availableFilterField = new JTextField();
    private final JCheckBox includeMinimalSystemPods = new JCheckBox("Always include minimal system PODs");
    private final JLabel validationLabel = new JLabel("Select or import a POD list.");
    private List<PodListItem> allEditorItems = List.of();
    private List<PodListItem> allAvailableItems = List.of();
    private AppPreferences preferences;

    public PodListManagerDialog(MainWindow owner) {
        super(owner, "POD List Manager", true);
        this.owner = owner;
        this.preferences = owner.preferencesSnapshot();

        savedLists.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        savedLists.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSelectedList();
            }
        });
        editorList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        availableList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        includeMinimalSystemPods.setSelected(preferences.includeMinimalSystemPodsOnUse());
        includeMinimalSystemPods.addActionListener(e -> {
            preferences = preferences.withIncludeMinimalSystemPodsOnUse(includeMinimalSystemPods.isSelected());
            persistPreferences();
        });
        PodListItemRenderer renderer = new PodListItemRenderer();
        editorList.setCellRenderer(renderer);
        availableList.setCellRenderer(renderer);
        installFilterListener(editorFilterField, this::applyEditorFilter);
        installFilterListener(availableFilterField, this::applyAvailableFilter);
        editorList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && selectClickedRow(editorList, e)) {
                    removeSelectedEntries();
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

        setLayout(new BorderLayout(8, 8));
        add(buildListsPanel(), BorderLayout.WEST);
        add(buildEditorPanel(), BorderLayout.CENTER);
        add(buildActionsPanel(), BorderLayout.SOUTH);

        loadSavedLists();
        reloadAvailablePodList();
        setMinimumSize(new Dimension(940, 520));
        setSize(new Dimension(1080, 640));
        setResizable(true);
        setLocationRelativeTo(owner);
    }

    private JPanel buildListsPanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createTitledBorder("Saved Lists"));
        panel.setPreferredSize(new Dimension(260, 420));
        panel.add(new JScrollPane(savedLists), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new GridLayout(1, 3, 4, 0));
        buttons.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        buttons.add(button("New", this::newList));
        buttons.add(button("Rename", this::renameList));
        buttons.add(button("Delete", this::deleteList));
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildEditorPanel() {
        JPanel editorPanel = new JPanel(new BorderLayout(4, 4));
        editorPanel.setBorder(BorderFactory.createTitledBorder("List Editor"));

        JPanel lists = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.insets = new Insets(0, 0, 0, 0);

        c.gridx = 0;
        c.weightx = 0.5;
        lists.add(listPanel("List PODs", editorList, editorFilterField, null), c);
        c.gridx = 1;
        c.weightx = 0;
        lists.add(buttonColumn(), c);
        c.gridx = 2;
        c.weightx = 0.5;
        lists.add(listPanel("Available PODs", availableList, availableFilterField, button("Add...", this::addPodFilesFromChooser)), c);

        JPanel footer = new JPanel(new BorderLayout(8, 0));
        validationLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        JPanel footerButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        footerButtons.add(button("Add current POD files", this::addCurrentPodFiles));
        footerButtons.add(includeMinimalSystemPods);
        footer.add(footerButtons, BorderLayout.WEST);
        footer.add(validationLabel, BorderLayout.CENTER);

        editorPanel.add(lists, BorderLayout.CENTER);
        editorPanel.add(footer, BorderLayout.SOUTH);
        return editorPanel;
    }

    private JPanel listPanel(String title, JList<PodListItem> list, JTextField filterField, JButton extraFilterButton) {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createTitledBorder(title));
        panel.setMinimumSize(LIST_PANEL_SIZE);
        panel.setPreferredSize(LIST_PANEL_SIZE);
        JPanel filterPanel = new JPanel(new BorderLayout(4, 0));
        filterPanel.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        filterPanel.add(new JLabel("Filter:"), BorderLayout.WEST);
        filterPanel.add(filterField, BorderLayout.CENTER);
        if (extraFilterButton != null) {
            filterPanel.add(extraFilterButton, BorderLayout.EAST);
        }
        panel.add(filterPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(list), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buttonColumn() {
        JButton add = new JButton("<< Add");
        add.addActionListener(e -> addSelectedAvailable());
        JButton remove = new JButton("Remove >>");
        remove.addActionListener(e -> removeSelectedEntries());
        JButton up = new JButton("Up");
        up.addActionListener(e -> moveSelectedEntry(-1));
        JButton down = new JButton("Down");
        down.addActionListener(e -> moveSelectedEntry(1));
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> refreshAvailablePods());

        JPanel buttons = new JPanel(new GridLayout(0, 1, 4, 4));
        buttons.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        buttons.setPreferredSize(CONTROL_PANEL_SIZE);
        buttons.setMinimumSize(CONTROL_PANEL_SIZE);
        buttons.add(add);
        buttons.add(remove);
        buttons.add(up);
        buttons.add(down);
        buttons.add(refresh);

        JPanel column = new JPanel(new GridBagLayout());
        column.setPreferredSize(new Dimension(CONTROL_PANEL_SIZE.width, CONTROL_PANEL_SIZE.height));
        column.setMinimumSize(new Dimension(CONTROL_PANEL_SIZE.width, 0));
        column.add(buttons, new GridBagConstraints());
        return column;
    }

    private JPanel buildActionsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panel.add(button("Import pod.ini...", this::importPodIni));
        panel.add(button("Save List", this::saveSelectedList));
        panel.add(button("Use This List", this::useSelectedList));
        panel.add(button("Close", this::dispose));
        return panel;
    }

    private JButton button(String label, Runnable action) {
        JButton button = new JButton(label);
        button.addActionListener(e -> action.run());
        return button;
    }

    private void loadSavedLists() {
        savedListsModel.clear();
        for (SavedPodList list : preferences.savedPodLists()) {
            savedListsModel.addElement(list);
        }
        if (!preferences.savedPodLists().isEmpty()) {
            savedLists.setSelectedIndex(0);
        }
    }

    private void loadSelectedList() {
        allEditorItems = List.of();
        applyEditorFilter();
        SavedPodList selected = savedLists.getSelectedValue();
        if (selected == null) {
            updateValidationLabel();
            reloadAvailablePodList();
            return;
        }
        replaceEditorEntries(SavedPodListService.dedupe(selected.entries()));
    }

    private void reloadAvailablePodList() {
        List<String> selected = currentEntries();
        List<PodListItem> available = new ArrayList<>();
        for (PodListItem item : owner.availablePodItems()) {
            if (!containsMountPath(selected, item.mountPath())) {
                available.add(item);
            }
        }
        allAvailableItems = List.copyOf(available);
        applyAvailableFilter();
        updateValidationLabel();
    }

    private void refreshAvailablePods() {
        owner.refreshAvailablePods();
        reloadAvailablePodList();
    }

    private void addSelectedAvailable() {
        List<String> entries = currentEntries();
        for (PodListItem item : availableList.getSelectedValuesList()) {
            entries.add(item.mountPath());
        }
        replaceEditorEntries(SavedPodListService.dedupe(entries));
    }

    private void addPodFilesFromChooser() {
        List<String> entries = currentEntries();
        entries.addAll(owner.choosePodFileEntries(this));
        replaceEditorEntries(SavedPodListService.dedupe(entries));
    }

    private void removeSelectedEntries() {
        List<PodListItem> selected = editorList.getSelectedValuesList();
        if (selected.isEmpty()) {
            return;
        }
        List<PodListItem> updated = new ArrayList<>(allEditorItems);
        for (PodListItem item : selected) {
            updated.removeIf(existing -> normalize(existing.mountPath()).equals(normalize(item.mountPath())));
        }
        allEditorItems = List.copyOf(updated);
        applyEditorFilter();
        reloadAvailablePodList();
    }

    private void moveSelectedEntry(int direction) {
        PodListItem selected = editorList.getSelectedValue();
        int index = selected == null ? -1 : indexOfMountPath(allEditorItems, selected.mountPath());
        int target = index + direction;
        if (index < 0 || target < 0 || target >= allEditorItems.size()) {
            return;
        }
        List<PodListItem> reordered = new ArrayList<>(allEditorItems);
        PodListItem item = reordered.remove(index);
        reordered.add(target, item);
        allEditorItems = List.copyOf(reordered);
        applyEditorFilter();
        selectEditorMountPath(item.mountPath());
        updateValidationLabel();
    }

    private void addCurrentPodFiles() {
        List<String> entries = new ArrayList<>();
        for (PodListItem item : owner.mountedPodItems()) {
            entries.add(item.mountPath());
        }
        entries.addAll(currentEntries());
        replaceEditorEntries(SavedPodListService.dedupe(entries));
    }

    private void importPodIni() {
        JFileChooser chooser = new JFileChooser(owner.gameRootPath().toFile());
        chooser.setDialogTitle("Import pod.ini");
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            SavedPodList imported = SavedPodListService.importPodIni(
                    chooser.getSelectedFile().toPath(),
                    null,
                    AppPreferences.MAX_POD_LIMIT);
            String name = JOptionPane.showInputDialog(this, "List name:", imported.name());
            if (name != null && !name.isBlank()) {
                imported = imported.withName(name);
            }
            preferences = preferences.withSavedPodLists(appendList(imported));
            persistPreferences();
            loadSavedLists();
            selectList(imported.id());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "pod.ini could not be imported:\n" + ex.getMessage(), "JPodman", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void newList() {
        String name = JOptionPane.showInputDialog(this, "New list name:", "New POD List");
        if (name == null || name.isBlank()) {
            return;
        }
        SavedPodList list = SavedPodList.create(name, List.of());
        preferences = preferences.withSavedPodLists(appendList(list));
        persistPreferences();
        loadSavedLists();
        selectList(list.id());
    }

    private void renameList() {
        SavedPodList selected = savedLists.getSelectedValue();
        if (selected == null) {
            return;
        }
        String name = JOptionPane.showInputDialog(this, "Rename list:", selected.name());
        if (name == null || name.isBlank()) {
            return;
        }
        replaceSelected(selected.withName(name).withEntries(currentEntries()));
    }

    private void deleteList() {
        SavedPodList selected = savedLists.getSelectedValue();
        if (selected == null) {
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "Delete \"" + selected.name() + "\"?", "JPodman", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return;
        }
        List<SavedPodList> lists = new ArrayList<>(preferences.savedPodLists());
        lists.removeIf(list -> list.id().equals(selected.id()));
        preferences = preferences.withSavedPodLists(lists);
        persistPreferences();
        loadSavedLists();
        allEditorItems = List.of();
        applyEditorFilter();
        reloadAvailablePodList();
    }

    private void saveSelectedList() {
        SavedPodList selected = savedLists.getSelectedValue();
        if (selected == null) {
            return;
        }
        replaceSelected(selected.withEntries(currentEntries()));
    }

    private void useSelectedList() {
        SavedPodList selected = savedLists.getSelectedValue();
        if (selected == null) {
            return;
        }
        ValidationResult result = validateCurrentList();
        if (!result.missing().isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "The following POD files will be removed because they are missing:\n" + String.join("\n", result.missing()),
                    "Missing POD Files",
                    JOptionPane.WARNING_MESSAGE);
        }
        try {
            SavedPodList updated = selected.withEntries(result.existing());
            List<String> entriesToWrite = entriesForUse(result.existing());
            SavedPodListService.writeExistingToPodIni(owner.gameRootPath(), entriesToWrite, preferences.podLimit());
            owner.replaceMountedEntriesFromDialog(entriesToWrite);
            replaceSelected(updated);
            JOptionPane.showMessageDialog(this, "pod.ini was updated.", "JPodman", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "pod.ini could not be written:\n" + ex.getMessage(), "JPodman", JOptionPane.WARNING_MESSAGE);
        }
    }

    private ValidationResult validateCurrentList() {
        return SavedPodListService.validate(owner.gameRootPath(), currentEntries(), owner.knownPodItems());
    }

    private List<String> entriesForUse(List<String> existingEntries) {
        List<String> entries = new ArrayList<>();
        if (includeMinimalSystemPods.isSelected()) {
            entries.addAll(owner.minimalSystemPodEntriesForUse());
        }
        entries.addAll(existingEntries);
        return SavedPodListService.dedupe(entries);
    }

    private void updateValidationLabel() {
        ValidationResult result = validateCurrentList();
        validationLabel.setText(currentEntries().size() + " entries, " + result.missing().size() + " missing.");
    }

    private void replaceEditorEntries(List<String> entries) {
        List<PodListItem> items = new ArrayList<>();
        for (String entry : entries) {
            items.add(owner.podListItemFor(entry));
        }
        allEditorItems = List.copyOf(items);
        applyEditorFilter();
        reloadAvailablePodList();
    }

    private List<String> currentEntries() {
        List<String> entries = new ArrayList<>();
        for (PodListItem item : allEditorItems) {
            entries.add(item.mountPath());
        }
        return entries;
    }

    private List<SavedPodList> appendList(SavedPodList list) {
        List<SavedPodList> lists = new ArrayList<>(preferences.savedPodLists());
        lists.add(list);
        return lists;
    }

    private void replaceSelected(SavedPodList replacement) {
        List<SavedPodList> lists = new ArrayList<>(preferences.savedPodLists());
        for (int i = 0; i < lists.size(); i++) {
            if (lists.get(i).id().equals(replacement.id())) {
                lists.set(i, replacement);
                break;
            }
        }
        preferences = preferences.withSavedPodLists(lists);
        persistPreferences();
        loadSavedLists();
        selectList(replacement.id());
    }

    private void persistPreferences() {
        try {
            owner.updatePreferencesFromDialog(preferences);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Preferences could not be saved:\n" + ex.getMessage(), "JPodman", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void selectList(String id) {
        for (int i = 0; i < savedListsModel.size(); i++) {
            if (savedListsModel.get(i).id().equals(id)) {
                savedLists.setSelectedIndex(i);
                return;
            }
        }
    }

    private void applyEditorFilter() {
        applyFilter(allEditorItems, editorModel, editorFilterField.getText());
    }

    private void applyAvailableFilter() {
        applyFilter(allAvailableItems, availableModel, availableFilterField.getText());
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

    private int indexOfMountPath(List<PodListItem> items, String mountPath) {
        for (int i = 0; i < items.size(); i++) {
            if (normalize(items.get(i).mountPath()).equals(normalize(mountPath))) {
                return i;
            }
        }
        return -1;
    }

    private void selectEditorMountPath(String mountPath) {
        for (int i = 0; i < editorModel.size(); i++) {
            if (normalize(editorModel.get(i).mountPath()).equals(normalize(mountPath))) {
                editorList.setSelectedIndex(i);
                editorList.ensureIndexIsVisible(i);
                return;
            }
        }
    }

    private static boolean containsMountPath(List<String> entries, String mountPath) {
        String target = normalize(mountPath);
        for (String entry : entries) {
            if (normalize(entry).equals(target)) {
                return true;
            }
        }
        return false;
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

    private static String normalize(String value) {
        return value == null ? "" : value.trim().replace('\\', '/').toLowerCase(Locale.ROOT);
    }
}
