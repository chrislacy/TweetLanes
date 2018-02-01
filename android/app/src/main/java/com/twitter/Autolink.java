package com.twitter;

import com.twitter.Extractor.Entity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import twitter4j.MediaEntity;
import twitter4j.URLEntity;

/**
 * A class for adding HTML links to hashtag, username and list references in
 * Tweet text.
 */
public class Autolink {

    public static interface LinkAttributeModifier {
        public void modify(Entity entity, Map<String, String> attributes);
    }

    public static interface LinkTextModifier {
        public CharSequence modify(Entity entity, CharSequence text);
    }

    private final String usernameUrlBase;
    private final String listUrlBase;
    private final String hashtagUrlBase;
    private final String cashtagUrlBase;
    private final String symbolTag = null;
    private final String textWithSymbolTag = null;
    private final LinkAttributeModifier linkAttributeModifier = null;
    private final LinkTextModifier linkTextModifier = null;

    private final Extractor extractor = new Extractor();

    private static CharSequence escapeHTML(CharSequence text) {
        StringBuilder builder = new StringBuilder(text.length() * 2);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '&':
                    builder.append("&amp;");
                    break;
                case '>':
                    builder.append("&gt;");
                    break;
                case '<':
                    builder.append("&lt;");
                    break;
                case '"':
                    builder.append("&quot;");
                    break;
                case '\'':
                    builder.append("&#39;");
                    break;
                default:
                    builder.append(c);
                    break;
            }
        }
        return builder;
    }

    public Autolink() {
        usernameUrlBase = "com.tweetlanes.android.core.profile://";
        listUrlBase = "com.tweetlanes.android.core.profile://";
        hashtagUrlBase = "com.tweetlanes.android.core.search://";
        cashtagUrlBase = "com.tweetlanes.android.core.search://";
        extractor.setExtractURLWithoutProtocol(false);
    }

    public void setExtractURLWithoutProtocol(boolean newValue) {
        extractor.setExtractURLWithoutProtocol(newValue);
    }

    String escapeBrackets(String text) {
        int len = text.length();
        if (len == 0) return text;

        StringBuilder sb = new StringBuilder(len + 16);
        for (int i = 0; i < len; ++i) {
            char c = text.charAt(i);
            if (c == '>')
                sb.append("&gt;");
            else if (c == '<')
                sb.append("&lt;");
            else
                sb.append(c);
        }
        return sb.toString();
    }

    void linkToText(Entity entity, CharSequence text,
                    Map<String, String> attributes, StringBuilder builder) {

        if (linkAttributeModifier != null) {
            linkAttributeModifier.modify(entity, attributes);
        }
        if (linkTextModifier != null) {
            text = linkTextModifier.modify(entity, text);
        }
        // append <a> tag
        builder.append("<a");
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            builder.append(" ").append(escapeHTML(entry.getKey()))
                    .append("=\"").append(escapeHTML(entry.getValue()))
                    .append("\"");
        }
        builder.append(">").append(text).append("</a>");
    }

    void linkToTextWithSymbol(Entity entity, CharSequence symbol,
                              CharSequence text, Map<String, String> attributes,
                              StringBuilder builder) {
        CharSequence taggedSymbol = symbolTag == null || symbolTag.isEmpty() ? symbol
                : String.format("<%s>%s</%s>", symbolTag, symbol, symbolTag);
        text = escapeHTML(text);
        CharSequence taggedText = textWithSymbolTag == null
                || textWithSymbolTag.isEmpty() ? text : String.format(
                "<%s>%s</%s>", textWithSymbolTag, text, textWithSymbolTag);

        linkToText(entity, taggedSymbol.toString() + taggedText, attributes, builder);
    }

    void linkToHashtag(Entity entity, String text, StringBuilder builder) {
        // Get the original hash char from text as it could be a full-width
        // char.
        CharSequence hashChar = text.subSequence(entity.getStart(),
                entity.getStart() + 1);
        CharSequence hashtag = entity.getValue();

        Map<String, String> attrs = new LinkedHashMap<String, String>();
        attrs.put("href", hashtagUrlBase + "#" + hashtag);

        linkToTextWithSymbol(entity, hashChar, hashtag, attrs, builder);
    }

    void linkToCashtag(Entity entity, String text, StringBuilder builder) {
        CharSequence cashtag = entity.getValue();

        Map<String, String> attrs = new LinkedHashMap<String, String>();
        attrs.put("href", cashtagUrlBase + cashtag);

        linkToTextWithSymbol(entity, "$", cashtag, attrs, builder);
    }

    void linkToMentionAndList(Entity entity, String text,
                              StringBuilder builder) {
        String mention = entity.getValue();
        // Get the original at char from text as it could be a full-width char.
        CharSequence atChar = text.subSequence(entity.getStart(),
                entity.getStart() + 1);

        Map<String, String> attrs = new LinkedHashMap<String, String>();
        if (entity.listSlug != null) {
            mention += entity.listSlug;

            attrs.put("href", listUrlBase + mention);
        } else {

            attrs.put("href", usernameUrlBase + mention);
        }

        linkToTextWithSymbol(entity, atChar, mention, attrs, builder);
    }

    void linkToURL(Entity entity, String text, StringBuilder builder,
                   URLEntity urlEntity, boolean showFullUrl) {
        CharSequence url = entity.getValue();
        String linkText = escapeHTML(url).toString();

        if (urlEntity != null && urlEntity.getExpandedURL() != null && showFullUrl) {
            linkText = urlEntity.getExpandedURL();
        } else if (urlEntity != null && urlEntity.getDisplayURL() != null) {
            linkText = urlEntity.getDisplayURL();
        } else if (entity.displayURL != null) {
            linkText = entity.displayURL;
        }

        //Remove "http://" or "https://"
        //Then remove "www."
        linkText = linkText.replaceAll("^https?://", "").replaceAll("^www.", "");

        Map<String, String> attrs = new LinkedHashMap<String, String>();

        if (urlEntity != null && urlEntity.getExpandedURL() != null) {
            attrs.put("href", urlEntity.getExpandedURL());
        } else {
            attrs.put("href", url.toString());
        }

        linkToText(entity, linkText, attrs, builder);
    }

    String autoLinkEntities(String text, List<Entity> entities,
                            MediaEntity[] mediaEntities, URLEntity[] urlEntities, boolean showFullUrl) {
        StringBuilder builder = new StringBuilder(text.length() * 2);
        int beginIndex = 0;

        int urlCount = 0;

        for (Entity entity : entities) {
            builder.append(text.subSequence(beginIndex, entity.start));

            switch (entity.type) {
                case URL:
                    URLEntity urlEntity = null;

                    if (urlEntities != null && urlCount < urlEntities.length) {
                        urlEntity = urlEntities[urlCount];
                        urlCount += 1;
                    }

                    if (urlEntity == null) {
                        if (entity != null && entity.value != null
                                && mediaEntities != null
                                && mediaEntities.length > 0) {
                            MediaEntity mediaEntity = mediaEntities[0];
                            if (entity.value
                                    .equals(mediaEntity.getURL())) {
                                entity.displayURL = mediaEntity.getDisplayURL();
                                entity.expandedURL = mediaEntity.getExpandedURL();
                            }
                        }
                    }

                    linkToURL(entity, text, builder, urlEntity, showFullUrl);
                    break;
                case HASHTAG:
                    linkToHashtag(entity, text, builder);
                    break;
                case MENTION:
                    linkToMentionAndList(entity, text, builder);
                    break;
                case CASHTAG:
                    linkToCashtag(entity, text, builder);
                    break;
            }
            beginIndex = entity.end;
        }
        builder.append(text.subSequence(beginIndex, text.length()));

        return builder.toString();
    }

    public String autoLinkAll(String text, MediaEntity[] mediaEntities,
                              URLEntity[] urlEntities, boolean showFullUrl) {
        text = escapeBrackets(text);

        // extract entities
        List<Entity> entities = extractor.extractEntitiesWithIndices(text);
        return autoLinkEntities(text, entities,
                mediaEntities, urlEntities, showFullUrl);
    }
}
