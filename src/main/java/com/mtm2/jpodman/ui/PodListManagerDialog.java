package com.mtm2.jpodman.ui;

import com.mtm2.jpodman.AppPreferences;
import com.mtm2.jpodman.PodListItem;
import com.mtm2.jpodman.SavedPodList;
import com.mtm2.jpodman.io.SavedPodListService;
import com.mtm2.jpodman.io.SavedPodListService.ValidationResult;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Modal, resizable Podswap-style manager for saved POD lists. */
public final class PodListManagerDialog extends JDialog {
    private final MainWindow owner;
    private final DefaultListModel<SavedPodList> savedListsModel = new DefaultListModel<>();
    private final JList<SavedPodList> savedLists = new JList<>(savedListsModel);
    private final DefaultListModel<EntryRow> entriesModel = new DefaultListModel<>();
    private final JList<EntryRow> entriesList = new JList<>(entriesModel);
    private final JTextArea alwaysMountArea = new JTextArea(5, 30);
    private final JLabel validationLabel = new JLabel("Select or import a POD list.");
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
        entriesList.setCellRenderer(new EntryRowRenderer());

        setLayout(new BorderLayout(8, 8));
        add(buildListsPanel(), BorderLayout.WEST);
        add(buildEditorPanel(), BorderLayout.CENTER);
        add(buildActionsPanel(), BorderLayout.SOUTH);

