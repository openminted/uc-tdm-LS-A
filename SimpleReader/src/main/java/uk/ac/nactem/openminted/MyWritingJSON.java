package uk.ac.nactem.openminted;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.examples.SourceDocumentInformation;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;

public class MyWritingJSON extends JCasAnnotator_ImplBase {
	public static final String PARAM_NAME_CONTEXT_URL = "ContextURL";
	public static final String PARAM_NAME_TARGET_TYPE = "TargetType";
	public static final String PARAM_NAME_OUTPUT_DIR = "outputFolder";
	public static final String PARAM_NAME_OVERWRITE = "overwrite";

	public static final String DEFAULT_ANNOTATION_TYPE = "uima.tcas.Annotation";
	private static final String OUTPUT_FILE_EXTENSION = ".json";
	
//	private static final String DEFAULT_CONTEXT = "{\n\t\t\"UCompareTS\" : \"http://nactem.ac.uk/schema/uima/typesystem/U_compareTypeSystem#\""
//			+ ",\n\t\t\"Sentence\" : \"UCompareTS:org.u_compare.shared.syntactic.Sentence\","
//			+ "\n\t\t\"NeuronTS\"=\"http:///uk/ac/nactem/uima/neuro.ecore\", "
//			+ "\n\t\t\"NeuronTS\" : \"http://nactem.ac.uk/schema/uima/typesystem/NeuroscienceTypeSystem#\","
//			+ "\n\t\t\"Neuron\" : \"NeuronTS:uk.ac.nactem.uima.neuro.Neuron\","
//			+ "\n\t\t\"BrainRegion\" : \"NeuronTS:uk.ac.nactem.uima.neuro.BrainRegion\","
//			+ "\n\t\t\"Synapse\" : \"NeuronTS:uk.ac.nactem.uima.neuro.Synapse\","
//			+ "\n\t\t\"IonicCurrent\" : \"NeuronTS:uk.ac.nactem.uima.neuro.IonicCurrent\","
//			+ "\n\t\t\"ModelOragnism\" : \"NeuronTS:uk.ac.nactem.uima.neuro.ModelOragnism\","
//			+ "\n\t\t\"IonicChannel\" : \"NeuronTS:uk.ac.nactem.uima.neuro.IonicChannel\","
//			+ "\n\t\t\"IonicConductance\" : \"NeuronTS:uk.ac.nactem.uima.neuro.IonicConductance\","
//			+ "\n\t\t\"ScientificUnit\" : \"NeuronTS:uk.ac.nactem.uima.neuro.ScientificUnit\","
//			+ "\n\t\t\"ScientificValue\" : \"NeuronTS:uk.ac.nactem.uima.neuro.ScientificValue\","
//			+ "\n\t\t\"NIFCELL\"=\"http://ontology.neuinfo.org/NIF/BiomaterialEntities/NIF-Cell.owl#\","
//			+ "\n\t\t\"NIFMOL\"=\"http://ontology.neuinfo.org/NIF/BiomaterialEntities/NIF-Molecule.owl#\","
//			+ "\n\t\t\"NIFNEURBR2\"=\"http://ontology.neuinfo.org/NIF/BiomaterialEntities/NIF-Neuron-Brain-Bridge.owl#\","
//			+ "\n\t\t\"ILX\"=\"http://uri.interlex.org/base/ilx_\","
//			+ "\n\t\t\"NIFNEURBR\"=\"http://ontology.neuinfo.org/NIF/BiomaterialEntities/NIF-Neuron-BrainRegion-Bridge.owl#\","
//			+ "\n\t\t\"NIFGA\"=\"http://ontology.neuinfo.org/NIF/BiomaterialEntities/NIF-GrossAnatomy.owl#\"}";
	
	private static final String DEFAULT_CONTEXT = "{\n\t\t\"UCompareTS\" : \"http://nactem.ac.uk/schema/uima/typesystem/U_compareTypeSystem#\""
			+ ",\n\t\t\"Sentence\" : \"UCompareTS:org.u_compare.shared.syntactic.Sentence\","
			+ "\n\t\t\"ChebiTS\"=\"http:///uk/ac/nactem/uima/chebi.ecore\", "
			+ "\n\t\t\"ChebiTS\" : \"http://nactem.ac.uk/schema/uima/typesystem/ChebiCurationTypeSystem#\","
			+ "\n\t\t\"Chemical\" : \"ChebiTS:uk.ac.nactem.uima.chebi.Chemical\","
			+ "\n\t\t\"Species\" : \"ChebiTS:uk.ac.nactem.uima.chebi.Species\","
			+ "\n\t\t\"Protein\" : \"ChebiTS:uk.ac.nactem.uima.chebi.Protein\","
			+ "\n\t\t\"Metabolite\" : \"ChebiTS:uk.ac.nactem.uima.chebi.Metabolite\","
			+ "\n\t\t\"SpectralData\" : \"ChebiTS:uk.ac.nactem.uima.chebi.SpectralData\","
			+ "\n\t\t\"BiologicalActivity\" : \"ChebiTS:uk.ac.nactem.uima.chebi.BiologicalActivity\"}";
	
	
	
