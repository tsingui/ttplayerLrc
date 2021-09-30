package com.example.ttplayerlrcsearch.service;

import com.example.ttplayerlrcsearch.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public abstract class LRCDispose {
    //设置是否下载双语翻译歌词
    private boolean willDownTrans = true;
    @Value("${MLC.trans.will_downlad}")
    private void setWillDownTrans(Boolean willDownTrans) {
        if(willDownTrans!=null){
            this.willDownTrans = willDownTrans;
        }
    }
    //翻译歌词是否在前
    private boolean translationInFirst = false;
    @Value("${MLC.trans.translation_in_first}")
    public void setTranslationInFirst(Boolean translationInFirst) {
        if(translationInFirst!=null){
            this.translationInFirst = translationInFirst;
        }
    }
    //翻译歌词是否在前
    private String addStr = "\t";
    @Value("${MLC.trans.add_str}")
    private void setAddStr(String addStr) {
        if(StringUtil.notEmpty(addStr)) {
            this.addStr = addStr;
        }
    }

    /**
     * 合并双语歌词
     * 注：只支持同时间点的歌词合并
     * @param souText lrc原文
     * @param transText lrc译文
     * @return
     */
    public String doTrans(String souText, String transText){
        if(!willDownTrans || StringUtil.isNull(transText)){
            return souText;
        }
        String t1,t2;
        souText = str_enter_do(souText);
        transText = str_enter_do(transText);
        if(translationInFirst){
            t1 = transText;
            t2 = souText;
        }else{
            t1 = souText;
            t2 = transText;
        }

        String[] t1_arr = t1.split("\n");
        Map<String,String> data = new LinkedHashMap<>();
        Pattern p = Pattern.compile("^(\\[.*\\])(.*)$");
        for(String line:t1_arr){
            Matcher m = p.matcher(line);
            if(m.find()){
                data.put(
                        m.group(1)
                        ,m.group(2)
                );
            }
        }
        String[] t2_arr = t2.split("\n");
        for(String line:t2_arr){
            Matcher m = p.matcher(line);
            if(m.find()){
                String time = m.group(1);
                String lrc = data.get(time);
                if(lrc==null){
                    lrc = "";
                }
                String t2_lrc_text = m.group(2);
                String ha = lrc.concat(addStr).concat(t2_lrc_text);
                //特殊处理，过滤掉双边空白语句
                if(StringUtil.isNull(str_blank_remove(lrc))
                        && StringUtil.isNull(str_blank_remove(t2_lrc_text))){
                    ha = "";
                }
                data.put(time,ha);
            }
        }

        StringBuffer buff = new StringBuffer();
        for(String time:data.keySet()){
            buff.append(time.concat(data.get(time)));
            buff.append("\r\n");
        }

        return buff.toString();
    }
    private String str_enter_do(String in){
        return in
                .replace("\\n","\n")
                .replace("\r\n","\n")
                .replace("\r","\n")
        ;
    }
    private String str_blank_remove(String in){
        return in
                .replace(" ","");
    }

//    @Test
//    public void test(){
//        String sou = "";
//        String trans = "";
//
//        sou = "[00:21.74]長い長い2秒間のため\\n[00:27.40]あたしは口を閉じた\\n[00:33.09]少し経って波音が響く\\n[00:38.85]かき消されない様に勇気出した\\n[00:44.71]-\\n[01:07.62]うだる暑さと詰まらない声\\n[01:13.13]窓から見える青さ\\n[01:18.79]まどろんでは頑張ってるフリで\\n[01:24.60]何気なくきみを見る\\n[01:30.40]誰もが欲しがる\\n[01:33.17]愛と恋の答え\\n[01:36.05]何度考えても解らない\\n[01:40.85]きみと全力疾走手をとって\\n[01:47.26]ふたり逃げ出した帰り道\\n[01:52.62]胸が鳴って顔が熱くて\\n[01:58.57]どうかどうか手を離さないで\\n[02:10.27]-\\n[02:52.99]苦しくなって\\n[02:58.64]くすぐったくて\\n[03:04.35]でも優しくて\\n[03:10.05]出てくる言葉がいっぱいで\\n[03:15.81]ため息が出ちゃう\\n[03:21.31]息を切らして立ち止まる\\n[03:27.23]青に滲んだ赤い絵の具\\n[03:32.53]きみの満点の笑顔向けて\\n[03:38.69]背伸びした\\n[03:40.97]好きよ\\n[03:42.08]-\\n[03:42.64]きみと全力疾走手をとって\\n[03:50.01]ふたり逃げ出して遠くまで\\n[03:55.37]答えがたとえ違ってても\\n[04:01.54]どうかどうか手を離さないで\\n[04:12.59]そう\\n[04:15.78]まだ\\n[04:17.30]走っていく\\n[04:19.67]きみと\\n[04:21.18]-\\n";
//        trans = "[by:Yukkuri_b]\\n[00:21.74]为这长长的二秒\\n[00:27.40]我紧闭双唇\\n[00:33.09]过了一会响起浪声\\n[00:38.85]为不被抹消 鼓起了勇气\\n[00:44.71]♪\\n[01:07.62]闷热的夏日和千篇一律的声音\\n[01:13.13]透窗映入满眼的青\\n[01:18.79]打盹装作有在努力\\n[01:24.60]看着若无其事的你\\n[01:30.40]无论谁都渴求\\n[01:33.17]爱与恋的答案\\n[01:36.05]考虑多少次都得不出来\\n[01:40.85]与你牵手全力疾跑\\n[01:47.26]一起逃出的归路\\n[01:52.62]心跳加速 脸颊发热\\n[01:58.57]请千万千万 别把手放开\\n[02:10.27]♪\\n[02:52.99]有些难受起来\\n[02:58.64]心痒难耐\\n[03:04.35]却如此温柔\\n[03:10.05]积了那么多想要倾诉\\n[03:15.81]叹了出来\\n[03:21.31]气息慌乱 停下脚步\\n[03:27.23]沁了青的红水彩\\n[03:32.53]向着你满分的笑颜\\n[03:38.69]踮起脚\\n[03:40.97]喜欢喔\\n[03:42.08]♯\\n[03:42.64]与你牵手全力疾走\\n[03:50.01]一起逃到世界尽头\\n[03:55.37]就算得出了错误答案\\n[04:01.54]也千万千万 别把手放开\\n[04:12.59]对\\n[04:15.78]再次\\n[04:17.30]放开脚步\\n[04:19.67]和你一起\\n[04:21.18]♪\\n";
//
//        String out = doTrans(sou, trans);
//        log.info(out);
//    }
}
