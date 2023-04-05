package com.example.lightdigitaltesttask;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@RestController
public class PhotoController {
    private final FtpClient ftpClient;

    public PhotoController(FtpClient ftpClient) {
        this.ftpClient = ftpClient;
    }

    @GetMapping("photos")
    public List<?> getAll(@RequestParam String targetFolder, @RequestParam String prefix) {
        try {
            System.out.println(prefix);
            ftpClient.open();
            Collection<String> files = ftpClient.listFiles(targetFolder, prefix);
            ftpClient.close();
            return Arrays.asList(files.toArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("photo")
    public ResponseEntity<Resource> getByName(@RequestParam String filename) {
        File file;
        try {
            ftpClient.open();
            file = ftpClient.downloadFile(filename);
            ftpClient.close();

            Path path = Paths.get(file.getAbsolutePath());
            ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));

            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(new InputStreamResource(resource.getInputStream()));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
