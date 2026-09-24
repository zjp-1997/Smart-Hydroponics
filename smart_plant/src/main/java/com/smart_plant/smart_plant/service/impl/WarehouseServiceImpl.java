package com.smart_plant.smart_plant.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.dto.ClientWarehouseOverviewResponse;
import com.smart_plant.smart_plant.dto.ClientWarehouseRecordPage;
import com.smart_plant.smart_plant.dto.WarehouseStatsResponse;
import com.smart_plant.smart_plant.dto.WarehouseOperatorOption;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.entity.Plot;
import com.smart_plant.smart_plant.entity.WarehouseItem;
import com.smart_plant.smart_plant.entity.WarehouseRecord;
import com.smart_plant.smart_plant.entity.IotDeviceFault;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.FarmTaskMapper;
import com.smart_plant.smart_plant.mapper.PlotMapper;
import com.smart_plant.smart_plant.mapper.WarehouseItemMapper;
import com.smart_plant.smart_plant.mapper.WarehouseRecordMapper;
import com.smart_plant.smart_plant.mapper.IotDeviceFaultMapper;
import com.smart_plant.smart_plant.mapper.UserMapper;
import com.smart_plant.smart_plant.response.ResponseCode;
import com.smart_plant.smart_plant.service.DataPermissionService;
import com.smart_plant.smart_plant.service.OperationLogService;
import com.smart_plant.smart_plant.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl implements WarehouseService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final WarehouseItemMapper itemMapper;

    private final WarehouseRecordMapper recordMapper;

    private final IotDeviceFaultMapper faultMapper;

    private final UserMapper userMapper;

    private final PlotMapper plotMapper;

    private final FarmTaskMapper farmTaskMapper;

    private final DataPermissionService dataPermissionService;

    private final OperationLogService operationLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WarehouseItem addItem(WarehouseItem item) {
        // userId 保留创建账号，农场主归属由独立字段明确指定。
        if (item != null) {
            item.setUserId(dataPermissionService.currentUser().getId());
        }
        validateItemCreate(item);
        item.setFarmOwnerId(resolveFarmOwnerId(item.getFarmOwnerId()));
        normalizeItemDefaults(item);
        // 新增接口不接受客户端指定编码，防止伪造、空值及并发取号冲突。
        item.setItemCode(null);
        item.setInboundOperatorId(resolveInboundOperatorId(item.getInboundOperatorId()));
        itemMapper.insert(item);
        item.setItemCode(generateItemCode(item.getId()));
        if (itemMapper.updateGeneratedItemCode(item.getId(), item.getItemCode()) != 1) {
            throw new BusinessException(ResponseCode.FAIL, "物资编码生成失败");
        }
        recordInitialInbound(item);
        operationLogService.record("新增仓库物资：" + item.getItemName());
        return itemMapper.selectById(item.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WarehouseItem updateItem(WarehouseItem item) {
        if (item == null || item.getId() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "物资ID不能为空");
        }
        WarehouseItem oldItem = getItemById(item.getId());
        dataPermissionService.requireFarmManager(oldItem.getFarmOwnerId());
        // 创建账号不可通过编辑接口伪造；农场主只能维护自己的物资。
        item.setUserId(oldItem.getUserId());
        item.setFarmOwnerId(resolveFarmOwnerId(item.getFarmOwnerId()));
        validateItemUpdate(item);
        if (!item.getFarmOwnerId().equals(oldItem.getFarmOwnerId())
                && recordMapper.countLinkedRecordsByItemId(item.getId()) > 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "物资已有业务关联记录，不能变更所属农场主");
        }
        Long inboundOperatorId = resolveInboundOperatorId(item.getInboundOperatorId());
        checkUniqueItemCode(item);
        int rows = itemMapper.updateById(item);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.FAIL, "物资修改失败");
        }
        if (!item.getFarmOwnerId().equals(oldItem.getFarmOwnerId())) {
            recordMapper.updateOwnerByItemId(item.getId(), item.getFarmOwnerId());
        }
        recordMapper.updateInitialInbound(item.getId(), inboundOperatorId, item.getInitialUnitPrice());
        operationLogService.record("修改仓库物资：" + oldItem.getItemName());
        return itemMapper.selectById(item.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteItem(Long id) {
        WarehouseItem item = getItemById(id);
        dataPermissionService.requireFarmManager(item.getFarmOwnerId());
        int rows = itemMapper.softDeleteById(id);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "物资不存在");
        }
        operationLogService.record("逻辑删除仓库物资：" + item.getItemName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchDeleteItems(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请选择需要删除的物资");
        }
        List<Long> normalizedIds = ids.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        if (normalizedIds.isEmpty()) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请选择需要删除的物资");
        }
        for (Long id : normalizedIds) {
            WarehouseItem item = getItemById(id);
            dataPermissionService.requireFarmManager(item.getFarmOwnerId());
        }
        int rows = itemMapper.softDeleteByIds(normalizedIds);
        operationLogService.record("批量逻辑删除仓库物资：" + rows + "条");
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateItemStatus(Long id, Integer status) {
        WarehouseItem item = getItemById(id);
        dataPermissionService.requireFarmManager(item.getFarmOwnerId());
        validateEnabledStatus(status);
        int rows = itemMapper.updateStatus(id, status);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "物资不存在");
        }
        operationLogService.record("修改仓库物资状态：" + item.getItemName());
    }

    @Override
    public WarehouseItem getItemById(Long id) {
        requireId(id, "物资ID不能为空");
        WarehouseItem item = itemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "物资不存在");
        }
        dataPermissionService.requireOwnedResource(item.getFarmOwnerId());
        return item;
    }

    @Override
    public PageInfo<WarehouseItem> listItems(String itemName, String itemCode, Integer category,
                                             Integer status, Integer pageNum, Integer pageSize) {
        validateCategory(category);
        validateEnabledStatus(status);
        int currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int currentSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        Long scopedUserId = dataPermissionService.restrictUserId(null);
        PageHelper.startPage(currentPage, currentSize);
        return new PageInfo<>(itemMapper.selectList(
                scopedUserId, normalizeOptionalText(itemName), normalizeOptionalText(itemCode), category, status));
    }

    @Override
    public WarehouseStatsResponse getStats() {
        Long scopedUserId = dataPermissionService.restrictUserId(null);
        return new WarehouseStatsResponse(
                itemMapper.countItems(scopedUserId),
                defaultZero(itemMapper.sumStockQty(scopedUserId)),
                defaultZero(recordMapper.sumQuantityByType(scopedUserId, 1)),
                defaultZero(recordMapper.sumQuantityByType(scopedUserId, 2))
        );
    }

    @Override
    public BigDecimal getTodayInboundQuantity() {
        Long scopedUserId = dataPermissionService.restrictUserId(null);
        LocalDateTime startTime = LocalDate.now().atStartOfDay();
        // 使用“今日零点（含）到明日零点（不含）”避免时间精度导致边界记录遗漏。
        LocalDateTime endTime = startTime.plusDays(1);
        return defaultZero(recordMapper.sumQuantityByTypeBetween(scopedUserId, 1, startTime, endTime));
    }

    @Override
    public ClientWarehouseOverviewResponse getClientOverview(String keyword, Integer category,
                                                              Integer pageNum, Integer pageSize) {
        LocalDateTime startTime = LocalDate.now().atStartOfDay();
        LocalDateTime endTime = startTime.plusDays(1);
        // 农场主与绑定的普通用户共享同一农场范围，不能读取其他农场仓库。
        Long ownerId = dataPermissionService.currentClientOwnerId();
        if (category != null && (category < 1 || category > 6)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "物资分类不合法");
        }
        int currentPage = pageNum == null ? 1 : Math.max(1, pageNum);
        int currentSize = pageSize == null ? 20 : Math.max(1, Math.min(pageSize, 50));
        PageHelper.startPage(currentPage, currentSize);
        PageInfo<WarehouseItem> page = new PageInfo<>(itemMapper.selectClientList(
                ownerId, normalizeOptionalText(keyword), category));
        List<ClientWarehouseOverviewResponse.Item> items = page.getList().stream()
                .map(item -> new ClientWarehouseOverviewResponse.Item(item.getId(), item.getFarmOwnerId(),
                        item.getFarmOwnerName(), item.getItemName(), item.getItemCode(), item.getImageUrl(),
                        item.getCategory(), item.getSpecification(), item.getUnit(), item.getStockQty(),
                        item.getWarningQty(), item.getCreateTime()))
                .toList();
        return new ClientWarehouseOverviewResponse(
                itemMapper.countClientItems(ownerId),
                defaultZero(itemMapper.sumClientStockQty(ownerId)),
                defaultZero(recordMapper.sumClientQuantityByTypeBetween(ownerId, 1, startTime, endTime)),
                items,
                page.getPageNum() < page.getPages()
        );
    }

    @Override
    public ClientWarehouseRecordPage listClientRecords(Integer recordType, Integer pageNum, Integer pageSize) {
        // 客户端只提供入库/出库筛选，不暴露后台的库存调整类型或用户ID筛选。
        if (recordType != null && recordType != 1 && recordType != 2) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "记录类型只能为入库或出库");
        }
        // 普通用户只读绑定农场主的流水，其他角色由统一权限方法拒绝。
        Long ownerId = dataPermissionService.currentClientOwnerId();
        int currentPage = pageNum == null ? 1 : Math.max(1, pageNum);
        int currentSize = pageSize == null ? 10 : Math.max(1, Math.min(pageSize, 50));
        PageHelper.startPage(currentPage, currentSize);
        PageInfo<WarehouseRecord> page = new PageInfo<>(recordMapper.selectClientList(ownerId, recordType));
        // 仅返回移动端需要的流水摘要，避免普通用户收到价格等后台字段。
        List<ClientWarehouseRecordPage.Entry> entries = page.getList().stream()
                .map(record -> new ClientWarehouseRecordPage.Entry(record.getId(), record.getItemName(),
                        record.getItemCode(), record.getItemUnit(), record.getRecordType(), record.getQuantity(),
                        record.getAfterQty(), record.getOperatorName(), record.getRecipient(),
                        record.getRecordTime(), record.getRemark()))
                .toList();
        return new ClientWarehouseRecordPage(entries, page.getPageNum() < page.getPages());
    }

    @Override
    public List<WarehouseOperatorOption> listInboundOperators() {
        requireInboundOperatorSelector();
        return userMapper.selectActiveUsers().stream()
                .map(user -> new WarehouseOperatorOption(user.getId(), user.getUsername(), user.getNickname()))
                .toList();
    }

    @Override
    public List<WarehouseOperatorOption> listFarmOwners() {
        requireInboundOperatorSelector();
        return userMapper.selectList(null, null, "farm_owner", 1, null).stream()
                .filter(user -> dataPermissionService.isAdmin()
                        || user.getId().equals(dataPermissionService.currentUser().getId()))
                .map(user -> new WarehouseOperatorOption(user.getId(), user.getUsername(), user.getNickname()))
                .toList();
    }

    @Override
    public List<IotDeviceFault> listRelatedFaultOptions(Long itemId) {
        WarehouseItem item = getItemById(itemId);
        dataPermissionService.requireFarmManager(item.getFarmOwnerId());
        return faultMapper.selectList(item.getFarmOwnerId(), null, null, null, null,
                null, null, null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WarehouseRecord addRecord(WarehouseRecord record) {
        validateRecordCreate(record);
        WarehouseItem item = getItemByIdForUpdate(record.getItemId());
        dataPermissionService.requireFarmManager(item.getFarmOwnerId());
        WarehouseRecord existing = recordMapper.selectByRequestIdForUpdate(record.getRequestId());
        if (existing != null) {
            validateIdempotentRetry(record, existing);
            WarehouseRecord saved = recordMapper.selectById(existing.getId());
            return saved == null ? existing : saved;
        }
        if (!Integer.valueOf(1).equals(item.getStatus())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "物资已停用，不能出入库");
        }
        validateRelatedResources(record, item.getFarmOwnerId());
        normalizeRecord(record, item);
        if (itemMapper.updateStock(item.getId(), record.getAfterQty()) != 1) {
            throw new BusinessException(ResponseCode.FAIL, "库存状态已变化，请刷新后重试");
        }
        if (recordMapper.insert(record) != 1) {
            throw new BusinessException(ResponseCode.FAIL, "出入库流水写入失败");
        }
        operationLogService.record("新增出入库记录：" + item.getItemName());
        return recordMapper.selectById(record.getId());
    }

    @Override
    public WarehouseRecord getRecordById(Long id) {
        requireId(id, "出入库记录ID不能为空");
        WarehouseRecord record = recordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "出入库记录不存在");
        }
        dataPermissionService.requireOwnedResource(getItemById(record.getItemId()).getFarmOwnerId());
        return record;
    }

    @Override
    public PageInfo<WarehouseRecord> listRecords(Long itemId, String itemName, Integer recordType,
                                                 LocalDateTime startTime, LocalDateTime endTime,
                                                 Integer pageNum, Integer pageSize) {
        validateRecordType(recordType);
        if (startTime != null && endTime != null && startTime.isAfter(endTime)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "开始时间不能晚于结束时间");
        }
        int currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int currentSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        Long scopedUserId = dataPermissionService.restrictUserId(null);
        PageHelper.startPage(currentPage, currentSize);
        return new PageInfo<>(recordMapper.selectList(
                scopedUserId, itemId, normalizeOptionalText(itemName), recordType, startTime, endTime));
    }

    private void validateItemCreate(WarehouseItem item) {
        if (item == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "物资信息不能为空");
        }
        if (item.getFarmOwnerId() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "所属农场主不能为空");
        }
        if (!StringUtils.hasText(item.getItemName())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "物资名称不能为空");
        }
        if (!StringUtils.hasText(item.getUnit())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "物资单位不能为空");
        }
        validateItemUpdate(item);
    }

    private void validateItemUpdate(WarehouseItem item) {
        if (!StringUtils.hasText(item.getImageUrl())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "物资图片不能为空");
        }
        if (item.getInitialUnitPrice() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "初始入库单价不能为空");
        }
        if (item.getInboundOperatorId() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "入库人不能为空");
        }
        validateNonNegative(item.getInitialUnitPrice(), "初始入库单价不能小于0");
        validateCategory(item.getCategory());
        validateEnabledStatus(item.getStatus());
        validateNonNegative(item.getStockQty(), "库存数量不能小于0");
        validateNonNegative(item.getWarningQty(), "预警数量不能小于0");
    }

    private void normalizeItemDefaults(WarehouseItem item) {
        item.setItemName(item.getItemName().trim());
        item.setItemCode(normalizeOptionalText(item.getItemCode()));
        item.setImageUrl(item.getImageUrl().trim());
        item.setSpecification(normalizeOptionalText(item.getSpecification()));
        item.setUnit(item.getUnit().trim());
        item.setStockQty(item.getStockQty() == null ? ZERO : item.getStockQty());
        item.setWarningQty(item.getWarningQty() == null ? ZERO : item.getWarningQty());
        item.setManufacturer(normalizeOptionalText(item.getManufacturer()));
        item.setStatus(item.getStatus() == null ? 1 : item.getStatus());
        item.setRemark(normalizeOptionalText(item.getRemark()));
    }

    private void validateRecordCreate(WarehouseRecord record) {
        if (record == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "出入库记录不能为空");
        }
        requireId(record.getItemId(), "物资ID不能为空");
        validateRecordType(record.getRecordType());
        if (record.getRecordType() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "出入库类型不能为空");
        }
        if (record.getQuantity() == null || record.getQuantity().compareTo(ZERO) <= 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "出入库数量必须大于0");
        }
        String requestId = normalizeOptionalText(record.getRequestId());
        if (requestId == null || requestId.length() < 8 || requestId.length() > 64
                || !requestId.matches("[A-Za-z0-9][A-Za-z0-9._:-]*")) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "业务请求幂等键格式不正确");
        }
        record.setRequestId(requestId);
        if (Integer.valueOf(2).equals(record.getRecordType()) && !StringUtils.hasText(record.getRecipient())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "领用人不能为空");
        }
        validateNonNegative(record.getPrice(), "单价不能小于0");
    }

    private void normalizeRecord(WarehouseRecord record, WarehouseItem item) {
        BigDecimal beforeQty = item.getStockQty() == null ? ZERO : item.getStockQty();
        BigDecimal afterQty = switch (record.getRecordType()) {
            case 1 -> beforeQty.add(record.getQuantity());
            case 2 -> beforeQty.subtract(record.getQuantity());
            case 3 -> record.getQuantity();
            default -> beforeQty;
        };
        if (afterQty.compareTo(ZERO) < 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "库存不足，不能出库");
        }
        record.setUserId(item.getFarmOwnerId());
        record.setOperatorId(dataPermissionService.currentUser().getId());
        record.setBeforeQty(beforeQty);
        record.setAfterQty(afterQty);
        record.setSupplier(normalizeOptionalText(record.getSupplier()));
        record.setRecipient(normalizeOptionalText(record.getRecipient()));
        record.setRemark(normalizeOptionalText(record.getRemark()));
        record.setStatus(1);
        if (record.getRecordTime() == null) {
            record.setRecordTime(LocalDateTime.now());
        }
        record.setTotalAmount(record.getPrice() == null
                ? null
                : record.getPrice().multiply(record.getQuantity()).setScale(2, RoundingMode.HALF_UP));
    }

    private WarehouseItem getItemByIdForUpdate(Long id) {
        WarehouseItem item = itemMapper.selectByIdForUpdate(id);
        if (item == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "物资不存在");
        }
        return item;
    }

    private void validateIdempotentRetry(WarehouseRecord request, WarehouseRecord existing) {
        Long currentUserId = dataPermissionService.currentUser().getId();
        boolean sameRequest = Objects.equals(existing.getOperatorId(), currentUserId)
                && Objects.equals(existing.getItemId(), request.getItemId())
                && Objects.equals(existing.getRecordType(), request.getRecordType())
                && sameNumber(existing.getQuantity(), request.getQuantity())
                && Objects.equals(existing.getRelatedPlotId(), request.getRelatedPlotId())
                && Objects.equals(existing.getRelatedTaskId(), request.getRelatedTaskId())
                && Objects.equals(existing.getRelatedFaultId(), request.getRelatedFaultId())
                && Objects.equals(existing.getSourceType(), request.getSourceType())
                && Objects.equals(existing.getRecipient(), normalizeOptionalText(request.getRecipient()))
                && Objects.equals(existing.getSupplier(), normalizeOptionalText(request.getSupplier()))
                && Objects.equals(existing.getRemark(), normalizeOptionalText(request.getRemark()))
                && sameNumber(existing.getPrice(), request.getPrice());
        if (!sameRequest) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "业务请求幂等键已用于其他出入库操作");
        }
    }

    private boolean sameNumber(BigDecimal left, BigDecimal right) {
        return left == null ? right == null : right != null && left.compareTo(right) == 0;
    }

    private void recordInitialInbound(WarehouseItem item) {
        BigDecimal stockQty = item.getStockQty() == null ? ZERO : item.getStockQty();
        if (stockQty.compareTo(ZERO) <= 0) {
            return;
        }
        WarehouseRecord record = new WarehouseRecord();
        record.setItemId(item.getId());
        record.setUserId(item.getFarmOwnerId());
        record.setOperatorId(item.getInboundOperatorId());
        record.setRecordType(1);
        record.setQuantity(stockQty);
        record.setBeforeQty(ZERO);
        record.setAfterQty(stockQty);
        record.setPrice(item.getInitialUnitPrice());
        record.setTotalAmount(item.getInitialUnitPrice().multiply(stockQty).setScale(2, RoundingMode.HALF_UP));
        record.setStatus(1);
        record.setRecordTime(LocalDateTime.now());
        record.setRemark("新增物资入库");
        recordMapper.insert(record);
    }

    private void validateRelatedResources(WarehouseRecord record, Long userId) {
        if (Integer.valueOf(2).equals(record.getRecordType()) && record.getRelatedFaultId() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "物资出库必须关联设备故障");
        }
        if (record.getRelatedFaultId() != null) {
            if (!Integer.valueOf(2).equals(record.getRecordType())) {
                throw new BusinessException(ResponseCode.PARAM_ERROR, "只有出库记录可以关联设备故障");
            }
            if (faultMapper.countActiveOwnedFault(record.getRelatedFaultId(), userId) == 0) {
                throw new BusinessException(ResponseCode.PARAM_ERROR, "关联故障不存在或不属于当前物资所属用户");
            }
        }
        if (record.getRelatedPlotId() != null) {
            Plot plot = plotMapper.selectById(record.getRelatedPlotId());
            if (plot == null) {
                throw new BusinessException(ResponseCode.NOT_FOUND, "关联地块不存在");
            }
            if (!userId.equals(plot.getUserId())) {
                throw new BusinessException(ResponseCode.FORBIDDEN, "不能关联其他用户的地块");
            }
        }
        if (record.getRelatedTaskId() != null) {
            var task = farmTaskMapper.selectById(record.getRelatedTaskId());
            if (task == null) {
                throw new BusinessException(ResponseCode.NOT_FOUND, "关联农事任务不存在");
            }
            if (!userId.equals(task.getUserId())) {
                throw new BusinessException(ResponseCode.FORBIDDEN, "不能关联其他用户的农事任务");
            }
        }
    }

    private void checkUniqueItemCode(WarehouseItem item) {
        if (StringUtils.hasText(item.getItemCode())
                && itemMapper.countByUserIdAndItemCode(item.getFarmOwnerId(), item.getItemCode(), item.getId()) > 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "该农场主下物资编码已存在");
        }
    }

    /**
     * 使用数据库自增主键生成稳定编码；编号不足三位时左侧补零，超过三位时完整保留。
     */
    private String generateItemCode(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ResponseCode.FAIL, "物资编号生成失败");
        }
        return "DF_" + String.format(Locale.ROOT, "%03d", id);
    }

    /** 在服务端验证角色和数据权限，防止客户端把物资绑定到其他账号。 */
    private Long resolveFarmOwnerId(Long requestedOwnerId) {
        if (requestedOwnerId == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "所属农场主不能为空");
        }
        User owner = userMapper.selectById(requestedOwnerId);
        if (owner == null || !Integer.valueOf(1).equals(owner.getStatus())
                || !"farm_owner".equalsIgnoreCase(owner.getRoleCode())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "所属农场主不存在或未启用");
        }
        dataPermissionService.requireFarmManager(requestedOwnerId);
        return requestedOwnerId;
    }

    private Long resolveInboundOperatorId(Long requestedOperatorId) {
        requireInboundOperatorSelector();
        if (requestedOperatorId == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "入库人不能为空");
        }
        User operator = userMapper.selectById(requestedOperatorId);
        if (operator == null || !Integer.valueOf(1).equals(operator.getStatus())) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "入库人不存在或已被禁用");
        }
        return requestedOperatorId;
    }

    private void requireInboundOperatorSelector() {
        String roleCode = dataPermissionService.currentUser().getRoleCode();
        if (!dataPermissionService.isAdmin() && !"farm_owner".equalsIgnoreCase(roleCode)) {
            throw new BusinessException(ResponseCode.FORBIDDEN, "仅管理员和农场主可以选择入库人");
        }
    }

    private void validateCategory(Integer category) {
        if (category != null && (category < 1 || category > 6)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "物资分类只能为1到6");
        }
    }

    private void validateRecordType(Integer recordType) {
        if (recordType != null && recordType != 1 && recordType != 2 && recordType != 3) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "记录类型只能为1、2或3");
        }
    }

    private void validateEnabledStatus(Integer status) {
        if (status != null && status != 0 && status != 1) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "状态只能为0或1");
        }
    }

    private void validateNonNegative(BigDecimal value, String message) {
        if (value != null && value.compareTo(ZERO) < 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, message);
        }
    }

    private String normalizeOptionalText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    private void requireId(Long id, String message) {
        if (id == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, message);
        }
    }
}
