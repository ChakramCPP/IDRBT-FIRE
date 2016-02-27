/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fire;

/**
 *
 * @author nageshbhattu
 */
import cc.mallet.util.CommandOption;
import cc.mallet.util.MalletLogger;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.*;

import org.terrier.matching.ResultSet;

import es.upv.nlel.eval.SimilarityMeasures;
import es.upv.nlel.preprocess.NGramTokeniser;
import es.upv.nlel.translit.Editex;
import es.upv.nlel.translit.TransliterationMiner;
import es.upv.nlel.wrapper.TerrierWrapper;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import java.util.logging.Logger;

public class TranslitRetrieval {
	TerrierWrapper terrier;
	TransliterationMiner tm; 
	
	Editex editex;
	String path_to_index="";

	TObjectIntHashMap<String> enReverseMap = new TObjectIntHashMap<String>();
	TIntObjectHashMap<String> enMap = new TIntObjectHashMap<String>();
	
	THashMap<String,TObjectDoubleHashMap<String>> ccaEquivalents = new THashMap<String, TObjectDoubleHashMap<String>>(); 
        private static Logger logger = MalletLogger.getLogger(TranslitRetrieval.class.getName());
        
        static CommandOption.File inputFile = new CommandOption.File
		(TranslitRetrieval.class, "input", "FILE", true, null,
		 "The file containing data to be classified, one instance per line", null);
        
        
        static CommandOption.String terrierPath = new CommandOption.String
		(TranslitRetrieval.class, "terrier-path", "STRING", true, null,
		 "terrier-path where index and other structures are kept", null);
        
        static CommandOption.String dataPath = new CommandOption.String
		(TranslitRetrieval.class, "data-path", "STRING", true, null,
		 "path where data consisting of documents ", null);
        
        static CommandOption.String matchModel = new CommandOption.String
		(TranslitRetrieval.class, "match-model", "STRING", true, null,
		 "matching model to be used in terrier ", null);
        
        static CommandOption.String queryFile = new CommandOption.String
		(TranslitRetrieval.class, "query-file", "STRING", true, null,
		 "the file containing the query file", null);
        
