package net.example;

import org.junit.Test;

import junit.framework.TestCase;
import net.example.HtmlLinkExtractor.HtmlLink;

import static org.assertj.core.api.Assertions.*;
/**
 * Example Tests
 */
public class HtmlLinkExtractorTest extends TestCase {

	
	@Test
	public void testNull(){
		assertThat( HtmlLinkExtractor.parseLinks(null) ).isNotNull().isEmpty();
	}

	@Test
	public void testEmpty(){
		
		assertThat( HtmlLinkExtractor.parseLinks("<a href=  >Heise</a>") )
		.isNotNull().isEmpty();
			
		assertThat( HtmlLinkExtractor.parseLinks("<a href=\"\">Heise</a>") )
			.isNotNull().hasSize(1).usingFieldByFieldElementComparator().containsExactly(new HtmlLink("", "Heise"));

		assertThat( HtmlLinkExtractor.parseLinks("<a href=\" \">Heise</a>") )
		.isNotNull().hasSize(1).usingFieldByFieldElementComparator().containsExactly(new HtmlLink(" ", "Heise"));

	}

	
	@Test
	public void testInvalidInput(){
		
		assertThat( HtmlLinkExtractor.parseLinks("") ).isNotNull().isEmpty();
		assertThat( HtmlLinkExtractor.parseLinks("   ") ).isNotNull().isEmpty();		
		assertThat( HtmlLinkExtractor.parseLinks("a") ).isNotNull().isEmpty();
		assertThat( HtmlLinkExtractor.parseLinks("<a>") ).isNotNull().isEmpty();
		assertThat( HtmlLinkExtractor.parseLinks("a</a>") ).isNotNull().isEmpty();
		assertThat( HtmlLinkExtractor.parseLinks("<a</a>") ).isNotNull().isEmpty();
		assertThat( HtmlLinkExtractor.parseLinks("<a></a>") ).isNotNull().isEmpty();
		assertThat( HtmlLinkExtractor.parseLinks("<a href></a>") ).isNotNull().isEmpty();
		assertThat( HtmlLinkExtractor.parseLinks("<a href=\"\">") ).isNotNull().isEmpty();
		assertThat( HtmlLinkExtractor.parseLinks("<a href=\"\"></a>") ).isNotNull().isEmpty();
		assertThat( HtmlLinkExtractor.parseLinks("<a href></a>") ).isNotNull().isEmpty();
	
		assertThat( HtmlLinkExtractor.parseLinks("<a href=\"www.heise.de\"></a>") ).isNotNull().isEmpty();
		
		assertThat( HtmlLinkExtractor.parseLinks("<a href=\"\"></a>") ).isNotNull().isEmpty();
	}
	
	
	@Test
	public void testValidInput(){
				
		assertThat( HtmlLinkExtractor.parseLinks("<a href=www.heise.de>Heise</a>") )
			.isNotNull().hasSize(1).usingFieldByFieldElementComparator().containsExactly(new HtmlLink("www.heise.de", "Heise"));
			
	}
	
	
}