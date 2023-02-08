import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class ReadServiceTest {

    @Test
    public void shouldReverseBytes() {
        byte[] bytes = {0x12, 0x34, 0x56, 0x78};
        assertTrue(Arrays.equals(new byte[]{0x78, 0x56, 0x34, 0x12}, ReadService.reverse(bytes)),
                "Массивы должны совпадать");
    }

    @Test
    public void shouldReturnCRCWhenArgsIsZero() {
        assertEquals(0xE1F0, ReadService.crc16(new byte[]{0}, 0, 1),
                "Контрольная сумма при нуле должна быть 57840");
    }

    @Test
    public void shouldReturnCRC16CCITTFALSE() {
        byte[] bytes = {0, 1, 2, 3, 4, 5};
        assertEquals(0x8C18, ReadService.crc16(bytes, 0, bytes.length),
                "Контрольная сумма должна быть равна 35864");
    }

    @Test
    public void shouldReturnCRCZeroWhenDataNull() {
        assertEquals(0, ReadService.crc16(null, 0, 1),
                "Метод должен вернуть 0");
    }

    @Test
    public void shouldReturnHexString() {
        assertEquals("125A193F", ReadService.byteArrayToHex(new byte[]{0x12, 0x5A, 0x19, 0x3F}),
                "Метод должен вернуть строку 125A193F");
    }

}
