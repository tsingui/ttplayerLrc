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

    public static boolean saveFile(String text, File out){
        return saveFile(text,out, Charset.defaultCharset().name());
    }
    public static boolean saveFile(String text, File out,String charse){
        if(!out.exists()){
            try {
                out.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else if(!out.isFile()){
            log.error("保存失败，保存位置错误，{} 不是一个文件",out.getPath());
            return false;
        }
        OutputStreamWriter osw = null;
        try {
            osw = new OutputStreamWriter(new FileOutputStream(out), charse);
            osw.write(text);
            osw.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try{if(osw!=null){osw.close();}}catch (Exception e){}
        }
        return true;
    }

    public static String readLastLine(File f,int lineNum){
        LastRead lr = new LastRead(lineNum);
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(f));
            String buff = null;
            while ((buff=br.readLine())!=null){
                lr.add(buff);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            closeSteam(br);
        }
        return lr.getText();
    }

    //关闭IO流
    private static void closeSteam(InputStream is){
        if(is!=null){try{is.close();}catch (Exception e){}}
    }
    private static void closeSteam(OutputStream os){
        if(os!=null){try{os.close();}catch (Exception e){}}
    }
    private static void closeSteam(Reader r){
        if(r!=null){try{r.close();}catch (Exception e){}}
    }
    private static void closeSteam(Writer w){
        if(w!=null){try{w.close();}catch (Exception e){}}
    }

    @Test
    public void t001(){
        String s = readLastLine(new File("./logs/info.log"), 100);
        saveFile(s,new File("z:/out.txt"));
    }
}
