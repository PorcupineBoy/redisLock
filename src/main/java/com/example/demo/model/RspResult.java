package com.example.demo.model;


import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RspResult<T> {

    private String msg;
    private Integer code;
    private T data;

    public RspResult(){};
    public RspResult(T data,String msg,Integer code){
        this.code=code;
        this.msg=msg;
        this.data=data;
    };
    public RspResult(String msg,Integer code){
        this.code=code;
        this.msg=msg;
    };
    public static RspResult ok(){
        RspResult result=new RspResult<>("操作成功",200);
        return result;
    };
    public static RspResult ok(Object data){
        RspResult result=new RspResult<>("操作成功",200);
        result.setData(data);
        return result;
    };
    public static RspResult  fail(Object data){
        RspResult result=new RspResult<>("操作失败",999);
        result.setData(data);
        return result;
    };
}
