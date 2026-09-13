package com.campusrepair.service;

public interface FileStorageService {
    String save(Long orderId, String fileName, byte[] content);
    byte[] load(String fileUrl);
    void delete(String fileUrl);
}
