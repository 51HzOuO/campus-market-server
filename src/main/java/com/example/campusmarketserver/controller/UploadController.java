package com.example.campusmarketserver.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/upload")
public class UploadController {

    private static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;
    private static final java.util.Set<String> ALLOWED_TYPES = java.util.Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp");

    @Value("${upload.path:/tmp/uploads}")
    private String uploadPath;

    @Value("${upload.url:https://springboot-4xrc-316010-10-1490372189.sh.run.tcloudbase.com}")
    private String uploadUrl;

    @PostMapping("/image")
    public Map<String, Object> uploadImage(@RequestParam("file") MultipartFile file) throws IOException {
        Map<String, Object> result = new HashMap<>();

        if (file == null || file.isEmpty()) {
            return error(400, "文件不能为空");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            return error(413, "图片不能超过 5MB");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if (!ALLOWED_TYPES.contains(contentType)) {
            return error(400, "仅支持 jpg/png/gif/webp 格式的图片");
        }

        // 创建上传目录
        File dir = new File(uploadPath);
        if (!dir.exists()) {
            if (!dir.mkdirs() && !dir.isDirectory()) {
                return error(500, "上传目录不可用，请联系管理员");
            }
        }

        // 生成文件名
        String originalFilename = file.getOriginalFilename();
        String suffix = switch (contentType) {
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
        String filename = UUID.randomUUID().toString().replace("-", "") + suffix;

        // 保存文件
        File dest = new File(dir, filename);
        file.transferTo(dest);

        // 返回访问路径
        String url = "/uploads/" + filename;
        String fullUrl = uploadUrl + url;

        result.put("code", 200);
        result.put("message", "上传成功");
        Map<String, Object> data = new HashMap<>();
        data.put("url", url);
        data.put("fullUrl", fullUrl);
        result.put("data", data);

        return result;
    }

    private static Map<String, Object> error(int code, String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", code);
        result.put("message", message);
        result.put("data", null);
        return result;
    }
}
