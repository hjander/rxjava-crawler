package net.example;

import org.bson.Document;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by jah on 2/17/16.
 */

class UrlDocument {

  public static final String FIELD_NAME_URL_HOST = "urlHost";
  public static final String FIELD_NAME_URL_PATH = "urlPath";


  static Document asDocument(URL url){
    return new Document()
            .append(FIELD_NAME_URL_HOST, url.getProtocol() + "://" + url.getAuthority())
            .append(FIELD_NAME_URL_PATH, url.getPath());
  }


  static URL asUrl(Document url){
    String u = url.getString(FIELD_NAME_URL_HOST) + url.getString(FIELD_NAME_URL_PATH);
    try {
      return new URL(u);

    } catch (MalformedURLException e) {
      System.out.printf("Invalid Url:" , u);
      return null;
    }
  }
}
