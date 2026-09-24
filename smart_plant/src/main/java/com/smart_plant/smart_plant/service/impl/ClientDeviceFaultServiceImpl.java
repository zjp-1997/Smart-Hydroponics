package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.dto.ClientDeviceFaultResponse;
import com.smart_plant.smart_plant.dto.ClientDeviceFaultRecordResponse;
import com.smart_plant.smart_plant.dto.CropImageUploadResult;
import com.smart_plant.smart_plant.entity.IotDeviceFault;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.IotDeviceFaultMapper;
import com.smart_plant.smart_plant.response.ResponseCode;
import com.smart_plant.smart_plant.service.ClientDeviceFaultService;
import com.smart_plant.smart_plant.service.CropImageService;
import com.smart_plant.smart_plant.service.DataPermissionService;
import com.smart_plant.smart_plant.service.IotDeviceFaultService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * farm 移动端设备故障业务实现。
 *
 * <p>复用后台故障服务中的数据权限和状态机，保证网页端与移动端处理规则一致。</p>
 */
@Service
@RequiredArgsConstructor
public class ClientDeviceFaultServiceImpl implements ClientDeviceFaultService {

    private static final int ASSIGN_STATUS_PENDING = 1;
    private static final int ASSIGN_STATUS_ACCEPTED = 2;
    private static final int FAULT_STATUS_PENDING = 0;
    private static final int FAULT_STATUS_PROCESSING = 1;
    private static final int FAULT_STATUS_COMPLETED = 2;
    /** 仅接受故障凭证目录下由上传接口生成的日期/UUID 文件名。 */
    private static final Pattern COMPLETION_IMAGE_PATH = Pattern.compile(
            "^/uploads/fault-completion-images/\\d{4}-\\d{2}-\\d{2}/[0-9a-fA-F-]{36}\\.(jpg|jpeg|png|webp)$");

    private final IotDeviceFaultService iotDeviceFaultService;
    private final DataPermissionService dataPermissionService;
    private final CropImageService cropImageService;
    private final IotDeviceFaultMapper faultMapper;

    @Override
    public List<ClientDeviceFaultResponse> listFaults(Integer status) {
        // 底层列表方法会依据农场主、维修人员等角色自动限制数据范围。
        List<IotDeviceFault> faults = iotDeviceFaultService.listFaults(
                null, null, null, null, null, null, status, null, 1, 500).getList();
        return faults.stream().map(this::toResponse).toList();
    }

    @Override
    public List<ClientDeviceFaultResponse> listTechnicianFaults() {
        requireTechnician();
        // 状态不设筛选即可读取本人全部指派；底层仍按 handle_user_id 限制数据范围。
        List<ClientDeviceFaultResponse> faults = new ArrayList<>(listFaults(null));
        // 待处理优先，其次处理中，已完成和已关闭位于末尾；同一状态内优先显示较新故障。
        faults.sort(Comparator
                .comparingInt((ClientDeviceFaultResponse fault) -> technicianStatusOrder(fault.getStatus()))
                .thenComparing(ClientDeviceFaultResponse::getStartTime,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ClientDeviceFaultResponse::getId,
                        Comparator.nullsLast(Comparator.reverseOrder())));
        return faults;
    }

    /** 将数据库状态转换为技术人员首页所需的展示顺序。 */
    private int technicianStatusOrder(Integer status) {
        if (status == null) return 4;
        return switch (status) {
            case FAULT_STATUS_PENDING -> 0;
            case FAULT_STATUS_PROCESSING -> 1;
            case FAULT_STATUS_COMPLETED -> 2;
            default -> 3;
        };
    }

    @Override
    public List<ClientDeviceFaultResponse> listTechnicianCompletedFaults() {
        requireTechnician();
        return listFaults(FAULT_STATUS_COMPLETED);
    }

    private void requireTechnician() {
        if (!dataPermissionService.isTechnician()) {
            throw new BusinessException(ResponseCode.FORBIDDEN, "仅技术人员可访问工作台");
        }
    }

    @Override
    public ClientDeviceFaultRecordResponse getFaultRecord(Long id) {
        // 与接单/完成接口共用 getFaultById 的设备归属及处理人权限校验。
        IotDeviceFault fault = iotDeviceFaultService.getFaultById(id);
        List<ClientDeviceFaultRecordResponse.Entry> records = new ArrayList<>();
        LocalDateTime reportedAt = fault.getStartTime() != null ? fault.getStartTime() : fault.getCreateTime();
        records.add(new ClientDeviceFaultRecordResponse.Entry(
                "reported", FAULT_STATUS_PENDING, "发现故障",
                StringUtils.hasText(fault.getFaultDesc()) ? fault.getFaultDesc() : "系统已生成设备故障记录",
                "系统记录", reportedAt, null));

        String handlerName = StringUtils.hasText(fault.getHandleNickname())
                ? fault.getHandleNickname() : fault.getHandleUsername();
        if (fault.getStatus() != null && fault.getStatus() >= FAULT_STATUS_PROCESSING) {
            // 旧数据曾在完成时覆盖 handle_time；这种情况不把完成时间冒充接单时间。
            LocalDateTime acceptedAt = Objects.equals(fault.getHandleTime(), fault.getEndTime())
                    ? null : fault.getHandleTime();
            records.add(new ClientDeviceFaultRecordResponse.Entry(
                    "accepted", FAULT_STATUS_PROCESSING, "开始维护", "已接受故障维护任务，开始处理",
                    StringUtils.hasText(handlerName) ? handlerName : "处理人员", acceptedAt, null));
        }
        if (fault.getStatus() != null && fault.getStatus() >= FAULT_STATUS_COMPLETED) {
            records.add(new ClientDeviceFaultRecordResponse.Entry(
                    "completed", FAULT_STATUS_COMPLETED, "完成维护",
                    StringUtils.hasText(fault.getHandleResult()) ? fault.getHandleResult() : "故障处理已完成",
                    StringUtils.hasText(handlerName) ? handlerName : "处理人员",
                    fault.getEndTime(), fault.getCompletionImageUrl()));
        }
        return new ClientDeviceFaultRecordResponse(toResponse(fault), List.copyOf(records));
    }

