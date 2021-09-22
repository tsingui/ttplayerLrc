package com.example.ttplayerlrcsearch.util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class LastRead{
    int line = 10;
    //游标
    int i = -1;
    //数据
    String[] data;
    public LastRead(int line){
        this.line = line;
        data = new String[line];
    }
    public void add(String t){
        i++;
        if(i>=line){
            i=0;
        }
        data[i]=t;
    }
    public String getText(){
        StringBuffer sb = new StringBuffer();
        //第一部分
        for (int j = i+1; j < line; j++) {
            String lineText = data[j];
            if(lineText!=null){
                sb.append(lineText);
                sb.append("\r\n");
            }
        }
        //第二部分
        for (int j = 0; j <= i; j++) {
            String lineText = data[j];
            if(lineText!=null){
                sb.append(lineText);
                sb.append("\r\n");
            }
        }
        if(sb.length()>=2){
            sb.setLength(sb.length()-2);
        }
        return sb.toString();
    }
    public List<String> readTextList(){
        List<String> array = new ArrayList<>();
        //第一部分
        for (int j = i+1; j < line; j++) {
            String lineText = data[j];
            if(lineText!=null){
                array.add(lineText);
            }
        }
        //第二部分
        for (int j = 0; j <= i; j++) {
            String lineText = data[j];
            if(lineText!=null){
                array.add(lineText);
            }
        }
        return array;
    }
}
