package net.example;

import com.mongodb.ServerAddress;
import com.mongodb.async.client.MongoClientSettings;
import com.mongodb.client.model.*;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.connection.ClusterSettings;
import com.mongodb.connection.ConnectionPoolSettings;
import com.mongodb.rx.client.MongoClient;
import com.mongodb.rx.client.MongoClients;
import com.mongodb.rx.client.MongoCollection;
import com.mongodb.rx.client.MongoDatabase;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.rx.RxClient;
import org.glassfish.jersey.client.rx.rxjava.RxObservable;
import org.glassfish.jersey.client.rx.rxjava.RxObservableInvoker;
import rx.Notification;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Arrays.asList;
import static net.example.UrlDocument.FIELD_NAME_URL_HOST;
import static net.example.UrlDocument.FIELD_NAME_URL_PATH;

/**
 * @author Hendrik Jander
 */
public class ReactiveCrawler {


    public static final String FIELD_NAME_STATUS = "status";

    public static final String HOST = "192.168.99.100:27017";

    enum CrawlStatus {
        OK, PROGRESS, ERROR
    }


    public static void main(String[] args) throws InterruptedException, MalformedURLException {

        CountDownLatch cl = new CountDownLatch(1);

        RxClient<RxObservableInvoker> client = RxObservable.newClient(Executors.newFixedThreadPool(32));
        MongoCollection<Document> linkCollection = setupLinkCollection();

        URL initialLink = new URL("http://www.zeit.de/index");
        linkCollection.insertOne(UrlDocument.asDocument(initialLink)).toBlocking().single();

        Observable.interval(200, TimeUnit.MILLISECONDS).onBackpressureBlock()
                .flatMap(tick -> links(linkCollection))
                .flatMap(linkDoc -> httpGet(client, linkDoc).observeOn(Schedulers.computation())
                        .map(html -> HtmlLinkExtractor.parseLinks(html, UrlDocument.asUrl(linkDoc)))
                        .map(ReactiveCrawler::linksAsDocuments).observeOn(Schedulers.io())
                        .flatMap(docs -> persist(linkCollection, linkDoc, docs)))
                .subscribeOn(Schedulers.io())
                .subscribe();

        cl.await();
    }


    static Observable<String> httpGet(RxClient<RxObservableInvoker> client, Document toCrawl) {

        URL targetUrl = UrlDocument.asUrl(toCrawl);
        System.out.printf("%s Getting document %s%n", Thread.currentThread(), targetUrl);

        return client
                .target(targetUrl.toExternalForm())
                .property(ClientProperties.FOLLOW_REDIRECTS, Boolean.TRUE)
                .request().rx().get(Response.class)

                .filter(response -> response.getHeaderString(HttpHeaders.CONTENT_TYPE).contains("html"))
                .map(htmlResponse -> htmlResponse.readEntity(String.class))
                .onErrorResumeNext(throwable -> {
                    System.out.printf("ERROR:%s", throwable.getMessage());
                    return Observable.empty();
                });

    }


    static public Observable<Document> links(MongoCollection<Document> linkCollection) {

        Bson documentNotInProgressFilter = Filters.ne(FIELD_NAME_STATUS, CrawlStatus.PROGRESS.name());
        Document setDocumentInProgress = new Document("$set", new Document(FIELD_NAME_STATUS, CrawlStatus.PROGRESS.name()));
        FindOneAndUpdateOptions foo = new FindOneAndUpdateOptions().sort(Sorts.ascending(FIELD_NAME_URL_HOST, FIELD_NAME_URL_PATH));

        return linkCollection.findOneAndUpdate(documentNotInProgressFilter, setDocumentInProgress, foo)
                .doOnNext(document -> {
                  System.out.printf("%s Next Link: %s %n", Thread.currentThread(), UrlDocument.asUrl(document).toExternalForm());
                })
                .doOnError(throwable -> {
                    System.out.printf("ERROR %s", throwable.getMessage());
                });

    }


