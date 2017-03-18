/*
 * Created on 23 April 2016 * Kunal Ghosh <kunal.ghosh@aalto.fi>, 
 * 							  Shishir Bhattarai <shishir.bhattarai@aalto.fi>, 
 * 							  Jussi Ojala <jussi.k.ojala@aalto.fi>,
 * 							  Preeti Lahoti <preethi.lahoti@aalto.fi>
 *
 */

package ir_course;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to process a List of DocumentInCollection objects.
 */
public class DocumentCollectionProcessor {
	// You should not be using this object for any calculation
	// Always use the filteredDocuments
	private List<DocumentInCollection> __originalDocuments;
	private List<DocumentInCollection> filteredDocuments;
	private Integer searchTask;
	// Stats - Always related to the filteredDocuments
	// If searchTask is not defined then
	// filteredDocuments == __originalDocuments
	private Integer docsInSearchTask = 0;
	private Integer relevantDocCount = 0;

	/**
	 * If searchTask number is provided then we filter the documents to have
	 * only the documents with only that particular search task. And Do further
	 * processing on that filtered document collection.
	 * 
	 * @param documents
	 *            List of DocumentInCollection objects.
	 * @param searchTask
	 *            Search Task Number (2 in our case)
	 */
	public DocumentCollectionProcessor(List<DocumentInCollection> documents, Integer searchTask) {
		this.__originalDocuments = documents;
		this.searchTask = searchTask;
		this.filteredDocuments = getFilteredDocuments(__originalDocuments, searchTask);
		updateCommonStats();
	}

	/**
	 * If searchTask number is not provided perform the processing and all
	 * calculations on the entire document collection.
	 * 
	 * @param documents
	 *            List of DocumentInCollection objects.
	 */
	public DocumentCollectionProcessor(List<DocumentInCollection> documents) {
		this.__originalDocuments = documents;
		// No searchTask defined, so filtered
		// documents == original documents collection.
		this.filteredDocuments = this.__originalDocuments;
		updateCommonStats();
	}

	/**
	 * @return The filteredDocuments
	 */
	public List<DocumentInCollection> getFilteredDocuments() {
		return filteredDocuments;
	}

	/**
	 * @return The docsInSearchTask
	 */
	public Integer getTotalDocCount() {
		return docsInSearchTask;
	}

	/**
	 * @return The relevantDocCount
	 */
	public Integer getRelevantDocCount() {
		return relevantDocCount;
	}

	/**
	 * Generates the : \n1. Number of documents after filtering \n2. The number
	 * of relevant documents in the filtered set.
	 */
	private void updateCommonStats() {
		this.docsInSearchTask = filteredDocuments.size();
		this.relevantDocCount = getRelevantDocs(filteredDocuments);
	}

	/**
	 * @param docs
	 *            List of DocumentInCollection objects.
	 * @return The count of relevant documents in the collection
	 */
	private Integer getRelevantDocs(List<DocumentInCollection> docs) {
		Integer relevantDocs = 0;
		for (DocumentInCollection doc : docs) {
			if (doc.isRelevant()) {
				relevantDocs++;
			}
		}
		return relevantDocs;
	}

	/**
	 * @param originalSet
	 *            list of DocumentInCollection objects in the un-filtered
	 *            Corpus)
	 * @param searchTask
	 *            Search Task Number (2 in our case).
	 * @return List of DocumentInCollection objects from the "originalSet"
	 *         matching the search task number.
	 */
	private List<DocumentInCollection> getFilteredDocuments(List<DocumentInCollection> originalSet,
			Integer searchTask) {
		List<DocumentInCollection> filteredDocuments = new ArrayList<DocumentInCollection>();
		Integer docsInSearchTask = 0;
		for (DocumentInCollection doc : originalSet) {
			if (doc.getSearchTaskNumber() == searchTask) {
				filteredDocuments.add(doc);
				docsInSearchTask++;
			}
		}
		return filteredDocuments;
	}

	/**
	 * Generates statistics for the a list of DocumentInCollection objects
	 * (ideally the search results)
	 * 
	 * @param searchResults
	 *            List of DocumentInCollection objects.
	 * @return SearchResultStats object.
	 */
	public SearchResultStats getRankedSearchResultStats(List<DocumentInCollection> searchResults) {
		// NOTE :
		// 1. The 11 point precision recall makes sense only if the
		// searchResults are ranked.
		// 2. The searchResults are assumed to have originated from the
		// filteredDocuments

		SearchResultStats stats = new SearchResultStats(searchResults, relevantDocCount);

		Integer relevantDocsInSearchCount = getRelevantDocs(searchResults);
		Integer searchResultCount = searchResults.size();

		// Recall : What fraction of the relevant documents in the collection
		// were returned by the system?
		stats.setRecall(relevantDocsInSearchCount / relevantDocCount.doubleValue());

		// Precision : What fraction of the returned results are relevant to the
		// information need?
		stats.setPrecision(relevantDocsInSearchCount / searchResultCount.doubleValue());

		// Get the 11 point precision recall values
		try {
			stats.getElevenPointPR();
		} catch (IllegalArgumentException e) {
			System.out.println(e);
		}
		return stats;
	}
}
