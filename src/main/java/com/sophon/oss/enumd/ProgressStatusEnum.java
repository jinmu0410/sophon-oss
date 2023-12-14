package com.sophon.oss.enumd;

/**
 *  @author: jinmu
 *  @Date: 2019-11-21 11:27
 *  @Description: 上传状态  -1 异常 0 等待上传  1上传中 2上传成功 3上传中断 4上传失败
 */
public enum ProgressStatusEnum {

    WAIT(0, "WAIT", "等待"),
    ING(1, "ING", "上传中"),
    SUCCESS(2, "SUCCESS", "成功"),
    INTERRUPT(3, "INTERRUPT", "上传中断"),
    FAIL(4, "FAIL", "失败"),
    ERROR(5, "ERROR", "异常"),
    OTHER(9, "OTHER", "其他");

    private final int code;

    private final String desc;

    private final String productDescription;


    ProgressStatusEnum(int code, String desc, String productDescription) {
        this.code = code;
        this.desc = desc;
        this.productDescription = productDescription;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public String getProductDescription() {
        return productDescription;
    }
}
