package com.household;

import com.household.db.DatabaseManager;
import com.household.ui.MainFrame;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        DatabaseManager.getInstance();

        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) { }

        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}
