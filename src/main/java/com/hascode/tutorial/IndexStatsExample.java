package com.hascode.tutorial;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.Version;

public class IndexStatsExample {
	private static final String INDEX = "target/index_stats";

	public static void main(final String[] args) throws IOException {
		createRandomData();
		IndexReader reader = DirectoryReader.open(MMapDirectory.open(new File(
				INDEX)));
		IndexSearcher searcher = new IndexSearcher(reader);
		CollectionStatistics stats = searcher.collectionStatistics("title");
		System.out.println("Statistics for the field 'title':");
		System.out.println("Number of documents with a term for the field: "
				+ stats.docCount());
		System.out.println("Total number of documents: " + stats.maxDoc());
		System.out.println("Total number of postings for the field: "
				+ stats.sumDocFreq());
		System.out.println("Total number of tokens for the field: "
				+ stats.sumTotalTermFreq());
	}

	public static void createRandomData() throws IOException {
		Directory dir = FSDirectory.open(new File(INDEX));
		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_40);
		IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_40,
				analyzer);
		iwc.setOpenMode(OpenMode.CREATE);
		IndexWriter writer = new IndexWriter(dir, iwc);

		for (long i = 1; i < 2000; i++) {
			Document doc = new Document();
			doc.add(new LongField("id", i, Store.YES));
			doc.add(new StringField("title", "The big book of boredom - Part "
					+ (int) i, Store.YES));
			writer.addDocument(doc);
		}
		Document doc = new Document();
		doc.add(new LongField("id", 9999l, Store.YES));
		writer.addDocument(doc);
		writer.close();
	}
}
