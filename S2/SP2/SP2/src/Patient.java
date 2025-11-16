import heapfile.IRecord;

import java.io.*;

public class Patient implements IRecord<Patient> {
    private static final int NAME_LEN = 15;
    private static final int SURNAME_LEN = 14;
    private static final int ID_LEN = 10;

    private String firstName;
    private String lastName;
    private String birthDate;  // 2000-12-24 (10 chars)
    private String patientId;

    public Patient(String name, String surname, String birthDate, String patientId) {
        this.firstName = name;
        this.lastName = surname;
        this.birthDate = birthDate;
        this.patientId = patientId;
    }

    @Override
    public boolean isEqualTo(Patient other) {
        return patientId.equals(other.patientId) && firstName.equals(other.firstName) && lastName.equals(other.lastName) && birthDate.equals(other.birthDate);
    }

    @Override
    public int getSize() {
        return 0;
    }

    @Override
    public byte[] getBytes() {
        ByteArrayOutputStream hlpByteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream hlpOutStream = new DataOutputStream(hlpByteArrayOutputStream);
        try {
            hlpOutStream.writeInt(this.firstName.length());
            hlpOutStream.writeUTF(this.firstName);
            hlpOutStream.writeInt(this.lastName.length());
            hlpOutStream.writeUTF(this.lastName);
            hlpOutStream.writeInt(this.birthDate.length());
            hlpOutStream.writeUTF(this.birthDate);
            hlpOutStream.writeInt(this.patientId.length());
            hlpOutStream.writeUTF(this.patientId);

            return hlpByteArrayOutputStream.toByteArray();

        } catch (IOException e) {
            throw new IllegalStateException("Error during conversion to byte array.");
        }
    }

    @Override
    public Patient fromBytes(byte[] bytes) {
        ByteArrayInputStream hlpByteArrayInputStream = new ByteArrayInputStream(bytes);
        DataInputStream hlpInStream = new DataInputStream(hlpByteArrayInputStream);
        try {
            int firstNameLen = hlpInStream.readInt();
            this.firstName = hlpInStream.readUTF();
            int lastNameLen = hlpInStream.readInt();
            this.lastName = hlpInStream.readUTF();
            int birthDateLen = hlpInStream.readInt();
            this.birthDate = hlpInStream.readUTF();
            int patientIdLen = hlpInStream.readInt();
            this.patientId = hlpInStream.readUTF();

        } catch (IOException e) {
            throw new IllegalStateException("Error during conversion from byte array.");
        }

        return null;
    }
}
