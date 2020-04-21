package com.gipstech.bletestcontact;

import java.util.Arrays;

/**
 * Some utility methods
 */
public class Utils
{
    public static String getEddystoneUid(byte[] record)
    {
        byte[] sign = new byte[] {0x03, 0x03, (byte)0xAA, (byte)0xFE};
        byte[] shortRecord = Arrays.copyOfRange(record, 0, 4);
        byte[] longRecord = Arrays.copyOfRange(record, 3, 7);
        boolean isShort = Arrays.equals(shortRecord, sign);
        int frameIndex = (isShort ? 8 : 11);
        boolean isEddystoneUid = (isShort || Arrays.equals(longRecord, sign)) && (record[frameIndex] == 0x00);

        if (isEddystoneUid)
        {
            byte[] uuid = Arrays.copyOfRange(record, frameIndex + 2, frameIndex + 18);
            return toHexString(uuid);
        }

        return null;
    }

    public static String toHexString(byte[] a)
    {
        StringBuilder sb = new StringBuilder(a.length * 2);

        for (byte b: a)
        {
            sb.append(String.format("%02x", b));
        }

        return sb.toString();
    }

    public static byte[] fromHexString(String s)
    {
        int len = s.length();
        byte[] data = new byte[len / 2];

        for (int i = 0; i < len; i += 2)
        {
            data[i / 2] = (byte)((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }

        return data;
    }
}