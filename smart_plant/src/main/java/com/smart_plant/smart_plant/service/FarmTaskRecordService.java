package com.smart_plant.smart_plant.service;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.FarmTaskRecord;

public interface FarmTaskRecordService {

    FarmTaskRecord addFarmTaskRecord(FarmTaskRecord record);

    FarmTaskRecord getFarmTaskRecordById(Long id);

    PageInfo<FarmTaskRecord> listFarmTaskRecords(Long taskId, Integer pageNum, Integer pageSize);
}
