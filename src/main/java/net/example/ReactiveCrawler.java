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
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Arrays.asList;

/**
 * @author Hendrik Jander
 *
 */
public class ReactiveCrawler {

	public static final String FIELD_NAME_URL = "url";


	public static void main(String[] args) throws InterruptedException {


		CountDownLatch cl = new CountDownLatch(1);

		RxClient<RxObservableInvoker> client = RxObservable.newClient(Executors.newFixedThreadPool(64));
		MongoCollection<Document> linkCollection = setupLinkCollection();

		//String initialLink = "http://localhost:8888/testImgTagBasicAuth.html";
		String initialLink = "http://www.faz.de";

		linkCollection.insertOne(new Document("url", initialLink)).toBlocking().single();

		
		Observable.interval(500, TimeUnit.MILLISECONDS)
			.flatMap(tick -> links(linkCollection))
				.flatMap(linkDoc -> httpGet(client, linkDoc)
						.map(HtmlLinkExtractor::parseLinks).map(ReactiveCrawler::linksAsDocuments).subscribeOn(Schedulers.computation())
						.flatMap(docs -> persist(linkCollection, linkDoc, docs)))//.subscribeOn(Schedulers.io())
				.subscribe();

		cl.await();
	}


	static Observable<String> httpGet(RxClient<RxObservableInvoker> client, Document url) {

		System.out.printf("%s Getting document %s%n", Thread.currentThread(), url.getString("url"));

		return client
				.target(url.getString("url"))
				.property(ClientProperties.FOLLOW_REDIRECTS, Boolean.TRUE)
				.request().rx().get(Response.class)
				.filter(response -> response.getHeaderString(HttpHeaders.CONTENT_TYPE).contains("html"))
				.map(htmlResponse -> htmlResponse.readEntity(String.class)).onExceptionResumeNext(Observable.<String>empty());

	}



	static public Observable<Document> links(MongoCollection<Document> linkCollection) {

		return linkCollection.findOneAndUpdate(Filters.ne("status", "DONE"),
				new Document("$set", new Document("status", "DONE")),
				new FindOneAndUpdateOptions().sort(Sorts.ascending(FIELD_NAME_URL)))
					.doOnNext(document -> {
						System.out.printf("%s Next Link: %s %n", Thread.currentThread(), document.getString(FIELD_NAME_URL));
					});
	}

	
	static public Observable<UpdateResult> persist(MongoCollection<Document> linkCollection, Document linkDoc, Set<Document> documents) {

		Func1<Document, Observable<UpdateResult>> insert =
				doc -> linkCollection.updateOne(
						Filters.eq("url", doc.getString("url")),
						new Document("$set", doc).append("$currentDate", new Document("lastVisit", new Document("$type", "timestamp") ) ),
						new UpdateOptions().upsert(true));

		return Observable.from(documents).flatMap(insert)
				.onBackpressureDrop(updateResult1 -> System.out.printf("BACKPESSURE"))
				.doOnNext(updateResult ->
						System.out.printf("%s Persisting Document: %s , inserted: %b %n",
								Thread.currentThread(), linkDoc.getString(FIELD_NAME_URL), updateResult.getMatchedCount()==0));

	}


	private static MongoCollection<Document> setupLinkCollection() {

		ClusterSettings clusterSettings = ClusterSettings.builder()
				.hosts(asList(new ServerAddress("192.168.99.105:27017"))).build();
		ConnectionPoolSettings connectionPoolSettings = ConnectionPoolSettings.builder().maxSize(500)
				.maxWaitQueueSize(10000).build();

		MongoClientSettings settings = MongoClientSettings.builder().clusterSettings(clusterSettings)
				.connectionPoolSettings(connectionPoolSettings).build();

		MongoClient mongoClient = MongoClients.create(settings);
		MongoDatabase database = mongoClient.getDatabase("crawler");
		MongoCollection<Document> collection = database.getCollection("links");

		cleanup: {
			collection.deleteMany(new Document()).toBlocking().single();
		}

		collection.createIndex(new Document(FIELD_NAME_URL, 1), new IndexOptions().unique(true).background(true))
				.toBlocking().single();

		return collection;
	}


	static public Set<Document> linksAsDocuments(Set<HtmlLinkExtractor.HtmlLink> links) {

		return links.stream()
				.filter(link -> link.getLink().startsWith("http://"))
				.collect(HashSet::new,
						(set, link) -> set.add(new Document("url", link.getLink()).append("linktext", link.getLinkText())),
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