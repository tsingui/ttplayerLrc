package com.example.ttplayerlrcsearch.service;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 查找所有下载来源的工具类
 */
@Service
public class FindAllMLC implements ApplicationContextAware, InitializingBean {

    private Map<String, MusicLrcSearch> MLC_Service = new HashMap<>();

    private ApplicationContext applicationContext;
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // 查找所有MusicLrcSearch接口的实现类
        MLC_Service = applicationContext.getBeansOfType(MusicLrcSearch.class);
    }

    public Map<String, MusicLrcSearch> getAllMLCService() {
        return MLC_Service;
    }
    public List<MusicLrcSearch> getAllMLCServiceList() {
        List<MusicLrcSearch> list = new ArrayList<>();
        for (String key:MLC_Service.keySet()){
            list.add(MLC_Service.get(key));
        }
        return list;
    }
}
