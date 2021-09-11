package com.example.ttplayerlrcsearch.entity;

import com.example.ttplayerlrcsearch.service.MusicLrcSearch;
import lombok.Data;

/**
 * 缓存实体类，绑定歌曲是从哪个渠道来的
 */
@Data
public class BuffDataEntity {
    String souId;
    MusicLrcSearch mlc;
    public BuffDataEntity(String souId,MusicLrcSearch mlc){
        this.souId = souId;
        this.mlc = mlc;
    }
}
