package com.smart_plant.smart_plant.service;

import com.smart_plant.smart_plant.dto.ClientPictureBatchRequest;
import com.smart_plant.smart_plant.dto.ClientPictureDownload;
import com.smart_plant.smart_plant.dto.ClientPictureListResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * farm 用户端图片服务。
 *
 * <p>该服务聚合手机图片和摄像头采集图片，并统一处理用户数据范围。</p>
 */
public interface ClientPictureService {

    /**
     * 查询当前登录用户可见的图片列表。
     *
     * @return 按手机图片和摄像头图片分组后的列表数据
     */
    ClientPictureListResponse listCurrentClientPictures();

    /**
     * 查询当前登录用户的手机图片列表。
     *
     * @return 只包含手机拍摄或上传图片的列表数据
     */
    List<ClientPictureListResponse.PictureItem> listCurrentClientPhonePictures();

    /**
     * 查询当前登录用户的摄像头采集图片列表。
     *
     * @return 只包含摄像头采集图片的列表数据
     */
    List<ClientPictureListResponse.PictureItem> listCurrentClientCameraPictures();

    /** 上传一张当前登录用户的手机图片。 */
    ClientPictureListResponse.PictureItem uploadCurrentClientPhonePicture(MultipartFile image);

    /**
     * 删除当前登录用户选中的图片。
     *
     * @param pictures 前端选中的图片引用，包含图片来源和图片ID
     * @return 实际删除数量
     */
    int deleteCurrentClientPictures(List<ClientPictureBatchRequest.PictureRef> pictures);

    /**
     * 查询当前登录用户选中图片的下载地址。
     *
     * @param pictures 前端选中的图片引用，包含图片来源和图片ID
     * @return 已通过权限校验的图片列表
     */
    List<ClientPictureListResponse.PictureItem> prepareCurrentClientPictureDownloads(List<ClientPictureBatchRequest.PictureRef> pictures);

    /**
     * 下载当前登录用户的一张图片。
     *
     * @param picture 图片来源和图片ID
     * @return 图片二进制、文件名和响应类型
     */
    ClientPictureDownload downloadCurrentClientPicture(ClientPictureBatchRequest.PictureRef picture);

    /**
     * 打包下载当前登录用户选中的多张图片。
     *
     * @param pictures 前端选中的图片引用
     * @return zip 二进制、文件名和响应类型
     */
    ClientPictureDownload downloadCurrentClientPictures(List<ClientPictureBatchRequest.PictureRef> pictures);
}
