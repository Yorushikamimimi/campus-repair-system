package com.campusrepair.storage;

import com.campusrepair.common.BusinessException;
import com.campusrepair.common.ErrorCode;
import com.campusrepair.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class LocalFileStorageServiceImpl implements FileStorageService {
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private final Path basePath;

    public LocalFileStorageServiceImpl(@Value("${app.storage.base-path:./uploads/repair}") String basePath) {
        this.basePath = Path.of(basePath).toAbsolutePath().normalize();
    }

    @Override
    public String save(Long orderId, String fileName, byte[] content) {
        validateFile(orderId, fileName, content);
        String safeOriginalName = Path.of(fileName).getFileName().toString();
        String extension = extensionOf(safeOriginalName);
        String storedName = UUID.randomUUID() + "." + extension;
        Path orderPath = basePath.resolve(String.valueOf(orderId)).normalize();
        Path target = orderPath.resolve(storedName).normalize();
        try {
            Files.createDirectories(orderPath);
            if (!target.startsWith(orderPath)) throw new BusinessException(ErrorCode.BAD_REQUEST, "非法文件路径");
            Files.write(target, content, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            return "/uploads/repair/" + orderId + "/" + storedName;
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "图片文件写入失败");
        }
    }

    @Override
    public byte[] load(String fileUrl) {
        try {
            Path target = resolveStoredPath(fileUrl);
            return Files.readAllBytes(target);
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "图片文件不存在");
        }
    }

    @Override
    public void delete(String fileUrl) {
        try {
            Files.deleteIfExists(resolveStoredPath(fileUrl));
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "图片文件删除失败");
        }
    }

    private void validateFile(Long orderId, String fileName, byte[] content) {
        validateOrderId(orderId);
        validateFilename(fileName);
        validateContent(content);
        validateExtension(fileName);
    }

    private void validateOrderId(Long orderId) {
        if (orderId == null || orderId <= 0) throw new BusinessException(ErrorCode.BAD_REQUEST, "工单ID不合法");
    }

    private void validateFilename(String fileName) {
        if (fileName == null || fileName.isBlank()) throw new BusinessException(ErrorCode.BAD_REQUEST, "图片文件名不能为空");
        Path path = Path.of(fileName);
        if (!path.getFileName().toString().equals(fileName) || fileName.contains("\\")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "图片文件名不合法");
        }
    }

    private void validateContent(byte[] content) {
        if (content == null || content.length == 0) throw new BusinessException(ErrorCode.BAD_REQUEST, "不能上传空图片");
    }

    private void validateExtension(String fileName) {
        if (!ALLOWED_EXTENSIONS.contains(extensionOf(fileName))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅支持JPG、PNG、GIF或WEBP图片");
        }
    }

    private Path resolveStoredPath(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith("/uploads/repair/")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "图片路径不合法");
        }
        String relative = fileUrl.substring("/uploads/repair/".length());
        Path target = basePath.resolve(relative).normalize();
        if (!target.startsWith(basePath) || relative.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "图片路径不合法");
        }
        return target;
    }

    private String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
