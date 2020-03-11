package io.floow;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.concurrent.BlockingQueue;

public class FileProcessor implements Runnable {

    private BlockingQueue<File> queue;
    private File filePath;

    FileProcessor(BlockingQueue<File> q, File filePath) {
        this.queue = q;
        this.filePath = filePath;
    }

    @Override
    public void run() {
        int sizeOfFiles = 10485760; // 10MB
        long fileLength = filePath.length();
        System.out.println("File Length::::::: " + fileLength);
        byte[] newFileByte = new byte[sizeOfFiles];
        int x = 1, j;
        String newFile;
        try {
            FileInputStream inputStream = new FileInputStream(filePath);
            FileOutputStream outputStream = null;

            int readByte;
            while (inputStream.available() != 0) {
                j = 0;
                newFile = (x <= 9) ? filePath.getParentFile() + File.separator + "00" + x + filePath.getName() : filePath.getParentFile() + File.separator + "\\0" + x + filePath.getName();
                System.out.println("NEW FILE NAME:::::::: " + newFile);
                File newFilePath = new File(newFile);
                outputStream = new FileOutputStream(newFilePath);

                while (j <= sizeOfFiles && inputStream.available() != 0) {
                    readByte = inputStream.read(newFileByte, 0, sizeOfFiles);
                    j += readByte;
                    outputStream.write(newFileByte, 0, readByte);
                }
                queue.put(newFilePath);
                System.out.println("File Written");
                x++;
            }
            if (outputStream != null) {
                outputStream.close();
            }
            inputStream.close();
            System.out.println("File Splitted Successfully");
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

}
