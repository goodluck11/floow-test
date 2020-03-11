package io.floow;

import com.mongodb.MongoClient;
import lombok.Getter;
import org.slf4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.util.concurrent.*;

@Component
@Getter
@CommandLine.Command
public class MyCommandLine  implements Callable<Integer> {
    @CommandLine.Option(names = "-source", required = true, description = "File path to the file which will be processed")
    private String source;

    @CommandLine.Option(names = "-mongo", required = true, description = "mongo db host and port" , split = ":")
    private String [] mongo;

    private String host;
    private String port;
    private int portInt;

    private void setHostAndPort(String host, String port) {
        this.host = host.replace("[","").replace("]","");
        this.port = port.replace("[","").replace("]","");
        setPortAsInt(this.port);
    }
    private void setPortAsInt(String port)  {
           try{ this.portInt = Integer.parseInt(port);}
           catch (Exception e){
               System.out.println("could not convert port to interger");
           }
    }
    @Override
    public Integer call() {
        System.out.println("mycommand was called with source =" +source);
        System.out.println("mycommand was called with mongo =" +mongo[0] +"and "+mongo[1]);
        setHostAndPort(mongo[0],mongo[1]);
//        BlockingQueue<File> queue = new ArrayBlockingQueue<>(4);
//        ExecutorService executorService = Executors.newSingleThreadExecutor();
//        new Thread(new FileProcessor(queue, new File("source"))).start();
//        executorService.submit(new WordProcessor(queue));
        System.out.println("Started Executing::::::::::::::");

        new MongoClient(getHost(), getPortInt());
        return 23;
    }
//    @Bean
//    public MongoClient mongo() {
//        return new MongoClient(this.host, this.portInt);
//    }
}
