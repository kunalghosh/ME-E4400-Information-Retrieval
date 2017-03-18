/*
 * Created on 23 April 2016 * Kunal Ghosh <kunal.ghosh@aalto.fi>, 
 * 							  Shishir Bhattarai <shishir.bhattarai@aalto.fi>, 
 * 							  Jussi Ojala <jussi.k.ojala@aalto.fi>,
 * 							  Preeti Lahoti <preethi.lahoti@aalto.fi>
 *
 */

package ir_course;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Class which acts as a placeholder and also calculates various IR statistics
 * for a List of DocumentInCollection objects.
 */
public class SearchResultStats {
	// Precision of the search results
	private Double precision = 0.0;
	// Recall of the search results
	private Double recall = 0.0;
	// The 11 point interpolated precision recall values
	private List<Double> elevenPointPR = new ArrayList<Double>();
	// Average Precision
	private Double average_precision;
	// precision when the kth relevant document is retrieved
	private List<Double> precisionAtK = new ArrayList<Double>();
	// recall when the kth relevant document is retrieved
	private List<Double> recallPercentAtK = new ArrayList<Double>();
	// Ordered list containing whether a document is relevant or not
	private List<Boolean> relevanceOfResults = new ArrayList<Boolean>();
	// Number of relevant documents in the corpus
	private Integer relevantsInCorpus = 0;

	/**
	 * Constructor to Initialize the class with default statistics like
	 * precision, recall and eleventPointPR. This would be useful when the user
	 * just wants to save this statistics and pass them around.
	 * 
	 * @param precision
	 *            The precision value.
	 * @param recall
	 *            The recall value.
	 * @param elevenPointPR
	 *            List of eleven Precision Recall Values.
	 */
	public SearchResultStats(Double precision, Double recall, List<Double> elevenPointPR) {
		this.precision = precision;
		this.recall = recall;
		this.elevenPointPR = elevenPointPR;
	}

	/**
	 * Default constructor
	 */
	public SearchResultStats() {
		// If you just want to use this class as a container
		// this default constructor allows you to just set the
		// corresponding values after creating the object of SearchResultStats
	}

	/**
	 * Constructor when the user wants to generate the statistics for a given
	 * list of DocumentInCollection objects
	 * 
	 * @param searchResults
	 *            List of DocumentInCollection objects (ideally the search
	 *            results).
	 * @param relevantsInCorpus
	 *            The count of relevant documents in the original "filtered"
	 *            document collection.
	 */
	public SearchResultStats(List<DocumentInCollection> searchResults, Integer relevantsInCorpus) {
		this.relevanceOfResults = searchResults.stream().map(item -> item.isRelevant()).collect(Collectors.toList());
		this.relevantsInCorpus = relevantsInCorpus;
		calculatePrecisionsAtK();
		calculateAveragePrecision();
		getElevenPointPR();
	}

	/**
	 * @return Get the Precision
	 */
	public Double getPrecision() {
		return precision;
	}

	/**
	 * @param precision
	 *            Set the precision.
	 */
	public void setPrecision(Double precision) {
		this.precision = precision;
	}

	/**
	 * @return Get the recall
	 */
	public Double getRecall() {
		return recall;
	}

	/**
	 * @param recall
	 *            Set the recall
	 */
	public void setRecall(Double recall) {
		this.recall = recall;
	}

	/**
	 * @return Get the 11 point precision recall values.
	 */
	public List<Double> getElevenPointPR() {
		try {
			elevenPointPR = calculateElevenPointPR();
		} catch (Exception e) {
			System.out.println(e);
		}
		return elevenPointPR;
	}

	/**
	 * @param elevenPointPR
	 *            Initialize (set) with a list of 11-point precision recall
	 *            values.
	 */
	public void setElevenPointPR(List<Double> elevenPointPR) {
		this.elevenPointPR = elevenPointPR;
	}

