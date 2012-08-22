package com.hascode.tutorial;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.facet.index.CategoryDocumentBuilder;
import org.apache.lucene.facet.search.FacetsCollector;
import org.apache.lucene.facet.search.params.CountFacetRequest;
import org.apache.lucene.facet.search.params.FacetSearchParams;
import org.apache.lucene.facet.search.results.FacetResult;
import org.apache.lucene.facet.search.results.FacetResultNode;
import org.apache.lucene.facet.taxonomy.CategoryPath;
import org.apache.lucene.facet.taxonomy.TaxonomyReader;
import org.apache.lucene.facet.taxonomy.TaxonomyWriter;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MultiCollector;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class FacetingExample {
	private static final String INDEX = "target/facet/index";
	private static final String INDEX_TAXO = "target/facet/taxo";

	public static void main(final String[] args) throws IOException {
		Directory dir = FSDirectory.open(new File(INDEX));
		Directory taxoDir = FSDirectory.open(new File(INDEX_TAXO));
		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_40);
		IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_40,
				analyzer);
		iwc.setOpenMode(OpenMode.CREATE);
		IndexWriter writer = new IndexWriter(dir, iwc);

		TaxonomyWriter taxoWriter = new DirectoryTaxonomyWriter(taxoDir,
				OpenMode.CREATE_OR_APPEND);
		Document doc = new Document();
		doc.add(new StringField("title", "Tom Sawyer", Store.YES));
		List<CategoryPath> categories = new ArrayList<CategoryPath>();
		categories.add(new CategoryPath("author", "Mark Twain"));
		categories.add(new CategoryPath("year", "2010"));
		CategoryDocumentBuilder categoryDocBuilder = new CategoryDocumentBuilder(
				taxoWriter);
		categoryDocBuilder.setCategoryPaths(categories);
		categoryDocBuilder.build(doc);
		writer.addDocument(doc);
		writer.close();
		taxoWriter.close();

		IndexReader indexReader = DirectoryReader.open(dir);
		IndexSearcher searcher = new IndexSearcher(indexReader);
		TaxonomyReader taxoReader = new DirectoryTaxonomyReader(taxoDir);
		Query q = new TermQuery(new Term("title", "Tom Sawyer"));
		TopScoreDocCollector tdc = TopScoreDocCollector.create(10, true);
		FacetSearchParams facetSearchParams = new FacetSearchParams();
		facetSearchParams.addFacetRequest(new CountFacetRequest(
				new CategoryPath("author"), 10));
		FacetsCollector facetsCollector = new FacetsCollector(
				facetSearchParams, indexReader, taxoReader);
		searcher.search(q, MultiCollector.wrap(tdc, facetsCollector));
		List<FacetResult> res = facetsCollector.getFacetResults();
		System.out.println("results: " + res.size());
		for (final FacetResult r : res) {
			System.out.println("x " + r.getFacetResultNode().getLabel());
			for (FacetResultNode n : r.getFacetResultNode().getSubResults()) {
				System.out.println("\t" + n.getLabel() + ": " + n.getValue());
			}
		}
	}
}
