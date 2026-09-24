package com.smart_plant.smart_plant.service;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.dto.CropImageDownload;
import com.smart_plant.smart_plant.dto.CropImageUploadResult;
import com.smart_plant.smart_plant.entity.CropImage;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

public interface CropImageService {

    CropImage addCropImage(CropImage cropImage);

    void deleteCropImage(Long id);

    int deleteCropImages(List<Long> ids);

    CropImage updateCropImage(CropImage cropImage);

    CropImage getCropImageById(Long id);

    CropImageDownload downloadCropImage(Long id);

    CropImageUploadResult uploadCropImage(MultipartFile file);

    CropImageUploadResult uploadPlotCropImage(MultipartFile file);

    /** 上传农事任务完成凭证，使用独立目录与普通作物图片隔离。 */
    CropImageUploadResult uploadTaskCompletionImage(MultipartFile file);

    /** 上传设备故障完成凭证，沿用统一的图片格式和大小校验。 */
    CropImageUploadResult uploadFaultCompletionImage(MultipartFile file);

    PageInfo<CropImage> listCropImages(Long userId, LocalDate startDate, LocalDate endDate,
                                       Integer pageNum, Integer pageSize);
}
