package net.example;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class HtmlLinkExtractor {

    private static final String HTML_A_TAG_PATTERN = "(?i)<a([^>]+)>(.+?)</a>";
    private static final String HTML_A_HREF_TAG_PATTERN = "\\s*(?i)href\\s*=\\s*((\"([^\"]*)\")|'[^']*'|([^'\">\\s]+))";

    private static final Pattern patternTag = Pattern.compile(HTML_A_TAG_PATTERN);
    private static final Pattern patternLink = Pattern.compile(HTML_A_HREF_TAG_PATTERN);

    /**
     * Validate html with regular expression
     *
     * @param html
     *            html content for validation
     * @return List links and link text
     */
    public static Set<HtmlLink> parseLinks(final String html) {

        if(html != null){
	        
	    	Set<HtmlLink> result = new HashSet<>();
	    	
	        Matcher matcherTag, matcherLink;
	        matcherTag = patternTag.matcher(html);
	
	        while (matcherTag.find()) {
	
	            String href = matcherTag.group(1); // href
	            String linkText = matcherTag.group(2); // link text
	
	            matcherLink = patternLink.matcher(href);
	
	            while (matcherLink.find()) {
	                String link = matcherLink.group(1); // link
	                result.add(new HtmlLink(link.replaceAll("\"", ""), linkText));
	            }
	        }

	        return (result);
	    }else{
            return Collections.emptySet();
	    }
    }


    static class HtmlLink {

        private String link;
        private String linkText;

       
        HtmlLink(String link, String linkText) {
			this.link = link;
			this.linkText = linkText;
		}

		@Override
        public String toString() {
            return new StringBuffer("Link : ").append(this.link).append(" Link Text : ").append(this.linkText)
                    .toString();
        }

        public String getLink() {
            return link;
        }

        public void setLink(String link) {
            this.link = replaceInvalidChar(link);
        }

        public String getLinkText() {
            return linkText;
        }

        public void setLinkText(String linkText) {
            this.linkText = linkText;
        }

        private String replaceInvalidChar(String link) {
            link = link.replaceAll("'", "");
            link = link.replaceAll("\"", "");
            return link;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            HtmlLink htmlLink = (HtmlLink) o;
            return Objects.equals(link, htmlLink.link) &&
                    Objects.equals(linkText, htmlLink.linkText);
        }

        @Override
        public int hashCode() {
            return Objects.hash(link, linkText);
        }
    }
}
