package com.sherlook.search.indexer;

import com.sherlook.search.utils.ConsoleColors;
import java.io.IOException;
import java.io.StringReader;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.springframework.stereotype.Component;

@Component
public class Stemmer {

  public String stem(String word) {
    try (Analyzer analyzer =
        new Analyzer() {
          @Override
          protected TokenStreamComponents createComponents(String fieldName) {
            WhitespaceTokenizer tokenizer = new WhitespaceTokenizer();
            TokenStream filter = new PorterStemFilter(tokenizer);
            return new TokenStreamComponents(tokenizer, filter);
          }
        }) {
      try (TokenStream tokenStream = analyzer.tokenStream(null, new StringReader(word))) {
        CharTermAttribute attr = tokenStream.addAttribute(CharTermAttribute.class);
        tokenStream.reset();
        if (tokenStream.incrementToken()) {
          String stemmed = attr.toString();
          tokenStream.end();
          return stemmed;
        }
        tokenStream.end();
      }
    } catch (IOException e) {
      ConsoleColors.printInfo("Stemmer");
      System.out.println("Error stemming word: " + word + " - " + e.getMessage());
    }

    return word;
  }
}
