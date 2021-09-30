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
public class QQMusicMLC extends LRCDispose implements MusicLrcSearch {
    private static final String searchName = "QQ音乐";
    @Override
    public String getSearchName() {
        return searchName;
    }

    private String searchUrl = "https://c.y.qq.com/soso/fcgi-bin/client_search_cp?new_json=1&n=%s&w=%s";
    private String downlooadUrl = "https://c.y.qq.com/lyric/fcgi-bin/fcg_query_lyric_new.fcg?format=json&g_tk=5381&songmid=%s";

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
        if(StringUtil.isNull(sourseLyric)){
            log.error("该歌曲没有上传歌词！");
        }
        try {
            String souText = new String(Base64.getDecoder().decode(sourseLyric),"UTF-8");
            String transText = null;
            String transLrc = lrcObj.getString("trans");
            if(StringUtil.notEmpty(transLrc)){
                transText = new String(Base64.getDecoder().decode(transLrc),"UTF-8");
            }
            result = ApiResponse.returnOK().setDataNow(doTrans(souText,transText));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            String errMessage = String.format("歌词解析出现异常\r\n%s", sourseLyric);
            log.error(errMessage);
            result = ApiResponse.returnFail(errMessage);
        }
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
    @Test
    public void t004() throws UnsupportedEncodingException {
        String sou = "W3RpOk1PTV0KW2FyOuabueWPr+W/g0NhcnJpZV0KW2FsOk1PTV0KW2J5Ol0KW29mZnNldDowXQpbMDA6MDAuMDBdTU9NIC0g6Jyh56yU5bCP5b+DClswMDowMi4zNl3or43vvJrmm7nlj6/lv4NDYXJyaWUKWzAwOjA0LjczXeabsu+8muabueWPr+W/g0NhcnJpZQpbMDA6MDcuMDldTWl4IGJ577ya6Iif5omS55quClswMDowOS40Nl1Qcm9kIGJ577yaQml6eQpbMDA6MTEuODNd5oiR5YGc5Zyo5qCh6Zeo5Y+jClswMDoxMy4wNV3mlrDkv67nmoTpgqPmnaHkvr/liKnlupfooZfpgZPkuIoKWzAwOjE1LjQ4XeS6uuadpeS6uuW+gApbMDA6MTYuMjJd5ZKM5oiR56ul5bm055yL5Yiw55qE6YO95LiN5LiA5qC3ClswMDoxOC41N13pl6jljavlj5Tlj5TkuI3nrJHkuoYKWzAwOjIwLjA2XeS7luivtOS7luWPr+iDveiAgeS6hgpbMDA6MjEuNTRd6Lev5LiK6YGH5Yiw5oiR55qE54+t5Li75Lu7ClswMDoyMy4zNF3ku5bor7TmiJHplb/lpKfkuoYKWzAwOjI1LjAxXemCo+aXtue/u+W8gOaVsOWtpuS9nOS4muacrOWBmuS6huWNgemBk+mimApbMDA6MjcuNzBd5Yqg5YeP5LmY6Zmk55yL6ZSZ5aaI5aaI5rCU5b6X5oqY5pat5LqG56yUClswMDozMC44MF3lm57liLDnj63nuqfogIHluIjlronmhbDor7Tlpb3kuobmsqHlhbPns7sKWzAwOjM0LjA4XeWPiOaYr+mCo+WPpeS9oOayoeeUqOWKn+WFtuWunuS9oOW+iOiBquaYjgpbMDA6MzcuMTJd6IOM5LiK5LqG5Lmm5YyF5Zue5Yiw5LqG5YWF5ruh5ZC16Ze555qE5a62ClswMDo0MC4yMV3miJHlk63nnYDor7TniLjniLjlpojlpojkvaDku6zkuI3opoHotbDllYoKWzAwOjQzLjEyXeaIkeS8muWKquWKm+WtpuS5oOeEtuWQjuaMo+mSseWFu+WutgpbMDA6NDYuMTVd5o2i5p2l5LiA5Y+l5aSn5Lq655qE5LqL5bCP5a2p5a2Q5Yir566h5LqGClswMDo0OS4zMV3miJHlm57liLDljaflrqTmiZPlvIDkuabljIUKWzAwOjUxLjA0XeaLv+WHuuaWreaOieeahOmTheeslApbMDA6NTIuNTBd6L+Y5pyJ6ICD5b6X5LiN5aW955qE6K+V5Y23ClswMDo1NC4wN13kvYbmmK/ov5vmraXkuobkuIDlkI0KWzAwOjU1LjU5XeWPluS4i+mynOiJs+eahOe6oumihuW3vgpbMDA6NTcuMDZd6L+Y5pyJ5b2p6Imy55qE5qmh55qu562LClswMDo1OC42OV3lnKjlopnkuIrnlLvkuIvmiJHniLjniLjlpojlpogKWzAxOjAwLjcyXei/mOacieS4gOmil+Wkp+eIseW/gwpbMDE6MDIuNTJd5aSp56m65piv6JSa6JOd6ImyClswMTowNC4wMF3nqpflpJbmnInljYPnurjpuaQKWzAxOjA1LjYxXemZquaIkeW8ueeQtOWGmeatjOavj+S4gOWIhuavj+S4gOWIuwpbMDE6MDguNDJd5YaZ5LiL5LqG5LiA6aaW5q2MClswMToxMC4yMF3mmK/pgIHnu5nlpojlpojnmoQKWzAxOjExLjg1XeaUvuS4i+aJi+S4reeahOW3peS9nOS7lOe7huWQrOWQrOaIkeivtApbMDE6MTQuNjld6YKj5bm055qE5rW36aOO5ZC5552A5rKZ5rup5LiK55qE5L2g5oiRClswMToxOC44N11UaGluayBiYWNrIHRvIGNoaWxkaG9vZApbMDE6MjEuMTBd5oiR6K+056ul5bm055qE5pWF5LqL5piv5oiR5ZSx57uZ5L2g55qEClswMToyNi4zMl3orrDlvpflsI/ml7blgJnnnIvov4fnmoTlpb3lpJp2Y2QKWzAxOjI5LjM5XeWImuaJk+W8gOiZueeMq+iTneWFlApbMDE6MzAuNjZd5ZCs5Yiw6ZKl5YyZ5byA6Zeo5Y+Y5oiQ5Y6G6Zmp6K6wClswMTozMy4wNF3ot5Hlm57miL/pl7TmiZPlvIDljaHlo7PnmoTlpI3or7vmnLoKWzAxOjM2LjIyXeaIkeS4jeaDs+WQrOacl+ivu0FCQ0QKWzAxOjM5LjAzXeaFouaFouS4jeefpeS9leaXtgpbMDE6NDAuMDJd5Y+q5pyJ5q+N5Lqy5LiA5Lq65omb6LW35oiR5Lus55qE5a62ClswMTo0MS45OF3pgqPlubTnlJ/ml6XmiJHnlKjpm7boirHpkrEKWzAxOjQzLjU0XemAgeS6huWlueesrOS4gOadn+eOq+eRsOiKsQpbMDE6NDUuMjNd5ou/5Ye65pyf5pyr56ys5LiA6K+V5Y23ClswMTo0Ni43M13lj4zmiYvoh6rosarnmoTkuqTnu5nlpbkKWzAxOjQ4LjMzXeW9k+WlueS9juWktOaIkeeci+WIsOS6humCo+S4gOS4neeZveWPkQpbMDE6NTAuOTVd5aaI5aaI6K+05a6d6LSdClswMTo1Mi4wNl3miJHmsqHnu5nkvaDkuKrlroznvo7nmoTlrrYKWzAxOjUzLjg5XeaIkeWRiuivieWluQpbMDE6NTQuNjdd5pyJ5L2g5Zyo5oiR5bCx5piv5pyA5bm456aP55qE5ZWKClswMTo1Ny4xMV0xOOWygeeUn+aXpeS9oOmAgee7meaIkeesrOS4gOaKiuWQieS7lgpbMDI6MDAuMDNd5a2m5Lya55qE56ys5LiA6aaW5q2MClswMjowMS40NF3miJHlvLnkvaDllLHmu7TnrZTmu7TnrZQKWzAyOjAzLjg0XeS4jeefpemBk+aIkei/mOiDvemZquS9oOWkmuS5hQpbMDI6MDYuMzRd55u05Yiw5oiR55Sf5ZG955qE5pyA5ZCO5LiA5Yi7ClswMjowOS45OV3lpoLmnpzml7blhYnlgJLmtYHlm57pgqPkuIDliIbpkp8KWzAyOjEyLjUyXeiQveWcsOeahOeerOmXtOaIkeWTreS6huS9oOeskeS6hgpbMDI6MTYuMTBd54mZ54mZ5a2m6K+t55qE5pe25YCZ5oiR5Lik5q2l5LiA5Zue5aS0ClswMjoyMi4yMV3ouZLot5rlrabmraXmi4nkvY/kvaDnmoTmiYvntKfot5/lhbblkI4KWzAyOjI4Ljc2XURlYXIgZmFpcnkgSSBJIEkgbG92ZSB5b3UKWzAyOjMxLjY3XURlYXIgYmVhdXR5IEkgSSBJIGxvdmUgeW91ClswMjozNC43OV1EZWFyIGhlcm9pbmUgSSBJIEkgbG92ZSB5b3U=";

        String text = new String(Base64.getDecoder().decode(sou),"GBK");

        log.info(text);
    }
}
