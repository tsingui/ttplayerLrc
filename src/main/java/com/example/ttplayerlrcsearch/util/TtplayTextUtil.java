package com.example.ttplayerlrcsearch.util;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TtplayTextUtil {

    public static String decode(String souText){
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < souText.length(); i+=4) {
            String t_ =
                    souText.substring(i+2, i + 4)
                            + souText.substring(i, i + 2);
            int ii_val = Integer.parseInt(t_, 16);
            result.append((char)ii_val);
        }
        return result.toString();
    }
}
