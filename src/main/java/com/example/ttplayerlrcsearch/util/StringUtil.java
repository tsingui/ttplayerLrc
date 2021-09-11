package com.example.ttplayerlrcsearch.util;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

public class StringUtil {
    public static boolean isNull(Object text){
        if (text==null)return true;
        if ("".equals(text)){
            return true;
        }
        return false;
    }
    public static boolean notEmpty(Object text){
        return !isNull(text);
    }
    private static final String[] u = "B,KB,MB,GB,TB".split(",");
    public static String formatSize(int size){
        double out = size;
        for (int i = 0; i < u.length; i++) {
            if(out<1024){
                return String.format("%.2f %s",out,u[i]);
            }
            out = out/1024;
        }
        return String.format("%.2f %s",out,u[u.length-1]);
    }

}
