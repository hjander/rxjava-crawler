package net.example;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.bson.Document;
import org.junit.Test;

import com.mongodb.rx.client.MongoClient;
import com.mongodb.rx.client.MongoClients;
import com.mongodb.rx.client.MongoCollection;
import com.mongodb.rx.client.MongoDatabase;

/**
 * Created by Hendrik Jander on 12/25/15.
 */
public class RxMongoExampleIntegrationTest {

    @Test
    public void testConnect() throws InterruptedException {


        MongoClient mongoClient = MongoClients.create("mongodb://192.168.99.105:27017");

        MongoDatabase database = mongoClient.getDatabase("exampleDb");
        MongoCollection<Document> collection = database.getCollection("exampleCollection");

        collection.deleteMany(new Document()).toBlocking();

        List<Document> documents = new ArrayList<Document>();
        for (int i = 0; i < 100; i++) {
            documents.add(new Document("i", i));
        }

        collection.insertMany(documents).timeout(5, TimeUnit.SECONDS)
        	.doOnCompleted(() -> 
        			collection.count().doOnEach( count -> assertThat(count).isEqualTo(100L) ) );
        
        
        mongoClient.close();
        
    }

}
