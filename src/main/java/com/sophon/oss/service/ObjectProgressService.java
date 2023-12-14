package com.sophon.oss.service;

import cn.hutool.json.JSONArray;
import com.sophon.oss.entity.ObjectProgress;


import java.util.List;
import java.util.Map;

/**
 *  @author: jinmu
 *  @Date: 2023/11/19 3:45 下午
 *  @Description: 对象上传进度接口
 */
public interface ObjectProgressService {

    /**
     *  @author: jinmu
     *  @Date: 2023/11/19 6:30 下午
     *  @Description: 持续跟踪，上报进度
     */
    int trackingObjectProgress(ObjectProgress objectProgressVo, boolean removeHis);

    /**
     *  @author: jinmu
     *  @Date: 2023/11/19 7:43 下午
     *  @Description: 获取上传进度
     */
    List<ObjectProgress> getObjectProgressListBatch(Map map, JSONArray jsonArray);

    int insertObjectProgress(ObjectProgress objectProgress);

    int updateObjectProgress(ObjectProgress objectProgress);

    ObjectProgress getObjectProgressDisp(ObjectProgress objectProgress);

    ObjectProgress getObjectProgressDispById(long id);
}
