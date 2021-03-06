package com.hascode.tutorial;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.Version;

public class FacetingExample {
	private static final String INDEX = "target/facet/index";
	private static final String INDEX_TAXO = "target/facet/taxo";

	public static void main(final String[] args) throws IOException {
		Directory dir = MMapDirectory.open(new File(INDEX));
		Directory taxoDir = MMapDirectory.open(new File(INDEX_TAXO));
		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_40);
		IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_40,
				analyzer);
		iwc.setOpenMode(OpenMode.CREATE);
		IndexWriter writer = new IndexWriter(dir, iwc);
		TaxonomyWriter taxoWriter = new DirectoryTaxonomyWriter(taxoDir,
				OpenMode.CREATE_OR_APPEND);

		List<Book> books = Arrays
				.asList(new Book("Tom Sawyer", "Mark Twain", "1840", "Novel"),
						new Book("Collected Tales", "Mark Twain", "1850",
								"Novel"), new Book("The Trial", "Franz Kafka",
								"1901", "Novel"), new Book("Some book",
								"Some author", "1901", "Novel"));

		createDocuments(writer, taxoWriter, books);
		taxoWriter.commit();
		writer.commit();
		writer.close();
		taxoWriter.close();

		IndexReader indexReader = DirectoryReader.open(dir);
		IndexSearcher searcher = new IndexSearcher(indexReader);
		TaxonomyReader taxoReader = new DirectoryTaxonomyReader(taxoDir);
		Query q = new TermQuery(new Term("category", "Novel"));
		TopScoreDocCollector tdc = TopScoreDocCollector.create(10, true);
		FacetSearchParams facetSearchParams = new FacetSearchParams();
		facetSearchParams.addFacetRequest(new CountFacetRequest(
				new CategoryPath("author"), 10));
		facetSearchParams.addFacetRequest(new CountFacetRequest(
				new CategoryPath("category"), 10));
		facetSearchParams.addFacetRequest(new CountFacetRequest(
				new CategoryPath("published"), 10));
		FacetsCollector facetsCollector = new FacetsCollector(
				facetSearchParams, indexReader, taxoReader);
		searcher.search(q, MultiCollector.wrap(tdc, facetsCollector));
		List<FacetResult> res = facetsCollector.getFacetResults();
		System.out
				.println("Search for books with the category:Novel returned : "
						+ res.size()
						+ " results\n---------------------------------");
		for (final FacetResult r : res) {
			System.out.println("\nMatching "
					+ r.getFacetResultNode().getLabel()
					+ ":\n------------------------------------");
			for (FacetResultNode n : r.getFacetResultNode().getSubResults()) {
				System.out.println(String.format("\t%s: %.0f", n.getLabel()
						.lastComponent(), n.getValue()));
			}
		}
	}

	private static void createDocuments(final IndexWriter writer,
			final TaxonomyWriter taxoWriter, final List<Book> books)
			throws IOException {
		for (final Book b : books) {
			Document doc = new Document();
			doc.add(new StringField("title", b.getTitle(), Store.YES));
			doc.add(new StringField("category", b.getCategory(), Store.YES));
			List<CategoryPath> categories = new ArrayList<CategoryPath>();
			categories.add(new CategoryPath("author", b.getAuthor()));
			categories.add(new CategoryPath("category", b.getCategory()));
			categories.add(new CategoryPath("published", b.getPublished()));
			CategoryDocumentBuilder categoryDocBuilder = new CategoryDocumentBuilder(
					taxoWriter);
			categoryDocBuilder.setCategoryPaths(categories);
			categoryDocBuilder.build(doc);
			writer.addDocument(doc);
		}
	}

	public static class Book {
		private final String title;
		private final String author;
		private final String published;
		private final String category;

		public Book(final String title, final String author,
				final String published, final String category) {
			this.title = title;
			this.author = author;
			this.published = published;
			this.category = category;
		}

		public String getTitle() {
			return title;
		}

		public String getAuthor() {
			return author;
		}

		public String getPublished() {
			return published;
		}

		public String getCategory() {
			return category;
		}
	}
}
