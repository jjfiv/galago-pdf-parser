package edu.umass.cs.ciir.galagopdfparser;

import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.util.PDFTextStripper;
import org.lemurproject.galago.core.parse.Document;
import org.lemurproject.galago.core.parse.DocumentStreamParser;
import org.lemurproject.galago.core.types.DocumentSplit;
import org.lemurproject.galago.utility.Parameters;
import org.lemurproject.galago.utility.StreamCreator;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * @author jfoley
 */
public class GalagoPDFParser extends DocumentStreamParser {
  private Parameters doc;

  public GalagoPDFParser(DocumentSplit split, Parameters p) throws IOException {
    super(split, p);
    this.doc = extract(DocumentStreamParser.getBufferedInputStream(split));
  }

  @Override
  public Document nextDocument() throws IOException {
    if(doc != null) {
      Document result = new Document();
      for (String k : doc.keySet()) {
        if(Objects.equals(k, "text")) continue;
        result.metadata.put(k, doc.getAsString(k));
      }
      result.text = doc.getString("text");
      doc = null;
      return result;
    }
    return null;
  }

  @Override
  public void close() throws IOException {
    doc = null;
  }

  public static Parameters extract(String path) throws IOException {
    return extract(StreamCreator.openInputStream(path));
  }

  public static Parameters extract(InputStream data) throws IOException {
    Parameters meta = Parameters.create();
    PDFParser parser = new PDFParser(data);
    parser.parse();
    try (PDDocument doc = parser.getPDDocument()) {
      PDDocumentInformation info = doc.getDocumentInformation();
      meta.putIfNotNull("title", info.getTitle());
      meta.putIfNotNull("author", info.getAuthor());
      meta.putIfNotNull("creator", info.getCreator());
      meta.putIfNotNull("producer", info.getProducer());

      //meta.putIfNotNull("creation-date", info.getCreationDate());
      //meta.putIfNotNull("modified-date", info.getModificationDate());
      //meta.putIfNotNull("trapped", info.getTrapped());

      meta.putIfNotNull("page-count", doc.getNumberOfPages());
      meta.putIfNotNull("subject", info.getSubject());
      meta.putIfNotNull("keywords", info.getKeywords());

      //PDFTextStripper stripper = new PDFText2HTML("UTF-8");
      PDFTextStripper stripper = new PDFTextStripper();
      stripper.setPageStart("\n<page>\n");
      stripper.setPageEnd("\n</page>\n");

      stripper.setLineSeparator("\n");
      stripper.setParagraphStart("\n<p>\n");
      stripper.setParagraphEnd("\n</p>\n");

      String text = stripper.getText(doc);
      System.out.println(text);
      meta.putIfNotNull("text", text);
    }

    return meta;
  }

  // Try it on an example file.
  public static void main(String[] args) throws IOException {
    System.out.println(extract(args[0]).toPrettyString());
  }
}

