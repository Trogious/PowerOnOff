package net.swmud.trog.poweronoff;

import android.support.annotation.Nullable;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Utils {
    private static final Pattern MAC_ADDRESS_REGEX = Pattern.compile("([0-9a-f]{2}:){5}[0-9a-f]{2}", Pattern.CASE_INSENSITIVE);

    @Nullable
    public static final InetAddress getBroadcastIpv4() throws SocketException {
        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
            NetworkInterface networkInterface = en.nextElement();
            if (!networkInterface.isLoopback() && networkInterface.isUp()) {
                for (InterfaceAddress address : networkInterface.getInterfaceAddresses()) {
                    InetAddress inetAddress = address.getBroadcast();
                    if (inetAddress instanceof Inet4Address) {
                        return inetAddress;
                    }
                }
            }
        }

        return null;
    }

    public static final void sendMagicPacket(final InetAddress broadcastAddress, final String targetMac) throws IOException {
        final byte[] macBytes = getMacBytes(targetMac);
        final byte[] wolBytes = new byte[102];
        for (int i = 0; i < 6; ++i) {
            wolBytes[i] = (byte) 0xff;
        }
        for (int i = 6; i < wolBytes.length; i += macBytes.length) {
            System.arraycopy(macBytes, 0, wolBytes, i, macBytes.length);
        }

        final DatagramPacket packet = new DatagramPacket(wolBytes, wolBytes.length, broadcastAddress, 9);
        final DatagramSocket socket = new DatagramSocket();
        socket.send(packet);
        socket.close();
    }

    public static final boolean isMacValid(final String macAddress) {
        return MAC_ADDRESS_REGEX.matcher(macAddress).matches();
    }

    static final byte[] getMacBytes(final String macAddress) throws IllegalArgumentException {
        if (isMacValid(macAddress)) {
            final byte[] bytes = new byte[6];
            int i = 0;
            for (final String twoBytes : macAddress.split(":")) {
                bytes[i++] = (byte) Integer.parseInt(twoBytes, 16);
            }
            return bytes;
        }

        throw new IllegalArgumentException("Invalid MAC address: " + macAddress);
    }
}
