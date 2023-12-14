package com.sophon.oss.entity;

import lombok.Data;

/**
 * @author wuque
 * @title: ObjectProgressReq
 * @projectName s3-web
 * @description:
 * @date 2023/8/2811:21
 */
@Data
public class ObjectProgressReq {

    private String fileNames;
    private boolean haveOneBoolean;
    private String batchNo;


}
