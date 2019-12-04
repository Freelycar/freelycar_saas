package com.freelycar.saas.util;

public class NormalTest {

    public static void main(String[] args) {
        test();
    }

    public static void test() {
        String plateLicense = " 苏 A 7   4E6 X ";

        String str = plateLicense.replaceAll("\\s*", "");

        System.out.println(str);
    }
}
