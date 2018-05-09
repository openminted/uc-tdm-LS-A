package uk.ac.nactem.openminted;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.examples.SourceDocumentInformation;
import org.apache.uima.fit.component.JCasCollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.LanguageCapability;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.xml.sax.SAXException;

/**
 * Read XMI files
 * 
 * @author NaCTeM - National Centre of Text Mining
 */
// @TypeCapability(inputs = {}, outputs = {}) // Input and output annotation types
@LanguageCapability({ "en" }) // Languages supported by this component
@ResourceMetaData(name = "AL Phenotypes XMI Reader")
public class ArgoPhenotypesXMIReader extends JCasCollectionReader_ImplBase {
	/**
	 * Location (directory) to read XMI files from
	 */
	public static final String PARAM_FILE_DIRECTORY = "directory";
	@ConfigurationParameter(name = PARAM_FILE_DIRECTORY, mandatory = false, defaultValue = "input")
	private File directory;
	/**
	 * Read sub-directory or not.
	 */
	public static final String PARAM_RECURSIVE = "recursive";
	@ConfigurationParameter(name = PARAM_RECURSIVE, mandatory = false, defaultValue = "false")
	private boolean recursive;

	private Iterator<File> fileIterator;
	private int numProcessed;
	private int numTotal;

	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
		// Check if input directory exists
		if (!directory.exists()) {
			throw new ResourceInitializationException(
					new RuntimeException("'" + directory.getAbsolutePath() + "' does not exist."));
		}
		// Check if input directory is a directory
		if (!directory.isDirectory()) {
			throw new ResourceInitializationException(
					new RuntimeException("'" + directory.getAbsolutePath() + "' is not a directory."));
		}
		// Read all files in the directory, count them
		fileIterator = FileUtils.iterateFiles(directory, null, recursive);
		while (fileIterator.hasNext()) {
			fileIterator.next();
			numTotal++;
		}
		fileIterator = FileUtils.iterateFiles(directory, null, recursive);
		this.numProcessed = 0;
	}

	public boolean hasNext() throws IOException, CollectionException {
		return this.fileIterator.hasNext();
	}

	public Progress[] getProgress() {
		return new Progress[] { new ProgressImpl(numProcessed, numTotal, Progress.ENTITIES) };
	}

	@Override
	public void getNext(JCas jCas) throws IOException, CollectionException {
		FileInputStream inputStream = new FileInputStream(this.fileIterator.next());
		try {
			// Deserialize XMI file, ignore unknown type
			XmiCasDeserializer.deserialize(new BufferedInputStream(inputStream), jCas.getCas(), true);
		} catch (SAXException e) {
			throw new CollectionException(e);
		}
		inputStream.close();
		numProcessed++;
		
//		SourceDocumentInformation docInfo = 
	}
}