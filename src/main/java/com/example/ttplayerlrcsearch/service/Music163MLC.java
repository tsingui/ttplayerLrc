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
import com.mashape.unirest.request.HttpRequestWithBody;
import com.mashape.unirest.request.body.MultipartBody;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

@Service
@Slf4j
public class Music163MLC extends LRCDispose implements MusicLrcSearch {
    private static final String searchName = "网易云";

    private static final String searchUrl = "https://music.163.com/weapi/cloudsearch/get/web";
    private static final String downlooadUrl = "http://music.163.com/api/song/lyric?id=%s&lv=1&kv=1&tv=-1";


    private String MUSIC_U = "";
    @Value("${MLC.music163.MUSIC_U}")
    private void setToken(String musicU){
        if(StringUtil.notEmpty(musicU)){
            this.MUSIC_U = String.format("MUSIC_U=%s;",musicU);
        }else{
            this.MUSIC_U = "";
        }
    }
    private int pageNum = 50;
    @Value("${MLC.pageNum}")
    private void setPageNum(Integer pageNum) {
        if(pageNum!=null){
            this.pageNum = pageNum;
        }
    }

    @Override
    public String getSearchName() {
        return searchName;
    }

    @Override
    public List<Map<String, String>> search(String artist, String title) {
        String resultText = searchApi(String.format("%s %s", title, artist));
        List<Map<String, String>> songList = parse(resultText);
        return songList;
    }

    @Override
    public ApiResponse download(String musicId) {
        log.info("歌曲id为：{}",musicId);
        String result = downloadLrc(musicId);
        if(StringUtil.isNull(result)){
            return ApiResponse.returnFail("歌词下载失败！");
        }
        return ApiResponse.returnOK().setDataNow(result);
    }

    static {
        //注册BouncyCastle，参考：https://blog.csdn.net/qq_29583513/article/details/78866461
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }
    public Music163MLC(){
        initHeaderMap();
    }

    private Map<String,String> headerMap = new HashMap<>();
    private void initHeaderMap() {
        headerMap.clear();
        headerMap.put("Content-Type","application/x-www-form-urlencoded");
        headerMap.put("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.63 Safari/537.36");
    }

    //算法参考：https://blog.csdn.net/qq_25816185/article/details/81626499
    // AES 算法
    // 需要 Pkcs7 填充算法
    private String b(String text,String key) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException {
        String vi_str = "0102030405060708";
        SecretKeySpec aes = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8),"AES");
        Cipher cbc = Cipher.getInstance("AES/CBC/PKCS7Padding");
        IvParameterSpec iv = new IvParameterSpec(vi_str.getBytes(StandardCharsets.UTF_8));
        //Cipher.ENCRYPT_MODE 加密模式
        cbc.init(Cipher.ENCRYPT_MODE,aes,iv);
        byte[] outBytes = cbc.doFinal(text.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(outBytes);
    }
