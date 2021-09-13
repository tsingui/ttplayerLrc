package com.example.ttplayerlrcsearch.util;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.io.*;
import java.nio.charset.Charset;

@Slf4j
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
    public static String formatXml(String souText){
        if(isNull(souText)){
            return "";
        }
        return souText
                .replace("'","&apos;")
        ;
    }

//    public static boolean saveFile(String text, File out){
//        return saveFile(text,out,Charset.defaultCharset().name());
//    }
//    public static boolean saveFile(String text, File out,String charse){
//        if(!out.exists()){
//            try {
//                out.createNewFile();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }else if(!out.isFile()){
//            log.error("保存失败，保存位置错误，{} 不是一个文件",out.getPath());
//            return false;
//        }
//        OutputStreamWriter osw = null;
//        try {
//            osw = new OutputStreamWriter(new FileOutputStream(out), charse);
//            osw.write(text);
//            osw.flush();
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }finally {
//            try{if(osw!=null){osw.close();}}catch (Exception e){}
//        }
//        return true;
//    }
}