	/**
	 * @return Calculate the precision recall values.
	 */
	private List<Double> calculateElevenPointPR() {
		Double[] ElevenPointPR = new Double[11];
		Integer relevantsInsearchResult = (int) relevanceOfResults.stream().filter(p -> p == true).count();
		Double recallInPercent = (relevantsInsearchResult * 100) / relevantsInCorpus.doubleValue();
		if (relevantsInsearchResult != relevantsInCorpus) {
			throw new IllegalArgumentException(
					"To Calculate 11 Point Precision Recall Curve. You must have 100% Recall, Current = "
							+ String.valueOf(recallInPercent.intValue())
							+ "% \nSuggestion : Try changing your query term till you get 100% recall.");
		} else {
			Double cumRelevants = 0.0; // sum of relevants so far
			Integer cumResults = 1; // sum of results processed so far
			Double currentRecallPercent = 0.0;
			Integer recall10thsPlace = 0;

			cumResults = 0;
			cumRelevants = 0.0; // sum of relevants so far
			for (Boolean relevance : relevanceOfResults) {
				if (relevance) {
					cumRelevants++;

					currentRecallPercent = cumRelevants / relevantsInCorpus;
					// at each 10% threshold we will update the precision
					recall10thsPlace = (int) (currentRecallPercent * 10);
					// * 10 because we just want the 10ths place for indexing
					// into our 11pointPR array.

					ElevenPointPR[recall10thsPlace] = precisionAtK
							.subList(cumRelevants.intValue() - 1, precisionAtK.size()).stream()
							.collect(Collectors.summarizingDouble(Double::doubleValue)).getMax();

				}
				cumResults++;
			}
		}
		return Arrays.asList(ElevenPointPR);
	}

	/**
	 * Calculates the Average Precision value over all recall levels.
	 */
	private void calculateAveragePrecision() {
		average_precision = precisionAtK.stream().collect(Collectors.summarizingDouble(Double::doubleValue))
				.getAverage();
	}

	/**
	 * Calculates the precision at all recall levels.
	 */
	private void calculatePrecisionsAtK() {
		Double cumRelevants = 0.0; // sum of relevants so far
		Integer cumResults = 1; // sum of results processed so far
		for (Boolean relevance : relevanceOfResults) {

			if (relevance) {
				cumRelevants++;
				precisionAtK.add(cumRelevants / cumResults.doubleValue());
				recallPercentAtK.add(cumRelevants / relevantsInCorpus);
			}
			cumResults++;
		}
	}

	/**
	 * Gets the average precision for first K relevant documents.
	 * 
	 * @param k
	 *            Integer representing the first K relevant documents.
	 * @return Average of the precisions calculated when each relevant document
	 *         is encountered.
	 */
	public Double getAveragePrecisionAtK(int k) {
		// returns average precision at top k relevant documents
		return precisionAtK.subList(0, Math.min(k, precisionAtK.size())).stream()
				.collect(Collectors.summarizingDouble(Double::doubleValue)).getAverage();
	}

	/**
	 * @return the average_precision
	 */
	public Double getAverage_precision() {

		return average_precision;
	}

	/**
	 * @param average_precision
	 *            The average_precision to set
	 */
	public void setAverage_precision(Double average_precision) {
		this.average_precision = average_precision;
	}

	/**
	 * @return The precisionAtK
	 */
	public List<Double> getPrecisionAtK() {
		return precisionAtK;
	}

	/**
	 * @param precisionAtK
	 *            The precisionAtK to set
	 */
	public void setPrecisionAtK(List<Double> precisionAtK) {
		this.precisionAtK = precisionAtK;
	}

	/**
	 * @return The relevanceOfResults
	 */
	public List<Boolean> getRelevanceOfResults() {
		return relevanceOfResults;
	}

	/**
	 * @param relevanceOfResults
	 *            The relevanceOfResults to set
	 */
	public void setRelevanceOfResults(List<Boolean> relevanceOfResults) {
		this.relevanceOfResults = relevanceOfResults;
	}

	/**
	 * @return The recallPercentAtK
	 */
	public List<Double> getRecallPercentAtK() {
		return recallPercentAtK;
	}

	/**
	 * @param recallPercentAtK
	 *            The recallPercentAtK to set
	 */
	public void setRecallPercentAtK(List<Double> recallPercentAtK) {
		this.recallPercentAtK = recallPercentAtK;
	}

	@Override
	public String toString() {
		return "[Search Stats: Precision = " + precision.toString() + ", Recall = " + recall.toString()
				+ ", Average Precision = " + average_precision.toString() + ", 11 Point Precision Recall = ["
				+ elevenPointPR.stream().map(item -> item.toString()).collect(Collectors.joining(", ")) + "]";
	}
}
