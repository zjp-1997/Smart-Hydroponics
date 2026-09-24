package com.smart_plant.smart_plant.dto;

public record CameraImageDownload(byte[] bytes, String fileName, String contentType) {
}
