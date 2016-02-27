/*
 * Skeleton class for the Lucene search program implementation
 *
 * Created on 2011-12-21 * Jouni Tuominen <jouni.tuominen@aalto.fi>
 * 
 * Modified on 2015-30-12 * Esko Ikkala <esko.ikkala@aalto.fi>
 * 
 */
package ir_course;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

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
  // Analyzer
  private static Analyzer analyzer;
  // Title String
  public static final String TITLE = "title";
  // Description String
  public static final String DESC = "description";
  // Publish Date string
  public static final String PUB_DATE = "pubDate";
  // Search results
  private ScoreDoc[] hits;
  // Date format being used
  private static SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd");
  // Formatter with TimeZone
  private static SimpleDateFormat tzDt = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");


  public LuceneSearchApp() {
    tzDt.setTimeZone(TimeZone.getTimeZone("UTC"));
  }

  public void index(IndexWriter writer, List<RssFeedDocument> docs) throws IOException {

    // implement the Lucene indexing here
    for (RssFeedDocument doc : docs) {
      Document luceneDoc = new Document();
      luceneDoc.add(new TextField(TITLE, doc.getTitle(), Field.Store.YES));
      luceneDoc.add(new TextField(DESC, new StringReader(doc.getDescription())));
      // luceneDoc.add(new LongField(PUB_DATE, doc.getPubDate().getTime(), Field.Store.NO));
      // luceneDoc.add(new LongField(PUB_DATE, dt.parse(dt.format(doc.getPubDate())).getTime(),
      System.out
          .println(
              "Old : " + doc.getPubDate().toInstant().toString() + " +2 : "
                  + doc.getPubDate().toInstant().plus(2, ChronoUnit.HOURS).toString() + " Trunc : "
                  + doc.getPubDate().toInstant().truncatedTo(ChronoUnit.DAYS) + " Final : "
                  + doc.getPubDate().toInstant().plus(2, ChronoUnit.HOURS)
                      .truncatedTo(ChronoUnit.DAYS)
                  + " - " + LocalDate.from(doc.getPubDate().toInstant().atZone(ZoneId.of("UTC")))
                      .toEpochDay()
                  + " Title : " + doc.getTitle());
                  // luceneDoc.add(new LongField(PUB_DATE, doc.getPubDate().toInstant().plus(2,
                  // ChronoUnit.HOURS)
                  // .truncatedTo(ChronoUnit.DAYS).toEpochMilli(), Field.Store.NO));

      // luceneDoc.add(new LongField(PUB_DATE,
      // doc.getPubDate().toInstant().truncatedTo(ChronoUnit.DAYS).toEpochMilli(),
      // Field.Store.NO));
      luceneDoc.add(new LongField(PUB_DATE,
          LocalDate.from(doc.getPubDate().toInstant().atZone(ZoneId.of("UTC"))).toEpochDay(),
          Field.Store.NO));

      writer.addDocument(luceneDoc);
    }
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
    // QueryBuilder builder = new QueryBuilder(analyzer);
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

    boolean includeLower = true;
    boolean includeUpper = true;
    Long lowerTerm = null;
    Long upperTerm = null;
    if (startDate != null) {
      // lowerTerm = new
      // BytesRef(Objects.toString(LocalDate.parse(startDate,DateTimeFormatter.ISO_LOCAL_DATE).toEpochDay()));
      lowerTerm = toFinnishDate(startDate);
    }
    if (endDate != null) {

      // ZonedDateTime zonedDateTime = ZonedDateTime.of(localDateTime, ZoneId.of("Helsinki"));
      // Instant instant = zonedDateTime.toInstant();
      // Long millis = instant.toEpochMilli();
      // upperTerm = new BytesRef(toFinnishDate(endDate));;
      upperTerm = toFinnishDate(endDate);
      // DateTimeFormatter format =
      // LocalDate.parse(endDate,DateTimeFormatter.ISO_LOCAL_DATE.withZone(new
      // DateTimeZone("UTC+02:00")));
      // String epochDate = new Date(date.toString());
      // String temp = Objects.toString(epochDate);
      // upperTerm = new BytesRef(temp);
    }
    NumericRangeQuery<Long> dateQuery =
        NumericRangeQuery.newLongRange(PUB_DATE, lowerTerm, upperTerm, includeLower, includeUpper);

    boolQuery.add(dateQuery, BooleanClause.Occur.MUST);
    BooleanQuery q = boolQuery.build();
    // The search cannot have more than the number of docs in index (reader.numDocs)
    // System.out.println(q.toString());
    hits = searcher.search(q, reader.numDocs()).scoreDocs;
    for (ScoreDoc hit : hits) {
      results.add(searcher.doc(hit.doc).get(TITLE));
    }
    return results;
  }

  private Long toFinnishDate(String isoDateStr) {
    Long dateMillis = -1L;
    LocalDate localDate = LocalDate.parse(isoDateStr, DateTimeFormatter.ISO_DATE);
    try {
      Instant instant = tzDt.parse(tzDt.format(dt.parse(isoDateStr))).toInstant();
      dateMillis = instant.toEpochMilli();
      System.out.println(
          "Finnish Time : " + instant.toString() + " LocalDate : " + localDate.toEpochDay());
    } catch (ParseException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    // return dateMillis;
    return localDate.toEpochDay();
  }

  // private Long toFinnishDate(String startDate) {
  //
  // Date date = null;
  // try {
  // date = dt.parse(startDate);
  // } catch (ParseException e) {
  // // TODO Auto-generated catch block
  // e.printStackTrace();
  // }
  // Calendar cal = new GregorianCalendar();
  // cal.setTime(date);
  // // cal.add(Calendar.HOUR_OF_DAY, 2);
  // // String dateString = Objects.toString(cal.getTime().getTime());
  // Long dateString = cal.getTime().getTime();
  // return dateString;
  // }

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
      // [ADDITION] Start Lucene specific initializations
      Analyzer analyzer = new StandardAnalyzer();
      IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
      iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);

      // Assuming the directory where index is stored is the PWD always.
      indexDir = Paths.get("").toAbsolutePath().toString();

      System.out.println("Indexing in " + indexDir + " directory");
      // Directory dir = NIOFSDirectory.open(Paths.get(indexDir));
      Directory dir = new RAMDirectory();
      IndexWriter writer = new IndexWriter(dir, iwc);
      // [END ADDITION]

      RssFeedParser parser = new RssFeedParser();
      parser.parse(args[0]);
      List<RssFeedDocument> docs = parser.getDocuments();

      engine.index(writer, docs);
      writer.close();
      // [ADDITION] Creating the reader and searcher
      reader = DirectoryReader.open(dir);
      searcher = new IndexSearcher(reader);
      // [END ADDITION]

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
