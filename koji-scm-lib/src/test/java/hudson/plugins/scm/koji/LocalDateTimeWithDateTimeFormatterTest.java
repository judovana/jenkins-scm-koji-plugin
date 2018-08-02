package hudson.plugins.scm.koji;

import org.junit.Test;

import java.time.LocalDateTime;

import static org.junit.Assert.*;

public class LocalDateTimeWithDateTimeFormatterTest {
    @Test
    public void testDTFOptionalWithMicros() {
        assertEquals(LocalDateTime.of(2018, 12, 31, 12, 34, 56, 123456000),
                     LocalDateTime.parse("2018-12-31 12:34:56.123456", Constants.DTF));
    }

    @Test
    public void testDTFOptionalWithoutMicros() {
        // Brew started sending datetime without microseconds for some builds and this format caused an issue with our
        // DateTimeFormatter. It is fixed now with optional microseconds.
        assertEquals(LocalDateTime.of(2018, 12, 31, 12, 34, 56),
                     LocalDateTime.parse("2018-12-31 12:34:56", Constants.DTF));
    }

    @Test
    public void testDTFOptionalToStringWithoutMicros() {
        LocalDateTime expectedTime = LocalDateTime.of(2018, 12, 31, 12, 34, 56);

        assertEquals("2018-12-31 12:34:56.0", Constants.DTF.format(expectedTime));
    }

    @Test
    public void testDTFOptionalToStringWithMicros() {
        LocalDateTime expectedTime = LocalDateTime.of(2018, 12, 31, 12, 34, 56, 123456000);

        assertEquals("2018-12-31 12:34:56.123456", Constants.DTF.format(expectedTime));
    }
}
