package net.example;

import net.example.HtmlLinkExtractor.HtmlLink;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by jah on 1/12/16.
 */
public class ReactiveCrawlerTest {

    @Test
    public void testLinksAsDocuments(){

        Set<HtmlLink> links = new HashSet<>(Arrays.asList(new HtmlLink("http://www1", "w1"), new HtmlLink("http://www2", "w2")));
        assertThat(ReactiveCrawler.linksAsDocuments(links)).hasSize(2);

    }
}
