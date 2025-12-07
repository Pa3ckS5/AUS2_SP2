package whoApp;

import whoApp.data.Patient;
import whoApp.data.PcrTest;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class WhoGUI extends JFrame {
    private WhoSystem system;
    private JTextArea outputArea;
    private JButton btnNewSystem;
    private JButton btnLoadSystem;
    private JButton btnSaveSystem;
    private JButton btnGenerate;
    private JButton btnInsertTest;
    private JButton btnFindPatient;
    private JButton btnFindTest;
    private JButton btnInsertPatient;
    private JButton btnDeleteTest;
    private JButton btnDeletePatient;
    private JButton btnEditPatient;
    private JButton btnEditTest;
    private JButton btnPrintBlocks;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public WhoGUI() {
        initializeGUI();
        setupWindowListener();
        updateButtonStates(false); // Initially disabled until system is loaded
    }

    private void initializeGUI() {
        // main window
        setTitle("WHO Patient Management System");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout());

        // button panel at the top - now in 2 rows
        JPanel buttonPanel = new JPanel(new GridLayout(2, 7, 5, 5));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // First row of buttons
        btnNewSystem = createStyledButton("New System");
        btnLoadSystem = createStyledButton("Load System");
        btnSaveSystem = createStyledButton("Save System");
        btnGenerate = createStyledButton("Generate");
        btnPrintBlocks = createStyledButton("Print Blocks");
        btnInsertTest = createStyledButton("1. Insert PCR Test");
        btnFindPatient = createStyledButton("2. Find Patient");

        // Second row of buttons
        btnFindTest = createStyledButton("3. Find PCR Test");
        btnInsertPatient = createStyledButton("4. Insert Patient");
        btnDeleteTest = createStyledButton("5. Delete PCR Test");
        btnDeletePatient = createStyledButton("6. Delete Patient");
        btnEditPatient = createStyledButton("7. Edit Patient");
        btnEditTest = createStyledButton("8. Edit PCR Test");

        // Add action listeners
        btnNewSystem.addActionListener(e -> createNewSystem());
        btnLoadSystem.addActionListener(e -> loadSystem());
        btnSaveSystem.addActionListener(e -> saveSystem());
        btnGenerate.addActionListener(e -> generateData());
        btnInsertPatient.addActionListener(e -> insertPatient());
        btnFindPatient.addActionListener(e -> findPatient());
        btnInsertTest.addActionListener(e -> insertPCRTest());
        btnFindTest.addActionListener(e -> findPCRTest());
        btnDeleteTest.addActionListener(e -> deletePCRTest());
        btnDeletePatient.addActionListener(e -> deletePatient());
        btnEditPatient.addActionListener(e -> editPatient());
        btnEditTest.addActionListener(e -> editPCRTest());
        btnPrintBlocks.addActionListener(e -> printBlocks());

        // Add buttons to panel - first row
        buttonPanel.add(btnNewSystem);
        buttonPanel.add(btnLoadSystem);
        buttonPanel.add(btnSaveSystem);
        buttonPanel.add(btnGenerate);
        buttonPanel.add(btnPrintBlocks);
        buttonPanel.add(btnInsertTest);
        buttonPanel.add(btnFindPatient);


        // Add buttons to panel - second row
        buttonPanel.add(btnFindTest);
        buttonPanel.add(btnInsertPatient);
        buttonPanel.add(btnDeleteTest);
        buttonPanel.add(btnDeletePatient);
        buttonPanel.add(btnEditPatient);
        buttonPanel.add(btnEditTest);

        // output area
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(outputArea);

        // main window assembly
        mainPanel.add(buttonPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        add(mainPanel);
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 11));
        button.setFocusPainted(false);
        button.setBackground(new Color(240, 240, 240));
        button.setMargin(new Insets(5, 10, 5, 10));
        return button;
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
        if (system != null) {
            int choice = JOptionPane.showConfirmDialog(this,
                    "Do you want to save the current system before closing?",
                    "Save System",
                    JOptionPane.YES_NO_CANCEL_OPTION);

            if (choice == JOptionPane.YES_OPTION) {
                system.close();
                outputArea.append("System saved successfully.\n");
            } else if (choice == JOptionPane.NO_OPTION) {
                // Just close without saving
            } else {
                return; // Cancel closing
            }
        }
        dispose();
        System.exit(0);
    }

    private void updateButtonStates(boolean enabled) {
        btnSaveSystem.setEnabled(enabled && system != null);
        btnGenerate.setEnabled(enabled);
        btnInsertPatient.setEnabled(enabled);
        btnFindPatient.setEnabled(enabled);
        btnInsertTest.setEnabled(enabled);
        btnFindTest.setEnabled(enabled);
        btnDeleteTest.setEnabled(enabled);
        btnDeletePatient.setEnabled(enabled);
        btnEditPatient.setEnabled(enabled);
        btnEditTest.setEnabled(enabled);
        btnPrintBlocks.setEnabled(enabled && system != null);
    }

    private void createNewSystem() {
        // Single dialog for all parameters
        JTextField nameField = new JTextField("mySystem");
        JTextField blockSizeField = new JTextField("1024");
        JTextField overflowField = new JTextField("512");

        Object[] message = {
                "System Name:", nameField,
                "Block Size:", blockSizeField,
                "Overflow Block Size:", overflowField
        };

        int option = JOptionPane.showConfirmDialog(this, message,
                "Create New System", JOptionPane.OK_CANCEL_OPTION);

        if (option == JOptionPane.OK_OPTION) {
            String systemName = nameField.getText().trim();
            String blockSizeStr = blockSizeField.getText().trim();
            String overflowSizeStr = overflowField.getText().trim();

            if (!systemName.isEmpty() && !blockSizeStr.isEmpty() && !overflowSizeStr.isEmpty()) {
                try {
                    int blockSize = Integer.parseInt(blockSizeStr);
                    int overflowSize = Integer.parseInt(overflowSizeStr);

                    system = new WhoSystem(systemName, blockSize, overflowSize);
                    updateButtonStates(true);
                    outputArea.append("✓ New system '" + systemName + "' created successfully.\n");
                    outputArea.append("  Block size: " + blockSize + ", Overflow size: " + overflowSize + "\n\n");
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this,
                            "Invalid number format! Please enter valid numbers.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this,
                            "Error creating system: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private void loadSystem() {
        String systemName = JOptionPane.showInputDialog(this,
                "Enter system name to load:", "mySystem");

        if (systemName != null && !systemName.trim().isEmpty()) {
            systemName = systemName.trim();

            try {
                system = new WhoSystem(systemName);
                updateButtonStates(true);
                outputArea.append("✓ System '" + systemName + "' loaded successfully.\n");
                outputArea.append("  Patients: " + system.getPatientCount() +
                        ", Tests: " + system.getTestCount() + "\n\n");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Error loading system: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void saveSystem() {
        if (system == null) {
            JOptionPane.showMessageDialog(this, "No system to save!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        system.close();
        outputArea.append("✓ System saved successfully.\n\n");
    }

    private void generateData() {
        if (system == null) {
            JOptionPane.showMessageDialog(this, "No system loaded!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JTextField patientCountField = new JTextField("100");
        JTextField testCountField = new JTextField("300");

        Object[] message = {
                "Number of patients to generate:", patientCountField,
                "Number of tests to generate:", testCountField
        };

        int option = JOptionPane.showConfirmDialog(this, message,
                "Generate Data", JOptionPane.OK_CANCEL_OPTION);

        if (option == JOptionPane.OK_OPTION) {
            try {
                int patientCount = Integer.parseInt(patientCountField.getText());
                int testCount = Integer.parseInt(testCountField.getText());

                system.generate(patientCount, testCount);

                outputArea.append("✓ Generated " + patientCount + " patients and " + testCount + " tests.\n");
                outputArea.append("  Total patients: " + system.getPatientCount() +
                        ", Total tests: " + system.getTestCount() + "\n\n");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this,
                        "Invalid number format!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // 1. Insert PCR Test
    private void insertPCRTest() {
        if (system == null) {
            JOptionPane.showMessageDialog(this, "No system loaded!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Test ID is assigned by the system automatically
        int testId = system.getTestCount();
        JLabel testIdLabel = new JLabel("Test ID (auto-assigned): " + testId);
        JTextField patientIdField = new JTextField();
        JTextField dateField = new JTextField(LocalDateTime.now().format(DATE_TIME_FORMATTER));
        JComboBox<String> resultCombo = new JComboBox<>(new String[]{"Positive", "Negative"});
        JTextField valueField = new JTextField("0.0");
        JTextField noteField = new JTextField("Test note");

        Object[] message = {
                testIdLabel,
                "Patient ID:", patientIdField,
                "Date & Time (yyyy-MM-dd HH:mm:ss):", dateField,
                "Result:", resultCombo,
                "Test Value:", valueField,
                "Note:", noteField
        };

        int option = JOptionPane.showConfirmDialog(this, message,
                "Insert PCR Test", JOptionPane.OK_CANCEL_OPTION);

        if (option == JOptionPane.OK_OPTION) {
            try {
                String patientId = patientIdField.getText();
                LocalDateTime testDateTime = LocalDateTime.parse(dateField.getText(), DATE_TIME_FORMATTER);
                boolean result = resultCombo.getSelectedIndex() == 0; // Positive = true
                double value = Double.parseDouble(valueField.getText());
                String note = noteField.getText();

                PcrTest test = new PcrTest(testId, patientId, testDateTime, result, value, note);

                if (system.insertPcrTest(test)) {
                    outputArea.append("✓ PCR Test #" + testId + " inserted successfully for patient " + patientId + "\n\n");
                } else {
                    outputArea.append("✗ Failed to insert PCR Test #" + testId + "\n\n");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Error: " + ex.getMessage() + "\nPlease check date format (yyyy-MM-dd HH:mm:ss)",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // 2. Find Patient with tests
    private void findPatient() {
        if (system == null) {
            JOptionPane.showMessageDialog(this, "No system loaded!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String patientId = JOptionPane.showInputDialog(this, "Enter patient ID to find:");

        if (patientId != null && !patientId.trim().isEmpty()) {
            patientId = patientId.trim();
            Patient patient = system.findPatient(patientId);

            if (patient != null) {
                StringBuilder sb = new StringBuilder();
                sb.append("=== PATIENT FOUND ===\n");
                sb.append("ID: ").append(patient.getPatientId()).append("\n");
                sb.append("Name: ").append(patient.getFirstName()).append(" ").append(patient.getLastName()).append("\n");
                sb.append("Birth Date: ").append(patient.getBirthDate()).append("\n");
                sb.append("=====================\n\n");

                outputArea.append(sb.toString());
            } else {
                outputArea.append("✗ Patient with ID '" + patientId + "' not found.\n\n");
            }
        }
    }

    // 3. Find PCR Test with patient info
    private void findPCRTest() {
        if (system == null) {
            JOptionPane.showMessageDialog(this, "No system loaded!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String testIdStr = JOptionPane.showInputDialog(this, "Enter test ID to find:", "0");

        if (testIdStr != null && !testIdStr.trim().isEmpty()) {
            try {
                int testId = Integer.parseInt(testIdStr.trim());
                PcrTest test = system.findPcrTest(testId);

                if (test != null) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("=== PCR TEST FOUND ===\n");
                    sb.append("Test ID: ").append(test.getTestId()).append("\n");
                    sb.append("Patient ID: ").append(test.getPatientId()).append("\n");
                    sb.append("Date/Time: ").append(test.getTestDateTime()).append("\n");
                    sb.append("Result: ").append(test.getResult() ? "Positive" : "Negative").append("\n");
                    sb.append("Value: ").append(String.format("%.2f", test.getTestValue())).append("\n");
                    sb.append("Note: ").append(test.getNote()).append("\n");
                    sb.append("======================\n\n");

                    outputArea.append(sb.toString());
                } else {
                    outputArea.append("✗ PCR Test with ID " + testId + " not found.\n\n");
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this,
                        "Invalid test ID format!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // 4. Insert Patient
    private void insertPatient() {
        if (system == null) {
            JOptionPane.showMessageDialog(this, "No system loaded!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Patient ID is assigned by the system automatically
        int patientIdNum = system.getPatientCount();
        String patientId = String.valueOf(patientIdNum);
        JLabel patientIdLabel = new JLabel("Patient ID (auto-assigned): " + patientId);
        JTextField firstNameField = new JTextField("John");
        JTextField lastNameField = new JTextField("Doe");
        JTextField birthDateField = new JTextField(LocalDate.now().minusYears(30).format(DATE_FORMATTER));

        Object[] message = {
                patientIdLabel,
                "First Name:", firstNameField,
                "Last Name:", lastNameField,
                "Birth Date (yyyy-MM-dd):", birthDateField
        };

        int option = JOptionPane.showConfirmDialog(this, message,
                "Insert Patient", JOptionPane.OK_CANCEL_OPTION);

        if (option == JOptionPane.OK_OPTION) {
            try {
                String firstName = firstNameField.getText();
                String lastName = lastNameField.getText();
                LocalDate birthDate = LocalDate.parse(birthDateField.getText(), DATE_FORMATTER);

                Patient patient = new Patient(firstName, lastName, birthDate, patientId);

                if (system.insertPatient(patient)) {
                    outputArea.append("✓ Patient " + patientId + " inserted successfully.\n\n");
                } else {
                    outputArea.append("✗ Failed to insert patient " + patientId + "\n\n");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Error: " + ex.getMessage() + "\nPlease check date format (yyyy-MM-dd)",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // 5. Delete PCR Test
    private void deletePCRTest() {
        if (system == null) {
            JOptionPane.showMessageDialog(this, "No system loaded!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String testIdStr = JOptionPane.showInputDialog(this, "Enter test ID to delete:", "0");

        if (testIdStr != null && !testIdStr.trim().isEmpty()) {
            try {
                int testId = Integer.parseInt(testIdStr.trim());

                int confirm = JOptionPane.showConfirmDialog(this,
                        "Are you sure you want to delete PCR Test #" + testId + "?\nThis action cannot be undone.",
                        "Confirm Delete",
                        JOptionPane.YES_NO_OPTION);

                if (confirm == JOptionPane.YES_OPTION) {
                    if (system.removePcrTest(testId)) {
                        outputArea.append("✓ PCR Test #" + testId + " deleted successfully.\n\n");
                    } else {
                        outputArea.append("✗ PCR Test #" + testId + " not found or could not be deleted.\n\n");
                    }
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this,
                        "Invalid test ID format!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // 6. Delete Patient with tests
    private void deletePatient() {
        if (system == null) {
            JOptionPane.showMessageDialog(this, "No system loaded!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String patientId = JOptionPane.showInputDialog(this, "Enter patient ID to delete:");

        if (patientId != null && !patientId.trim().isEmpty()) {
            patientId = patientId.trim();

            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to delete Patient " + patientId + "?\n" +
                            "This will delete ALL associated tests and cannot be undone.",
                    "Confirm Delete",
                    JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                if (system.removePatient(patientId)) {
                    outputArea.append("✓ Patient " + patientId + " and all associated tests deleted successfully.\n\n");
                } else {
                    outputArea.append("✗ Patient " + patientId + " not found or could not be deleted.\n\n");
                }
            }
        }
    }

    // 7. Edit Patient (excluding tests)
    private void editPatient() {
        if (system == null) {
            JOptionPane.showMessageDialog(this, "No system loaded!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String patientId = JOptionPane.showInputDialog(this, "Enter patient ID to edit:");

        if (patientId != null && !patientId.trim().isEmpty()) {
            patientId = patientId.trim();
            Patient patient = system.findPatient(patientId);

            if (patient != null) {
                JTextField firstNameField = new JTextField(patient.getFirstName());
                JTextField lastNameField = new JTextField(patient.getLastName());
                JTextField birthDateField = new JTextField(patient.getBirthDate().format(DATE_FORMATTER));

                Object[] message = {
                        "First Name:", firstNameField,
                        "Last Name:", lastNameField,
                        "Birth Date (yyyy-MM-dd):", birthDateField
                };

                int option = JOptionPane.showConfirmDialog(this, message,
                        "Edit Patient " + patientId, JOptionPane.OK_CANCEL_OPTION);

                if (option == JOptionPane.OK_OPTION) {
                    try {
                        patient.setFirstName(firstNameField.getText());
                        patient.setLastName(lastNameField.getText());

                        // Note: You need to add setBirthDate method to Patient class
                        // patient.setBirthDate(LocalDate.parse(birthDateField.getText(), DATE_FORMATTER));

                        if (system.editPatient(patient)) {
                            outputArea.append("✓ Patient " + patientId + " updated successfully.\n\n");
                        } else {
                            outputArea.append("✗ Failed to update patient " + patientId + "\n\n");
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(this,
                                "Error: " + ex.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            } else {
                outputArea.append("✗ Patient with ID '" + patientId + "' not found.\n\n");
            }
        }
    }

    // 8. Edit PCR Test
    private void editPCRTest() {
        if (system == null) {
            JOptionPane.showMessageDialog(this, "No system loaded!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String testIdStr = JOptionPane.showInputDialog(this, "Enter test ID to edit:", "0");

        if (testIdStr != null && !testIdStr.trim().isEmpty()) {
            try {
                int testId = Integer.parseInt(testIdStr.trim());
                PcrTest test = system.findPcrTest(testId);

                if (test != null) {
                    JTextField patientIdField = new JTextField(test.getPatientId());
                    JTextField dateField = new JTextField(test.getTestDateTime().format(DATE_TIME_FORMATTER));
                    JComboBox<String> resultCombo = new JComboBox<>(new String[]{"Positive", "Negative"});
                    resultCombo.setSelectedIndex(test.getResult() ? 0 : 1);
                    JTextField valueField = new JTextField(String.valueOf(test.getTestValue()));
                    JTextField noteField = new JTextField(test.getNote());

                    Object[] message = {
                            "Patient ID:", patientIdField,
                            "Date/Time (yyyy-MM-dd HH:mm:ss):", dateField,
                            "Result:", resultCombo,
                            "Test Value:", valueField,
                            "Note:", noteField
                    };

                    int option = JOptionPane.showConfirmDialog(this, message,
                            "Edit PCR Test #" + testId, JOptionPane.OK_CANCEL_OPTION);

                    if (option == JOptionPane.OK_OPTION) {
                        try {
                            test.setPatientId(patientIdField.getText());

                            // Note: You need to add setTestDateTime method to PcrTest class
                            // test.setTestDateTime(LocalDateTime.parse(dateField.getText(), DATE_TIME_FORMATTER));

                            test.setNote(noteField.getText());

                            // Note: You need to add setResult and setTestValue methods to PcrTest class
                            // test.setResult(resultCombo.getSelectedIndex() == 0);
                            // test.setTestValue(Double.parseDouble(valueField.getText()));

                            if (system.editPcrTest(test)) {
                                outputArea.append("✓ PCR Test #" + testId + " updated successfully.\n\n");
                            } else {
                                outputArea.append("✗ Failed to update PCR Test #" + testId + "\n\n");
                            }
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(this,
                                    "Error: " + ex.getMessage(),
                                    "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                } else {
                    outputArea.append("✗ PCR Test with ID " + testId + " not found.\n\n");
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this,
                        "Invalid test ID format!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // New method to print all blocks
    private void printBlocks() {
        if (system == null) {
            JOptionPane.showMessageDialog(this, "No system loaded!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Ask user if they want to see full details or summary
        Object[] options = {"Full Details", "Summary Only", "Cancel"};
        int choice = JOptionPane.showOptionDialog(this,
                "Select level of detail for block display:",
                "Print Blocks",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);

        if (choice == 0) { // Full Details
            String allBlocks = system.getHashFilesAsString();
            outputArea.append("\n" + "=".repeat(80) + "\n");
            outputArea.append("ALL HASH FILE BLOCKS - FULL DETAILS\n");
            outputArea.append("=".repeat(80) + "\n\n");
            outputArea.append(allBlocks);
            outputArea.append("\n" + "=".repeat(80) + "\n");
            outputArea.append("END OF BLOCK DISPLAY\n");
            outputArea.append("=".repeat(80) + "\n\n");
        } else if (choice == 1) { // Summary Only
            outputArea.append("\n" + "=".repeat(80) + "\n");
            outputArea.append("SYSTEM SUMMARY\n");
            outputArea.append("=".repeat(80) + "\n");
            outputArea.append("Total Patients: " + system.getPatientCount() + "\n");
            outputArea.append("Total PCR Tests: " + system.getTestCount() + "\n");

            // You could add more statistics here if available
            outputArea.append("=".repeat(80) + "\n\n");
        }
        // Choice 2 is Cancel - do nothing
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }

            WhoGUI gui = new WhoGUI();
            gui.setVisible(true);
        });
    }
}