package com.mrincredible.aistudio;

import android.text.Html;
import android.text.Spanned;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

public class Formmatter {

    public static Spanned fromMarkdown(String markdownText) {
        // Create a parser and a renderer for CommonMark (Markdown)
        Parser parser = Parser.builder().build();
        HtmlRenderer renderer = HtmlRenderer.builder().build();

        // Parse the Markdown text into an abstract syntax tree (AST)
        Node document = parser.parse(markdownText);

        // Render the AST as HTML
        String html = renderer.render(document);

        // Convert the HTML to a Spanned object for display in Android
        return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY);
    }
}