package com.smart_plant.smart_plant.service;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.dto.ClientWarehouseOverviewResponse;
import com.smart_plant.smart_plant.dto.ClientWarehouseRecordPage;
import com.smart_plant.smart_plant.dto.WarehouseStatsResponse;
import com.smart_plant.smart_plant.dto.WarehouseOperatorOption;
import com.smart_plant.smart_plant.entity.WarehouseItem;
import com.smart_plant.smart_plant.entity.WarehouseRecord;
import com.smart_plant.smart_plant.entity.IotDeviceFault;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

public interface WarehouseService {

    WarehouseItem addItem(WarehouseItem item);

    WarehouseItem updateItem(WarehouseItem item);

    void deleteItem(Long id);

    int batchDeleteItems(List<Long> ids);

    void updateItemStatus(Long id, Integer status);

    WarehouseItem getItemById(Long id);

    PageInfo<WarehouseItem> listItems(String itemName, String itemCode, Integer category,
                                      Integer status, Integer pageNum, Integer pageSize);

    WarehouseStatsResponse getStats();

    /** 查询当前用户在服务器当前自然日内的有效入库数量。 */
    BigDecimal getTodayInboundQuantity();

    /** farm 农场主和绑定的普通用户按相同农场归属查看启用物资。 */
    ClientWarehouseOverviewResponse getClientOverview(String keyword, Integer category,
                                                       Integer pageNum, Integer pageSize);

    /** farm 用户端按当前账号权限分页查看入库和出库流水。 */
    ClientWarehouseRecordPage listClientRecords(Integer recordType, Integer pageNum, Integer pageSize);

    List<WarehouseOperatorOption> listInboundOperators();

    /** 仅列出启用的农场主供物资归属选择。 */
    List<WarehouseOperatorOption> listFarmOwners();

    List<IotDeviceFault> listRelatedFaultOptions(Long itemId);

    WarehouseRecord addRecord(WarehouseRecord record);

    WarehouseRecord getRecordById(Long id);

    PageInfo<WarehouseRecord> listRecords(Long itemId, String itemName, Integer recordType,
                                          LocalDateTime startTime, LocalDateTime endTime,
                                          Integer pageNum, Integer pageSize);
}
