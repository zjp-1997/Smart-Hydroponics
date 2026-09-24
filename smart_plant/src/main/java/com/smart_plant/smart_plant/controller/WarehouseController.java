package com.smart_plant.smart_plant.controller;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.dto.CropImageUploadResult;
import com.smart_plant.smart_plant.dto.WarehouseStatsResponse;
import com.smart_plant.smart_plant.dto.WarehouseOperatorOption;
import com.smart_plant.smart_plant.entity.WarehouseItem;
import com.smart_plant.smart_plant.entity.WarehouseRecord;
import com.smart_plant.smart_plant.entity.IotDeviceFault;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.security.RequirePermission;
import com.smart_plant.smart_plant.service.ManagedImageService;
import com.smart_plant.smart_plant.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/warehouse")
@RequiredArgsConstructor
@RequirePermission("warehouse:manage")
public class WarehouseController {

    private final WarehouseService warehouseService;
    private final ManagedImageService managedImageService;

    /** 仓库专用上传入口，避免仓库功能依赖未部署的公共图片控制器。 */
    @PostMapping("/item/image/upload")
    public R<CropImageUploadResult> uploadItemImage(@RequestParam("file") MultipartFile file) {
        return R.success(managedImageService.upload(file, "warehouse"));
    }

    @PostMapping("/item/add")
    public R<WarehouseItem> addItem(@RequestBody WarehouseItem item) {
        return R.success(warehouseService.addItem(item));
    }

    @PutMapping("/item")
    public R<WarehouseItem> updateItem(@RequestBody WarehouseItem item) {
        return R.success(warehouseService.updateItem(item));
    }

    @DeleteMapping("/item/{id}")
    public R<Void> deleteItem(@PathVariable Long id) {
        warehouseService.deleteItem(id);
        return R.success();
    }

    @DeleteMapping("/item/batch")
    public R<Integer> batchDeleteItems(@RequestBody List<Long> ids) {
        return R.success(warehouseService.batchDeleteItems(ids));
    }

    @PutMapping("/item/{id}/status")
    public R<Void> updateItemStatus(@PathVariable Long id, @RequestParam Integer status) {
        warehouseService.updateItemStatus(id, status);
        return R.success();
    }

    @GetMapping("/item/{id}")
    public R<WarehouseItem> getItemById(@PathVariable Long id) {
        return R.success(warehouseService.getItemById(id));
    }

    @GetMapping("/item/list")
    public R<PageInfo<WarehouseItem>> listItems(@RequestParam(required = false) String itemName,
                                                @RequestParam(required = false) String itemCode,
                                                @RequestParam(required = false) Integer category,
                                                @RequestParam(required = false) Integer status,
                                                @RequestParam(defaultValue = "1") Integer pageNum,
                                                @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.success(warehouseService.listItems(itemName, itemCode, category, status, pageNum, pageSize));
    }

    @GetMapping("/stats")
    public R<WarehouseStatsResponse> getStats() {
        return R.success(warehouseService.getStats());
    }

    @GetMapping("/operator/options")
    public R<List<WarehouseOperatorOption>> listInboundOperators() {
        return R.success(warehouseService.listInboundOperators());
    }

    /** 管理端物资归属下拉选项。 */
    @GetMapping("/farm-owner/options")
    public R<List<WarehouseOperatorOption>> listFarmOwners() {
        return R.success(warehouseService.listFarmOwners());
    }

    @GetMapping("/item/{itemId}/fault/options")
    public R<List<IotDeviceFault>> listRelatedFaultOptions(@PathVariable Long itemId) {
        return R.success(warehouseService.listRelatedFaultOptions(itemId));
    }

    @PostMapping("/record/add")
    public R<WarehouseRecord> addRecord(@RequestBody WarehouseRecord record) {
        return R.success(warehouseService.addRecord(record));
    }

    @GetMapping("/record/{id}")
    public R<WarehouseRecord> getRecordById(@PathVariable Long id) {
        return R.success(warehouseService.getRecordById(id));
    }

    @GetMapping("/record/list")
    public R<PageInfo<WarehouseRecord>> listRecords(@RequestParam(required = false) Long itemId,
                                                    @RequestParam(required = false) String itemName,
                                                    @RequestParam(required = false) Integer recordType,
                                                    @RequestParam(required = false)
                                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                                    LocalDateTime startTime,
                                                    @RequestParam(required = false)
                                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                                    LocalDateTime endTime,
                                                    @RequestParam(defaultValue = "1") Integer pageNum,
                                                    @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.success(warehouseService.listRecords(
                itemId, itemName, recordType, startTime, endTime, pageNum, pageSize));
    }
}
