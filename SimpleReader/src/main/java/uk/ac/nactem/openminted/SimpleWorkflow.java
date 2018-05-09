package uk.ac.nactem.openminted;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.InvalidXMLException;


public class SimpleWorkflow {
	public static void main(String[] args) throws UIMAException, IOException{
		
		if(args.length < 2) {
			System.out.println("Usage: simpleReader.jar input_folder output_folder");
			return;
		}
		
		List<AnalysisEngineDescription> engines =  new LinkedList<AnalysisEngineDescription>();
		
		TypeSystemDescription builtInTypes = TypeSystemDescriptionFactory.createTypeSystemDescription(); 
//        TypeSystemDescription customTypes = TypeSystemDescriptionFactory.createTypeSystemDescriptionFromPath("descs/NeuroscienceTypeSystem.xml"); 
		TypeSystemDescription customTypes = TypeSystemDescriptionFactory.createTypeSystemDescriptionFromPath("descs/chebi.xml");
        TypeSystemDescription allTypes = CasCreationUtils.mergeTypeSystems(Arrays.asList(builtInTypes, customTypes));
        
		CollectionReaderDescription reader =  CollectionReaderFactory.createReaderDescription(ArgoPhenotypesXMIReader.class, 
				allTypes, ArgoPhenotypesXMIReader.PARAM_FILE_DIRECTORY, args[0], ArgoPhenotypesXMIReader.PARAM_RECURSIVE, false);
		
		AnalysisEngineDescription writer  = AnalysisEngineFactory.createEngineDescription(MyWritingJSON.class,
				MyWritingJSON.PARAM_NAME_CONTEXT_URL, "", 
				MyWritingJSON.PARAM_NAME_TARGET_TYPE, "",
				MyWritingJSON.PARAM_NAME_OUTPUT_DIR, args[1], MyWritingJSON.PARAM_NAME_OVERWRITE, true);
		
		engines.add(writer);
		
		SimplePipeline.runPipeline(reader, engines.toArray(new AnalysisEngineDescription[engines.size()]));
	}
}