    static public Observable<UpdateResult> persist(MongoCollection<Document> linkCollection, Document parentDoc, Set<Document> linkedDocuments) {

        System.out.printf("ParentDoc  %s", parentDoc);

        Func1<Document, Observable<UpdateResult>> upsertDoc =
                doc -> linkCollection.updateOne(
                          Filters.and(
                                  Filters.eq(FIELD_NAME_URL_HOST, doc.getString(FIELD_NAME_URL_HOST)),
                                  Filters.eq(FIELD_NAME_URL_PATH, doc.getString(FIELD_NAME_URL_PATH))),
                          new Document("$set", doc)
                                  .append("$currentDate", new Document("gathered", new Document("$type", "timestamp")))
                                  .append("$addToSet", new Document("incoming", parentDoc.getString(FIELD_NAME_URL_HOST))),
                          new UpdateOptions().upsert(true));


        return Observable.from(linkedDocuments).flatMap(upsertDoc)
                .doOnNext(updateResult -> System.out.printf("%s Persisting Document id: %s , inserted: %s %n",
                                Thread.currentThread(), updateResult.getUpsertedId(),
                                updateResult.getMatchedCount() == 0))

                .doOnError(throwable -> linkCollection.updateOne(
                                new Document(FIELD_NAME_URL_HOST, parentDoc.getString(FIELD_NAME_URL_HOST))
                                        .append(FIELD_NAME_URL_PATH, parentDoc.getString(FIELD_NAME_URL_PATH)),
                                new Document(FIELD_NAME_STATUS, CrawlStatus.ERROR))
                )
                .onErrorResumeNext(throwable -> {
                    System.out.printf("Error: %s", throwable.getMessage());
                    return Observable.empty();
                })
                .doOnCompleted(() -> linkCollection.updateOne(
                                new Document(FIELD_NAME_URL_HOST, parentDoc.getString(FIELD_NAME_URL_HOST))
                                        .append(FIELD_NAME_URL_PATH, parentDoc.getString(FIELD_NAME_URL_PATH)),
                                new Document(FIELD_NAME_STATUS, CrawlStatus.OK))
                );
    }


  static private MongoCollection<Document> setupLinkCollection() {

        ClusterSettings clusterSettings = ClusterSettings.builder()
                .hosts(asList(new ServerAddress(HOST))).build();

        ConnectionPoolSettings connectionPoolSettings = ConnectionPoolSettings.builder().maxSize(100)
                .maxWaitQueueSize(100000).build();

        MongoClientSettings settings = MongoClientSettings.builder().clusterSettings(clusterSettings)
                .connectionPoolSettings(connectionPoolSettings).build();

        MongoClient mongoClient = MongoClients.create(settings);
        MongoDatabase database = mongoClient.getDatabase("crawler");
        MongoCollection<Document> collection = database.getCollection("links");

        cleanup:
        {
            collection.deleteMany(new Document()).toBlocking().single();
        }

        collection.createIndex(
                new Document(FIELD_NAME_URL_HOST, 1).append(FIELD_NAME_URL_PATH, 1),
                new IndexOptions().unique(true).background(true))
                .toBlocking().single();

        return collection;
    }


    static Set<Document> linksAsDocuments(Set<HtmlLinkExtractor.HtmlLink> links) {

        return links.stream()
                .filter(link -> link.getLink().getProtocol().equals("http"))
                .collect(HashSet::new,
                        (set, link) -> set.add(
                                UrlDocument.asDocument(link.getLink()).append("linktext", link.getLinkText())),
                        HashSet::addAll);

    }


    static <T> Action1<Notification<? super T>> debug(String description, String offset) {

        AtomicReference<String> nextOffset = new AtomicReference<>(">");
        return (Notification<? super T> notification) -> {

            switch (notification.getKind()) {
                case OnNext:
                    System.out.println(Thread.currentThread().getName() + "|" + description + ": " + offset
                            + nextOffset.get());
                    break;
                case OnError:
                    System.err.println(Thread.currentThread().getName() + "|" + description + ": " + offset
                            + nextOffset.get() + " X " + notification.getThrowable());
                    break;
                case OnCompleted:
                    System.out.println(
                            Thread.currentThread().getName() + "|" + description + ": " + offset + nextOffset.get() + "|");
                default:
                    break;
            }
            nextOffset.getAndUpdate(p -> "-" + p);
        };
    }

}