package com.bonc.dx.crawler_manage.task.crawler.xl;

import java.util.Arrays;

public class Test {

    public static void main(String[] args) {

        String s = "/GS8/upload/verifyCodes/feead87bf7a24a51208c7b7833135d2a.jpg";

        String ss = Arrays.stream(s.split("\\/")).filter( e -> e.contains(".jpg")).findFirst().orElse("");
        String code = ss.split("\\.")[0];
        System.out.println(code);

        String s1= "./detail/2c8382a9785796a901787d14bf292844?contractSign=0";
        System.out.println(s1.substring(1,s1.length()));
    }
}
