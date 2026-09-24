package com.smart_plant.smart_plant.service;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.dto.ClientFarmListResponse;
import com.smart_plant.smart_plant.dto.FarmAddressResolveResult;
import com.smart_plant.smart_plant.dto.FarmImageUploadResult;
import com.smart_plant.smart_plant.entity.Farm;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface FarmService {

    Farm addFarm(Farm farm);

    FarmImageUploadResult uploadFarmImage(MultipartFile file);

    FarmAddressResolveResult resolveAddressByCoordinate(String coordinate, String coordinateSystem);

    Farm updateFarm(Farm farm);

    void deleteFarm(Long id);

    int deleteFarms(List<Long> ids);

    void updateStatus(Long id, Integer status);

    Farm getFarmById(Long id);

    PageInfo<Farm> listFarms(String farmName, String farmCode, Long userId, Integer status,
                             Integer pageNum, Integer pageSize);

    Map<String, Object> statisticsFarms();

    List<ClientFarmListResponse> listCurrentClientFarms();
}
