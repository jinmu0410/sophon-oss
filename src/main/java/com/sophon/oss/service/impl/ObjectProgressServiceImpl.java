package com.sophon.oss.service.impl;

import cn.hutool.json.JSONArray;
import com.sophon.oss.entity.ObjectProgress;
import com.sophon.oss.mapper.ObjectProgressMapper;
import com.sophon.oss.service.ObjectProgressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *  @author: jinmu
 *  @Date: 2023/11/19 3:54 下午
 *  @Description:
 */
@Service(value = "objectProgressService")
public class ObjectProgressServiceImpl implements ObjectProgressService {

    @Autowired
    private ObjectProgressMapper objectProgressMapper;

    @Override
    public int trackingObjectProgress(ObjectProgress objectProgress, boolean removeHis) {

        int result = 0;
        ObjectProgress objectProgressDisp = objectProgressMapper.getObjectProgressDisp(objectProgress);
        if(null != objectProgressDisp){

            if(objectProgressDisp.getSecRate() < objectProgress.getSecRate()){
                objectProgress.setId(objectProgressDisp.getId());
                result =  objectProgressMapper.updateObjectProgress(objectProgress);
            }

        }else{
            result = objectProgressMapper.insertObjectProgress(objectProgress);
        }

        return result;
    }

    @Override
    public List<ObjectProgress> getObjectProgressListBatch(Map map, JSONArray jsonArray) {

        List<ObjectProgress> allList = new ArrayList<>();
        List<ObjectProgress> list = objectProgressMapper.getObjectProgressListBatch(map);
        allList.addAll(list);

        if(null != jsonArray && jsonArray.size() > 0){
            int size = jsonArray.size();
            if(size > list.size()){
                for (int i = 0; i < size; i++) {
                    if(null != jsonArray.get(i)){
                        String filename = (String) jsonArray.get(i);
                        boolean ishave =false;
                        for (int j = 0; j < list.size(); j++) {
                            ObjectProgress objectProgress = list.get(j);
                            if(filename.equals(objectProgress.getFileName())){
                                ishave=true;
                            }
                        }
                        if(!ishave){
                            ObjectProgress objectProgress = new ObjectProgress();
                            objectProgress.setFileName(jsonArray.get(i).toString());
                            objectProgress.setStatus(-2); //校验解析中
                            allList.add(objectProgress);
                        }
                    }
                }
            }
        }

        return allList;
    }

    @Override
    public int insertObjectProgress(ObjectProgress objectProgress) {
        return objectProgressMapper.insertObjectProgress(objectProgress);
    }

    @Override
    public int updateObjectProgress(ObjectProgress objectProgress) {
        return objectProgressMapper.updateObjectProgress(objectProgress);
    }

    @Override
    public ObjectProgress getObjectProgressDisp(ObjectProgress objectProgress) {
        return objectProgressMapper.getObjectProgressDisp(objectProgress);
    }

    @Override
    public ObjectProgress getObjectProgressDispById(long id) {
        return objectProgressMapper.getObjectProgressDispById(id);
    }


}
