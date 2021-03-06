package net.example;

import net.example.HtmlLinkExtractor.HtmlLink;
import org.junit.Before;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Example Tests
 */
public class HtmlLinkExtractorTest {

	private static URL BASE_URL;

	@Before
	public void  before() throws MalformedURLException {
		BASE_URL = new URL("http://www.home.de");
	}

	@Test
	public void testNull(){
		assertThat( HtmlLinkExtractor.parseLinks(null, BASE_URL) ).isNotNull().isEmpty();
	}

	@Test
	public void testEmpty() throws MalformedURLException {
		
		assertThat(HtmlLinkExtractor.parseLinks("<a href=\"\">Heise</a>", BASE_URL))
			.isNotNull().isEmpty();

        assertThat(HtmlLinkExtractor.parseLinks("<a href=\" \">Heise</a>", BASE_URL))
                .isNotNull().isEmpty();

	}

	
	@Test
	public void testInvalidInput(){


		assertThat( HtmlLinkExtractor.parseLinks("", BASE_URL) ).isNotNull().isEmpty();
		assertThat( HtmlLinkExtractor.parseLinks("   ", BASE_URL) ).isNotNull().isEmpty();
		assertThat( HtmlLinkExtractor.parseLinks("a", BASE_URL) ).isNotNull().isEmpty();
		assertThat( HtmlLinkExtractor.parseLinks("<a>", BASE_URL) ).isNotNull().isEmpty();
		assertThat( HtmlLinkExtractor.parseLinks("a</a>", BASE_URL) ).isNotNull().isEmpty();
		assertThat( HtmlLinkExtractor.parseLinks("<a</a>", BASE_URL) ).isNotNull().isEmpty();
		assertThat( HtmlLinkExtractor.parseLinks("<a></a>", BASE_URL) ).isNotNull().isEmpty();
		assertThat( HtmlLinkExtractor.parseLinks("<a href></a>", BASE_URL) ).isNotNull().isEmpty();
        assertThat( HtmlLinkExtractor.parseLinks("<a href=  >Heise</a>", BASE_URL) ).isNotNull().isEmpty();
		assertThat( HtmlLinkExtractor.parseLinks("<a href=\"\">", BASE_URL) ).isNotNull().isEmpty();
		assertThat( HtmlLinkExtractor.parseLinks("<a href=\"\"></a>", BASE_URL) ).isNotNull().isEmpty();
		assertThat( HtmlLinkExtractor.parseLinks("<a href></a>", BASE_URL) ).isNotNull().isEmpty();

		assertThat( HtmlLinkExtractor.parseLinks("<a href=\"www.heise.de\"></a>", BASE_URL) ).isNotNull().isEmpty();
		
		assertThat( HtmlLinkExtractor.parseLinks("<a href=\"\"></a>", BASE_URL) ).isNotNull().isEmpty();
	}
	
	
	@Test
	public void testValidInput() throws MalformedURLException {
				
		assertThat( HtmlLinkExtractor.parseLinks("<a href=www.heise.de>Heise</a>", BASE_URL) )
			.isNotNull().hasSize(1).usingFieldByFieldElementComparator().containsExactly(new HtmlLink("www.heise.de", "Heise"));
			
	}
	
	
}