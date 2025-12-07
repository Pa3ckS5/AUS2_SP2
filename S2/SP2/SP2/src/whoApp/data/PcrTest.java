package whoApp.data;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PcrTest implements IRecord<PcrTest>  {

    private int testId;
    private String patientId;
    private LocalDateTime testDateTime;
    private boolean result;
    private double testValue;
    private String note;

    private static final int MAX_PATIENT_ID_LENGTH = 10;
    private static final int MAX_NOTE_LENGTH = 11;
    private static final int MAX_DATE_TIME_LENGTH = 19; //yyyy-MM-dd HH:mm:ss
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private int patientIdLength;
    private int noteLength;

    public PcrTest(int testId, String patientId, LocalDateTime testDateTime,
                   boolean result, double testValue, String note) {
        this.testDateTime = testDateTime;
        setPatientId(patientId);
        this.testId = testId;
        this.result = result;
        this.testValue = testValue;
        setNote(note);

        this.patientIdLength = patientId != null ? patientId.length() : 0;
        this.noteLength = note != null ? note.length() : 0;
    }

    public PcrTest() {
        this.testDateTime = LocalDateTime.of(1, 1, 1, 0, 0, 0);
        this.patientId = "";
        this.testId = 0;
        this.result = false;
        this.testValue = 0.0;
        this.note = "";

        this.patientIdLength = 0;
        this.noteLength = 0;
    }

    public PcrTest(int testId) {
        this.testDateTime = LocalDateTime.of(1, 1, 1, 0, 0, 0);
        this.patientId = "";
        this.testId = testId;
        this.result = false;
        this.testValue = 0.0;
        this.note = "";

        this.patientIdLength = 0;
        this.noteLength = 0;
    }

    //copy
    public PcrTest(PcrTest test) {
        this.testDateTime = test.getTestDateTime();
        setPatientId(test.getPatientId());
        this.testId = test.getTestId();
        this.result = test.getResult();
        this.testValue = test.getTestValue();
        setNote(test.getNote());

        this.patientIdLength = patientId != null ? patientId.length() : 0;
        this.noteLength = note != null ? note.length() : 0;
    }

    public void setPatientId(String patientId) {
        this.patientId = cutString(patientId, MAX_PATIENT_ID_LENGTH);
        this.patientIdLength = patientId.length();
    }

    public void setNote(String note) {
        this.note = cutString(note, MAX_NOTE_LENGTH);
        this.noteLength = note.length();
    }

    public void setTestDateTime(LocalDateTime dateTime) {
        this.testDateTime = dateTime;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    public void setTestValue(double value) {
        this.testValue = value;
    }

    public int getTestId() { return testId; }
    public String getPatientId() { return patientId; }
    public LocalDateTime getTestDateTime() { return testDateTime; }
    public boolean getResult() { return result; }
    public double getTestValue() { return testValue; }
    public String getNote() { return note; }

    public int compareTo(PcrTest other) {
        return Integer.compare(this.testId, other.testId);
    }

    @Override
    public String toString() {
        return String.format("PCR: %d, date=%s, patient=%s, result=%s, value=%.2f, note=%s",
                testId,
                testDateTime.toString(),
                patientId,
                result ? "+" : "-",
                testValue,
                note
        );
    }

    public String showDate() {
        return DATE_TIME_FORMATTER.format(testDateTime);
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(testId);
    }

    @Override
    public boolean isEqualTo(PcrTest other) {
        return this.testId == other.testId;
    }

    @Override
    public PcrTest createClass() {
        return new PcrTest();
    }

    @Override
    public int getSize() {
        return Integer.BYTES + // testId
                Integer.BYTES + // patientIdLength
                (Character.BYTES * MAX_PATIENT_ID_LENGTH) + // patientId
                (Character.BYTES * MAX_DATE_TIME_LENGTH) + // testDateTime
                1 + // result (boolean)
                Double.BYTES + // testValue
                Integer.BYTES + // noteLength
                (Character.BYTES * MAX_NOTE_LENGTH); // note
    }

    @Override
    public byte[] getBytes() {
        ByteArrayOutputStream hlpByteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream hlpOutStream = new DataOutputStream(hlpByteArrayOutputStream);

        try {
            hlpOutStream.writeInt(testId);

            hlpOutStream.writeInt(patientIdLength);
            hlpOutStream.writeChars(padString(patientId, MAX_PATIENT_ID_LENGTH));

            // Serialize LocalDateTime as ISO string
            String dateTimeStr = testDateTime.format(DATE_TIME_FORMATTER);
            hlpOutStream.writeChars(dateTimeStr);

            hlpOutStream.writeBoolean(result);

            hlpOutStream.writeDouble(testValue);

            hlpOutStream.writeInt(noteLength);
            hlpOutStream.writeChars(padString(note, MAX_NOTE_LENGTH));
        } catch (IOException e) {
            throw new IllegalStateException("Error during conversion to byte array.");
        }

        return hlpByteArrayOutputStream.toByteArray();
    }

    @Override
    public PcrTest fromBytes(byte[] bytes) {
        ByteArrayInputStream hlpByteArrayInputStream = new ByteArrayInputStream(bytes);
        DataInputStream hlpInStream = new DataInputStream(hlpByteArrayInputStream);

        try {
            this.testId = hlpInStream.readInt();

            this.patientIdLength = hlpInStream.readInt();
            this.patientId = "";
            for (int i = 0; i < MAX_PATIENT_ID_LENGTH; i++) {
                if (i < patientIdLength) {
                    this.patientId += hlpInStream.readChar();
                } else {
                    hlpInStream.readChar();
                }
            }

            String dateTimeStr = "";
            for (int i = 0; i < MAX_DATE_TIME_LENGTH; i++) {
                dateTimeStr += hlpInStream.readChar();
            }
            dateTimeStr = dateTimeStr.trim();
            try {
                if (!dateTimeStr.isEmpty()) {
                    this.testDateTime = LocalDateTime.parse(dateTimeStr, DATE_TIME_FORMATTER);
                } else {
                    this.testDateTime = LocalDateTime.of(1, 1, 1, 0, 0, 0);
                }
            } catch (Exception e) {
                this.testDateTime = LocalDateTime.of(1, 1, 1, 0, 0, 0);
            }

            this.result = hlpInStream.readBoolean();

            this.testValue = hlpInStream.readDouble();

            this.noteLength = hlpInStream.readInt();
            this.note = "";
            for (int i = 0; i < MAX_NOTE_LENGTH; i++) {
                if (i < noteLength) {
                    this.note += hlpInStream.readChar();
                } else {
                    hlpInStream.readChar();
                }
            }

        } catch (IOException e) {
            throw new IllegalStateException("Error during conversion from byte array.");
        }

        return this;
    }

    private String padString(String str, int length) {
        if (str == null) return " ".repeat(length);
        if (str.length() > length) return str.substring(0, length);
        return String.format("%-" + length + "s", str);
    }

    private String cutString(String str, int length) {
        if (str == null) return "";
        if (str.length() > length) return str.substring(0, length);
        return str;
    }
}