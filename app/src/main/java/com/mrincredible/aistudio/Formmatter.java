package com.mrincredible.aistudio;

import android.text.Html;
import android.text.Spanned;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

public class Formmatter {
    public static Spanned fromMarkdown(String markdownText) {
        Parser parser = Parser.builder().build();
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        Node document = parser.parse(markdownText);
        String html = renderer.render(document);
        return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY);
    }
}