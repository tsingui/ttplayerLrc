package com.example.ttplayerlrcsearch.controller;

import com.example.ttplayerlrcsearch.entity.ApiResponse;
import com.example.ttplayerlrcsearch.entity.ServerRunData;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/")
public class ServerStatus {

    @RequestMapping("")
    public String statusIndex(){
        return "status";
    }

    @RequestMapping("info")
    @ResponseBody
    public ApiResponse getBaseInfo(String lineNum){
        return ApiResponse.returnOK().setDataNow(ServerRunData.getBaseInfo(lineNum));
    }
}
