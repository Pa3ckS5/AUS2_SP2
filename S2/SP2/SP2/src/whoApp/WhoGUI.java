package whoApp;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;



public class WhoGUI extends JFrame {
    private WhoSystem system;
    private JTextArea outputArea;
    private JButton generateButton;
    private JButton printBlocksButton;

    public WhoGUI() {
        this.system = new WhoSystem(false);
        initializeGUI();
        setupWindowListener();
    }

    private void initializeGUI() {
        // main window
        setTitle("Patient Management System");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // Changed to handle close manually
        setSize(800, 600);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout());

        // buttons area
        JPanel buttonPanel = new JPanel(new FlowLayout());

        generateButton = new JButton("Generate patients");
        generateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                generatePatients();
            }
        });

        printBlocksButton = new JButton("Print blocks");
        printBlocksButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                printBlocks();
            }
        });

        buttonPanel.add(generateButton);
        buttonPanel.add(printBlocksButton);

        // print area
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(outputArea);

        // main window
        mainPanel.add(buttonPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        add(mainPanel);
    }

    private void setupWindowListener() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeApplication();
            }
        });
    }

    private void closeApplication() {
        system.close();
        dispose();
        System.exit(0);
    }

    private void generatePatients() {
        // window for num of patients
        String input = JOptionPane.showInputDialog(this,
                "Enter number of patients to generate:", "300");

        if (input != null && !input.trim().isEmpty()) {
            try {
                int count = Integer.parseInt(input.trim());
                system.generateRandomPatients(count);
                outputArea.append("Generated " + count + " patients.\n");
                outputArea.append("Total patients in system: " + system.getPatientCount() + "\n\n");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this,
                        "Invalid number format!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void printBlocks() {
        String blocksInfo = system.getBlocksForPrint();
        outputArea.append("=== BLOCKS WITH RECORDS ===\n");
        outputArea.append(blocksInfo);
        outputArea.append("\nTotal patients in system: " + system.getPatientCount() + "\n");
        outputArea.append("============================\n\n");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new WhoGUI().setVisible(true);
            }
        });
    }
}