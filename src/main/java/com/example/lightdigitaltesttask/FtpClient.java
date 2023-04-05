package com.example.lightdigitaltesttask;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;
import java.util.stream.Stream;

@Service
class FtpClient {
    @Value("${app.ftp.host}")
    private String server;
    @Value("${app.ftp.port}")
    private int port;
    @Value("${app.ftp.user}")
    private String user;
    @Value("${app.ftp.password}")
    private String password;

    private String target;
    private FTPClient ftp;


    void open() throws IOException {
        ftp = new FTPClient();
        ftp.setControlEncoding("utf-8");
        ftp.connect(server, port);
        int reply = ftp.getReplyCode();
        if (!FTPReply.isPositiveCompletion(reply)) {
            ftp.disconnect();
            throw new IOException("Exception in connecting to FTP Server");
        }

        ftp.login(user, password);
    }

    void close() throws IOException {
        ftp.disconnect();
    }

    Collection<String> listFiles(String target, String prefix) throws IOException {
        this.target = target;
        List<String> folders = filterDots(ftp.listDirectories("")).map(ftpFile -> {
            try {
                return recursiveDirectorySearch(ftpFile, ftp.printWorkingDirectory());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).reduce(new ArrayList<>(), (strings, strings2) -> {
            strings.addAll(strings2);
            return strings;
        });
        return folders.stream().map(s -> {
                    try {
                        String cwd = ftp.printWorkingDirectory();
                        ftp.changeWorkingDirectory(s);
                        var files = filterDots(ftp.listFiles())
                                .filter(ftpFile -> ftpFile.getName().contains(prefix));
                        ftp.changeWorkingDirectory(cwd);
                        return files.map(ftpFile -> s + "/" + ftpFile.getName()).toList();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .reduce(new ArrayList<>(), (strings, strings2) -> {
                    strings.addAll(strings2);
                    return strings;
                });
    }

    File downloadFile(String source) throws IOException {
        ftp.setFileType(FTP.BINARY_FILE_TYPE);
        ftp.enterLocalPassiveMode();
        var s = source.split("/");
        var name = s[s.length - 1];
        var suf = name.split("\\.")[1];

        File downloadFile1 = File.createTempFile("tmp", "." + suf);
        downloadFile1.deleteOnExit();
        OutputStream outputStream1 = new BufferedOutputStream(new FileOutputStream(downloadFile1));
        ftp.retrieveFile(source, outputStream1);
        outputStream1.close();


        return downloadFile1;
    }


    private List<String> recursiveDirectorySearch(FTPFile directory, String wd) throws IOException {
        if (!directory.isDirectory()) return List.of();
        ftp.changeWorkingDirectory(directory.getName());
        if (directory.getName().equals(target)) {
            ftp.changeWorkingDirectory("..");
            return Collections.singletonList(wd + "/" + directory.getName());
        }
        var directories = filterDots(ftp.listDirectories());

        var l = directories.map(ftpFile -> {
            try {
                return recursiveDirectorySearch(ftpFile, ftp.printWorkingDirectory());

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).reduce(new ArrayList<>(), (strings, strings2) -> {
            strings.addAll(strings2);
            return strings;
        });

        ftp.changeWorkingDirectory("..");
        return l;
    }

    private Stream<FTPFile> filterDots(FTPFile[] src) {
        return Arrays.stream(src)
                .filter(ftpFile -> !List.of(".", "..").contains(ftpFile.getName()));
    }
}
