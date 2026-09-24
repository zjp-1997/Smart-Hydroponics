package com.smart_plant.smart_plant.service;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.dto.CameraImageDownload;
import com.smart_plant.smart_plant.entity.CameraImage;

import java.time.LocalDate;
import java.util.List;

public interface CameraImageService {

    CameraImage addCameraImage(CameraImage cameraImage);

    void deleteCameraImage(Long id);

    int deleteCameraImages(List<Long> ids);

    CameraImage updateCameraImage(CameraImage cameraImage);

    CameraImage getCameraImageById(Long id);

    CameraImageDownload downloadCameraImage(Long id);

    PageInfo<CameraImage> listCameraImages(Long cameraId, Long plotId, String cameraName, String plotName,
                                           LocalDate startDate, LocalDate endDate,
                                           Integer pageNum, Integer pageSize);
}
