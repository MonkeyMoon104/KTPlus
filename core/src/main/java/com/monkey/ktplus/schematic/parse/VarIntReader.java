package com.monkey.ktplus.schematic.parse;

public final class VarIntReader {
    private VarIntReader() {}

    public static int[] decode(byte[] data) {
        if (data == null || data.length == 0) {
            return new int[0];
        }
        int[] values = new int[Math.max(16, data.length)];
        int count = 0;
        int index = 0;
        while (index < data.length) {
            int value = 0;
            int shift = 0;
            int current;
            do {
                if (index >= data.length) {
                    break;
                }
                current = data[index++] & 0xFF;
                value |= (current & 0x7F) << shift;
                shift += 7;
            } while ((current & 0x80) != 0);
            if (count >= values.length) {
                int[] expanded = new int[values.length * 2];
                System.arraycopy(values, 0, expanded, 0, values.length);
                values = expanded;
            }
            values[count++] = value;
        }
        int[] trimmed = new int[count];
        System.arraycopy(values, 0, trimmed, 0, count);
        return trimmed;
    }
}
