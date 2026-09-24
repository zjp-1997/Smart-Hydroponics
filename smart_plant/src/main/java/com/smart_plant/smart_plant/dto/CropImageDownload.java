package com.smart_plant.smart_plant.dto;

public record CropImageDownload(byte[] bytes, String fileName, String contentType) {
}