//    private String b_de(String text,String key) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException {
//        String vi_str = "0102030405060708";
//        SecretKeySpec aes = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8),"AES");
//        Cipher cbc = Cipher.getInstance("AES/CBC/PKCS7Padding");
//        IvParameterSpec iv = new IvParameterSpec(vi_str.getBytes(StandardCharsets.UTF_8));
//        //Cipher.DECRYPT_MODE 解密模式
//        cbc.init(Cipher.DECRYPT_MODE,aes,iv);
//        byte[] outBytes = cbc.doFinal(Base64.getDecoder().decode(text));
//        return new String(outBytes);
//    }

    /**
     *
     * @param a "{"logs":"[{\"action\":\"searchkeywordclient\",\"json\":{\"type\":\"song\",\"keyword\":\"飞鸟和蝉\",\"offset\":0,\"device_id\":\"null\"}}]","csrf_token":"f4bb831c2d7666cb36984c9788391035"}"
     * @param b "010001"
     * @param c "00e0b509f6259df8642dbc35662901477df22677ec152b5ff68ace615bb7b725152b3ab17a876aea8a5aa76d2e417629ec4ee341f56135fccf695280104e0312ecbda92557c93870114af6c9d05c4f7f0c3685b7a46bee255932575cce10b424d813cfe4875d3e82047b97ddef52741d546b8e289dc6935b3ece0462db0a22b8e7"
     * @param d "0CoJUm6Qyw8W8jud"
     * @return
     */
    private Map<String,String> d(String a,String b,String c,String d){
        String encSecKey = "ac63ea8b4e59d7ecdaa1b2d0b7df0e2fb7a269bf830b1ee042efbd0704dda31f4ac4c1680ad7505b3c101fc1c21127d0695d67c7c805e6bdd4a941ec11baf459ca9236674876bd450a2b43571dc80e306766c6f7dccbca7328729c4f5b107fab8a7f2bb3879ea2399db5beb2472c232a1b0e1bf3eac7ff29d7eba1415bc81dce";
        String encText = "";
        //由a随机生成，会影响到后续两个值
        String i = "qYpxGZT3agL1T2o1";
        try {
            String tmp = b(a,d);
            encText = b(tmp,i);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Map<String,String> result = new HashMap<>();
        result.put("encSecKey",encSecKey);
        result.put("encText",encText);
        return result;
    }

    private String searchApi(String text){
        String temple = "{\"hlpretag\":\"<span class=\\\"s-fc7\\\">\",\"hlposttag\":\"</span>\",\"s\":\"%s\",\"type\":\"1\",\"offset\":\"0\",\"total\":\"true\",\"limit\":\"%s\"}";

        // 未登录只能查找到前20首
        Map<String, String> d_en = d(String.format(temple, text, pageNum)
                , "010001"
                , "00e0b509f6259df8642dbc35662901477df22677ec152b5ff68ace615bb7b725152b3ab17a876aea8a5aa76d2e417629ec4ee341f56135fccf695280104e0312ecbda92557c93870114af6c9d05c4f7f0c3685b7a46bee255932575cce10b424d813cfe4875d3e82047b97ddef52741d546b8e289dc6935b3ece0462db0a22b8e7"
                , "0CoJUm6Qyw8W8jud"
        );
        log.debug("params=encText= {}",d_en.get("encText"));
        log.debug("encSecKey=encSecKey= {}",d_en.get("encSecKey"));
        HttpRequestWithBody request = Unirest.post(searchUrl).headers(headerMap);
        if(StringUtil.notEmpty(MUSIC_U)){
            request.header("cookie", MUSIC_U);
        }
        MultipartBody u = request
                //.header("cookie", MUSIC_U)
                .field("params", d_en.get("encText"))
                .field("encSecKey", d_en.get("encSecKey"));

        HttpResponse<String> asString = null;
        try {
            long start = System.currentTimeMillis();
            asString = u.asString();
            log.debug("Api Use Time:{} ms",(System.currentTimeMillis() - start)/1000.0);
        } catch (UnirestException e) {
            e.printStackTrace();
        }
        String data = null;
        data = asString.getBody();
//        log.debug(data);
//        data = "{\"needLogin\":true,\"result\":{\"songs\":[{\"name\":\"飞鸟和蝉\",\"id\":1870160342,\"pst\":0,\"t\":0,\"ar\":[{\"id\":46360460,\"name\":\"夏绿sama\",\"tns\":[],\"alias\":[]}],\"alia\":[],\"pop\":100.0,\"st\":0,\"rt\":\"\",\"fee\":0,\"v\":5,\"crbt\":null,\"cf\":\"\",\"al\":{\"id\":131870089,\"name\":\"飞鸟和蝉\",\"picUrl\":\"http://p3.music.126.net/ancD8WAFzkfvqMdTWd8bVA==/109951166291799778.jpg\",\"tns\":[],\"pic_str\":\"109951166291799778\",\"pic\":109951166291799778},\"dt\":284280,\"h\":{\"br\":320000,\"fid\":0,\"size\":11373165,\"vd\":-48782.0},\"m\":{\"br\":192000,\"fid\":0,\"size\":6823917,\"vd\":-46180.0},\"l\":{\"br\":128000,\"fid\":0,\"size\":4549293,\"vd\":-44549.0},\"a\":null,\"cd\":\"01\",\"no\":1,\"rtUrl\":null,\"ftype\":0,\"rtUrls\":[],\"djId\":0,\"copyright\":0,\"s_id\":0,\"mark\":128,\"originCoverType\":2,\"originSongSimpleData\":null,\"resourceState\":true,\"version\":5,\"single\":0,\"noCopyrightRcmd\":null,\"cp\":0,\"mv\":0,\"rtype\":0,\"rurl\":null,\"mst\":9,\"publishTime\":0,\"privilege\":{\"id\":1870160342,\"fee\":0,\"payed\":0,\"st\":0,\"pl\":320000,\"dl\":999000,\"sp\":7,\"cp\":1,\"subp\":1,\"cs\":false,\"maxbr\":999000,\"fl\":320000,\"toast\":false,\"flag\":1,\"preSell\":false,\"playMaxbr\":999000,\"downloadMaxbr\":999000,\"rscl\":null,\"freeTrialPrivilege\":{\"resConsumable\":false,\"userConsumable\":false},\"chargeInfoList\":[{\"rate\":128000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":0},{\"rate\":192000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":0},{\"rate\":320000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":0},{\"rate\":999000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":1}]}},{\"name\":\"飞鸟和蝉（cover：任然）\",\"id\":1871365037,\"pst\":0,\"t\":0,\"ar\":[{\"id\":31092744,\"name\":\"Kite\",\"tns\":[],\"alias\":[]}],\"alia\":[],\"pop\":100.0,\"st\":0,\"rt\":\"\",\"fee\":0,\"v\":10,\"crbt\":null,\"cf\":\"\",\"al\":{\"id\":131590538,\"name\":\"Kite翻唱专辑\",\"picUrl\":\"http://p4.music.126.net/S8ufTeE6tqleMFCJAHv_nA==/109951166365918521.jpg\",\"tns\":[],\"pic_str\":\"109951166365918521\",\"pic\":109951166365918521},\"dt\":296100,\"h\":{\"br\":320001,\"fid\":0,\"size\":11847097,\"vd\":-21910.0},\"m\":{\"br\":192001,\"fid\":0,\"size\":7108275,\"vd\":-19122.0},\"l\":{\"br\":128001,\"fid\":0,\"size\":4738865,\"vd\":-16902.0},\"a\":null,\"cd\":\"01\",\"no\":2,\"rtUrl\":null,\"ftype\":0,\"rtUrls\":[],\"djId\":0,\"copyright\":0,\"s_id\":0,\"mark\":128,\"originCoverType\":2,\"originSongSimpleData\":null,\"resourceState\":true,\"version\":10,\"single\":0,\"noCopyrightRcmd\":null,\"cp\":0,\"mv\":0,\"rtype\":0,\"rurl\":null,\"mst\":9,\"publishTime\":0,\"privilege\":{\"id\":1871365037,\"fee\":0,\"payed\":0,\"st\":0,\"pl\":320000,\"dl\":999000,\"sp\":7,\"cp\":1,\"subp\":1,\"cs\":false,\"maxbr\":999000,\"fl\":320000,\"toast\":false,\"flag\":1,\"preSell\":false,\"playMaxbr\":999000,\"downloadMaxbr\":999000,\"rscl\":null,\"freeTrialPrivilege\":{\"resConsumable\":false,\"userConsumable\":false},\"chargeInfoList\":[{\"rate\":128000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":0},{\"rate\":192000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":0},{\"rate\":320000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":0},{\"rate\":999000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":1}]}},{\"name\":\"飞鸟和蝉\",\"id\":1875758160,\"pst\":0,\"t\":0,\"ar\":[{\"id\":47703896,\"name\":\"麻雀\",\"tns\":[],\"alias\":[]}],\"alia\":[],\"pop\":95.0,\"st\":0,\"rt\":\"\",\"fee\":0,\"v\":3,\"crbt\":null,\"cf\":\"\",\"al\":{\"id\":126885900,\"name\":\"米浆\",\"picUrl\":\"http://p3.music.126.net/S8eDfFi3Cw6mlLAtxt3lUQ==/109951166359264629.jpg\",\"tns\":[],\"pic_str\":\"109951166359264629\",\"pic\":109951166359264629},\"dt\":295810,\"h\":null,\"m\":{\"br\":192000,\"fid\":0,\"size\":7100752,\"vd\":-16965.0},\"l\":{\"br\":128000,\"fid\":0,\"size\":4733849,\"vd\":-15332.0},\"a\":null,\"cd\":\"01\",\"no\":1,\"rtUrl\":null,\"ftype\":0,\"rtUrls\":[],\"djId\":0,\"copyright\":0,\"s_id\":0,\"mark\":0,\"originCoverType\":2,\"originSongSimpleData\":{\"songId\":1460571716,\"name\":\"飞鸟和蝉\",\"artists\":[{\"id\":9255,\"name\":\"任然\"}],\"albumMeta\":{\"id\":91981856,\"name\":\"飞鸟和蝉\"}},\"resourceState\":true,\"version\":3,\"single\":0,\"noCopyrightRcmd\":null,\"cp\":0,\"mv\":0,\"rtype\":0,\"rurl\":null,\"mst\":9,\"publishTime\":0,\"privilege\":{\"id\":1875758160,\"fee\":0,\"payed\":0,\"st\":0,\"pl\":320000,\"dl\":999000,\"sp\":7,\"cp\":1,\"subp\":1,\"cs\":false,\"maxbr\":999000,\"fl\":320000,\"toast\":false,\"flag\":128,\"preSell\":false,\"playMaxbr\":999000,\"downloadMaxbr\":999000,\"rscl\":null,\"freeTrialPrivilege\":{\"resConsumable\":false,\"userConsumable\":false},\"chargeInfoList\":[{\"rate\":128000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":0},{\"rate\":192000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":0},{\"rate\":320000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":0},{\"rate\":999000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":1}]}},{\"name\":\"飞鸟和蝉（原唱）\",\"id\":1869200578,\"pst\":0,\"t\":0,\"ar\":[{\"id\":48719052,\"name\":\"李茜\",\"tns\":[],\"alias\":[]}],\"alia\":[],\"pop\":90.0,\"st\":0,\"rt\":\"\",\"fee\":8,\"v\":5,\"crbt\":null,\"cf\":\"\",\"al\":{\"id\":131737374,\"name\":\"你骄傲的飞远我栖息的夏天\",\"picUrl\":\"http://p4.music.126.net/BzNWTfzlnWBqamTHaTcsRQ==/109951166279685441.jpg\",\"tns\":[],\"pic_str\":\"109951166279685441\",\"pic\":109951166279685441},\"dt\":111292,\"h\":{\"br\":320000,\"fid\":0,\"size\":4454444,\"vd\":2327.0},\"m\":{\"br\":192000,\"fid\":0,\"size\":2672684,\"vd\":4945.0},\"l\":{\"br\":128000,\"fid\":0,\"size\":1781804,\"vd\":6618.0},\"a\":null,\"cd\":\"01\",\"no\":1,\"rtUrl\":null,\"ftype\":0,\"rtUrls\":[],\"djId\":0,\"copyright\":0,\"s_id\":0,\"mark\":524288,\"originCoverType\":0,\"originSongSimpleData\":null,\"resourceState\":true,\"version\":5,\"single\":0,\"noCopyrightRcmd\":null,\"cp\":0,\"mv\":0,\"rtype\":0,\"rurl\":null,\"mst\":9,\"publishTime\":0,\"privilege\":{\"id\":1869200578,\"fee\":8,\"payed\":0,\"st\":0,\"pl\":128000,\"dl\":0,\"sp\":7,\"cp\":1,\"subp\":1,\"cs\":false,\"maxbr\":999000,\"fl\":128000,\"toast\":false,\"flag\":0,\"preSell\":false,\"playMaxbr\":999000,\"downloadMaxbr\":999000,\"rscl\":null,\"freeTrialPrivilege\":{\"resConsumable\":false,\"userConsumable\":false},\"chargeInfoList\":[{\"rate\":128000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":0},{\"rate\":192000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":1},{\"rate\":320000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":1},{\"rate\":999000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":1}]}},{\"name\":\"任然-飞鸟和蝉（DJJV Bounce Mix）（DJJV remix）\",\"id\":1871976311,\"pst\":0,\"t\":0,\"ar\":[{\"id\":1050730,\"name\":\"DJJV\",\"tns\":[],\"alias\":[]}],\"alia\":[],\"pop\":85.0,\"st\":0,\"rt\":\"\",\"fee\":0,\"v\":4,\"crbt\":null,\"cf\":\"\",\"al\":{\"id\":132150830,\"name\":\"中文Bounce Style Vol.1\",\"picUrl\":\"http://p4.music.126.net/bvCWRQpOpzVoeWpJR21dbA==/109951166311896992.jpg\",\"tns\":[],\"pic_str\":\"109951166311896992\",\"pic\":109951166311896992},\"dt\":518948,\"h\":{\"br\":320000,\"fid\":0,\"size\":20760076,\"vd\":-81692.0},\"m\":{\"br\":192000,\"fid\":0,\"size\":12456063,\"vd\":-79361.0},\"l\":{\"br\":128000,\"fid\":0,\"size\":8304057,\"vd\":-77916.0},\"a\":null,\"cd\":\"01\",\"no\":5,\"rtUrl\":null,\"ftype\":0,\"rtUrls\":[],\"djId\":0,\"copyright\":0,\"s_id\":0,\"mark\":128,\"originCoverType\":0,\"originSongSimpleData\":null,\"resourceState\":true,\"version\":4,\"single\":0,\"noCopyrightRcmd\":null,\"cp\":0,\"mv\":0,\"rtype\":0,\"rurl\":null,\"mst\":9,\"publishTime\":0,\"privilege\":{\"id\":1871976311,\"fee\":0,\"payed\":0,\"st\":0,\"pl\":320000,\"dl\":320000,\"sp\":7,\"cp\":1,\"subp\":1,\"cs\":false,\"maxbr\":320000,\"fl\":320000,\"toast\":false,\"flag\":129,\"preSell\":false,\"playMaxbr\":320000,\"downloadMaxbr\":320000,\"rscl\":null,\"freeTrialPrivilege\":{\"resConsumable\":false,\"userConsumable\":false},\"chargeInfoList\":[{\"rate\":128000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":0},{\"rate\":192000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":0},{\"rate\":320000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":0},{\"rate\":999000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":1}]}},{\"name\":\"飞鸟和蝉\",\"id\":1872338487,\"pst\":0,\"t\":0,\"ar\":[{\"id\":49283331,\"name\":\"Fancii6\",\"tns\":[],\"alias\":[]}],\"alia\":[],\"pop\":90.0,\"st\":0,\"rt\":\"\",\"fee\":8,\"v\":4,\"crbt\":null,\"cf\":\"\",\"al\":{\"id\":132221729,\"name\":\"闲的一半\",\"picUrl\":\"http://p4.music.126.net/L5WDqcfJS8IfN_RBZBP3Bg==/109951166316895524.jpg\",\"tns\":[],\"pic_str\":\"109951166316895524\",\"pic\":109951166316895524},\"dt\":295332,\"h\":{\"br\":320000,\"fid\":0,\"size\":11815750,\"vd\":-1801.0},\"m\":{\"br\":192000,\"fid\":0,\"size\":7089467,\"vd\":787.0},\"l\":{\"br\":128000,\"fid\":0,\"size\":4726326,\"vd\":2436.0},\"a\":null,\"cd\":\"01\",\"no\":1,\"rtUrl\":null,\"ftype\":0,\"rtUrls\":[],\"djId\":0,\"copyright\":0,\"s_id\":0,\"mark\":64,\"originCoverType\":0,\"originSongSimpleData\":null,\"resourceState\":true,\"version\":4,\"single\":0,\"noCopyrightRcmd\":null,\"cp\":0,\"mv\":0,\"rtype\":0,\"rurl\":null,\"mst\":9,\"publishTime\":0,\"privilege\":{\"id\":1872338487,\"fee\":8,\"payed\":0,\"st\":0,\"pl\":128000,\"dl\":0,\"sp\":7,\"cp\":1,\"subp\":1,\"cs\":false,\"maxbr\":999000,\"fl\":128000,\"toast\":false,\"flag\":2,\"preSell\":false,\"playMaxbr\":999000,\"downloadMaxbr\":999000,\"rscl\":null,\"freeTrialPrivilege\":{\"resConsumable\":false,\"userConsumable\":false},\"chargeInfoList\":[{\"rate\":128000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":0},{\"rate\":192000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":1},{\"rate\":320000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":1},{\"rate\":999000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":1}]}},{\"name\":\"飞鸟和蝉\",\"id\":1875301972,\"pst\":0,\"t\":0,\"ar\":[{\"id\":49087674,\"name\":\"YoYo\",\"tns\":[],\"alias\":[]}],\"alia\":[],\"pop\":35.0,\"st\":0,\"rt\":\"\",\"fee\":8,\"v\":3,\"crbt\":null,\"cf\":\"\",\"al\":{\"id\":131349648,\"name\":\"Demo\",\"picUrl\":\"http://p3.music.126.net/HuqLwM_n1nzSKZB9YqaSbA==/109951166277341291.jpg\",\"tns\":[],\"pic_str\":\"109951166277341291\",\"pic\":109951166277341291},\"dt\":290716,\"h\":{\"br\":320000,\"fid\":0,\"size\":11630803,\"vd\":19695.0},\"m\":{\"br\":192000,\"fid\":0,\"size\":6978499,\"vd\":22275.0},\"l\":{\"br\":128000,\"fid\":0,\"size\":4652347,\"vd\":23800.0},\"a\":null,\"cd\":\"01\",\"no\":15,\"rtUrl\":null,\"ftype\":0,\"rtUrls\":[],\"djId\":0,\"copyright\":0,\"s_id\":0,\"mark\":0,\"originCoverType\":0,\"originSongSimpleData\":null,\"resourceState\":true,\"version\":3,\"single\":0,\"noCopyrightRcmd\":null,\"cp\":0,\"mv\":0,\"rtype\":0,\"rurl\":null,\"mst\":9,\"publishTime\":0,\"privilege\":{\"id\":1875301972,\"fee\":8,\"payed\":0,\"st\":0,\"pl\":128000,\"dl\":0,\"sp\":7,\"cp\":1,\"subp\":1,\"cs\":false,\"maxbr\":999000,\"fl\":128000,\"toast\":false,\"flag\":0,\"preSell\":false,\"playMaxbr\":999000,\"downloadMaxbr\":999000,\"rscl\":null,\"freeTrialPrivilege\":{\"resConsumable\":false,\"userConsumable\":false},\"chargeInfoList\":[{\"rate\":128000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":0},{\"rate\":192000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":1},{\"rate\":320000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":1},{\"rate\":999000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":1}]}},{\"name\":\"飞鸟和蝉(抖音片段)\",\"id\":1831479468,\"pst\":0,\"t\":0,\"ar\":[{\"id\":32060448,\"name\":\"浩然吖\",\"tns\":[],\"alias\":[]}],\"alia\":[],\"pop\":30.0,\"st\":0,\"rt\":\"\",\"fee\":8,\"v\":2,\"crbt\":null,\"cf\":\"\",\"al\":{\"id\":124901386,\"name\":\"飞鸟和蝉(抖音片段)\",\"picUrl\":\"http://p3.music.126.net/cD_cf9Rszj3uI_9NMMbxGw==/109951165825635328.jpg\",\"tns\":[],\"pic_str\":\"109951165825635328\",\"pic\":109951165825635328},\"dt\":161697,\"h\":{\"br\":320000,\"fid\":0,\"size\":6470052,\"vd\":7409.0},\"m\":{\"br\":192000,\"fid\":0,\"size\":3882049,\"vd\":10041.0},\"l\":{\"br\":128000,\"fid\":0,\"size\":2588047,\"vd\":11800.0},\"a\":null,\"cd\":\"01\",\"no\":5,\"rtUrl\":null,\"ftype\":0,\"rtUrls\":[],\"djId\":0,\"copyright\":1,\"s_id\":0,\"mark\":0,\"originCoverType\":0,\"originSongSimpleData\":null,\"resourceState\":true,\"version\":2,\"single\":0,\"noCopyrightRcmd\":null,\"cp\":1416503,\"mv\":0,\"rtype\":0,\"rurl\":null,\"mst\":9,\"publishTime\":1546272000000,\"privilege\":{\"id\":1831479468,\"fee\":8,\"payed\":0,\"st\":0,\"pl\":128000,\"dl\":0,\"sp\":7,\"cp\":1,\"subp\":1,\"cs\":false,\"maxbr\":999000,\"fl\":128000,\"toast\":false,\"flag\":0,\"preSell\":false,\"playMaxbr\":999000,\"downloadMaxbr\":999000,\"rscl\":null,\"freeTrialPrivilege\":{\"resConsumable\":false,\"userConsumable\":false},\"chargeInfoList\":[{\"rate\":128000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":0},{\"rate\":192000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":1},{\"rate\":320000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":1},{\"rate\":999000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":1}]}},{\"name\":\"飞鸟和蝉\",\"id\":1835373187,\"pst\":0,\"t\":0,\"ar\":[{\"id\":34001336,\"name\":\"小也\",\"tns\":[],\"alias\":[]}],\"alia\":[],\"pop\":60.0,\"st\":0,\"rt\":\"\",\"fee\":8,\"v\":2,\"crbt\":null,\"cf\":\"\",\"al\":{\"id\":88683678,\"name\":\"小也纯音乐\",\"picUrl\":\"http://p4.music.126.net/7nSpL0dlV891d6D-5aQwJQ==/109951164947448962.jpg\",\"tns\":[],\"pic_str\":\"109951164947448962\",\"pic\":109951164947448962},\"dt\":266475,\"h\":{\"br\":320000,\"fid\":0,\"size\":10661138,\"vd\":1410.0},\"m\":{\"br\":192000,\"fid\":0,\"size\":6396700,\"vd\":4042.0},\"l\":{\"br\":128000,\"fid\":0,\"size\":4264481,\"vd\":5798.0},\"a\":null,\"cd\":\"01\",\"no\":1,\"rtUrl\":null,\"ftype\":0,\"rtUrls\":[],\"djId\":0,\"copyright\":0,\"s_id\":0,\"mark\":131136,\"originCoverType\":0,\"originSongSimpleData\":null,\"resourceState\":true,\"version\":2,\"single\":0,\"noCopyrightRcmd\":null,\"cp\":0,\"mv\":0,\"rtype\":0,\"rurl\":null,\"mst\":9,\"publishTime\":0,\"privilege\":{\"id\":1835373187,\"fee\":8,\"payed\":0,\"st\":0,\"pl\":128000,\"dl\":0,\"sp\":7,\"cp\":1,\"subp\":1,\"cs\":false,\"maxbr\":999000,\"fl\":128000,\"toast\":false,\"flag\":2,\"preSell\":false,\"playMaxbr\":999000,\"downloadMaxbr\":999000,\"rscl\":null,\"freeTrialPrivilege\":{\"resConsumable\":false,\"userConsumable\":false},\"chargeInfoList\":[{\"rate\":128000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":0},{\"rate\":192000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":1},{\"rate\":320000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":1},{\"rate\":999000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":1}]}},{\"name\":\"飞鸟和蝉\",\"id\":1475461837,\"pst\":0,\"t\":0,\"ar\":[{\"id\":32781069,\"name\":\"音昱\",\"tns\":[],\"alias\":[]}],\"alia\":[],\"pop\":25.0,\"st\":0,\"rt\":\"\",\"fee\":0,\"v\":9,\"crbt\":null,\"cf\":\"\",\"al\":{\"id\":82008608,\"name\":\"钢琴夜曲\",\"picUrl\":\"http://p3.music.126.net/03sr4jSxGxYGISGdOCwV1g==/109951164401019655.jpg\",\"tns\":[],\"pic_str\":\"109951164401019655\",\"pic\":109951164401019655},\"dt\":157377,\"h\":{\"br\":320000,\"fid\":0,\"size\":6297645,\"vd\":10428.0},\"m\":{\"br\":192000,\"fid\":0,\"size\":3778605,\"vd\":13074.0},\"l\":{\"br\":128000,\"fid\":0,\"size\":2519085,\"vd\":14881.0},\"a\":null,\"cd\":\"01\",\"no\":1,\"rtUrl\":null,\"ftype\":0,\"rtUrls\":[],\"djId\":0,\"copyright\":0,\"s_id\":0,\"mark\":131200,\"originCoverType\":0,\"originSongSimpleData\":null,\"resourceState\":true,\"version\":9,\"single\":0,\"noCopyrightRcmd\":null,\"cp\":0,\"mv\":0,\"rtype\":0,\"rurl\":null,\"mst\":9,\"publishTime\":0,\"privilege\":{\"id\":1475461837,\"fee\":0,\"payed\":0,\"st\":0,\"pl\":320000,\"dl\":999000,\"sp\":7,\"cp\":1,\"subp\":1,\"cs\":false,\"maxbr\":999000,\"fl\":320000,\"toast\":false,\"flag\":1,\"preSell\":false,\"playMaxbr\":999000,\"downloadMaxbr\":999000,\"rscl\":null,\"freeTrialPrivilege\":{\"resConsumable\":false,\"userConsumable\":false},\"chargeInfoList\":[{\"rate\":128000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":0},{\"rate\":192000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":0},{\"rate\":320000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":0},{\"rate\":999000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":1}]}},{\"name\":\"飞鸟和蝉(抖音DJ热搜版)\",\"id\":1831451434,\"pst\":0,\"t\":0,\"ar\":[{\"id\":47111182,\"name\":\"绝世的陈逗逗\",\"tns\":[],\"alias\":[]}],\"alia\":[],\"pop\":25.0,\"st\":0,\"rt\":\"\",\"fee\":8,\"v\":9,\"crbt\":null,\"cf\":\"\",\"al\":{\"id\":124894108,\"name\":\"断桥残雪\",\"picUrl\":\"http://p4.music.126.net/_sDmgs1nOSxJf_Y78d_mNQ==/109951165825446089.jpg\",\"tns\":[],\"pic_str\":\"109951165825446089\",\"pic\":109951165825446089},\"dt\":256966,\"h\":{\"br\":320000,\"fid\":0,\"size\":10280795,\"vd\":-66563.0},\"m\":{\"br\":192000,\"fid\":0,\"size\":6168494,\"vd\":-64005.0},\"l\":{\"br\":128000,\"fid\":0,\"size\":4112344,\"vd\":-62706.0},\"a\":null,\"cd\":\"01\",\"no\":8,\"rtUrl\":null,\"ftype\":0,\"rtUrls\":[],\"djId\":0,\"copyright\":1,\"s_id\":0,\"mark\":0,\"originCoverType\":0,\"originSongSimpleData\":null,\"resourceState\":true,\"version\":9,\"single\":0,\"noCopyrightRcmd\":null,\"cp\":1416503,\"mv\":0,\"rtype\":0,\"rurl\":null,\"mst\":9,\"publishTime\":1546272000000,\"privilege\":{\"id\":1831451434,\"fee\":8,\"payed\":0,\"st\":0,\"pl\":128000,\"dl\":0,\"sp\":7,\"cp\":1,\"subp\":1,\"cs\":false,\"maxbr\":999000,\"fl\":128000,\"toast\":false,\"flag\":0,\"preSell\":false,\"playMaxbr\":999000,\"downloadMaxbr\":999000,\"rscl\":null,\"freeTrialPrivilege\":{\"resConsumable\":false,\"userConsumable\":false},\"chargeInfoList\":[{\"rate\":128000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":0},{\"rate\":192000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":1},{\"rate\":320000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":1},{\"rate\":999000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":1}]}},{\"name\":\"飞鸟和蝉dj\",\"id\":1876273102,\"pst\":0,\"t\":0,\"ar\":[{\"id\":49606569,\"name\":\"Liu Daming\",\"tns\":[],\"alias\":[]}],\"alia\":[],\"pop\":15.0,\"st\":0,\"rt\":\"\",\"fee\":0,\"v\":2,\"crbt\":null,\"cf\":\"\",\"al\":{\"id\":132872298,\"name\":\"Battlestar\",\"picUrl\":\"http://p4.music.126.net/zsZYi34AA2K5OHZC9RpoZg==/109951166363712137.jpg\",\"tns\":[],\"pic_str\":\"109951166363712137\",\"pic\":109951166363712137},\"dt\":138144,\"h\":{\"br\":320000,\"fid\":0,\"size\":5528685,\"vd\":-86784.0},\"m\":{\"br\":192000,\"fid\":0,\"size\":3317229,\"vd\":-83809.0},\"l\":{\"br\":128000,\"fid\":0,\"size\":2211501,\"vd\":-81759.0},\"a\":null,\"cd\":\"01\",\"no\":1,\"rtUrl\":null,\"ftype\":0,\"rtUrls\":[],\"djId\":0,\"copyright\":0,\"s_id\":0,\"mark\":655488,\"originCoverType\":2,\"originSongSimpleData\":null,\"resourceState\":true,\"version\":2,\"single\":0,\"noCopyrightRcmd\":null,\"cp\":0,\"mv\":0,\"rtype\":0,\"rurl\":null,\"mst\":9,\"publishTime\":0,\"privilege\":{\"id\":1876273102,\"fee\":0,\"payed\":0,\"st\":0,\"pl\":320000,\"dl\":999000,\"sp\":7,\"cp\":1,\"subp\":1,\"cs\":false,\"maxbr\":999000,\"fl\":320000,\"toast\":false,\"flag\":129,\"preSell\":false,\"playMaxbr\":999000,\"downloadMaxbr\":999000,\"rscl\":null,\"freeTrialPrivilege\":{\"resConsumable\":false,\"userConsumable\":false},\"chargeInfoList\":[{\"rate\":128000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":0},{\"rate\":192000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":0},{\"rate\":320000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":0},{\"rate\":999000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":1}]}},{\"name\":\"飞鸟和蝉(抖音热搜版)\",\"id\":1831818068,\"pst\":0,\"t\":0,\"ar\":[{\"id\":31148207,\"name\":\"浩然吖\",\"tns\":[],\"alias\":[]}],\"alia\":[],\"pop\":60.0,\"st\":0,\"rt\":\"\",\"fee\":8,\"v\":2,\"crbt\":null,\"cf\":\"\",\"al\":{\"id\":124997341,\"name\":\"飞鸟和蝉(抖音热搜版)\",\"picUrl\":\"http://p4.music.126.net/p8GVapkiFMmJ__QZgXm1HA==/109951165827861747.jpg\",\"tns\":[],\"pic_str\":\"109951165827861747\",\"pic\":109951165827861747},\"dt\":260702,\"h\":{\"br\":320000,\"fid\":0,\"size\":10430215,\"vd\":-56482.0},\"m\":{\"br\":192000,\"fid\":0,\"size\":6258146,\"vd\":-53845.0},\"l\":{\"br\":128000,\"fid\":0,\"size\":4172112,\"vd\":-52067.0},\"a\":null,\"cd\":\"01\",\"no\":1,\"rtUrl\":null,\"ftype\":0,\"rtUrls\":[],\"djId\":0,\"copyright\":1,\"s_id\":0,\"mark\":0,\"originCoverType\":0,\"originSongSimpleData\":null,\"resourceState\":true,\"version\":2,\"single\":0,\"noCopyrightRcmd\":null,\"cp\":1416503,\"mv\":0,\"rtype\":0,\"rurl\":null,\"mst\":9,\"publishTime\":1546272000000,\"privilege\":{\"id\":1831818068,\"fee\":8,\"payed\":0,\"st\":0,\"pl\":128000,\"dl\":0,\"sp\":7,\"cp\":1,\"subp\":1,\"cs\":false,\"maxbr\":999000,\"fl\":128000,\"toast\":false,\"flag\":0,\"preSell\":false,\"playMaxbr\":999000,\"downloadMaxbr\":999000,\"rscl\":null,\"freeTrialPrivilege\":{\"resConsumable\":false,\"userConsumable\":false},\"chargeInfoList\":[{\"rate\":128000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":0},{\"rate\":192000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":1},{\"rate\":320000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":1},{\"rate\":999000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":1}]}},{\"name\":\"飞鸟和蝉\",\"id\":1831756595,\"pst\":0,\"t\":0,\"ar\":[{\"id\":46435909,\"name\":\"AYuan\",\"tns\":[],\"alias\":[]}],\"alia\":[],\"pop\":25.0,\"st\":0,\"rt\":\"\",\"fee\":8,\"v\":2,\"crbt\":null,\"cf\":\"\",\"al\":{\"id\":124984427,\"name\":\"飞鸟和蝉\",\"picUrl\":\"http://p3.music.126.net/H3LFOlBQavUIcXSz5ekUeA==/109951165827553062.jpg\",\"tns\":[],\"pic_str\":\"109951165827553062\",\"pic\":109951165827553062},\"dt\":203920,\"h\":{\"br\":320000,\"fid\":0,\"size\":8159652,\"vd\":-51166.0},\"m\":{\"br\":192000,\"fid\":0,\"size\":4895809,\"vd\":-48639.0},\"l\":{\"br\":128000,\"fid\":0,\"size\":3263887,\"vd\":-47311.0},\"a\":null,\"cd\":\"01\",\"no\":1,\"rtUrl\":null,\"ftype\":0,\"rtUrls\":[],\"djId\":0,\"copyright\":1,\"s_id\":0,\"mark\":0,\"originCoverType\":0,\"originSongSimpleData\":null,\"resourceState\":true,\"version\":2,\"single\":0,\"noCopyrightRcmd\":null,\"cp\":1416503,\"mv\":0,\"rtype\":0,\"rurl\":null,\"mst\":9,\"publishTime\":1546272000000,\"privilege\":{\"id\":1831756595,\"fee\":8,\"payed\":0,\"st\":0,\"pl\":128000,\"dl\":0,\"sp\":7,\"cp\":1,\"subp\":1,\"cs\":false,\"maxbr\":999000,\"fl\":128000,\"toast\":false,\"flag\":0,\"preSell\":false,\"playMaxbr\":999000,\"downloadMaxbr\":999000,\"rscl\":null,\"freeTrialPrivilege\":{\"resConsumable\":false,\"userConsumable\":false},\"chargeInfoList\":[{\"rate\":128000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":0},{\"rate\":192000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":1},{\"rate\":320000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":1},{\"rate\":999000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":1}]}},{\"name\":\"飞鸟和蝉(抖音DJ完整版)\",\"id\":1831800118,\"pst\":0,\"t\":0,\"ar\":[{\"id\":47143674,\"name\":\"东泽润\",\"tns\":[],\"alias\":[]}],\"alia\":[],\"pop\":20.0,\"st\":0,\"rt\":\"\",\"fee\":8,\"v\":2,\"crbt\":null,\"cf\":\"\",\"al\":{\"id\":124994258,\"name\":\"何日是归途\",\"picUrl\":\"http://p3.music.126.net/4TSCT4h1Iw2NmSMTkd6Adw==/109951165827748427.jpg\",\"tns\":[],\"pic_str\":\"109951165827748427\",\"pic\":109951165827748427},\"dt\":239830,\"h\":{\"br\":320000,\"fid\":0,\"size\":9595342,\"vd\":-77908.0},\"m\":{\"br\":192000,\"fid\":0,\"size\":5757222,\"vd\":-75451.0},\"l\":{\"br\":128000,\"fid\":0,\"size\":3838163,\"vd\":-74507.0},\"a\":null,\"cd\":\"01\",\"no\":1,\"rtUrl\":null,\"ftype\":0,\"rtUrls\":[],\"djId\":0,\"copyright\":1,\"s_id\":0,\"mark\":0,\"originCoverType\":0,\"originSongSimpleData\":null,\"resourceState\":true,\"version\":2,\"single\":0,\"noCopyrightRcmd\":null,\"cp\":1416503,\"mv\":0,\"rtype\":0,\"rurl\":null,\"mst\":9,\"publishTime\":1546272000000,\"privilege\":{\"id\":1831800118,\"fee\":8,\"payed\":0,\"st\":0,\"pl\":128000,\"dl\":0,\"sp\":7,\"cp\":1,\"subp\":1,\"cs\":false,\"maxbr\":999000,\"fl\":128000,\"toast\":false,\"flag\":0,\"preSell\":false,\"playMaxbr\":999000,\"downloadMaxbr\":999000,\"rscl\":null,\"freeTrialPrivilege\":{\"resConsumable\":false,\"userConsumable\":false},\"chargeInfoList\":[{\"rate\":128000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":0},{\"rate\":192000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":1},{\"rate\":320000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":1},{\"rate\":999000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":1}]}},{\"name\":\"飞鸟和蝉\",\"id\":1872789352,\"pst\":0,\"t\":0,\"ar\":[{\"id\":12085096,\"name\":\"阿进二\",\"tns\":[],\"alias\":[]},{\"id\":0,\"name\":\"四弦一声\",\"tns\":[],\"alias\":[]}],\"alia\":[],\"pop\":15.0,\"st\":0,\"rt\":\"\",\"fee\":0,\"v\":6,\"crbt\":null,\"cf\":\"\",\"al\":{\"id\":132295554,\"name\":\"四弦一声排练记录\",\"picUrl\":\"http://p3.music.126.net/7_b_fsPMoHJOmcQbw6AIOw==/109951166322146093.jpg\",\"tns\":[],\"pic_str\":\"109951166322146093\",\"pic\":109951166322146093},\"dt\":291891,\"h\":{\"br\":320000,\"fid\":0,\"size\":11677823,\"vd\":-22454.0},\"m\":{\"br\":192000,\"fid\":0,\"size\":7006711,\"vd\":-19823.0},\"l\":null,\"a\":null,\"cd\":\"01\",\"no\":1,\"rtUrl\":null,\"ftype\":0,\"rtUrls\":[],\"djId\":0,\"copyright\":0,\"s_id\":0,\"mark\":128,\"originCoverType\":2,\"originSongSimpleData\":null,\"resourceState\":true,\"version\":6,\"single\":0,\"noCopyrightRcmd\":null,\"cp\":0,\"mv\":0,\"rtype\":0,\"rurl\":null,\"mst\":9,\"publishTime\":0,\"privilege\":{\"id\":1872789352,\"fee\":0,\"payed\":0,\"st\":0,\"pl\":320000,\"dl\":999000,\"sp\":7,\"cp\":1,\"subp\":1,\"cs\":false,\"maxbr\":999000,\"fl\":320000,\"toast\":false,\"flag\":129,\"preSell\":false,\"playMaxbr\":999000,\"downloadMaxbr\":999000,\"rscl\":null,\"freeTrialPrivilege\":{\"resConsumable\":false,\"userConsumable\":false},\"chargeInfoList\":[{\"rate\":128000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":0},{\"rate\":192000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":0},{\"rate\":320000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":0},{\"rate\":999000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":1}]}},{\"name\":\"飞鸟和蝉(男声版)\",\"id\":1871782048,\"pst\":0,\"t\":0,\"ar\":[{\"id\":34539384,\"name\":\"浪子阿超\",\"tns\":[],\"alias\":[]}],\"alia\":[],\"pop\":15.0,\"st\":0,\"rt\":\"\",\"fee\":0,\"v\":4,\"crbt\":null,\"cf\":\"\",\"al\":{\"id\":132129237,\"name\":\"夏未了\",\"picUrl\":\"http://p4.music.126.net/rkHkllg1GYbHLYpPz-kk2w==/109951166309563459.jpg\",\"tns\":[],\"pic_str\":\"109951166309563459\",\"pic\":109951166309563459},\"dt\":82988,\"h\":{\"br\":320000,\"fid\":0,\"size\":3321774,\"vd\":-29233.0},\"m\":{\"br\":192000,\"fid\":0,\"size\":1993082,\"vd\":-26624.0},\"l\":{\"br\":128000,\"fid\":0,\"size\":1328736,\"vd\":-24946.0},\"a\":null,\"cd\":\"01\",\"no\":1,\"rtUrl\":null,\"ftype\":0,\"rtUrls\":[],\"djId\":0,\"copyright\":0,\"s_id\":0,\"mark\":128,\"originCoverType\":2,\"originSongSimpleData\":null,\"resourceState\":true,\"version\":4,\"single\":0,\"noCopyrightRcmd\":null,\"cp\":0,\"mv\":0,\"rtype\":0,\"rurl\":null,\"mst\":9,\"publishTime\":0,\"privilege\":{\"id\":1871782048,\"fee\":0,\"payed\":0,\"st\":0,\"pl\":320000,\"dl\":999000,\"sp\":7,\"cp\":1,\"subp\":1,\"cs\":false,\"maxbr\":999000,\"fl\":320000,\"toast\":false,\"flag\":129,\"preSell\":false,\"playMaxbr\":999000,\"downloadMaxbr\":999000,\"rscl\":null,\"freeTrialPrivilege\":{\"resConsumable\":false,\"userConsumable\":false},\"chargeInfoList\":[{\"rate\":128000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":0},{\"rate\":192000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":0},{\"rate\":320000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":0},{\"rate\":999000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":1}]}},{\"name\":\"飞鸟和蝉(热搜版)\",\"id\":1831826595,\"pst\":0,\"t\":0,\"ar\":[{\"id\":47144534,\"name\":\"香瓜啊\",\"tns\":[],\"alias\":[]}],\"alia\":[],\"pop\":20.0,\"st\":0,\"rt\":\"\",\"fee\":8,\"v\":8,\"crbt\":null,\"cf\":\"\",\"al\":{\"id\":124999627,\"name\":\"香瓜啊合集\",\"picUrl\":\"http://p3.music.126.net/UeTuwE7pvjBpypWLudqukA==/3132508627578625.jpg\",\"tns\":[],\"pic\":0},\"dt\":575424,\"h\":{\"br\":320000,\"fid\":0,\"size\":23018925,\"vd\":-60906.0},\"m\":{\"br\":192000,\"fid\":0,\"size\":13811373,\"vd\":-58353.0},\"l\":{\"br\":128000,\"fid\":0,\"size\":9207597,\"vd\":-56983.0},\"a\":null,\"cd\":\"01\",\"no\":9,\"rtUrl\":null,\"ftype\":0,\"rtUrls\":[],\"djId\":0,\"copyright\":1,\"s_id\":0,\"mark\":0,\"originCoverType\":0,\"originSongSimpleData\":null,\"resourceState\":true,\"version\":8,\"single\":0,\"noCopyrightRcmd\":null,\"cp\":1416503,\"mv\":0,\"rtype\":0,\"rurl\":null,\"mst\":9,\"publishTime\":1546272000000,\"privilege\":{\"id\":1831826595,\"fee\":8,\"payed\":0,\"st\":0,\"pl\":128000,\"dl\":0,\"sp\":7,\"cp\":1,\"subp\":1,\"cs\":false,\"maxbr\":320000,\"fl\":128000,\"toast\":false,\"flag\":0,\"preSell\":false,\"playMaxbr\":320000,\"downloadMaxbr\":320000,\"rscl\":null,\"freeTrialPrivilege\":{\"resConsumable\":false,\"userConsumable\":false},\"chargeInfoList\":[{\"rate\":128000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":0},{\"rate\":192000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":1},{\"rate\":320000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":1},{\"rate\":999000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":1}]}},{\"name\":\"飛鳥和蟬\",\"id\":1478040536,\"pst\":0,\"t\":0,\"ar\":[{\"id\":49104864,\"name\":\"Isaac Yong\",\"tns\":[],\"alias\":[]}],\"alia\":[],\"pop\":5.0,\"st\":0,\"rt\":\"\",\"fee\":8,\"v\":1,\"crbt\":null,\"cf\":\"\",\"al\":{\"id\":95200969,\"name\":\"飛鳥和蟬\",\"picUrl\":\"http://p3.music.126.net/dAvAx8FNhbdg6Fcx839abg==/109951165306768515.jpg\",\"tns\":[],\"pic_str\":\"109951165306768515\",\"pic\":109951165306768515},\"dt\":293513,\"h\":{\"br\":320000,\"fid\":0,\"size\":11743652,\"vd\":19403.0},\"m\":{\"br\":192000,\"fid\":0,\"size\":7046209,\"vd\":22046.0},\"l\":{\"br\":128000,\"fid\":0,\"size\":4697487,\"vd\":23783.0},\"a\":null,\"cd\":\"01\",\"no\":1,\"rtUrl\":null,\"ftype\":0,\"rtUrls\":[],\"djId\":0,\"copyright\":1,\"s_id\":0,\"mark\":270336,\"originCoverType\":0,\"originSongSimpleData\":null,\"resourceState\":true,\"version\":1,\"single\":0,\"noCopyrightRcmd\":null,\"cp\":1416692,\"mv\":0,\"rtype\":0,\"rurl\":null,\"mst\":9,\"publishTime\":1600876800000,\"privilege\":{\"id\":1478040536,\"fee\":8,\"payed\":0,\"st\":0,\"pl\":128000,\"dl\":0,\"sp\":7,\"cp\":1,\"subp\":1,\"cs\":false,\"maxbr\":999000,\"fl\":128000,\"toast\":false,\"flag\":4,\"preSell\":false,\"playMaxbr\":999000,\"downloadMaxbr\":999000,\"rscl\":null,\"freeTrialPrivilege\":{\"resConsumable\":false,\"userConsumable\":false},\"chargeInfoList\":[{\"rate\":128000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":0},{\"rate\":192000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":1},{\"rate\":320000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":1},{\"rate\":999000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":1}]}},{\"name\":\"飞鸟和蝉\",\"id\":1837129663,\"pst\":0,\"t\":0,\"ar\":[{\"id\":47199522,\"name\":\"老二\",\"tns\":[],\"alias\":[]}],\"alia\":[],\"pop\":60.0,\"st\":0,\"rt\":\"\",\"fee\":8,\"v\":4,\"crbt\":null,\"cf\":\"\",\"al\":{\"id\":125663738,\"name\":\"老二行\",\"picUrl\":\"http://p3.music.126.net/Ev9gkGcbKcFTM5b_8e3ApQ==/109951165869146999.jpg\",\"tns\":[],\"pic_str\":\"109951165869146999\",\"pic\":109951165869146999},\"dt\":299624,\"h\":{\"br\":320000,\"fid\":0,\"size\":11986068,\"vd\":-19326.0},\"m\":{\"br\":192000,\"fid\":0,\"size\":7191658,\"vd\":-16705.0},\"l\":{\"br\":128000,\"fid\":0,\"size\":4794453,\"vd\":-14948.0},\"a\":null,\"cd\":\"01\",\"no\":1,\"rtUrl\":null,\"ftype\":0,\"rtUrls\":[],\"djId\":0,\"copyright\":0,\"s_id\":0,\"mark\":0,\"originCoverType\":0,\"originSongSimpleData\":null,\"resourceState\":true,\"version\":4,\"single\":0,\"noCopyrightRcmd\":null,\"cp\":0,\"mv\":0,\"rtype\":0,\"rurl\":null,\"mst\":9,\"publishTime\":0,\"privilege\":{\"id\":1837129663,\"fee\":8,\"payed\":0,\"st\":0,\"pl\":128000,\"dl\":0,\"sp\":7,\"cp\":1,\"subp\":1,\"cs\":false,\"maxbr\":320000,\"fl\":128000,\"toast\":false,\"flag\":0,\"preSell\":false,\"playMaxbr\":320000,\"downloadMaxbr\":320000,\"rscl\":null,\"freeTrialPrivilege\":{\"resConsumable\":false,\"userConsumable\":false},\"chargeInfoList\":[{\"rate\":128000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":0},{\"rate\":192000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":1},{\"rate\":320000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":1},{\"rate\":999000,\"chargeUrl\":null,\"chargeMessage\":null,\"chargeType\":1}]}}],\"songCount\":20},\"code\":200}";
        return data;
    }

    private List<Map<String, String>> parse(String sourseData){
        List<Map<String, String>> result = new ArrayList<>();
        long start = System.currentTimeMillis();
        JSONObject obj = JSON.parseObject(sourseData);
        log.debug("parseJSON Use Time: {} ms,data size: {}"
                ,(System.currentTimeMillis() - start)/1000.0
                ,StringUtil.formatSize(sourseData.getBytes(StandardCharsets.UTF_8).length)
        );
        if(200 != obj.getInteger("code")){
            log.error("接口返回了异常数据：{}",sourseData);
        }
        JSONArray songList = obj.getJSONObject("result").getJSONArray("songs");
        for (int i = 0; i < songList.size(); i++) {
            JSONObject songItem = songList.getJSONObject(i);
            Map<String, String> song = new HashMap<>();

            song.put("id_sou", songItem.getString("id"));
            //ID特殊处理
            song.put("id", BuffData.addId(songItem.getString("id"),this));

            song.put("title",String.format("【%s】%s",this.getSearchName(),songItem.getString("name")));
            JSONArray singerList = songItem.getJSONArray("ar");
            song.put("artist",arrayJoinSinger(singerList,"、"));

//            log.info(JSON.toJSONString(song));
            result.add(song);
            //内存优化
            singerList.clear();
            singerList = null;
        }
//        log.info("数量：{}",songList.size());
        //内存优化
        songList.clear();
        songList = null;
        obj.clear();
        obj = null;
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

    //下载歌词参考：https://blog.csdn.net/weixin_42742658/article/details/103484096
    private String downloadLrc(String musicId){
        GetRequest getRequest = Unirest.get(String.format(downlooadUrl, musicId));
        getRequest.headers(headerMap);
        HttpResponse<String> response = null;
        try {
            response = getRequest.asString();
        } catch (UnirestException e) {
            e.printStackTrace();
        }
        String jsonResult = response.getBody();

        JSONObject obj = JSON.parseObject(jsonResult);
        Integer code = obj.getInteger("code");
        if(code !=null && !code.equals(200)){
            log.warn("警告，接口似乎未返回正确数据：{}",jsonResult);
        }
        JSONObject lrcNode = obj.getJSONObject("lrc");
        String lrc = null;
        if(lrcNode==null){
            //lrc = "纯音乐，无歌词";
            log.warn("警告，该歌曲未上传歌词！");
        }else{
            JSONObject transNode = obj.getJSONObject("tlyric");
            lrc = lrcNode.getString("lyric");

            if(transNode!=null){
                String trans = transNode.getString("lyric");
                if(StringUtil.notEmpty(trans)){
                    lrc = doTrans(lrc,trans);
                }
            }

        }
        //内存优化
        obj.clear();
        jsonResult = null;
        response = null;
        return lrc;
    }

    @Test
    public void t001(){
        //注册BouncyCastle，参考：https://blog.csdn.net/qq_29583513/article/details/78866461
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        //log.info(a());
        String key = "I8Ahd7zESq2Y6ayg";
        try {
            log.info(b("vhY17KdNnne2XDo1o92GriSeUlyB21S7fSCFaJdNCRR5IIThLG+zl3ieOqOq/7eJKM12J0xGu4ma2JSPi81p4Q=="
                    ,key));

//            log.info(b_de("p20JSR7AqR5phLlIRFU6ppWXrPA8qtGVZy4djW63WULz08bRWFQXxECsXcoIp4H7ZuHjB1Ej2OcJLuTc6p8KGhLyJCsx97VEs9mYdC45vVgLVMHOgGO8CDUKf7kbxihs"
//                    ,key
//            ));
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        }
    }
    @Test
    public void t002(){
        String data = searchApi("飞鸟和蝉");
        List<Map<String, String>> parse = parse(data);
    }
    @Test
    public void t003(){
        Unirest.setTimeouts(0, 0);
        try {
            HttpResponse<String> response = Unirest.post("https://music.163.com/weapi/cloudsearch/get/web")
                    .header("cookie", "" +
//                            "_iuqxldmzr_=32;" +
//                            " _ntes_nnid=ee1b6d8d4388b765fd6a12042f95e531,1631272587945;" +
//                            " _ntes_nuid=ee1b6d8d4388b765fd6a12042f95e531;" +
//                            " NMTID=00O1L2Vd2MweM4iBklQv5sO3MB-hAIAAAF7z2xUbg;" +
                            //" csrfToken=7PrFGeSQgu8FGhktzOxNv0sl;" +
//                            " WNMCID=jiklll.1631272589918.01.0;" +
//                            " WEVNSM=1.0.0;" +
//                            " WM_NI=y9Z0z6wogPPCt0doYZvwXxdtNVy0wp6b313UBUcYsVC5mRHXVXYjsDUP1BUk5nL9fc25ZBsSvH18l8ii6Dd%2BgjA8LkXZUw80twTvRV%2BQ%2FnAy8hoPuqS3CknN0vXRQ%2FF5eno%3D;" +
//                            " WM_NIKE=9ca17ae2e6ffcda170e2e6eeb6aa45a8ef81b7c64bb7a88eb2d14e839a9eaaf566e988beb0e54af2acfbb0b72af0fea7c3b92abbe796d1f93eb5b1a9b1d349b8f0ac90dc52b6a99899e7808e8ab7d1f67cf6befdd2b25f828badbaf25e81eba3b3cb549688978bf2509790ffa3d861f7b284dae569f2eb96a3f259b698ffd9ef34abb7faa5e721a5b68fd9cf619c869b98bb3cb8a99c97d15ee98fe18fc473fcb8a1b1f87b91ed8d8ced46f4aa98b3d42586ebab8fee37e2a3;" +
//                            " WM_TID=MIA8pxinKzdBBRRQQFdq3%2FX%2FfLBztn%2Fl; ntes_kaola_ad=1; JSESSIONID-WYYY=5fVAmKvFrKsBP4P3mEVB6Gk9BK%2FT5X7O12XqW2069wtHctkx5fWU5cy4AQ4AWnG0gM%2B4%5CTRFdhqxAtgoHjJvwfTem0lcXR%2BC%2Fl9NcpeDJ%2FxdAzwP263%2BHmt2PepJpPOw%5C0s1%2FQKZztdZcr27KJ%2F2O9WOEynCsInOCQ3Y%2FdsPYmEYzYu5%3A1631284833842;" +
                            //" __csrf=b0f515d95b739febadb9676c8ecfc5e4;" +
//                            " __remember_me=true;" +
                            " MUSIC_U=a165ec086e22b99cc5085d4dbeb807475e9b5233b736633d60f4ef141ef9ad89e2b245612b211a3db72149bd3b14523943124f3fcebe94e446b14e3f0c3f8af9090f3ad7c86ad1db;" +
                            //" NMTID=00O6ngI72iOFJcGbkfor5mjZFrOcrgAAAF7yok-PQ;" +
//                            " ntes_kaola_ad=1" +
                            ""
                    )
//                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .field("params", "5/e2SJnA6lzr5v5S3QJ3lOxddWW4RVqp0bLpVWL0aaajBglvA26QgouXt/pTcLscIrclCDi9tFaO7QWsh4p59+uOtz0beIGX340THAdI5pRr3UKQhtgI0FDq5r1HRcZWqTYExNLKmxUXlx68ZrWZSSU03ks2+w2cYktsDwgWkPE2B9Dhxmx50UbzOTZQJjKT01rA7QTAS3h8YtmYrh/PJem2ziBWfl6TcvfyiCXalIsHdZLcAotfUDn1w5Iqr/wj1Rlldj9pCV0LcV+q0s2RSUM80tkoEKRUBu2hXsgOzc2sngHjJKCBJlbC3sIkcoN99UFKw7RJiz3DtCzrUjw/StqXE20MhIEp+dyHnakdwc1Uxz493YBqjlyuAXWG696iTFXGb4NHe0kOLd6XMOLChg==")
                    .field("encSecKey", "2cc17b0e0b0a29cb56e345f22081494f17232d301a0f6ce517138ea36bfcfa23e4b3981ce1151ef56ab71ba92000e9cc64f33e66d29ecac70e1c650c310f18fbe851198839027b92d5df7905865de6f2b87ea291d29df74cbdf233b792bbfd55aad25b76ef9e23a554f2d0070caa1af6a62bfb9e9b3460f7ded39fbee727f3ea")
                    .asString();
            String body = response.getBody();
            parse(body);
        } catch (UnirestException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void t004() throws UnirestException {
        Unirest.setTimeouts(0, 0);
        HttpResponse<String> response = Unirest.post("https://music.163.com/weapi/cloudsearch/get/web")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Cookie", "NMTID=00O6ngI72iOFJcGbkfor5mjZFrOcrgAAAF7yok-PQ; ntes_kaola_ad=1")
                .header("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.63 Safari/537.36")
                .field("params", "v7WJ502sT6l4CuhhI++3F19x9hk8KeYTJmWxJSDUSqg2sTo363xklvi9gtX5D0mYuHU6tQqjU81hQUPnq3FMNqzu3NUY7AtRM62ayFaJky+ob/mciLMGBxyR6Wt8oxxpbC829kWYBR7ZLVpcIubRIKKbfLP43081s6txB8MhLm9J8SCncfI9BamtkyohsLIllh06flwu7ulyPzVK20tSFgEM3RxnGP+dTX3RPxDO6MdsQMdiCfgaE1EzCl3oyu4l74n4MsLhIcopDE87v0x/Qg==")
                .field("encSecKey", "ac63ea8b4e59d7ecdaa1b2d0b7df0e2fb7a269bf830b1ee042efbd0704dda31f4ac4c1680ad7505b3c101fc1c21127d0695d67c7c805e6bdd4a941ec11baf459ca9236674876bd450a2b43571dc80e306766c6f7dccbca7328729c4f5b107fab8a7f2bb3879ea2399db5beb2472c232a1b0e1bf3eac7ff29d7eba1415bc81dce")
                .asString();
        log.info(response.getBody());

    }
}
