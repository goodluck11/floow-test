package io.floow;


import com.mongodb.MongoClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDbFactory;

@Configuration
public class SimpleMongoConfig {
    @Autowired
    MyCommandLine myCommandLine;
    @Bean
    public MongoClient mongo() {
        System.out.println("before mongo creation " +myCommandLine.getHost());
        return new MongoClient(myCommandLine.getHost(), myCommandLine.getPortInt());
    }
    @Bean
    public MongoClient secondary(final MongoProperties mongo) {
        return new MongoClient(mongo.getHost(), mongo.getPort());
    }
}
