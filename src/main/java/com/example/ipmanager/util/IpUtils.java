package com.example.ipmanager.util;

public class IpUtils {

    public static long ipToLong(String ip) {
        String[] parts = ip.trim().split("\\.");
        if (parts.length != 4) {
            throw new IllegalArgumentException("IP inválida: " + ip);
        }

        long result = 0;
        for (String part : parts) {
            int octet = Integer.parseInt(part);
            if (octet < 0 || octet > 255) {
                throw new IllegalArgumentException("IP inválida: " + ip);
            }
            result = (result << 8) + octet;
        }
        return result;
    }

    public static String longToIp(long value) {
        return ((value >> 24) & 255) + "."
                + ((value >> 16) & 255) + "."
                + ((value >> 8) & 255) + "."
                + (value & 255);
    }

    public static long maskFromCidr(int cidr) {
        if (cidr < 0 || cidr > 32) {
            throw new IllegalArgumentException("Máscara inválida: " + cidr);
        }
        if (cidr == 0) {
            return 0;
        }
        return (0xFFFFFFFFL << (32 - cidr)) & 0xFFFFFFFFL;
    }

    public static long networkAddress(String ip, int cidr) {
        long ipLong = ipToLong(ip);
        long mask = maskFromCidr(cidr);
        return ipLong & mask;
    }

    public static long broadcastAddress(String ip, int cidr) {
        long network = networkAddress(ip, cidr);
        long mask = maskFromCidr(cidr);
        long wildcard = ~mask & 0xFFFFFFFFL;
        return network | wildcard;
    }

    public static boolean overlaps(String ip1, int cidr1, String ip2, int cidr2) {
        long start1 = networkAddress(ip1, cidr1);
        long end1 = broadcastAddress(ip1, cidr1);

        long start2 = networkAddress(ip2, cidr2);
        long end2 = broadcastAddress(ip2, cidr2);

        return start1 <= end2 && start2 <= end1;
    }

    public static boolean sameIp(String ip1, String ip2) {
        if (ip1 == null || ip2 == null || ip1.isBlank() || ip2.isBlank()) {
            return false;
        }
        return ip1.trim().equals(ip2.trim());
    }

    public static boolean isZeroIp(String ip) {
        return "0.0.0.0".equals(ip);
    }

    public static String plusOctet(String ip, int add) {
        long value = ipToLong(ip);
        return longToIp((value + add) & 0xFFFFFFFFL);
    }
}