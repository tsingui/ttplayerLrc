package com.example.ttplayerlrcsearch.entity;

import com.example.ttplayerlrcsearch.util.LastRead;
import com.example.ttplayerlrcsearch.util.StringUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class ServerRunData {
    private static Date startTime = new Date();
    private static int serachNum = 0;
    private static int downloadNUm = 0;

    public static Date getStartTime() {
        return startTime;
    }
    public static int getSerachNum() {
        return serachNum;
    }
    public static int getDownloadNUm() {
        return downloadNUm;
    }

    public static void addSerachNum() {
        serachNum++;
    }
    public static void addDownloadNUm() {
        downloadNUm++;
    }

    public static Map<String,Object> getBaseInfo(String lineNum){
        Map<String,Object> result = new HashMap<>();
        int ln = 1000;
        if(StringUtil.notEmpty(lineNum)){
            try {
                ln = Integer.valueOf(lineNum).intValue();
            } catch (NumberFormatException e) {
                //e.printStackTrace();
                log.error("日志行数参数有误：{}",lineNum);
            }
        }
        result.put("startTime",startTime.getTime());
        result.put("serachNum",serachNum);
        result.put("downloadNUm",downloadNUm);
        result.put("log",readLog(ln));
        return result;
    }

    private static String readLog(int lineNum){
        File logFile = new File("./logs/info.log");
        if(!logFile.exists()){
            return null;
        }
        LastRead lr = new LastRead(lineNum);
        StringUtil.readLastLine(logFile, lr);
        List<String> logSourse = lr.readTextList();
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < logSourse.size(); i++) {
            result.append(formatHTML(logSourse.get(i)));
            if(i+1<logSourse.size()){
                result.append("<br/>\r\n");
            }
        }
        //释放内存
        logSourse.clear();
        lr = null;
        return result.toString();
    }
    private static Map<String,String> color = null;
    public static Map<String, String> getColor() {
        if(color==null){
            initColor();
        }
        return color;
    }
    private static void initColor(){
        String[] d0 = new String[]{"black",     "red",      "green",      "yellow",    "blue",      "magenta",   "cyan",      "white",     "gray",        "boldRed",     "boldGreen",   "boldYellow",  "boldBlue",    "boldMagenta", "boldCyan",    "boldWhite",   "highlight"};
        String[] d1 = new String[]{"\u001B[30m","\u001B[31m","\u001B[32m","\u001B[33m","\u001B[34m","\u001B[35m","\u001B[36m","\u001B[37m","\u001B[1;30m","\u001B[1;31m","\u001B[1;32m","\u001B[1;33m","\u001B[1;34m","\u001B[1;35m","\u001B[1;36m","\u001B[1;37m","\u001B[34m"};
        String[] d2 = new String[]{"black",     "red",      "green","     #ff8500",    "blue",      "magenta",   "cyan",      "white",     "gray",        "boldRed",     "boldGreen",   "boldYellow",  "boldBlue",    "boldMagenta", "boldCyan",    "boldWhite",   "highlight"};

        color = new LinkedHashMap<>();

        for (int i = 0; i < d1.length; i++) {
            color.put(d1[i],d2[i]);
        }
    }
    private static String formatHTML(String text){
        Map<String, String> color = getColor();
        String t = text
                .replace(" ","&nbsp;")
                .replace("\t","&nbsp;&nbsp;&nbsp;&nbsp;")
        ;
        Set<String> keys = color.keySet();
        for(String k:keys){
            String style = "";
            //是否加粗
            if(color.get(k).startsWith("bold")){
                style="font-weight:700;";
            }
            //加入颜色
            style = String.format("color: %s;%s",color.get(k).replace("bold",""),style);
            ///加入标签
            t = t.replace(k,
                    String.format("<span style='%s'>", style));
        }
        //标签结束
        t = t
            .replace("\u001B[0;39m","</span>")
        ;
        return t;
    }
}
