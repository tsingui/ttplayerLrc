package com.example.ttplayerlrcsearch.entity;

import com.alibaba.fastjson.JSON;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class ApiResponse<T> implements Serializable {
    private int code;
    private String message;
    private String time;
    private T data;
    private String response;


    public ApiResponse(){
        this.setTime(String.valueOf(new Date().getTime()));
    }
    public static <T> ApiResponse<T> returnOK(){
        ApiResponse<T> apiResponse = new ApiResponse<>();
        apiResponse.setCode(200);
        apiResponse.setResponse("success");
        return apiResponse;
    }
    public static <T> ApiResponse<T> returnFail(String message, int code){
        ApiResponse<T> apiResponse = new ApiResponse<>();
        apiResponse.setCode(code);
        apiResponse.setResponse("fail");
        apiResponse.setMessage(message);
        return apiResponse;
    }
    public static <T> ApiResponse<T> returnFail(String message){
        return returnFail(message,501);
    }

    public ApiResponse<T> setDataNow(T data){
        this.data = data;
        return this;
    }
    public ApiResponse<T> setMessageNow(String message){
        this.message = message;
        return this;
    }
    public boolean isSuccess(){
        if("success".equalsIgnoreCase(this.getResponse())){
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }
}