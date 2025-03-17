package com.giozar04.client.presentation.views;

import javax.swing.SwingUtilities;

import com.giozar04.client.presentation.views.transactions.TransactionFormFrame;

public class MainView {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            TransactionFormFrame frame = new TransactionFormFrame();
            frame.setVisible(true);
        });
    }
}
