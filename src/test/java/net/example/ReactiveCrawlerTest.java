package net.example;

import java.util.Arrays;
import java.util.List;

import net.example.HtmlLinkExtractor.HtmlLink;
import org.bson.Document;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by jah on 1/12/16.
 */
public class ReactiveCrawlerTest {

    @Test
    public void testLinksAsDocuments(){

        List<HtmlLink> links = Arrays.asList(new HtmlLink("http://www1", "w1"), new HtmlLink("http://www2", "w2"));
        assertThat(ReactiveCrawler.linksAsDocuments(links)).hasSize(2);

    }
}
