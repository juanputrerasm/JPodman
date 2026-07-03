package com.mtm2.jpodman.ui;

import com.mtm2.jpodman.AppPreferences;

import java.awt.Frame;

/** Compatibility wrapper for Cowpod's separate directory preferences concept. */
public final class DirectoryPreferencesDialog extends PreferencesDialog {
    public DirectoryPreferencesDialog(Frame owner, AppPreferences preferences) {
        super(owner, preferences);
        setTitle("POD Folders");
    }
}
