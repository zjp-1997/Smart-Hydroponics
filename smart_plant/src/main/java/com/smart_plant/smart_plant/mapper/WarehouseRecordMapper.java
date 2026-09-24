package com.smart_plant.smart_plant.mapper;

import com.smart_plant.smart_plant.entity.WarehouseRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;

@Mapper
public interface WarehouseRecordMapper {

    int insert(WarehouseRecord record);

    int updateInitialInbound(@Param("itemId") Long itemId,
                             @Param("operatorId") Long operatorId,
                             @Param("price") BigDecimal price);

    /** 物资重新指定农场主时同步流水的归属快照。 */
    int updateOwnerByItemId(@Param("itemId") Long itemId, @Param("farmOwnerId") Long farmOwnerId);

    /** 已关联故障、地块或任务的流水不能直接改归属。 */
    int countLinkedRecordsByItemId(Long itemId);

    WarehouseRecord selectById(Long id);

    /** 锁定幂等键对应流水；不存在时由唯一索引防止并发重复插入。 */
    WarehouseRecord selectByRequestIdForUpdate(String requestId);

    List<WarehouseRecord> selectList(@Param("userId") Long userId,
                                     @Param("itemId") Long itemId,
                                     @Param("itemName") String itemName,
                                     @Param("recordType") Integer recordType,
                                     @Param("startTime") LocalDateTime startTime,
                                     @Param("endTime") LocalDateTime endTime);

    /** 查询 farm 可见的有效入库/出库流水；ownerId 为空仅表示普通用户共享视图。 */
    List<WarehouseRecord> selectClientList(@Param("ownerId") Long ownerId,
                                           @Param("recordType") Integer recordType);

    BigDecimal sumQuantityByType(@Param("userId") Long userId, @Param("recordType") Integer recordType);

    /** 按用户、记录类型和半开时间区间汇总数量，供今日入库统计使用。 */
    BigDecimal sumQuantityByTypeBetween(@Param("userId") Long userId,
                                        @Param("recordType") Integer recordType,
                                        @Param("startTime") LocalDateTime startTime,
                                        @Param("endTime") LocalDateTime endTime);

    /** farm 端按已分配的物资归属统计今日入库。 */
    BigDecimal sumClientQuantityByTypeBetween(@Param("farmOwnerId") Long farmOwnerId,
                                              @Param("recordType") Integer recordType,
                                              @Param("startTime") LocalDateTime startTime,
                                              @Param("endTime") LocalDateTime endTime);
}
