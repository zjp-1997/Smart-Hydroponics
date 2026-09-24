package com.smart_plant.smart_plant.controller;

import com.smart_plant.smart_plant.dto.ClientDeviceFaultCompleteRequest;
import com.smart_plant.smart_plant.dto.ClientDeviceFaultResponse;
import com.smart_plant.smart_plant.dto.ClientDeviceFaultRecordResponse;
import com.smart_plant.smart_plant.dto.CropImageUploadResult;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.service.ClientDeviceFaultService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * farm 移动端设备故障接口。
 *
 * <p>用户身份由登录令牌确定，接口不接收 userId，防止越权读取或处理他人故障。</p>
 */
@RestController
@RequestMapping({"/client/device-faults", "/api/client/device-faults"})
@RequiredArgsConstructor
public class ClientDeviceFaultController {

    private final ClientDeviceFaultService clientDeviceFaultService;

    @GetMapping("/list")
    public R<List<ClientDeviceFaultResponse>> listFaults(@RequestParam(required = false) Integer status) {
        // 状态为空时返回当前用户有权查看的全部故障。
        return R.success(clientDeviceFaultService.listFaults(status));
    }

    /** 技术人员首页读取本人全部故障，并按待处理、处理中、已完成顺序返回。 */
    @GetMapping({"/technician/tasks", "/technician/active"})
    public R<List<ClientDeviceFaultResponse>> listTechnicianFaults() {
        // 保留旧 active 地址兼容尚未升级的客户端，两条路径均返回新的完整任务列表。
        return R.success(clientDeviceFaultService.listTechnicianFaults());
    }

    /** “处理记录”只展示该技术人员已完成的故障。 */
    @GetMapping("/technician/completed")
    public R<List<ClientDeviceFaultResponse>> listTechnicianCompletedFaults() {
        return R.success(clientDeviceFaultService.listTechnicianCompletedFaults());
    }

    /** farm 维护记录详情，ID 只用于定位故障，访问范围由登录令牌决定。 */
    @GetMapping("/{id}/record")
    public R<ClientDeviceFaultRecordResponse> getFaultRecord(@PathVariable Long id) {
        return R.success(clientDeviceFaultService.getFaultRecord(id));
    }

    @PostMapping("/{id}/actions/accept")
    public R<ClientDeviceFaultResponse> acceptFault(@PathVariable Long id) {
        // 使用动作型接口表达“接受指派”，避免客户端直接篡改状态字段。
        return R.success(clientDeviceFaultService.acceptFault(id));
    }

    @PostMapping("/{id}/actions/complete")
    public R<ClientDeviceFaultResponse> completeFault(
            @PathVariable Long id,
            @RequestBody ClientDeviceFaultCompleteRequest request) {
        // 服务层确认图片来自本系统上传目录，再通过统一状态机完成故障。
        return R.success(clientDeviceFaultService.completeFault(
                id, request.getHandleResult(), request.getCompletionImageUrl()));
    }

    @PostMapping("/{id}/completion-image")
    public R<CropImageUploadResult> uploadCompletionImage(@PathVariable Long id,
                                                           @RequestParam("image") MultipartFile image) {
        // 上传前校验故障归属及处理中状态，避免未授权用户上传凭证。
        return R.success(clientDeviceFaultService.uploadCompletionImage(id, image));
    }
}
