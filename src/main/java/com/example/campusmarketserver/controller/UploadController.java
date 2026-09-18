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

    @Value("${upload.path:/tmp/uploads}")
    private String uploadPath;

    @Value("${upload.url:https://springboot-4xrc-316010-10-1490372189.sh.run.tcloudbase.com}")
    private String uploadUrl;

    @PostMapping("/image")
    public Map<String, Object> uploadImage(@RequestParam("file") MultipartFile file) throws IOException {
        Map<String, Object> result = new HashMap<>();

        // 创建上传目录
        File dir = new File(uploadPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 生成文件名
        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".jpg";
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
}