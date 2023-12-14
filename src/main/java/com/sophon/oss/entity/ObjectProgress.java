package com.sophon.oss.entity;

import java.util.Date;

/**
 *  @author: jinmu
 *  @Date: 2023/11/19 3:34 下午
 *  @Description: 文件上传进度跟踪 对象
 */
public class ObjectProgress {

    private long id;
    private String fileName; //文件名
    private String prefix;  //前缀,即目录
    private String bucketName; //bucket
    private String tossName; //存储的新名称
    private double fileSize; //文件大小
    private String streamMd5; //读取的字节流MD5码
    private double secRate; //上传比例
    private int status; //上传状态
    private String threadName; //线程名称
    private String md5;  //全文件MD5
    private Date updateTime;
    private Date createTime;
    private String createBy;
    private String updateBy;

    private String batchNo; //批次号
    private int operateFlag; //操作标识 0 正常 1挂起 2中断取消

    public ObjectProgress() {
    }

    public ObjectProgress(String fileName, String prefix, String bucketName) {
        this.fileName = fileName;
        this.prefix = prefix;
        this.bucketName = bucketName;
    }

    public ObjectProgress(long id, String fileName, String prefix, String bucketName, String tossName, double fileSize, String streamMd5, double secRate) {
        this.id = id;
        this.fileName = fileName;
        this.prefix = prefix;
        this.bucketName = bucketName;
        this.tossName = tossName;
        this.fileSize = fileSize;
        this.streamMd5 = streamMd5;
        this.secRate = secRate;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getTossName() {
        return tossName;
    }

    public void setTossName(String tossName) {
        this.tossName = tossName;
    }

    public double getFileSize() {
        return fileSize;
    }

    public void setFileSize(double fileSize) {
        this.fileSize = fileSize;
    }

    public String getStreamMd5() {
        return streamMd5;
    }

    public void setStreamMd5(String streamMd5) {
        this.streamMd5 = streamMd5;
    }

    public double getSecRate() {
        return secRate;
    }

    public void setSecRate(double secRate) {
        this.secRate = secRate;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public int getOperateFlag() {
        return operateFlag;
    }

    public void setOperateFlag(int operateFlag) {
        this.operateFlag = operateFlag;
    }
}
