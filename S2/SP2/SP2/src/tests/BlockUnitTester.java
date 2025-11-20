package tests;

import app.Patient;
import heapfile.Block;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BlockUnitTester {

    private Block<Patient> block;
    private Patient patient1;
    private Patient patient2;
    private Patient patient3;

    @BeforeEach
    void setUp() {
        block = new Block<>(3, Patient.class);

        patient1 = new Patient("John", "Doe", java.time.LocalDate.of(1990, 5, 15), "P001");
        patient2 = new Patient("Jane", "Smith", java.time.LocalDate.of(1985, 8, 20), "P002");
        patient3 = new Patient("Bob", "Johnson", java.time.LocalDate.of(1978, 3, 10), "P003");
    }

    @Test
    void testConstructor() {
        assertNotNull(block);
        assertEquals(3, block.getCapacity());
        assertEquals(0, block.getValidCount());
        assertTrue(block.isEmpty());
        assertTrue(block.isPartiallyEmpty());
        assertFalse(block.isFull());
    }

    @Test
    void testAddRecord() {
        // Test pridanie prvého záznamu
        assertTrue(block.addRecord(patient1));
        assertEquals(1, block.getValidCount());
        assertFalse(block.isEmpty());
        assertTrue(block.isPartiallyEmpty());
        assertFalse(block.isFull());

        // Test pridanie druhého záznamu
        assertTrue(block.addRecord(patient2));
        assertEquals(2, block.getValidCount());

        // Test pridanie tretieho záznamu (plný blok)
        assertTrue(block.addRecord(patient3));
        assertEquals(3, block.getValidCount());
        assertFalse(block.isEmpty());
        assertFalse(block.isPartiallyEmpty());
        assertTrue(block.isFull());

        // Test pridanie do plného bloku
        Patient patient4 = new Patient("Alice", "Brown", java.time.LocalDate.of(1995, 12, 5), "P004");
        assertFalse(block.addRecord(patient4));
        assertEquals(3, block.getValidCount());
    }

    @Test
    void testRemoveRecord() {
        // Pridanie záznamov
        block.addRecord(patient1);
        block.addRecord(patient2);
        block.addRecord(patient3);

        // Test odstránenie existujúceho záznamu
        assertTrue(block.removeRecord(patient2));
        assertEquals(2, block.getValidCount());
        assertTrue(block.isPartiallyEmpty());

        // Test odstránenie neexistujúceho záznamu
        Patient nonExistentPatient = new Patient("Non", "Existent", java.time.LocalDate.of(2000, 1, 1), "P999");
        assertFalse(block.removeRecord(nonExistentPatient));
        assertEquals(2, block.getValidCount());

        // Test odstránenie záznamu s rovnakým ID ale rôznymi ostatnými údajmi
        Patient sameIdPatient = new Patient("Different", "Name", java.time.LocalDate.of(1999, 9, 9), "P001");
        assertTrue(block.removeRecord(sameIdPatient));
        assertEquals(1, block.getValidCount());

        // Test odstránenie z prázdneho bloku
        Block<Patient> emptyBlock = new Block<>(2, Patient.class);
        assertFalse(emptyBlock.removeRecord(patient1));
    }

    @Test
    void testGetRecordByIndex() {
        // Pridanie záznamov
        block.addRecord(patient1);
        block.addRecord(patient2);

        // Test získanie záznamu platným indexom
        Patient retrieved1 = block.getRecord(0);
        assertNotNull(retrieved1);
        assertTrue(retrieved1.isEqualTo(patient1));

        Patient retrieved2 = block.getRecord(1);
        assertNotNull(retrieved2);
        assertTrue(retrieved2.isEqualTo(patient2));

        // Test získanie záznamu neplatným indexom
        assertNull(block.getRecord(-1));
        assertNull(block.getRecord(2));
        assertNull(block.getRecord(5));
    }

    @Test
    void testGetRecordByInstance() {
        // Pridanie záznamov
        block.addRecord(patient1);
        block.addRecord(patient2);

        // Test získanie existujúceho záznamu
        Patient retrieved = block.getRecord(patient1);
        assertNotNull(retrieved);
        assertTrue(retrieved.isEqualTo(patient1));

        // Test získanie neexistujúceho záznamu
        Patient nonExistent = new Patient("Non", "Existent", java.time.LocalDate.of(2000, 1, 1), "P999");
        assertNull(block.getRecord(nonExistent));

        // Test získanie záznamu po odstránení
        block.removeRecord(patient1);
        assertNull(block.getRecord(patient1));
    }

    @Test
    void testGetRecords() {
        // Test prázdny blok
        assertEquals(3, block.getRecords().size());
        assertEquals(0, block.getValidCount());

        // Test po pridaní záznamov
        block.addRecord(patient1);
        block.addRecord(patient2);

        var records = block.getRecords();
        assertEquals(3, records.size());
        assertEquals(2, block.getValidCount());
    }

    @Test
    void testGetSize() {
        int expectedSize = Integer.BYTES + // validCount
                Integer.BYTES + // bitmap length
                (int) Math.ceil(3 / 8.0) + // bitmap size
                new Patient().getSize() * 3; // records size

        assertEquals(expectedSize, block.getSize());
    }

    @Test
    void testSerialization() {
        // Pridanie záznamov
        block.addRecord(patient1);
        block.addRecord(patient2);

        // Serializácia
        byte[] serializedData = block.getBytes();
        assertNotNull(serializedData);
        assertTrue(serializedData.length > 0);

        // Vytvorenie nového bloku a deserializácia
        Block<Patient> deserializedBlock = new Block<>(3, Patient.class);
        deserializedBlock.fromBytes(serializedData);

        // Overenie dát
        assertEquals(block.getValidCount(), deserializedBlock.getValidCount());
        assertEquals(block.getCapacity(), deserializedBlock.getCapacity());

        // Overenie záznamov
        Patient deserializedPatient1 = deserializedBlock.getRecord(0);
        assertNotNull(deserializedPatient1);
        assertTrue(deserializedPatient1.isEqualTo(patient1));

        Patient deserializedPatient2 = deserializedBlock.getRecord(1);
        assertNotNull(deserializedPatient2);
        assertTrue(deserializedPatient2.isEqualTo(patient2));
    }

    @Test
    void testEmptyBlockSerialization() {
        // Serializácia prázdneho bloku
        byte[] serializedData = block.getBytes();
        assertNotNull(serializedData);

        // Deserializácia
        Block<Patient> deserializedBlock = new Block<>(3, Patient.class);
        deserializedBlock.fromBytes(serializedData);

        assertEquals(0, deserializedBlock.getValidCount());
        assertTrue(deserializedBlock.isEmpty());
    }

    @Test
    void testFullBlockSerialization() {
        // Naplnenie bloku
        block.addRecord(patient1);
        block.addRecord(patient2);
        block.addRecord(patient3);

        // Serializácia a deserializácia
        byte[] serializedData = block.getBytes();
        Block<Patient> deserializedBlock = new Block<>(3, Patient.class);
        deserializedBlock.fromBytes(serializedData);

        assertEquals(3, deserializedBlock.getValidCount());
        assertTrue(deserializedBlock.isFull());

        // Overenie všetkých záznamov
        for (int i = 0; i < 3; i++) {
            Patient original = block.getRecord(i);
            Patient deserialized = deserializedBlock.getRecord(i);
            assertTrue(deserialized.isEqualTo(original));
        }
    }

    @Test
    void testStateMethods() {
        // Test prázdneho bloku
        assertTrue(block.isEmpty());
        assertTrue(block.isPartiallyEmpty());
        assertFalse(block.isFull());

        // Test čiastočne naplneného bloku
        block.addRecord(patient1);
        assertFalse(block.isEmpty());
        assertTrue(block.isPartiallyEmpty());
        assertFalse(block.isFull());

        // Test plného bloku
        block.addRecord(patient2);
        block.addRecord(patient3);
        assertFalse(block.isEmpty());
        assertFalse(block.isPartiallyEmpty());
        assertTrue(block.isFull());

        // Test po odstránení záznamu
        block.removeRecord(patient1);
        assertFalse(block.isEmpty());
        assertTrue(block.isPartiallyEmpty());
        assertFalse(block.isFull());
    }
}