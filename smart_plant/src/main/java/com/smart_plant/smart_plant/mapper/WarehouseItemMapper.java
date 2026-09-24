package com.smart_plant.smart_plant.mapper;

import com.smart_plant.smart_plant.entity.WarehouseItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface WarehouseItemMapper {

    int insert(WarehouseItem item);

    /** 按新增记录主键回填系统生成的物资编码，主键保证并发新增时编码不会重复。 */
    int updateGeneratedItemCode(@Param("id") Long id, @Param("itemCode") String itemCode);

    int updateById(WarehouseItem item);

    int softDeleteById(Long id);

    int softDeleteByIds(@Param("ids") List<Long> ids);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    int updateStock(@Param("id") Long id, @Param("stockQty") BigDecimal stockQty);

    WarehouseItem selectById(Long id);

    /** 在出入库事务内锁定物资行，确保库存快照和流水一致。 */
    WarehouseItem selectByIdForUpdate(Long id);

    List<WarehouseItem> selectList(@Param("userId") Long userId,
                                   @Param("itemName") String itemName,
                                   @Param("itemCode") String itemCode,
                                   @Param("category") Integer category,
                                   @Param("status") Integer status);

    long countItems(@Param("userId") Long userId);

    BigDecimal sumStockQty(@Param("userId") Long userId);

    int countByUserIdAndItemCode(@Param("userId") Long userId,
                                 @Param("itemCode") String itemCode,
                                 @Param("excludeId") Long excludeId);

    int countRecordByItemId(Long itemId);

    /** farm 端只读取已明确归属的物资。 */
    List<WarehouseItem> selectClientList(@Param("farmOwnerId") Long farmOwnerId,
                                         @Param("keyword") String keyword,
                                         @Param("category") Integer category);

    long countClientItems(@Param("farmOwnerId") Long farmOwnerId);

    BigDecimal sumClientStockQty(@Param("farmOwnerId") Long farmOwnerId);
}