        static CommandOption.String models = new CommandOption.String
		(TranslitRetrieval.class, "models", "STRING", true, null,
		 "the models to be used for comparison", null);
	public static void main(String[] args) throws Exception {
            
            CommandOption.setSummary (TranslitRetrieval.class,"A tool for classifying a stream of unlabeled instances");
            CommandOption.process (TranslitRetrieval.class, args);

        // Print some helpful messages for error cases
            if (args.length == 0) {
                    CommandOption.getList(TranslitRetrieval.class).printUsage(false);
                    System.exit (-1);
            }
            
            TranslitRetrieval tr = new TranslitRetrieval();
            NGramTokeniser gramTokeniser = new NGramTokeniser();
            String corpus = "translit-grams"; // "translit" or "translit-grams";
            String lang = "hindi";
            String dir = "/home/nageshbhattu/Work/FIRE-2015/subtask2-small-data-2015/FinalDataSet_MS";
            String dir_grams = "/home/nageshbhattu/Work/FIRE-2015/subtask2-small-data-2015/FinalDataSet_MS_grams";
            DecimalFormat df = new DecimalFormat("#0.0000");
		
            FileOutputStream fosTable = new FileOutputStream("output/translit/result-table.txt", true);
            PrintStream pTable = new PrintStream(fosTable);
		
		/**************************
		 * Parameters
		 **************************/
		int id = 5;
		String matchModel = "XSqrA_M"; // XSqrA_M | LemurTF_IDF
		String method = "deep"; // cca, pca, editex, editex+trans, deep, naive
		int neglectSmallUnit = 2;
		double simTheta = 0.96;
		boolean expand = true; // automatically set based on the method name
		boolean transliterate = false; // automatically set based on the method name
		boolean evaluate = true;
		boolean verbose = false;
		boolean multiScript = true; // to turn-on/off devnagari script terms inclusion 
		
		if(method.contains("naive"))
			expand = false;
		if(method.contains("trans"))
			transliterate = true;
		

		int pcaDim = 50;
		String partition = "test";
		String lexiconFile = "etc/translit/lexicon.txt";
		
		String queryFile = "", transQueryFile = "";
		if(partition.equals("dev"))
				queryFile = "/home/nageshbhattu/Work/FIRE-2015/subtask2-small-data-2015/query-file.txt";
		else {
			queryFile = "/home/nageshbhattu/Work/FIRE-2015/subtask2-small-data-2015/queryIndex.txt";
			transQueryFile = "/home/nageshbhattu/Work/FIRE-2015/data/translit/Subtask2_TestData/queryIndexTrans.txt";
		}
		String qrelFile = "/home/nageshbhattu/Work/FIRE-2015/data/translit/Subtask2_TestData/newQrelsTest.txt";
		
		String gram = "";
		if(corpus.contains("gram"))
			gram = "bigram";
		else
			gram = "unigram";
		
		String runFile = "output/translit/results-"+method+"-"+simTheta+"-"+gram+".txt";
	
                /**************************/
		
		int totalQueryTerms = 0;
		int totalEquivalents = 0;
		
		FileOutputStream fos = new FileOutputStream(runFile);
		PrintStream p = new PrintStream(fos);
		
		boolean stopword_removal = false, stem = false;
		tr.terrier = new TerrierWrapper(terrierPath.value);
		tr.setIndexPath(terrierPath.value+"var/index/"+corpus+"/"); 
		
		if(!new File(tr.path_to_index+"/"+lang+".docid.map").exists()) {
			if(!new File(tr.path_to_index).exists())
				new File(tr.path_to_index).mkdirs();
			tr.createIndex(tr.path_to_index, lang, "txt", dir_grams, lang, stopword_removal, stem);
			tr.terrier.learnDocId(terrierPath.value +"etc/collection.spec");
		}
		
		// Setting up the Pipeline
		if(stopword_removal)
			tr.terrier.setStopwordRemoval(lang);
		if(stem)
			tr.terrier.setStemmer(lang);
	
		tr.loadEnDocs(lang);
		
		tr.terrier.loadIndex(tr.path_to_index, lang, lang);
		
		// Load the Transliteration Module
		if(expand)
		tr.loadTranslitModel();
		
		if(method.contains("pca") && expand)
			tr.loadPCA(pcaDim);
		
		if(method.contains("editex") && expand)
			tr.loadEditex();
		
		if(method.contains("cca") && expand) {
			tr.loadCCAEquivalents("/home/parth/workspace/2014/publish/sigir/cca/cca-roman-output-mining-lexicon.txt");
			tr.loadCCAEquivalents("/home/parth/workspace/2014/publish/sigir/cca/cca-hindi-output-mining-lexicon.txt");
		}
		
		
		Map<Integer, String> query = tr.loadQueries(queryFile);
		Map<Integer, String> queryTrans = tr.loadQueries(transQueryFile);
		
		Map<Integer, Map<String, Double>> qrel = tr.loadQrel(qrelFile);
		
		if(!new File(lexiconFile).exists())
			tr.terrier.writeLexicon(lexiconFile);
		
		System.setProperty("ignore.low.idf.terms","false");
		double avgNDCG = 0.0, score=0.0, avgMRR=0.0, mAP=0.0;
		int count = 0;
		SortedSet<Integer> keys = new TreeSet<Integer>(query.keySet());
		for (int i : keys) { 
			
			String q = query.get(i);
//			String q_trans = tr.transliterateQuery(q);
			String q_trans = queryTrans.get(i);
			String q_grams = gramTokeniser.tokeniseString(q, 2);
			String q_trans_grams = gramTokeniser.tokeniseString(q_trans, 2);
			
			System.out.println("\n\n Query id: " + i + " = " + q + "\t" + q_trans);
			String extendedQuery = "";
			if(expand) {
				if(corpus.contains("grams"))
					extendedQuery = tr.expandEquivalents(q, neglectSmallUnit, simTheta, method, multiScript);
				else
					extendedQuery = tr.expandUnigramEquivalents(q, neglectSmallUnit, simTheta, method);
				if(verbose)
					System.out.println("Query Grams : " + extendedQuery);
				String[] temp = extendedQuery.split(" ");
				totalQueryTerms += temp.length;
			}
		/*	ResultSet rs;
			if(expand && !transliterate)
				rs = tr.terrier.getResultSet(extendedQuery, matchModel, false, 0);
			else if(expand && transliterate) {
				
				rs = tr.terrier.getResultSet(extendedQuery+" "+q_trans_grams, matchModel, false, 0);
			}
			else if(!expand && transliterate) {
				if(corpus.contains("grams"))
					rs = tr.terrier.getResultSet(q_grams+" "+q_trans_grams, matchModel, false, 0);
				else
					rs = tr.terrier.getResultSet(q+" "+q_trans, matchModel, false, 0);
			}
			else {
				if(corpus.contains("grams"))
					rs = tr.terrier.getResultSet(q_grams, matchModel, false, 0);
				else
					rs = tr.terrier.getResultSet(q, matchModel, false, 0);
			}
			System.setProperty("ignore.low.idf.terms", "false");
			int[] docid = rs.getDocids();
			double[] scores = rs.getScores();
			List<String> rl = new ArrayList<String>();
			int maxRank = (docid.length<10)?docid.length:10;
			p.print("query-" + i);
			for(int j=0; j<maxRank; j++) {
				if(j<10) {
//					p.println(tr.enMap.get(docid[j]).substring(0, tr.enMap.get(docid[j]).indexOf(".txt")));
					System.out.println("Top Document: " + (j+1) + "\t" +  tr.enMap.get(docid[j]) + "\t" + scores[j]);
				}
				rl.add(tr.enMap.get(docid[j]));
			}
			
		/*	if(evaluate) {
				int[] rankList = SimilarityMeasures.getRankList(rl, qrel.get(i));
				int[] idealRankList = SimilarityMeasures.getIdealRankList(qrel.get(i));
				double dcg = SimilarityMeasures.nDCG(rankList, idealRankList,5);
				avgNDCG += dcg;
				
				List<Integer> relValues= new ArrayList<Integer>();
				relValues.add(4);
				relValues.add(5);
				int[] binaryRL = SimilarityMeasures.getBinaryRankList(rl, qrel.get(i), relValues);
				int[] binaryIRL = SimilarityMeasures.getIdealBinaryRankList(qrel.get(i), relValues);
				double mrr = SimilarityMeasures.meanReciprocalRank(binaryRL);
				avgMRR += mrr;
				
				double ap = SimilarityMeasures.averagePrecision(binaryRL, binaryIRL);
				mAP += ap; 
//				score = SimilarityMeasures.nDCG(rl, qrel.get(i),5);
//				avgNDCG += score;
				count++;
				System.out.println(i+ "\t" + "ndcg = " + df.format(dcg) + "\t"+"mrr=" + df.format(mrr) + "\t ap = " + df.format(ap));
				p.println("\t" + df.format(dcg)+"\t"+df.format(mrr)+"\t"+df.format(ap));
			}*/
		}
		/*if(evaluate)
			System.out.println("Avg NDCG = "+ df.format(avgNDCG/count) + "\t MRR = " + df.format(avgMRR/count) +"\t MAP = " + df.format(mAP/count));
		
		System.out.println("\n\nAverage Query Length = " + (double)totalQueryTerms/(double)query.size());
		pTable.println(method+"-"+simTheta + "\t"+ df.format(avgNDCG/count) + "\t" + df.format(avgMRR/count) + "\t" + df.format(mAP/count)+"\t"+df.format((double)totalQueryTerms/(double)query.size()));
		p.println("average\t" + df.format(avgNDCG/count) + "\t" + df.format(avgMRR/count) + "\t" + df.format(mAP/count));*/
		p.close();
		fos.close();
		
		pTable.close();
		fosTable.close();
		System.exit(0);
		
		
	}
	public void loadEditex() throws IOException {
		editex = new Editex();
		editex.loadLexicon("etc/translit/lexicon.txt");
	}
	public void loadPCA(int dim) throws IOException, ClassNotFoundException {
		tm.loadPCA("objects/deep/translit/X-pca"+dim+"-Vt");
		tm.loadPCAData("objects/deep/translit/termIndexPCA"+dim+"Songs.obj","objects/deep/translit/projectedDataPCA"+dim+"Songs.obj","etc/translit/lexicon.txt", 1);
	}
	public void loadTranslitModel() throws Exception {
		this.tm = new TransliterationMiner();
		int[] g = {1,2};
		tm.setGrams(g);
		tm.loadModel1Network("objects/deep/translit/fineenrsmweights20linear", true, "linear");
		System.out.println("[info] Autoencoder Loaded.");
		tm.loadFeatureIndex("etc/translit/feature-id-model1.txt");
		System.out.println("[info] Feature Index Loaded.");
//		tm.loadData("objects/deep/translit/termIndexModel1.obj","objects/deep/translit/projectedDataModel1.obj","etc/translit/en-hi-translit-pairs.txt", 2);
		tm.loadData("objects/deep/translit/termIndexModel1Songs.obj","objects/deep/translit/projectedDataModel1Songs.obj","etc/translit/lexicon.txt", 1);
		

		System.out.println("[info] Data Loaded.\n\n");
	}
	
