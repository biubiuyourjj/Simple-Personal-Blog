package com.itheima.controller;

import com.itheima.pojo.Result;
import com.itheima.utils.AliOssUtil;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * ClassName:FileUploadController
 * Package:com.itheima.controller
 * Description:
 * Num:
 *
 * @Author SGG_蒋志凯
 * @Create 2026/1/21 下午5:12
 * @Version 1.0
 **/
@RestController
public class FileUploadController {
    @PostMapping("/upload")
    public Result<String> upload(MultipartFile file) throws Exception {
        //把文件的内容存储到本地磁盘上
        String originalFilename = file.getOriginalFilename();
        //保证文件名字唯一的，从而防止文件覆盖
        String filename = UUID.randomUUID().toString()+originalFilename.substring(originalFilename.lastIndexOf("."));
        //file.transferTo(new File("D:\\fileadd\\"+filename));
        String url = AliOssUtil.uploadFile(filename,file.getInputStream());
        return Result.success(url);
    }
}
