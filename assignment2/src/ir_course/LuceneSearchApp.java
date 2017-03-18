/*
 * Skeleton class for the Lucene search program implementation - By Jouni and Esko 
 * 
 * Created on 2011-12-21 * Jouni Tuominen <jouni.tuominen@aalto.fi>
 * 
 * Modified on 2015-30-12 * Esko Ikkala <esko.ikkala@aalto.fi>
 * 
 * Modified on 2015-27-02 * Kunal Ghosh <kunal.ghosh@aalto.fi>
 * - Added Implementations for Methods Index(), Search()
 * - Added Methods GetTermQueryFromList() and toISOEpochDay()
 *
 */
package ir_course;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

/**
 * Main Class for the LuceneSearch Application, responsible for Indexing,
 * Searching etc.
 */
public class LuceneSearchApp {
	// TODO : remove static qualifiers, each of these members should be object
	// specific
	// Lucene index reader
	private IndexReader reader;
	// Index searcher
	private IndexSearcher searcher;
	// Index Writer Config
	private IndexWriterConfig iwc;
	// Index Writer
	private IndexWriter writer;
	// Index Directory
	private Directory dir;
	// Analyzer
	private Analyzer analyzer;
	// Stores all the configuration related to a task
	private Configuration config;
	private static final Integer RECOMMENDER_SYSTEM_TASK = 2;

	/**
	 * @param config
	 *            The configuration object to set the Stemmer, stop word
	 *            processing status and the Similarity used.
	 * @throws IOException
	 *             If the index writer couldn't open the Index Directory.
	 */
	public LuceneSearchApp(Configuration config) throws IOException {
		this.config = config;
		analyzer = config.getAnalyzer();

		iwc = new IndexWriterConfig(analyzer);
		iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);

