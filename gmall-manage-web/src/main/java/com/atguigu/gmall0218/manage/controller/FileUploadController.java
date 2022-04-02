package com.atguigu.gmall0218.manage.controller;

import org.apache.commons.lang3.StringUtils;
import org.csource.common.MyException;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@CrossOrigin
public class FileUploadController {
    //String ip = "http://81.69.33.96" 硬编码
    //把服务器Id作为一个配置文件放入项目中！要软编码！
    //@Value 使用的前提条件，当前类必须在 spring 容器中
    @Value("${fileServer.url}")
    private String fileUrl;//fileUrl = "http://81.69.33.96"


    //获取上传文件需要使用, springmvc 技术 # MultipartFile file
    @RequestMapping("fileUpload")
    public String fileUpload(MultipartFile file) throws IOException, MyException {
        String imgUrl = fileUrl;
        //当文件不为空的时候才能上传
        if(file != null){
            // JAVA获取当前文件路径
            String configFile = this.getClass().getResource("/tracker.conf").getFile();
            ClientGlobal.init(configFile);
            TrackerClient trackerClient=new TrackerClient();

            //获取连接
            TrackerServer trackerServer=trackerClient.getTrackerServer();
            StorageClient storageClient=new StorageClient(trackerServer,null);

            //获取上传文件名称
            String orginalFilename = file.getOriginalFilename();
            //获取文件后缀名
            String extName = StringUtils.substringAfterLast(orginalFilename, ".");
            //String orginalFilename = "C://Users//16503//Desktop//a.jpg";

            //String[] upload_file = storageClient.upload_file(orginalFilename, "jpg", null);
            //上传图片//因为获取的是服务器传过来的不带本地名
            String[] upload_file = storageClient.upload_file(file.getBytes(), extName, null);

            for (int i = 0; i < upload_file.length; i++) {
                String path = upload_file[i];
                imgUrl += "/" + path;
            }
        }
        return imgUrl;
    }


}