        loadSavedLists();
        setMinimumSize(new Dimension(860, 520));
        setSize(new Dimension(980, 620));
        setResizable(true);
        setLocationRelativeTo(owner);
    }

    private JPanel buildListsPanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createTitledBorder("Saved Lists"));
        panel.setPreferredSize(new Dimension(220, 420));
        panel.add(new JScrollPane(savedLists), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttons.add(button("New", this::newList));
        buttons.add(button("Rename", this::renameList));
        buttons.add(button("Delete", this::deleteList));
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildEditorPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;

        c.gridx = 0;
        c.gridy = 0;
        c.weighty = 0.7;
        JPanel entriesPanel = new JPanel(new BorderLayout(4, 4));
        entriesPanel.setBorder(BorderFactory.createTitledBorder("List Editor"));
        entriesPanel.add(new JScrollPane(entriesList), BorderLayout.CENTER);
        JPanel editButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        editButtons.add(button("Add Entry", this::addEntry));
        editButtons.add(button("Remove Entry", this::removeEntry));
        entriesPanel.add(editButtons, BorderLayout.SOUTH);
        panel.add(entriesPanel, c);

        c.gridy = 1;
        c.weighty = 0.25;
        JPanel alwaysPanel = new JPanel(new BorderLayout(4, 4));
        alwaysPanel.setBorder(BorderFactory.createTitledBorder("Always Mount (one POD per line)"));
        alwaysPanel.add(new JScrollPane(alwaysMountArea), BorderLayout.CENTER);
        panel.add(alwaysPanel, c);

        c.gridy = 2;
        c.weighty = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        validationLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        panel.add(validationLabel, c);
        return panel;
    }

    private JPanel buildActionsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panel.add(button("Import pod.ini...", this::importPodIni));
        panel.add(button("Find Missing Files", this::findMissingFiles));
        panel.add(button("Explore MTM Folder", this::exploreGameFolder));
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
        SavedPodList selected = savedLists.getSelectedValue();
        entriesModel.clear();
        alwaysMountArea.setText("");
        if (selected == null) {
            return;
        }
        for (String entry : selected.entries()) {
            entriesModel.addElement(rowFor(entry));
        }
        alwaysMountArea.setText(String.join(System.lineSeparator(), selected.alwaysMount()));
        updateValidationLabel();
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
        replaceSelected(selected.withName(name));
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
    }

    private void addEntry() {
        String entry = JOptionPane.showInputDialog(this, "POD path:");
        if (entry != null && !entry.isBlank()) {
            entriesModel.addElement(rowFor(entry.trim()));
            updateValidationLabel();
        }
    }

    private void removeEntry() {
        int index = entriesList.getSelectedIndex();
        if (index >= 0) {
            entriesModel.remove(index);
            updateValidationLabel();
        }
    }

    private void saveSelectedList() {
        SavedPodList selected = savedLists.getSelectedValue();
        if (selected == null) {
            return;
        }
        replaceSelected(selected.withEntries(currentEntries()).withAlwaysMount(currentAlwaysMount()));
    }

    private void findMissingFiles() {
        ValidationResult result = validateCurrentList();
        if (result.missing().isEmpty()) {
            JOptionPane.showMessageDialog(this, "No missing files.", "JPodman", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Missing POD files:\n" + String.join("\n", result.missing()), "JPodman", JOptionPane.WARNING_MESSAGE);
        }
        updateValidationLabel();
    }

    private void useSelectedList() {
        SavedPodList selected = savedLists.getSelectedValue();
        if (selected == null) {
            return;
        }
        SavedPodList working = selected.withEntries(currentEntries()).withAlwaysMount(currentAlwaysMount());
        ValidationResult result = SavedPodListService.validate(owner.gameRootPath(), SavedPodListService.combineForMount(working), owner.knownPodItems());
        if (!result.missing().isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "These missing PODs will be removed before writing pod.ini:\n" + String.join("\n", result.missing()),
                    "JPodman",
                    JOptionPane.WARNING_MESSAGE);
        }
        try {
            SavedPodListService.writeExistingToPodIni(owner.gameRootPath(), result.existing(), preferences.podLimit());
            owner.reloadGamePodIniFromDialog();
            JOptionPane.showMessageDialog(this, "pod.ini was updated.", "JPodman", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "pod.ini could not be written:\n" + ex.getMessage(), "JPodman", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void exploreGameFolder() {
        try {
            java.awt.Desktop.getDesktop().open(owner.gameRootPath().toFile());
        } catch (IOException | UnsupportedOperationException ex) {
            JOptionPane.showMessageDialog(this, "The MTM folder could not be opened:\n" + ex.getMessage(), "JPodman", JOptionPane.WARNING_MESSAGE);
        }
    }

    private ValidationResult validateCurrentList() {
        List<String> combined = new ArrayList<>(currentEntries());
        combined.addAll(currentAlwaysMount());
        ValidationResult result = SavedPodListService.validate(owner.gameRootPath(), combined, owner.knownPodItems());
        for (int i = 0; i < entriesModel.size(); i++) {
            EntryRow row = entriesModel.get(i);
            entriesModel.set(i, rowFor(row.mountPath()));
        }
        return result;
    }

    private void updateValidationLabel() {
        ValidationResult result = SavedPodListService.validate(owner.gameRootPath(), currentEntries(), owner.knownPodItems());
        validationLabel.setText(currentEntries().size() + " entries, " + result.missing().size() + " missing.");
    }

    private EntryRow rowFor(String mountPath) {
        ValidationResult result = SavedPodListService.validate(owner.gameRootPath(), List.of(mountPath), owner.knownPodItems());
        return new EntryRow(mountPath, displayLabel(mountPath), !result.missing().isEmpty());
    }

    private String displayLabel(String mountPath) {
        for (PodListItem item : owner.knownPodItems()) {
            if (normalize(item.mountPath()).equals(normalize(mountPath))) {
                return item.displayLabel();
            }
        }
        return mountPath;
    }

    private List<String> currentEntries() {
        List<String> entries = new ArrayList<>();
        for (int i = 0; i < entriesModel.size(); i++) {
            entries.add(entriesModel.get(i).mountPath());
        }
        return entries;
    }

    private List<String> currentAlwaysMount() {
        List<String> entries = new ArrayList<>();
        for (String line : alwaysMountArea.getText().split("\\R")) {
            if (!line.isBlank()) {
                entries.add(line.trim());
            }
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

    private static String normalize(String value) {
        return value == null ? "" : value.trim().replace('\\', '/').toLowerCase(java.util.Locale.ROOT);
    }

    private record EntryRow(String mountPath, String displayLabel, boolean missing) {
        @Override
        public String toString() {
            return displayLabel;
        }
    }

    private static final class EntryRowRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof EntryRow row && row.missing() && !isSelected) {
                label.setForeground(Color.RED);
            }
            return label;
        }
    }
}
