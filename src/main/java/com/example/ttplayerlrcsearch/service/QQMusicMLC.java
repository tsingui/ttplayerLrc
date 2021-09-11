package com.example.ttplayerlrcsearch.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.example.ttplayerlrcsearch.entity.ApiResponse;
import com.example.ttplayerlrcsearch.entity.BuffData;
import com.example.ttplayerlrcsearch.util.StringUtil;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class QQMusicMLC implements MusicLrcSearch{
    private static final String searchName = "QQ音乐";
    @Override
    public String getSearchName() {
        return searchName;
    }

    private String searchUrl = "https://c.y.qq.com/soso/fcgi-bin/client_search_cp?new_json=1&n=%s&w=%s";
    private String downlooadUrl = "https://c.y.qq.com/lyric/fcgi-bin/fcg_query_lyric_new.fcg?format=json&inCharset=utf-8&outCharset=utf-8&needNewCode=1&songmid=%s";

    private int pageNum = 50;
    @Value("${MLC.pageNum}")
    private void setPageNum(Integer pageNum) {
        if(pageNum!=null){
            this.pageNum = pageNum;
        }
    }

    @Override
    public List<Map<String, String>> search(String artist, String title) {
        String searchText = String.format("%s %s",title,artist);
        List<Map<String, String>> result = null;
        //查询结果
        String sourse_result = getResult(searchText);
        if(StringUtil.isNull(sourse_result)){
            return null;
        }
        //解析结果
        result = parse(sourse_result);
        return result;
    }

    @Override
    public ApiResponse download(String musicId) {
        ApiResponse result=null;

        GetRequest request = Unirest.get(String.format(downlooadUrl,musicId))
                .header("referer", "https://y.qq.com/");
        HttpResponse<String> lrcResponse = null;
        try {
            lrcResponse = request.asString();
        } catch (UnirestException e) {
            //e.printStackTrace();
            log.error(e.getMessage());
            return ApiResponse.returnFail(e.getMessage());
        }
        String body = lrcResponse.getBody();
        JSONObject lrcObj = JSON.parseObject(body);
        String sourseLyric = lrcObj.getString("lyric");
        result = ApiResponse.returnOK().setDataNow(
                new String(Base64.getDecoder().decode(sourseLyric))
        );
        return result;
    }

    private Pattern result_p = Pattern.compile("^callback\\((.*)\\)");
    private String getResult(String text){
        GetRequest request = null;
        try {
            request = Unirest.get(String.format(searchUrl,pageNum, URLEncoder.encode(text,"UTF-8")));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if(request==null)return null;
        HttpResponse<String> asString = null;
        try {
            long start = System.currentTimeMillis();
            asString = request.asString();
            log.debug("Api Use Time:{} ms",(System.currentTimeMillis() - start)/1000.0);
        } catch (UnirestException e) {
            e.printStackTrace();
        }
        if(asString==null)return null;
        String body = asString.getBody();
        if(StringUtil.isNull(body))return null;

        Matcher m = result_p.matcher(body);
        if(m.find()){
            body = m.group(1);
        }
//        log.debug(body);
        return body;
    }

    private List<Map<String, String>> parse(String sourseData){
        List<Map<String, String>> result = new ArrayList<>();
        long start = System.currentTimeMillis();
        JSONObject resultObj = JSON.parseObject(sourseData);
        log.debug("parseJSON Use Time: {} ms,data size: {}"
                ,(System.currentTimeMillis() - start)/1000.0
                ,StringUtil.formatSize(sourseData.getBytes(StandardCharsets.UTF_8).length)
        );
        JSONArray dataList = resultObj.getJSONObject("data").getJSONObject("song").getJSONArray("list");
        for (int i = 0; i < dataList.size(); i++) {
            JSONObject songItem = dataList.getJSONObject(i);
            Map<String, String> song = new HashMap<>();

            song.put("id_sou",songItem.getString("mid"));
            //ID特殊处理
            song.put("id", BuffData.addId(songItem.getString("mid"),this));

            song.put("title",String.format("【%s】%s",this.getSearchName(),songItem.getString("name")));
            JSONArray singerList = songItem.getJSONArray("singer");
            song.put("artist",arrayJoinSinger(singerList,"、"));

            result.add(song);
        }
        return result;
    }
    private String arrayJoinSinger(JSONArray arr,String c){
        if(arr==null)return null;
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < arr.size(); i++) {
            sb.append(
                    arr.getJSONObject(i).getString("name")
            );
            if(i+1<arr.size()){
                sb.append(c);
            }
        }
        return sb.toString();
    }

    @Test
    public void t001(){
        String text = "飞鸟和蝉 任然";
        GetRequest request = null;
        try {
            request = Unirest.get(String.format("https://c.y.qq.com/soso/fcgi-bin/client_search_cp?w=%s", URLEncoder.encode(text,"UTF-8")));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        HttpResponse<String> asString = null;
        try {
            asString = request.asString();
        } catch (UnirestException e) {
            e.printStackTrace();
        }
        String body = asString.getBody();
        System.out.println(body);
    }
    @Test
    public void t002(){
        String result = getResult("飞鸟和蝉");

    }
    @Test
    public void t003() throws UnirestException {
        HttpResponse<String> response = Unirest.get("https://c.y.qq.com/lyric/fcgi-bin/fcg_query_lyric_new.fcg?format=json&inCharset=utf-8&outCharset=utf-8&needNewCode=1&songmid=002Lk1O21zCIjx")
                .header("referer", "https://y.qq.com/")
                .asString();
        log.info(response.getBody());
    }
}
