package com.sophon.oss.mapper;



import com.sophon.oss.entity.ObjectProgress;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface ObjectProgressMapper {

    int insertObjectProgress(ObjectProgress objectProgress);

    int updateObjectProgress(ObjectProgress objectProgress);

    int deleteObjectProgressByIds(List<Long> list);

    ObjectProgress getObjectProgressDisp(ObjectProgress objectProgress);

    ObjectProgress getObjectProgressDispById(long id);

    List<ObjectProgress> getObjectProgressList(ObjectProgress objectProgress);

    List<ObjectProgress> getObjectProgressListBatch(Map parmMap);

}
