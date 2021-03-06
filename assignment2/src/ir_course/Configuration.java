/*
 * Created on 23 April 2016 * Kunal Ghosh <kunal.ghosh@aalto.fi>, 
 * 							  Shishir Bhattarai <shishir.bhattarai@aalto.fi>, 
 * 							  Jussi Ojala <jussi.k.ojala@aalto.fi>,
 * 							  Preeti Lahoti <preethi.lahoti@aalto.fi>
 *
 */

package ir_course;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.en.EnglishMinimalStemFilter;
import org.apache.lucene.analysis.en.KStemFilter;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.Similarity;

/**
 * Class to represent the configuration to be used in LuceneSearchApp run. This
 * is sort of like a factory class where for e.g. setting stemmer as
 * Constants.PORTER_STEMMER would return the analyser with porter stemer on
 * calling getAnalyzer()
 *
 */
public class Configuration {

	private boolean removeStopWords = false;
	private String stemmer = "";
	private String similarity = "";
	private Similarity usedSim = null;

	/**
	 * Specify the configurations as Constants.*
	 * 
	 * @param stopWords
	 *            from {Constants.REMOVE_STOP_WORDS,
	 *            Constants.NO_REMOVE_STOP_WORDS}
	 * @param stemmer
	 *            from {Constants.PORTER_STEMMER, Constants.ENGLISH_STEMMER,
	 *            Constants.ENGLISH_MIN_STEMMER, Constants.K_STEMMER}
	 * @param similarity
	 *            from {Constants.BM25, Constants.TFIDF}
	 */
	public Configuration(String stopWords, String stemmer, String similarity) {
		if (stopWords.equals(Constants.REMOVE_STOP_WORDS)) {
			removeStopWords = true;
		}
		this.stemmer = stemmer;
		this.similarity = similarity;
	}

	/**
	 * Generates an Analyzer with the given stemming method and StopWord (Remove
	 * or Not) as specified in the Constructor.
	 * 
	 * @param StemmingMethod
	 * @param removeStopWords
	 * @return Analyzer
	 */
	private Analyzer BuildSnowballAnalyzer(String StemmingMethod, boolean removeStopWords) {
		Analyzer analyzer = new Analyzer() {
			@Override
			protected TokenStreamComponents createComponents(String fieldName) {
				final Tokenizer source = new StandardTokenizer();
				TokenStream result = new StandardFilter(source);
				result = new LowerCaseFilter(result);

				// Remove stop words using default stop words set
				if (removeStopWords) {
					result = new StopFilter(result, EnglishAnalyzer.getDefaultStopSet());
				}
				// Use the specified Stemming Method
				if (StemmingMethod == Constants.PORTER_STEMMER) {
					result = new SnowballFilter(result, new org.tartarus.snowball.ext.PorterStemmer());
				} else if (StemmingMethod == Constants.K_STEMMER) {
					result = new KStemFilter(result);
				} else if (StemmingMethod == Constants.ENG_MIN_STEMMER) {
					result = new EnglishMinimalStemFilter(result);
				} else if (StemmingMethod == Constants.ENGLISH_STEMMER) {
					result = new SnowballFilter(result, new org.tartarus.snowball.ext.EnglishStemmer());
				}
				return new TokenStreamComponents(source, result);
			}
		};
		return analyzer;
	}

	public boolean isStopWordUsed() {
		return removeStopWords;
	}

	/**
	 * @return The Lucene Similarity object as specified in the constructor
	 */
	public Similarity getSimilarity() {
		if (similarity.equals(Constants.BM25)) {
			usedSim = new BM25Similarity();
		} else if (similarity.equals(Constants.TFIDF)) {
			usedSim = new ClassicSimilarity();
		}
		System.out.println("Similarity " + usedSim);
		return usedSim;
	}

	/**
	 * @return The analyzer generated by BuildSnowballAnalyzer() as specified in
	 *         the Constructor.
	 */
	public Analyzer getAnalyzer() {
		return BuildSnowballAnalyzer(stemmer, removeStopWords);
	}

	@Override
	public String toString() {
		return "[Configuration: Removing Stop Words = " + removeStopWords + ", Stemmer = " + stemmer + ", Similarity = "
				+ similarity + "]";
	}
}