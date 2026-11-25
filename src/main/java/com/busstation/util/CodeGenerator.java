package com.busstation.util;

public final class CodeGenerator {

    private CodeGenerator() {
    }

    public static String generate(String prefix, long count) {
        return String.format("%s-%04d", prefix, count + 1);
    }
}
