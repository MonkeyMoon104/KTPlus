package com.monkey.ktplus.util.compat;

public final class EnumCompat {
    private EnumCompat() {}

    @SuppressWarnings("unchecked")
    public static <T extends Enum<T>> T valueOf(Class<?> enumClass, String constant) {
        if (!enumClass.isEnum()) {
            throw new IllegalArgumentException(enumClass.getName() + " is not an enum");
        }
        return Enum.valueOf((Class<T>) enumClass, constant);
    }

    public static <T extends Enum<T>> T valueOfOrNull(Class<?> enumClass, String constant) {
        try {
            return valueOf(enumClass, constant);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