    @Override
    public ClientDeviceFaultResponse acceptFault(Long id) {
        IotDeviceFault fault = iotDeviceFaultService.getFaultById(id);
        if (isCurrentOwner(fault) && Objects.equals(fault.getStatus(), FAULT_STATUS_PENDING)) {
            // 农场主可直接处理自己设备的故障；条件更新防止并发认领。
            if (faultMapper.startOwnedFault(id, dataPermissionService.currentUser().getId()) != 1) {
                throw new BusinessException(ResponseCode.PARAM_ERROR, "故障已被处理，请刷新列表");
            }
        } else {
            // 技术员仍沿用后台接单状态机及其权限校验。
            iotDeviceFaultService.acceptAssignment(id);
        }
        return toResponse(iotDeviceFaultService.getFaultById(id));
    }

    @Override
    public CropImageUploadResult uploadCompletionImage(Long id, MultipartFile image) {
        // 复用故障服务的权限检查，并只允许已接单的处理人上传。
        requireCompletableFault(id);
        return cropImageService.uploadFaultCompletionImage(image);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClientDeviceFaultResponse completeFault(Long id, String handleResult, String completionImageUrl) {
        requireCompletableFault(id);
        // 说明可省略，但底层状态机要求非空结果，使用明确的默认记录。
        String result = StringUtils.hasText(handleResult) ? handleResult.trim() : "已上传现场图片，故障处理完成";
        if (result.length() > 255) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "完成说明不能超过255个字符");
        }
        if (!StringUtils.hasText(completionImageUrl)
                || !COMPLETION_IMAGE_PATH.matcher(completionImageUrl).matches()
                || !Files.isRegularFile(Path.of(System.getProperty("user.dir"), completionImageUrl.substring(1)))) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请先上传故障完成凭证图片");
        }
        // 同一事务内更新故障状态、关联设备在线状态与图片地址，任一步失败都回滚。
        iotDeviceFaultService.updateStatus(id, FAULT_STATUS_COMPLETED, result);
        if (faultMapper.updateCompletionImage(id, completionImageUrl) != 1) {
            throw new BusinessException(ResponseCode.FAIL, "故障完成凭证保存失败");
        }
        return toResponse(iotDeviceFaultService.getFaultById(id));
    }

    private void requireCompletableFault(Long id) {
        IotDeviceFault fault = iotDeviceFaultService.getFaultById(id);
        User user = dataPermissionService.currentUser();
        if (user == null || !(dataPermissionService.isTechnician() || isCurrentOwner(fault))
                || !Objects.equals(user.getId(), fault.getHandleUserId())) {
            throw new BusinessException(ResponseCode.FORBIDDEN, "只有当前处理人可以完成故障");
        }
        if (!Objects.equals(fault.getStatus(), FAULT_STATUS_PROCESSING)
                || !Objects.equals(fault.getAssignStatus(), ASSIGN_STATUS_ACCEPTED)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "只有处理中且已接单的故障可以完成");
        }
    }

    /** 农场主身份和故障归属必须同时成立，避免仅凭角色访问他人的设备。 */
    private boolean isCurrentOwner(IotDeviceFault fault) {
        User user = dataPermissionService.currentUser();
        return user != null && "farm_owner".equals(user.getRoleCode())
                && Objects.equals(user.getId(), fault.getDeviceOwnerId());
    }

    /**
     * 将数据库实体转换为移动端稳定模型，并计算当前用户可执行的动作。
     */
    private ClientDeviceFaultResponse toResponse(IotDeviceFault fault) {
        User currentUser = dataPermissionService.currentUser();
        boolean isAssignedTechnician = dataPermissionService.isTechnician()
                && currentUser != null
                && Objects.equals(currentUser.getId(), fault.getHandleUserId());
        boolean isOwner = isCurrentOwner(fault);
        boolean canAccept = (isOwner && Objects.equals(fault.getStatus(), FAULT_STATUS_PENDING))
                || (isAssignedTechnician && Objects.equals(fault.getAssignStatus(), ASSIGN_STATUS_PENDING)
                && (Objects.equals(fault.getStatus(), FAULT_STATUS_PENDING)
                || Objects.equals(fault.getStatus(), FAULT_STATUS_PROCESSING)));
        boolean canComplete = (isAssignedTechnician || isOwner
                && currentUser != null && Objects.equals(currentUser.getId(), fault.getHandleUserId()))
                && Objects.equals(fault.getAssignStatus(), ASSIGN_STATUS_ACCEPTED)
                && Objects.equals(fault.getStatus(), FAULT_STATUS_PROCESSING);

        String handleUserName = StringUtils.hasText(fault.getHandleNickname())
                ? fault.getHandleNickname()
                : fault.getHandleUsername();
        return new ClientDeviceFaultResponse(
                fault.getId(), fault.getDeviceId(), fault.getDeviceCode(), fault.getDeviceName(),
                fault.getPlotName(), fault.getFaultCode(), fault.getFaultName(), fault.getFaultType(),
                fault.getSeverity(), fault.getFaultDesc(), fault.getStatus(), fault.getAssignStatus(),
                fault.getHandleUserId(), handleUserName, fault.getStartTime(), fault.getHandleTime(),
                fault.getEndTime(), fault.getDuration(), fault.getHandleResult(),
                fault.getCompletionImageUrl(), canAccept, canComplete);
    }
}
