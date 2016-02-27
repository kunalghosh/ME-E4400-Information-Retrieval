/*
 * Skeleton class for the Lucene search program implementation - By Jouni and Esko
 * Index and Search implementation - By Kunal
 * 
 * Created on 2011-12-21 * Jouni Tuominen <jouni.tuominen@aalto.fi>
 * 
 * Modified on 2015-30-12 * Esko Ikkala <esko.ikkala@aalto.fi>
 * 
 * Modified on 2015-27-02 * Kunal Ghosh <kunal.ghosh@aalto.fi>
 */
package ir_course;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.analysis.Analyzer;

public class LuceneSearchApp {

  // Create Lucene index in this dir
  private static String indexDir;
  // Lucene index reader
  private static IndexReader reader;
  // Index searcher
  private static IndexSearcher searcher;
  // Index Writer Config
  private static IndexWriterConfig iwc;
  // Index Writer
  private static IndexWriter writer;
  // Index Directory
  private static Directory dir;
  // Analyzer
  private static Analyzer analyzer;
  // Title String
  public static final String TITLE = "title";
  // Description String
  public static final String DESC = "description";
  // Publish Date string
  public static final String PUB_DATE = "pubDate";
  // Search result doc numbers 
  private ScoreDoc[] hits;

  public LuceneSearchApp() throws IOException {
    analyzer = new StandardAnalyzer();
    iwc = new IndexWriterConfig(analyzer);
    iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);

    // Assuming the directory where index is stored is the PWD always.
    indexDir = Paths.get("").toAbsolutePath().toString();

