package io.floow;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class WordProcessor implements Runnable {

    private BlockingQueue<File> queue;

    WordProcessor(BlockingQueue<File> queue) {
        this.queue = queue;
    }

    @Override
    public void run() {
        File file;
        Pattern p = Pattern.compile("<anchor>(\\S+)</anchor>");

        try {
            while ((file = queue.take()) != null) {
                System.out.println("Consuming....");
                List<String> collect = Files.lines(Paths.get(file.getAbsolutePath()))
                        .map(val -> StringUtils.substringBetween(val, "<anchor>", "</anchor>"))
                        .filter(value -> !StringUtils.isEmpty(value)).collect(Collectors.toList());
                collect.forEach(w -> {
                    Word word = Word.builder().filePath(">>>>>>>>>>>>.").word(w).build();
                    System.out.println("Words " + word);
                });
            }
        } catch (InterruptedException | IOException exception) {
            exception.printStackTrace();
        }
    }
}
