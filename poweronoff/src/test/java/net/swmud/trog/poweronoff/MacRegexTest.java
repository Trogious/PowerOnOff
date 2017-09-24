package net.swmud.trog.poweronoff;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MacRegexTest {
    private static final String[] VALID_MACS = {
            "05:dc:98:00:5a:25", "a9:a0:67:ed:53:c5", "d8:90:d7:59:6f:d1", "1c:72:d0:a3:1f:a6", "96:c1:3a:93:8d:4d",
            "ea:03:ac:5b:2c:69", "d4:47:23:35:b1:f8", "ed:f7:03:70:f9:c3", "db:e3:eb:12:a0:11", "fb:7d:c2:44:93:bc",
            "18:56:c2:5c:72:97", "41:50:ba:a9:06:89", "42:7e:a9:16:0d:da", "63:b0:c0:4b:04:0d", "8f:1d:2b:6f:61:13",
            "ed:f6:19:92:7a:45", "2c:69:12:01:a7:5e", "59:4b:20:6e:54:b5", "5e:96:ec:b6:98:10", "03:90:af:bc:f3:f9",
            "05:DC:98:00:5A:25", "A9:A0:67:ED:53:C5", "D8:90:D7:59:6F:D1", "1C:72:D0:A3:1F:A6", "96:C1:3A:93:8D:4D",
            "EA:03:AC:5B:2C:69", "D4:47:23:35:B1:F8", "ED:F7:03:70:F9:C3", "DB:E3:EB:12:A0:11", "FB:7D:C2:44:93:BC",
            "18:56:C2:5C:72:97", "41:50:BA:A9:06:89", "42:7E:A9:16:0D:DA", "63:B0:C0:4B:04:0D", "8F:1D:2B:6F:61:13",
            "ED:F6:19:92:7A:45", "2C:69:12:01:A7:5E", "59:4B:20:6E:54:B5", "5E:96:EC:B6:98:10", "03:90:AF:BC:F3:F9"
    };

    private static final String[] INVALID_MACS = {
            "05:dc:98:z3:5a:25", "9:a0:67:ed:53:c5", "d890:d7:59:6f:d1", ":1c:72:d0:a3:1f:a6", "96:c1:3a:93:8d:4d:",
            "03:90:AF:BC:F3:F9E"
    };

    @Test
    public void isMacValid() throws Exception {
        for (final String mac : VALID_MACS) {
            assertTrue(Utils.isMacValid(mac));
        }
    }

    @Test
    public void isMacInValid() throws Exception {
        for (final String mac : INVALID_MACS) {
            assertFalse(Utils.isMacValid(mac));
        }
    }

    @Test
    public void regexCorrect() throws Exception {
        for (final String mac : VALID_MACS) {
            byte bytes[] = Utils.getMacBytes(mac);
            assertNotNull(bytes);
            assertEquals(6, bytes.length);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void regexInCorrect0() {
        Utils.getMacBytes(INVALID_MACS[0]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void regexInCorrect1() {
        Utils.getMacBytes(INVALID_MACS[1]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void regexInCorrect2() {
        Utils.getMacBytes(INVALID_MACS[2]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void regexInCorrect3() {
        Utils.getMacBytes(INVALID_MACS[3]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void regexInCorrect4() {
        Utils.getMacBytes(INVALID_MACS[4]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void regexInCorrect5() {
        Utils.getMacBytes(INVALID_MACS[5]);
    }
}