    System.out.println("Indexing in " + indexDir + " directory");
    // Directory dir = NIOFSDirectory.open(Paths.get(indexDir));
    dir = new RAMDirectory();
    writer = new IndexWriter(dir, iwc);
  }

  public void index(List<RssFeedDocument> docs) throws IOException {
    // implement the Lucene indexing here
    for (RssFeedDocument doc : docs) {
      Document luceneDoc = new Document();
      luceneDoc.add(new TextField(TITLE, doc.getTitle(), Field.Store.YES));
      luceneDoc.add(new TextField(DESC, new StringReader(doc.getDescription())));
      luceneDoc.add(new LongField(PUB_DATE,
          LocalDate.from(doc.getPubDate().toInstant().atZone(ZoneId.of("UTC"))).toEpochDay(),
          Field.Store.NO));
      writer.addDocument(luceneDoc);
    }
    writer.close();
    reader = DirectoryReader.open(dir);
    searcher = new IndexSearcher(reader);
  }

  private List<Query> getTermQueryFromList(List<String> terms, String field) {
    List<Query> queries = new ArrayList<Query>();
    if (terms != null) {
      for (String term : terms) {
        queries.add(new TermQuery(new Term(field, term)));
      }
    }
    return queries;
  }

  public List<String> search(List<String> inTitle, List<String> notInTitle,
      List<String> inDescription, List<String> notInDescription, String startDate, String endDate)
          throws IOException {

    printQuery(inTitle, notInTitle, inDescription, notInDescription, startDate, endDate);

    List<String> results = new LinkedList<String>();

    // implement the Lucene search here
    BooleanQuery.Builder boolQuery = new BooleanQuery.Builder();
    getTermQueryFromList(inTitle, TITLE).forEach((query) -> {
      boolQuery.add(query, BooleanClause.Occur.MUST);
    });
    getTermQueryFromList(notInTitle, TITLE).forEach((query) -> {
      boolQuery.add(query, BooleanClause.Occur.MUST_NOT);
    });
    getTermQueryFromList(inDescription, DESC).forEach((query) -> {
      boolQuery.add(query, BooleanClause.Occur.MUST);
    });
    getTermQueryFromList(notInDescription, DESC).forEach((query) -> {
      boolQuery.add(query, BooleanClause.Occur.MUST_NOT);
    });
    
    // Always include the lower and upper range in date search.
    boolean includeLower = true;
    boolean includeUpper = true;
    Long lowerTerm = null;
    Long upperTerm = null;
    if (startDate != null) {
      lowerTerm = toISOEpochDay(startDate);
    }
    if (endDate != null) {
      upperTerm = toISOEpochDay(endDate);
    }
    NumericRangeQuery<Long> dateQuery =
        NumericRangeQuery.newLongRange(PUB_DATE, lowerTerm, upperTerm, includeLower, includeUpper);

    boolQuery.add(dateQuery, BooleanClause.Occur.MUST);
    BooleanQuery q = boolQuery.build();
    // The search cannot have more than the number of docs in index (reader.numDocs)
    hits = searcher.search(q, reader.numDocs()).scoreDocs;
    for (ScoreDoc hit : hits) {
      results.add(searcher.doc(hit.doc).get(TITLE));
    }
    return results;
  }

  private Long toISOEpochDay(String isoDateStr) {
    LocalDate localDate = LocalDate.parse(isoDateStr, DateTimeFormatter.ISO_DATE);
    return localDate.toEpochDay();
  }

  public void printQuery(List<String> inTitle, List<String> notInTitle, List<String> inDescription,
      List<String> notInDescription, String startDate, String endDate) {
    System.out.print("Search (");
    if (inTitle != null) {
      System.out.print("in title: " + inTitle);
      if (notInTitle != null || inDescription != null || notInDescription != null
          || startDate != null || endDate != null)
        System.out.print("; ");
    }
    if (notInTitle != null) {
      System.out.print("not in title: " + notInTitle);
      if (inDescription != null || notInDescription != null || startDate != null || endDate != null)
        System.out.print("; ");
    }
    if (inDescription != null) {
      System.out.print("in description: " + inDescription);
      if (notInDescription != null || startDate != null || endDate != null)
        System.out.print("; ");
    }
    if (notInDescription != null) {
      System.out.print("not in description: " + notInDescription);
      if (startDate != null || endDate != null)
        System.out.print("; ");
    }
    if (startDate != null) {
      System.out.print("startDate: " + startDate);
      if (endDate != null)
        System.out.print("; ");
    }
    if (endDate != null)
      System.out.print("endDate: " + endDate);
    System.out.println("):");
  }

  public void printResults(List<String> results) {
    if (results.size() > 0) {
      Collections.sort(results);
      for (int i = 0; i < results.size(); i++)
        System.out.println(" " + (i + 1) + ". " + results.get(i));
    } else
      System.out.println(" no results");
  }

  public static void main(String[] args) throws IOException {
    if (args.length > 0) {
      LuceneSearchApp engine = new LuceneSearchApp();

      RssFeedParser parser = new RssFeedParser();
      parser.parse(args[0]);
      List<RssFeedDocument> docs = parser.getDocuments();

      engine.index(docs);

      List<String> inTitle;
      List<String> notInTitle;
      List<String> inDescription;
      List<String> notInDescription;
      List<String> results;

      // 1) search documents with words "kim" and "korea" in the title
      inTitle = new LinkedList<String>();
      inTitle.add("kim");
      inTitle.add("korea");
      results = engine.search(inTitle, null, null, null, null, null);
      engine.printResults(results);

      // 2) search documents with word "kim" in the title and no word "korea" in the description
      inTitle = new LinkedList<String>();
      notInDescription = new LinkedList<String>();
      inTitle.add("kim");
      notInDescription.add("korea");
      results = engine.search(inTitle, null, null, notInDescription, null, null);
      engine.printResults(results);

      // 3) search documents with word "us" in the title, no word "dawn" in the title and word ""
      // and "" in the description
      inTitle = new LinkedList<String>();
      inTitle.add("us");
      notInTitle = new LinkedList<String>();
      notInTitle.add("dawn");
      inDescription = new LinkedList<String>();
      inDescription.add("american");
      inDescription.add("confession");
      results = engine.search(inTitle, notInTitle, inDescription, null, null, null);
      engine.printResults(results);

      // 4) search documents whose publication date is 2011-12-18
      results = engine.search(null, null, null, null, "2011-12-18", "2011-12-18");
      engine.printResults(results);

      // 5) search documents with word "video" in the title whose publication date is 2000-01-01 or
      // later
      inTitle = new LinkedList<String>();
      inTitle.add("video");
      results = engine.search(inTitle, null, null, null, "2000-01-01", null);
      engine.printResults(results);

      // 6) search documents with no word "canada" or "iraq" or "israel" in the description whose
      // publication date is 2011-12-18 or earlier
      notInDescription = new LinkedList<String>();
      notInDescription.add("canada");
      notInDescription.add("iraq");
      notInDescription.add("israel");
      results = engine.search(null, null, null, notInDescription, null, "2011-12-18");
      engine.printResults(results);
    } else
      System.out.println(
          "ERROR: the path of a RSS Feed file has to be passed as a command line argument.");
  }
}
