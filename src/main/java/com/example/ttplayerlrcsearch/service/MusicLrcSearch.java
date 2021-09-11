package com.example.ttplayerlrcsearch.service;

import com.example.ttplayerlrcsearch.entity.ApiResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 定义查询接口，只要实现了该接口，就会被自动调用
 */
@Service
public interface MusicLrcSearch {
    public String getSearchName();

    //查询歌曲
    public List<Map<String,String>> search(String artist,String title);
    //根据歌曲id下载歌词
    public ApiResponse<String> download(String musicId);
}
