package com.example.ttplayerlrcsearch.entity;

import com.example.ttplayerlrcsearch.service.MusicLrcSearch;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 为了处理千千静听默认歌曲id不支持非数字，此处创建一个内存类来缓存数据
 * 警告：此类目前仅考虑单机运行，如果考虑服务器运行需要重新考虑内存释放问题
 */
public class BuffData {
    //存储数据
    static private List<BuffDataEntity> data = new Vector<>();
    //存储索引，快速查找
    static private Map<String,Integer> quickPositioning = new ConcurrentHashMap<>();

    //验证ID
//    static{
//        for (int i = 0; i < 1000; i++) {
//            addId(String.valueOf(i),null);
//        }
//    }

    public static String addId(String id, MusicLrcSearch mlc){
        Integer key_int = quickPositioning.get(id);
        if(key_int==null){
            BuffDataEntity entity = new BuffDataEntity(id, mlc);
            //添加数据
            data.add(entity);
            //记录外部id/index位置
            int index = data.size() - 1;
            quickPositioning.put(id,index);
            return String.valueOf(index);
        }
        return key_int.toString();
    }

    public static void clear(){
        data.clear();
        quickPositioning.clear();
    }

    public static BuffDataEntity findId(String indexId){
        Integer ii = Integer.valueOf(indexId);
        if(ii.intValue() < data.size()){
            return data.get(ii);
        }
        return null;
    }
}
