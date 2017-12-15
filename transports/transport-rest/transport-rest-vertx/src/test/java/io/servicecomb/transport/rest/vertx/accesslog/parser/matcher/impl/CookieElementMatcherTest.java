package io.servicecomb.transport.rest.vertx.accesslog.parser.matcher.impl;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import io.servicecomb.transport.rest.vertx.accesslog.element.impl.CookieElement;
import io.servicecomb.transport.rest.vertx.accesslog.parser.AccessLogElementExtraction;

public class CookieElementMatcherTest {

  private static final CookieElementMatcher MATCHER = new CookieElementMatcher();

  @Test
  public void extractElementPlaceholder() {
    List<AccessLogElementExtraction> extractionList = MATCHER
        .extractElementPlaceholder("%{header0}c %h %{yyyyMMdd HH:mm:ss zzz}t %{header1}c %b%b %H %{header2}c");

    assertEquals(3, extractionList.size());
    assertEquals(0, extractionList.get(0).getStart());
    assertEquals(11, extractionList.get(0).getEnd());
    assertEquals(41, extractionList.get(1).getStart());
    assertEquals(52, extractionList.get(1).getEnd());
    assertEquals(61, extractionList.get(2).getStart());
    assertEquals(72, extractionList.get(2).getEnd());

    assertEquals(CookieElement.class, extractionList.get(0).getAccessLogElement().getClass());
    assertEquals(CookieElement.class, extractionList.get(1).getAccessLogElement().getClass());
    assertEquals(CookieElement.class, extractionList.get(2).getAccessLogElement().getClass());

    assertEquals("header0",
        ((CookieElement) (extractionList.get(0).getAccessLogElement())).getIdentifier());
    assertEquals("header1",
        ((CookieElement) (extractionList.get(1).getAccessLogElement())).getIdentifier());
    assertEquals("header2",
        ((CookieElement) (extractionList.get(2).getAccessLogElement())).getIdentifier());
  }

  @Test
  public void extractElementPlaceholderOnNotMatched() {
    List<AccessLogElementExtraction> extractionList = MATCHER
        .extractElementPlaceholder("%{header0}i %h %{yyyyMMdd HH:mm:ss zzz}t %{header1}i %b%b %H %{header2}i");

    assertEquals(0, extractionList.size());
  }
}