	public List<String> expandTerms(String query) throws IOException {
		String[] cols = this.terrier.tokenizeTerrier(query).trim().split(" ");
		List<String> expandedTerms = new ArrayList<String>();
		for(String s: cols) {
			expandedTerms.add(s);
			if(s.length()>3) {
				List<String> terms = tm.topTerms(s, "cosine", 0.99, true);
				expandedTerms.addAll(terms);
			}
		}
		return expandedTerms;
	}
	
	public void loadCCAEquivalents(String file) throws NumberFormatException, IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(file)),"UTF-8"));
		String line = "";
		while((line=br.readLine())!=null) {
			if(line.startsWith("<") && !line.endsWith("/>")) {
				String romanTerm = line.substring(1,line.length()-1);
				TObjectDoubleHashMap<String> inner = new TObjectDoubleHashMap<String>();
				
				line = br.readLine();
				while(!line.endsWith("/>")) {
					String[] cols = null;
					if(line.contains("\t"))
						cols = line.split("\t");
					else if(line.contains(" "))
						cols = line.split(" ");
					else {
						cols = new String[2];
						cols[0] = line.substring(0, line.indexOf("0."));
						cols[1] = line.substring(line.indexOf("0.")); 
					}
					String devnagariTerm = cols[0].trim().replaceAll("[*-+.^:,)(]","");;
					double score = Double.parseDouble(cols[1].trim());
					inner.put(devnagariTerm, score);
					line = br.readLine();
				}
				if(!this.ccaEquivalents.contains(romanTerm))
					this.ccaEquivalents.put(romanTerm, inner);
				else
					this.ccaEquivalents.get(romanTerm).putAll(inner);
			}
		}
		br.close();
	}

	public List<String> getCCAEquivalents(String term, double theta) {
		List<String> equivalents = new ArrayList<String>();
		if(this.ccaEquivalents.contains(term.toLowerCase().trim())) {
			TObjectDoubleHashMap<String> inner = this.ccaEquivalents.get(term.toLowerCase().trim());
			for(String s: inner.keySet()) {
				if(inner.get(s)>=theta)
					equivalents.add(s);
			}
		}
		return equivalents;
	}
	public String expandEquivalents(String query, int smallUnits, double theta, String method, boolean multiScript) throws IOException {
		String newQuery = "";
        String[] cols = this.terrier.tokenizeTerrier(query).trim().split(" ");
		Map<Integer, Set<String>> equi = new HashMap<Integer, Set<String>>();
		// For terms in devnagari script (original forward transliterated space)
		Map<Integer, Set<String>> equiT = new HashMap<Integer, Set<String>>();
		
		int index = 0;
		for(int i = 0; i<cols.length; i++) {
			if(cols[i].length()>0) {
				Set<String> inner = new HashSet<String>();
				Set<String> innerT = new HashSet<String>();
				if(cols[i].length()>=smallUnits) {
					List<String> terms = null;
					if(method.contains("deep")) {
						terms = tm.topTerms(cols[i], "cosine", theta, true);
					}
					else if(method.contains("pca")) {
						terms = tm.topPCATerms(cols[i], "cosine", theta, true);
					}
					else if(method.contains("editex") && !method.contains("cca")) {
						terms = editex.editexMiner(cols[i], theta);
					}
					else if(method.contains("cca") && !method.contains("editex")) {
						terms = this.getCCAEquivalents(cols[i],theta);
					}
					else if(method.contains("editex+cca") || method.contains("cca+editex")) {
						terms = editex.editexMiner(cols[i], 0.0);
						terms.addAll(this.getCCAEquivalents(cols[i],theta));
					}
					inner.add(cols[i].trim());
					for(String t: terms) {
						if(t.trim().matches(".*[\\u0900-\\u097f].*"))
							innerT.add(t.toLowerCase().trim());
						else
							inner.add(t.toLowerCase().trim());
					}
				}
				else {
					inner.add(cols[i].trim());
					innerT.add(tm.transliterate(cols[i].trim(), "cosine", false));
				}
				equi.put(index, inner);
				equiT.put(index, innerT);
				index++;
			}                
		}
		for(int i=0; i<equi.size()-1; i++) {
			newQuery+=this.getExpandedNGrams(equi.get(i), equi.get(i+1)) + " ";
			if(multiScript)
				newQuery+=this.getExpandedNGrams(equiT.get(i), equiT.get(i+1)) + " ";
		}
		return newQuery;
	}
	
	public String expandUnigramEquivalents(String query, int smallUnits, double theta, String method) throws IOException {
		String newQuery = "";
        String[] cols = this.terrier.tokenizeTerrier(query).trim().split(" ");
		for(int i = 0; i<cols.length; i++) {
			if(cols[i].length()>0) {
				if(cols[i].length()>=smallUnits) {
					List<String> terms = null;
					if(method.contains("deep")) {
						terms = tm.topTerms(cols[i], "cosine", theta, true);
					}
					else if(method.contains("pca")) {
						terms = tm.topPCATerms(cols[i], "cosine", theta, true);
					}
					else if(method.contains("editex") && !method.contains("cca")) {
						terms = editex.editexMiner(cols[i], theta);
					}
					else if(method.contains("cca") && !method.contains("editex")) {
						terms = this.getCCAEquivalents(cols[i],theta);
					}
					else if(method.contains("editex+cca") || method.contains("cca+editex")) {
						terms = editex.editexMiner(cols[i], 0.0);
						terms.addAll(this.getCCAEquivalents(cols[i],theta));
					}
					newQuery += cols[i].trim()+" ";
					for(String t: terms) {
						newQuery+= t+" ";
					}
                }
				else {
					newQuery+= cols[i].trim()+" ";
					newQuery+= tm.transliterate(cols[i].trim(), "cosine", false)+" ";
				}

			}
                        
        }
		return newQuery;
        }

	public String getExpandedNGrams(Set<String> set1, Set<String> set2) {
		String grams = "";
		for(String t1 : set1) {
			for(String t2: set2) {
				grams+= t1.trim()+"8"+t2.trim() + " ";
			}
		}
		return grams;
	}
	
	public String transliterateQuery(String query) throws IOException {
		String transQuery = "";
		String[] terms = this.terrier.tokenizeTerrier(query).trim().split(" ");
		for(int i=0; i<terms.length; i++) {
			if(terms.length>0) {
				String token = tm.transliterate(terms[i], "cosine", false);
				transQuery += token +" ";
			}
		}
		return transQuery;
	}
	public Map<Integer, String> loadQueries(String queryFile) throws IOException {
		Map<Integer, String> queries =  new HashMap<Integer,String>();
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(queryFile)),"UTF-8"));
		String line="";
		while((line=br.readLine())!=null) {
			String[] cols = line.trim().split("\t");
			queries.put(Integer.parseInt(cols[0].trim()), cols[1].trim());
		}
		return queries;
	}
	
	public Map<Integer, Map<String, Double>> loadQrel(String qrelFile) throws IOException {
		Map<Integer, Map<String, Double>> qrel = new HashMap<Integer, Map<String, Double>>();
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(qrelFile)),"UTF-8"));
		String line="";
		while((line= br.readLine())!=null){
			if(line.startsWith("query-id")) {
				String cols[] = line.split("\t"); 
				int qid = Integer.parseInt(cols[0].substring(cols[0].trim().lastIndexOf("-")+1));
				String inLine = br.readLine();
				Map<String, Double> inner = new HashMap<String, Double>();
				while(inLine!=null && inLine.length()>1) {
					cols = inLine.split("\t");
					if(cols[0].trim().startsWith("doc"))
						inner.put(cols[0].trim()+".txt", Double.parseDouble(cols[1]));
					else
						inner.put("doc-id-"+cols[0].trim()+".txt", 1.0);
					inLine = br.readLine();
				}
				qrel.put(qid, inner);
				
			}
		}
		return qrel;
	}
	public void setIndexPath(String path) {
		this.path_to_index= path;
	}
	
	public void createIndex(String path_to_index, String prefix, String ext, String path_to_data,
			String lang, boolean stopword_removal, boolean stem) throws IOException {
		this.terrier.setIndex(path_to_index, prefix);
		this.terrier.prepareIndex(path_to_data, ext, lang, stopword_removal, stem);
	}
	public void loadEnDocs(String lang) throws IOException {
		this.enMap = this.terrier.learnDocName(this.path_to_index+lang+".docid.map");
		for(int i: this.enMap.keys()) {
			this.enReverseMap.put(this.enMap.get(i), i);
		}
	}
	
	
}
