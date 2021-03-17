package com.bonc.dx.crawler_manage.util;


import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.Properties;


public class ProperGet {
    public static final String defaultPath = "config/configs.properties";
    public static Properties properties;

    public ProperGet(String... propertyPath) {
        Properties properties = new Properties();
        try {
            properties.load(Files.newInputStream(Paths.get(propertyPath[0])));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        this.properties = properties;
    }

    public ProperGet() {
        Properties properties = new Properties();
        try {
            InputStream in = ProperGet.class.getClassLoader().getResourceAsStream(defaultPath);
            properties.load(in);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        this.properties = properties;
    }

    public Properties getPro() {
        return properties;
    }


}