		dir = new RAMDirectory();
		writer = new IndexWriter(dir, iwc);
	}

	/**
	 * Prints out the configuration object
	 */
	public void printConfig() {
		System.out.println(config);
	}

	/**
	 * Searches the index
	 * 
	 * @param queryString
	 *            The Query string to process.
	 * @param maxHits
	 *            The maximum number of search results to return.
	 * @return Search results as a List of DocumentInCollection objects.
	 */
	public List<DocumentInCollection> search(String queryString, int maxHits) {
		List<DocumentInCollection> results = new LinkedList<DocumentInCollection>();
		try {
			QueryParser parser = new QueryParser(Constants.ABSTRACT_TEXT, analyzer);
			Query query = parser.parse(queryString);
			System.out.println(query);
			TopDocs hits = searcher.search(query, maxHits);
			System.out.println("Search Hits :" + hits.totalHits);
			ScoreDoc[] scoreDocs = hits.scoreDocs;
			for (int n = 0; n < scoreDocs.length; ++n) {
				int docid = scoreDocs[n].doc;
				Document doc = searcher.doc(docid);
				results.add(new DocumentInCollection(doc.get(Constants.TITLE), doc.get(Constants.ABSTRACT_TEXT),
						RECOMMENDER_SYSTEM_TASK, queryString, Boolean.parseBoolean(doc.get(Constants.RELEVANCE))));
			}
			reader.close();

		} catch (Exception e) {
			System.out.println("Error in search" + e);
		}

		return results;
	}

	/**
	 * Convenience method to print out all elements in a collection of
	 * DocumentInCollection objects.
	 * 
	 * @param results
	 *            List of DocumentInCollection objects.
	 */
	public void printResults(List<DocumentInCollection> results) {
		printResults(results, results.size());
	}

	/**
	 * Prints first k the elements in a list of DocumentInCollection objects.
	 * 
	 * @param results
	 *            List of DocumentInCollection objects.
	 * @param topK
	 *            Integer "K" representing the first "K" elements in result,
	 *            which are to be printed.
	 */
	public void printResults(List<DocumentInCollection> results, int topK) {
		if (results.size() > 0) {
			for (int i = 0; i < Math.min(topK, results.size()); i++)
				System.out.println(" " + (i + 1) + ". [" + (results.get(i).isRelevant() ? 1 : 0) + "] ."
						+ results.get(i).getTitle());
		} else
			System.out.println(" no results");
	}

	/**
	 * Indexes a list of DocumentInCollection objects.
	 * 
	 * @param docs
	 *            list of DocumentInCollection objects.
	 * @throws IOException
	 *             If the writer couldn't add Lucene Document object, the
	 *             writer.close() failed or the reader couldn't open the Index
	 *             Directory.
	 */
	public void index(List<DocumentInCollection> docs) throws IOException {
		for (DocumentInCollection doc : docs) {
			// Each RSS Feed Document goes into a luceneDocument
			Document luceneDoc = new Document();
			luceneDoc.add(new TextField(Constants.ABSTRACT_TEXT, doc.getAbstractText(), Field.Store.YES));
			luceneDoc.add(new TextField(Constants.TITLE, doc.getTitle(), Field.Store.YES));
			luceneDoc.add(new TextField(Constants.RELEVANCE, Boolean.toString(doc.isRelevant()), Field.Store.YES));
			// if required can add query and isrelevant
			// Write the lucene document to the Index
			writer.addDocument(luceneDoc);
		}
		// Closing the Index is Important
		writer.close();
		// Open the directory and create the searcher which will be used in the
		// search method.
		reader = DirectoryReader.open(dir);
		searcher = new IndexSearcher(reader);
		searcher.setSimilarity(config.getSimilarity());
	}

	/**
	 * Method to store the list of averagePrecision values for each
	 * configuration, the mean of this collection would be reported as the MAP
	 * (or Mean Average Precision)
	 * 
	 * @param mapByConfiguration
	 *            Map with "Configuration Used" as key and a list of
	 *            AveragePrecision Values per query as the value.
	 * @param Key
	 *            The string representing the "Configuration Used" (just
	 *            config.toString())
	 * @param data
	 *            The Average precision value to be inserted into
	 *            mapByConfiguration with "Configuration Used" as the "Key"
	 *            parameter.
	 */
	private void addToMapByConfiguration(Map<String, List<Double>> mapByConfiguration, String Key, Double data) {
		if (!mapByConfiguration.containsKey(config.toString())) {
			mapByConfiguration.put(config.toString(), new ArrayList<Double>());
		}
		mapByConfiguration.get(config.toString()).add(data);
	}

	/**
	 * Method used to collect the 11-point precision recall values over all
	 * queries as a concatenated list as the value in a Map with a string
	 * representing "Configuration Used" as the "Key".
	 * 
	 * @param avg11ptPRByConfig
	 *            Map with "Configuration Used" as key and a list of
	 *            concatenated 11-point precision recall values per query as
	 *            value.
	 * @param Key
	 *            String representing the "Configuration Used"
	 * @param elevenPointPR
	 *            List of Doubles representing the 11-point Precision Recall
	 *            values corresponding to a query.
	 */
	private void addToAvg11ptPRByConfig(Map<String, List<Double>> avg11ptPRByConfig, String Key,
			List<Double> elevenPointPR) {
		if (!avg11ptPRByConfig.containsKey(Key)) {
			avg11ptPRByConfig.put(Key, new ArrayList<Double>());
		}
		avg11ptPRByConfig.get(Key).addAll(elevenPointPR);
	}

	public static void main(String[] args) throws IOException {
		if (args.length > 0) {
			DocumentCollectionParser parser = new DocumentCollectionParser();
			parser.parse(args[0]);
			List<DocumentInCollection> OriginalDocSet = parser.getDocuments();

			// filter the docs
			DocumentCollectionProcessor docProcessor = new DocumentCollectionProcessor(OriginalDocSet,
					RECOMMENDER_SYSTEM_TASK);
			List<DocumentInCollection> docs = docProcessor.getFilteredDocuments();

			List<String> queryStrings = new ArrayList<String>();
			queryStrings.add("information retrieval system recommendation");
			queryStrings.add("recommender systems information retrieval recommendation");
			queryStrings.add("systems collaborative filtering recommendation");
			queryStrings.add("systems recommendation systems classification");

			List<Configuration> configurations = new ArrayList<Configuration>();
			configurations
					.add(new Configuration(Constants.REMOVE_STOP_WORDS, Constants.ENGLISH_STEMMER, Constants.TFIDF));
			configurations
					.add(new Configuration(Constants.REMOVE_STOP_WORDS, Constants.ENG_MIN_STEMMER, Constants.TFIDF));
			configurations
					.add(new Configuration(Constants.REMOVE_STOP_WORDS, Constants.ENGLISH_STEMMER, Constants.BM25));
			configurations
					.add(new Configuration(Constants.REMOVE_STOP_WORDS, Constants.ENG_MIN_STEMMER, Constants.BM25));
			configurations
					.add(new Configuration(Constants.NO_REMOVE_STOP_WORDS, Constants.ENGLISH_STEMMER, Constants.TFIDF));
			configurations
					.add(new Configuration(Constants.NO_REMOVE_STOP_WORDS, Constants.ENG_MIN_STEMMER, Constants.TFIDF));
			configurations
					.add(new Configuration(Constants.NO_REMOVE_STOP_WORDS, Constants.ENGLISH_STEMMER, Constants.BM25));
			configurations
					.add(new Configuration(Constants.NO_REMOVE_STOP_WORDS, Constants.ENG_MIN_STEMMER, Constants.BM25));

			// Aggeregate data for average 11 point precision recall
			Map<String, List<Double>> avg11ptPRByConfig = new HashMap<String, List<Double>>();
			// Mean average precision per query
			Map<String, List<Double>> mapByConfiguration = new HashMap<String, List<Double>>();

			for (String queryString : queryStrings) {
				System.out.println("----------------------------------------------------------------------");
				System.out.println("Processing Query : " + queryString);
				System.out.println("----------------------------------------------------------------------");
				for (Configuration config : configurations) {
					System.out.println("-----------------------------------");
					LuceneSearchApp engine = new LuceneSearchApp(config);
					engine.index(docs);
					engine.printConfig();

					List<DocumentInCollection> searchResults = engine.search(queryString,
							docProcessor.getTotalDocCount());
					SearchResultStats stats = docProcessor.getRankedSearchResultStats(searchResults);
					System.out.println(stats);
					System.out.println("Printing top 10 results:");
					engine.printResults(searchResults, 10);

					engine.addToMapByConfiguration(mapByConfiguration, config.toString(),
							// Use the below method to get Mean Average
							// Precision (MAP) for top K relevant documents.
							// stats.getAveragePrecisionAtK(20));
							stats.getAverage_precision());
					engine.addToAvg11ptPRByConfig(avg11ptPRByConfig, config.toString(), stats.getElevenPointPR());
				}
			}
			System.out.println("----------------------------------------------------------------------");
			System.out.println("Mean Average Precision:");
			System.out.println("----------------------------------------------------------------------");
			// Calculate and Print the Mean Average Precision.
			mapByConfiguration.forEach((k,
					v) -> System.out.println("MAP = "
							+ v.stream().collect(Collectors.summarizingDouble(Double::doubleValue)).getAverage()
							+ " , for Config : " + k));
			System.out.println("----------------------------------------------------------------------");
			System.out.println("Averaged 11 Point Precision Recall Values:");
			System.out.println("----------------------------------------------------------------------");
			for (Map.Entry<String, List<Double>> entry : avg11ptPRByConfig.entrySet()) {
				// 11 denotes the number of elements in the 11 point precision
				// recall values
				System.out.print(entry.getKey() + " --> ");
				double[] sum = new double[11];
				for (int idx = 0; idx < entry.getValue().size(); idx++) {
					sum[idx % 11] += entry.getValue().get(idx);
				}
				System.out.print("[");
				for (Double val : sum) {
					System.out.print(val / queryStrings.size());
					System.out.print(", ");
				}
				System.out.println("]");
			}
		} else
			System.out.println("ERROR: File path not found.");
	}
}