	public static final String[] ID_SUFFIXES = { "id", "name" };
	private static final String TMP_DIR = "/tmp";
	private String targetTypeString;
	private Set<Map.Entry<String, JsonElement>> contextSet;
	private Map<String, String> objMappings;
	private Map<String, String> urlMappings;
	private File outputDirectory = null;
	private String contextContent;
	private boolean overwrite = false;
	private int docCount = 0;

	private SourceDocumentInformation getDocumentInfo(CAS cas) {
		try {
			Iterator<CAS> viewIterator = cas.getViewIterator();
			while (viewIterator.hasNext()) {
				CAS view = viewIterator.next();
				JCas jcas = view.getJCas();
				AnnotationIndex<Annotation> docInfos = jcas.getAnnotationIndex(SourceDocumentInformation.type);
				Iterator<Annotation> iter = docInfos.iterator();
				if (iter.hasNext()) {
					return (SourceDocumentInformation) iter.next();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private File getOutputFile(String name) {
		String filename = name;
		if (!filename.endsWith(OUTPUT_FILE_EXTENSION))
			filename += OUTPUT_FILE_EXTENSION;
		if (outputDirectory == null)
			return new File(filename);
		return new File(outputDirectory, filename);
	}

	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
		String outDir = (String) context.getConfigParameterValue(PARAM_NAME_OUTPUT_DIR);
		if ((outDir != null) && !outDir.equals("")) {
			outputDirectory = new File(outDir);
		} else {
			outputDirectory = null;
		}
		overwrite = (Boolean) context.getConfigParameterValue(PARAM_NAME_OVERWRITE);
		try {
			// contextURL = (String)
			// context.getConfigParameterValue(PARAM_NAME_CONTEXT_URL);
			targetTypeString = (String) context.getConfigParameterValue(PARAM_NAME_TARGET_TYPE);
			if (targetTypeString == null || targetTypeString.isEmpty()) {
				targetTypeString = DEFAULT_ANNOTATION_TYPE;
			}
			contextContent = "{\n\t\"@context\":\n\t" + DEFAULT_CONTEXT + "\n}";
//			System.out.println(contextContent);
			JsonObject graph = new JsonParser().parse(contextContent).getAsJsonObject();
			JsonObject contextF = graph.getAsJsonObject("@context");
			contextSet = contextF.entrySet();

			objMappings = new HashMap<String, String>();
			urlMappings = new HashMap<String, String>();
			Iterator<Map.Entry<String, JsonElement>> iterator = contextSet.iterator();
			while (iterator.hasNext()) {
				Map.Entry<String, JsonElement> mapEntry = iterator.next();
				String fullname = mapEntry.getValue().getAsString();
				if (fullname.startsWith("http://") || fullname.startsWith("https://"))
					urlMappings.put(mapEntry.getKey(), fullname);
				else if (fullname.indexOf(":") != -1) {
					fullname = fullname.split(":")[1];
					objMappings.put(fullname, mapEntry.getKey());
				}
			}
		} catch (Exception e) {
			throw new ResourceInitializationException(e);
		}

	}

	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {
		try {
			
			CAS cas = jCas.getCas();
			
			TypeSystem typeSystem = cas.getTypeSystem();
//			System.out.println(typeSystem.toString());
			Type targetType = typeSystem.getType(targetTypeString);

			//Type tokenType = typeSystem.getType("org.u_compare.shared.syntactic.Token");

			JsonObject jsonifiedCAS = new JsonObject();
			jsonifiedCAS.add("@context", (new JsonParser()).parse(DEFAULT_CONTEXT).getAsJsonObject());

			JsonPrimitive text = new JsonPrimitive(cas.getDocumentText());
			jsonifiedCAS.add("text", text);

			String outputString = "";
			
			
		
			AnnotationIndex<AnnotationFS> annotations = cas.getAnnotationIndex(targetType);
			JsonArray denotationsArray = new JsonArray();

			for (AnnotationFS annotation : annotations) {
				//System.out.println(annotation.toString());
				JsonObject denotation = new JsonObject();
				JsonObject span = new JsonObject();
				JsonPrimitive begin = new JsonPrimitive(annotation.getBegin());
				JsonPrimitive end = new JsonPrimitive(annotation.getEnd());
				
				span.add("begin", begin);
				span.add("end", end);

				String specificType = null;
				if (targetTypeString.equals(DEFAULT_ANNOTATION_TYPE)) {
					specificType = annotation.getType().getName();					
				}
				
				String shortName;
//				shortName = getValueByName(annotation, "name");
				shortName = getNameByParam(annotation);
				if (shortName == null)
					shortName = getNameByExternalReferenceParam(annotation);
				if (shortName == null)
					shortName = objMappings.get(specificType != null ? specificType : targetTypeString);
				if (shortName != null) {
					JsonPrimitive type = new JsonPrimitive(shortName);
//					denotation.add("id", new JsonPrimitive(getValueByName(annotation, "id")));
					denotation.add("span", span);
					// denotation.add("log", new JsonPrimitive(sb.toString()));
					// RB: remove this for now
					denotation.add("word_form",
							new JsonPrimitive(text.getAsString().substring(begin.getAsInt(), end.getAsInt())));
					String category = objMappings.get(specificType);
					
					if(category.equals(shortName) == false) {
							denotation.add("norm_id", type);
					} 
					
					denotation.add("category", new JsonPrimitive(category));
//				
					// System.out.println("Finding score for
					// '"+text.getAsString().substring(begin.getAsInt(),
					// end.getAsInt())+"', "+type);
					/*double score = tryGetScore(annotation);
					if (!Double.isNaN(score))
						denotation.add("score", new JsonPrimitive(new Double(score)));
						*/
					// other features
//					processFeatures(annotation, denotation);

					denotationsArray.add(denotation);
				}
			}

			jsonifiedCAS.add("denotations", denotationsArray);

			// Gson gson = new
			// GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
			// JsonParser jp = new JsonParser();
			// JsonElement je = jp.parse(jsonifiedCAS.toString());
			// outputString = gson.toJson(je);
			
//			JsonObject medlineCitationJSon = (JsonObject) new JsonParser().parse(metaData.getDocumentTitle());
			JsonObject annotatedDocumentJson = jsonifiedCAS;
			JsonObject insertJson = new JsonObject();
//			insertJson.add("MedlineCitation", medlineCitationJSon);
			insertJson.add("annotatedDocument", annotatedDocumentJson);
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			outputString = gson.toJson(insertJson);
			String name = "";
			DocumentMetaData docInfo = DocumentMetaData.get(cas);
//			SourceDocumentInformation docInfo = getDocumentInfo(cas);			
			if (docInfo == null) {
//				throw new AnalysisEngineProcessException();
				name = "temp.xmi" ;
			} else 
				name = docInfo.getDocumentId();
//			String name = docInfo.getDocumentId();
			
//			int pos1 = name.lastIndexOf(File.separator), pos2 = name.lastIndexOf(".");
//			name = name.substring(pos1+1, pos2);
			
			File outFile = getOutputFile(name);
			int counter = 0;
			while (outFile.exists() && !overwrite) {
				outFile = getOutputFile(name + "." + (++counter));
			}
			System.err.println("Writing to " + outFile.getAbsolutePath());
			FileOutputStream output = new FileOutputStream(outFile);
			output.write(outputString.getBytes("UTF8"));
			output.close();

		} catch (Exception e) {
			throw new AnalysisEngineProcessException(e);
		}
	}

	/*private double tryGetScore(AnnotationFS annotation) {
		Type targetType = annotation.getType();
		Feature scoreFeature = targetType.getFeatureByBaseName("scores");
		if (scoreFeature == null) {
			// System.out.println("Could not find feature 'scores'");
			return Double.NaN;
		}
		FeatureStructure fs = annotation.getFeatureValue(scoreFeature);
		if (fs == null) {
			// System.out.println("No value assigned to the feature");
			return Double.NaN;
		}
		if (!(fs instanceof ArrayFS)) {
			// System.out.println("Value doesn't seem to be an array");
			return Double.NaN;
		}
		ArrayFS array = (ArrayFS) fs;
		for (int i = 0; i < array.size(); ++i) {
			FeatureStructure score = array.get(i);
			if (!(score instanceof Score))
				continue;
			Score scorescore = (Score) score;
			if (scorescore.getScoreName() != null && scorescore.getScoreName().equals("normalisationScore"))
				return scorescore.getScoreValue();
		}
		// System.out.println("No normalisation score found");
		return Double.NaN;
	}*/

	private String getNameByExternalReferenceParam(AnnotationFS annotation) {
		Type type = annotation.getType();
		// sb.append("Type=" + type.getName() + "\n");
		// sb.append("Features=" + type.getAppropriateFeatures() + "\n");
		Feature feature = type.getFeatureByBaseName("externalReference");
		if (feature == null) {
			// sb.append("Feature null\n");
			return null;
		}
		try {
			FeatureStructure external = annotation.getFeatureValue(feature);
			// sb.append("For feature=" + feature + " got value=" + external +
			// "\n");
			if (external == null) {
				// sb.append("Abnormal external reference\n");
				return null;
			}
			Type type2 = external.getType();
			// sb.append("Type2=" + type2.getName() + "\n");
			// sb.append("Features2=" + type2.getAppropriateFeatures() + "\n");
			Feature feature2 = null;
			loop: for (String suffix : ID_SUFFIXES) {
				for (Feature it : type2.getFeatures())
					if (it.getName().toLowerCase().endsWith(suffix))
						try {
							if (external.getStringValue(it) != null) {
								feature2 = it;
								break loop;
							}
						} catch (CASRuntimeException e) {
							;
						}
			}
			if (feature2 == null) {
				// sb.append("Feature null\n");
				return null;
			}
			String value = external.getStringValue(feature2);
			// sb.append("For feature=" + feature2 + " got value=" + value +
			// "\n");
			if (value == null || value.indexOf(":") == -1) {
				// sb.append("Abnormal value\n");
				return null;
			}
			String prefix = value.split(":")[0];
			if (urlMappings.containsKey(prefix)) {
				// sb.append("Found prefix=" + prefix + "\n");
				return value;
			} else {
				// sb.append("Not found prefix=" + prefix + "\n");
				return null;
			}
		} catch (CASRuntimeException e) {
			// sb.append("CAS Exception\n");
			return null;
		}
	}
	
	private String getValueByName(AnnotationFS annotation, String param) {
		Type type = annotation.getType();
		Feature feature = null;
		for (Feature it : type.getFeatures()) {
			if (it.getName().toLowerCase().endsWith(param))
				try {
					if (annotation.getStringValue(it) != null) {
						feature = it;
						break;
					}
				} catch (CASRuntimeException e) {
					;
				}
		}
		
	
		if (feature == null) {
			// sb.append("Feature null\n");
			return null;
		}
		
		try {
			String value = annotation.getStringValue(feature);
			if (value == null){
				// sb.append("Abnormal value\n");
				return null;
			}
			return value;
		} catch (CASRuntimeException e) {
			// sb.append("CAS Exception\n");
			return null;
		}
	}

	private String getNameByParam(AnnotationFS annotation) {
		Type type = annotation.getType();
		// sb.append("Type=" + type.getName() + "\n");
		// sb.append("Features=" + type.getAppropriateFeatures() + "\n");
		Feature feature = null;
		loop: for (String suffix : ID_SUFFIXES) {
			for (Feature it : type.getFeatures())
				if (it.getName().toLowerCase().endsWith(suffix))
					try {
						if (annotation.getStringValue(it) != null) {
							feature = it;
							break loop;
						}
					} catch (CASRuntimeException e) {
						;
					}
		}
		if (feature == null) {
			// sb.append("Feature null\n");
			return null;
		}
		try {
			String value = annotation.getStringValue(feature);
			// sb.append("For feature=" + feature + " got value=" + value +
			// "\n");
			if (value == null || value.indexOf(":") == -1) {
				// sb.append("Abnormal value\n");
				return null;
			}
			String prefix = value.split(":")[0];
			if (urlMappings.containsKey(prefix)) {
				// sb.append("Found prefix=" + prefix + "\n");
				return value;
			} else {
				// sb.append("Not found prefix=" + prefix + "\n");
				return null;
			}
		} catch (CASRuntimeException e) {
			// sb.append("CAS Exception\n");
			return null;
		}
	}

	public void processFeatures(FeatureStructure annotation, JsonObject jsonObject) {
		List<Feature> features = annotation.getType().getFeatures();
		for (Feature feature : features) {
			String featureName = feature.getShortName();
			String jsonPropertyName = objMappings.get(featureName);

			if (jsonPropertyName != null) {
				// FeatureStructure fs = annotation.getFeatureValue(feature);
				Type range = feature.getRange();

				// primitive
				// Type fsType = fs.getType();
				if (range.isPrimitive()) {
					String value = annotation.getFeatureValueAsString(feature);
					if (value != null) {
						jsonObject.addProperty(jsonPropertyName, value);
					}
				} else {
					FeatureStructure fs = annotation.getFeatureValue(feature);
					if (fs != null) {
						JsonObject newJsonElement = new JsonObject();
						jsonObject.add(jsonPropertyName, newJsonElement);
						processFeatures(fs, newJsonElement);
					}
				}
			}

		}
	}
}
