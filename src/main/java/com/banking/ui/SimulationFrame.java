package com.banking.ui;

import com.banking.model.Account;
import com.banking.model.Bank;
import com.banking.service.ConcurrentTransferService;
import com.banking.simulation.SimulationController;
import com.banking.simulation.SimulationController.SimulationState;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class SimulationFrame extends JFrame {

    private final JButton startButton;
    private final JButton pauseButton;
    private final JButton resumeButton;
    private final JButton stopButton;
    private final JLabel statusLabel;
    private final JTextArea logArea;

    private SimulationController controller;
    private final AtomicLong successfulTransfers = new AtomicLong(0);
    private final AtomicLong failedTransfers = new AtomicLong(0);

    public SimulationFrame() {
        this(null);
    }

    public SimulationFrame(SimulationController controller) {
        super("GhostSim - Concurrent Banking Simulation");

        // UI Components
        startButton = new JButton("START");
        pauseButton = new JButton("PAUSE");
        resumeButton = new JButton("RESUME");
        stopButton = new JButton("STOP");
        statusLabel = new JLabel("Status: STOPPED", SwingConstants.CENTER);
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 14));

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        // Control Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.add(startButton);
        buttonPanel.add(pauseButton);
        buttonPanel.add(resumeButton);
        buttonPanel.add(stopButton);

        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        topPanel.add(statusLabel, BorderLayout.NORTH);
        topPanel.add(buttonPanel, BorderLayout.CENTER);

        // Layout
        setLayout(new BorderLayout(5, 5));
        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        // Attach controller
        if (controller != null) {
            this.controller = controller;
        } else {
            this.controller = createDefaultSimulationController();
        }

        // Attach Event Listeners
        startButton.addActionListener(e -> onStart());
        pauseButton.addActionListener(e -> onPause());
        resumeButton.addActionListener(e -> onResume());
        stopButton.addActionListener(e -> onStop());

        // Update initial button states based on controller state
        updateStateUI(this.controller.getState());

        // Clean close handling
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (SimulationFrame.this.controller != null && !SimulationFrame.this.controller.isStopped()) {
                    new Thread(() -> SimulationFrame.this.controller.stop()).start();
                }
            }
        });

        setSize(700, 500);
        setLocationRelativeTo(null);
    }

    public void setController(SimulationController controller) {
        if (this.controller != null && !this.controller.isStopped()) {
            this.controller.stop();
        }
        this.controller = controller;
        updateStateUI(this.controller.getState());
    }

    public SimulationController getController() {
        return controller;
    }

    public JButton getStartButton() {
        return startButton;
    }

    public JButton getPauseButton() {
        return pauseButton;
    }

    public JButton getResumeButton() {
        return resumeButton;
    }

    public JButton getStopButton() {
        return stopButton;
    }

    public JLabel getStatusLabel() {
        return statusLabel;
    }

    public JTextArea getLogArea() {
        return logArea;
    }

    public void onStart() {
        if (controller == null) return;
        controller.start();
        updateStateUI(SimulationState.RUNNING);
        appendLog("Simulation STARTED.");
    }

    public void onPause() {
        if (controller == null) return;
        controller.pause();
        updateStateUI(SimulationState.PAUSED);
        appendLog("Simulation PAUSED.");
    }

    public void onResume() {
        if (controller == null) return;
        controller.resume();
        updateStateUI(SimulationState.RUNNING);
        appendLog("Simulation RESUMED.");
    }

    public void onStop() {
        if (controller == null) return;
        updateStateUI(SimulationState.STOPPED);
        appendLog("Simulation STOPPING...");
        // Run stop in background thread to avoid blocking EDT during thread join
        new Thread(() -> {
            controller.stop();
            SwingUtilities.invokeLater(() -> appendLog("Simulation STOPPED."));
        }).start();
    }

    public void updateStateUI(SimulationState state) {
        Runnable updateAction = () -> {
            switch (state) {
                case RUNNING:
                    statusLabel.setText("Status: RUNNING");
                    startButton.setEnabled(false);
                    pauseButton.setEnabled(true);
                    resumeButton.setEnabled(false);
                    stopButton.setEnabled(true);
                    break;
                case PAUSED:
                    statusLabel.setText("Status: PAUSED");
                    startButton.setEnabled(false);
                    pauseButton.setEnabled(false);
                    resumeButton.setEnabled(true);
                    stopButton.setEnabled(true);
                    break;
                case STOPPED:
                default:
                    statusLabel.setText("Status: STOPPED");
                    startButton.setEnabled(true);
                    pauseButton.setEnabled(false);
                    resumeButton.setEnabled(false);
                    stopButton.setEnabled(false);
                    break;
            }
        };

        if (SwingUtilities.isEventDispatchThread()) {
            updateAction.run();
        } else {
            SwingUtilities.invokeLater(updateAction);
        }
    }

    public void appendLog(String message) {
        Runnable appendAction = () -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        };

        if (SwingUtilities.isEventDispatchThread()) {
            appendAction.run();
        } else {
            SwingUtilities.invokeLater(appendAction);
        }
    }

    private SimulationController createDefaultSimulationController() {
        Bank bank = new Bank("GhostSim Central Bank");
        List<Account> accounts = new ArrayList<>();
        double initialPerAccount = 1000.0;
        int numAccounts = 10;
        for (int i = 1; i <= numAccounts; i++) {
            Account acc = new Account("ACC-" + String.format("%03d", i), initialPerAccount);
            bank.addAccount(acc);
            accounts.add(acc);
        }

        ConcurrentTransferService transferService = new ConcurrentTransferService();
        Random random = new Random();
        int workerCount = 5;
        long workerDelayMillis = 100;

        Runnable transferTask = () -> {
            int srcIdx = random.nextInt(accounts.size());
            int dstIdx = random.nextInt(accounts.size());
            while (dstIdx == srcIdx) {
                dstIdx = random.nextInt(accounts.size());
            }

            Account src = accounts.get(srcIdx);
            Account dst = accounts.get(dstIdx);
            double amount = 10.0 + random.nextInt(90);

            try {
                transferService.transfer(src, dst, amount);
                long succ = successfulTransfers.incrementAndGet();
                if (succ % 10 == 0 || succ < 10) {
                    double totalBankBalance = accounts.stream().mapToDouble(Account::getBalance).sum();
                    appendLog(String.format("[%s] Transferred $%.2f from %s (bal: $%.2f) to %s (bal: $%.2f) | Total Bank: $%.2f | Tx Count: %d",
                            Thread.currentThread().getName(), amount, src.getId(), src.getBalance(), dst.getId(), dst.getBalance(), totalBankBalance, succ));
                }
            } catch (Exception ex) {
                failedTransfers.incrementAndGet();
            }
        };

        return new SimulationController(workerCount, transferTask, workerDelayMillis);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SimulationFrame frame = new SimulationFrame();
            frame.setVisible(true);
        });
    }
}
