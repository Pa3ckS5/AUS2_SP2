import heapfile.IRecord;

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
        return false;
    }

    @Override
    public int getSize() {
        return 0;
    }

    @Override
    public byte[] getBytes() {
        return new byte[0];
    }

    @Override
    public Patient fromBytes(byte[] bytes) {
        return null;
    }
}
