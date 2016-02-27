/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fire;
import cc.mallet.classify.Classifier;
import cc.mallet.classify.tui.*;
import cc.mallet.pipe.iterator.CsvIterator;
import cc.mallet.types.Instance;
import cc.mallet.types.Labeling;
import cc.mallet.util.CommandOption;
import cc.mallet.util.CommandOption.Set;
import cc.mallet.util.MalletLogger;
import static fire.SentenceTagger.inputFile;
import java.io.BufferedInputStream;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.logging.Logger;
import java.util.regex.Pattern;
/**
 *
 * @author nageshbhattu
 */
public class FIRE {
    private static Logger logger = MalletLogger.getLogger(FIRE.class.getName());
    static CommandOption.File inputFile = new CommandOption.File
		(FIRE.class, "input", "FILE", true, null,
		 "The file containing data to be classified, one instance per line", null);
    
    static CommandOption.File trainingFile = new CommandOption.File
		(FIRE.class, "training-file", "FILE", true, null,
		 "The training file containing data to be classified, one instance per line", null);
    
    static CommandOption.File labelFile = new CommandOption.File
		(FIRE.class, "label-file", "FILE", true, null,
		 "The file containing labels of the classified, one instance per line", null);

    static CommandOption.File outputFile = new CommandOption.File
		(FIRE.class, "output", "FILE", true, new File("output"),
		 "Write predictions to this file; Using - indicates stdout.", null);
    
    static CommandOption.Boolean showNEs = new CommandOption.Boolean
		(FIRE.class, "show-ne", "BOOLEAN", true, false,
		 "To Show the named entities in the training data, --training-file and --label-file must be set", null);
    
    static CommandOption.Boolean showNETestData = new CommandOption.Boolean
		(FIRE.class, "show-ne-testdata", "BOOLEAN", true, false,
		 "To Show the named entities in the test data, --training-file and --label-file and --input must be set", null);
    
    static CommandOption.Boolean buildSentClassifier = new CommandOption.Boolean
		(FIRE.class, "build-sent-classifier", "BOOLEAN", true, false,
		 "To build the sentence level classifier , --training-file and --label-file and --input must be set", null);
    static CommandOption.Boolean genNGrams = new CommandOption.Boolean
		(FIRE.class, "gen-ngrams", "BOOLEAN", true, false,
		 "To generate the ngrams, --training-file and --label-file and --input must be set", null);
    static CommandOption.Boolean appendNGrams = new CommandOption.Boolean
		(FIRE.class, "append-ngrams", "BOOLEAN", true, false,
		 "To generate the ngrams, --training-file and --label-file and --input must be set", null);
    static CommandOption.String labelString = new CommandOption.String
		(FIRE.class, "label-string", "STRING", true, null,
		 "String representing the class of instances", null);
    
    static CommandOption.Boolean labelTestData = new CommandOption.Boolean
		(FIRE.class, "label-testdata", "BOOLEAN", true, false,
		 "Flag indicating labelling task for test data, --input and output must be set ", null);
    static CommandOption.Boolean genSentNgrams = new CommandOption.Boolean
		(FIRE.class, "gen-sent-ngrams", "BOOLEAN", true, false,
		 "Flag indicating generating ngrams for the input sentences, --input and --output must be set ", null);
    static CommandOption.Boolean no1grams = new CommandOption.Boolean
		(FIRE.class, "no1grams", "BOOLEAN", true, false,
		 "Flag indicating that 1grams to be omitted ", null);
    static CommandOption.String sentClassifierFile = new CommandOption.String
		(FIRE.class, "sentence-classifier", "STRING", true, null,
		 "File Containing sentence level classifer", null);
    static CommandOption.File goldSentenceTags = new CommandOption.File
		(FIRE.class, "gold-tags", "STRING", true, null,
		 "File Containing sentence level golden tags", null);
    
    static CommandOption.String wordClassifierFolder = new CommandOption.String
		(FIRE.class, "word-classifier", "STRING", true, null,
		 "Folder Containing word level classifers", null);
    
    static CommandOption.Integer numGrams = new CommandOption.Integer
		(FIRE.class, "ngrams", "INT", true, 5,
		 "number of grams used for sentence and word classification", null);
   
    
    /**
     * @param args the command line arguments
     */
    static HashMap<String,Classifier> classifiers = new HashMap<String,Classifier>();
    static String tempFile = "SomeTempFile.txt";
    static String tempMalletFile = "tempMallet.mall";
    static Pattern emptyLinePat = Pattern.compile("^\\s*$");
    static Pattern xPat = Pattern.compile("^http|^www|[.]com$i|^\\d$|^\\d+.\\d*$|^#|^@|[.|*!\"'?:;,\\/()\\[\\]_\\-&$+\\u2026%\\u2018\\u2019\\u201c\\u201d]+"); // includes urls digits, # tags @ mentions
    static Pattern strPat = Pattern.compile("^[a-zA-Z]+.[a-zA-Z]*$|^.[a-zA-Z]+|[a-zA-Z]");
    public static void main(String[] args) throws FileNotFoundException, IOException, ClassNotFoundException {

        CommandOption.setSummary (FIRE.class,
                                                              "A tool for classifying a stream of unlabeled instances");
        CommandOption.process (FIRE.class, args);

        // Print some helpful messages for error cases
        if (args.length == 0) {
                CommandOption.getList(SentenceTagger.class).printUsage(false);
                System.exit (-1);
        }
        
        if(labelTestData.value){
            testDataLabeller();
        }
        else if(showNEs.value){
            trainingStats();
        }
        else if(showNETestData.value){
            testNEStats();
        }
        else if(buildSentClassifier.value){
            
        }
        else if(genNGrams.value){
            generateNGrams();
        }
        else if(appendNGrams.value){
            appendNGrams();
        }
        else if(genSentNgrams.value){
            testDataNgrams();
        }
    }
    
    public static void testDataNgrams() throws FileNotFoundException, IOException{
        if(inputFile == null){
                throw new IllegalArgumentException ("You must include `--input FILE ...' in order to specify a"+
                                                        "file containing the instances, one per line.");
        }
        BufferedReader br = new BufferedReader(new FileReader(inputFile.value));
        String line;
        BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile.value));
        int lineno = 0;
        while((line=br.readLine())!=null){
            String label = line.substring(0,line.indexOf(',')+1);
            line = line.substring(line.indexOf(',')+1);
            String[] words = line.split("\\s+");
            StringBuilder outLine = new StringBuilder();
           // String[] initWords = words[0].split(",");
            outLine.append(label);
            for(int i = 1;i<words.length;i++){
                
                if(emptyLinePat.matcher(words[i]).find()){
                    // Matched empty string
                }
                else if(xPat.matcher(words[i]).find()){
                    
                }
                else if(strPat.matcher(words[i]).find()){
                    outLine.append(ngrams(words[i]));
                }
                else{

                }
            }
            if(outLine.length()==0){ // No string meant for classification
                System.out.println("Length is zero for line number " + (lineno+1));
            }else{
                bw.write(outLine+"\n");
            }
            lineno++;
        }
        bw.close();
        br.close();
        
    } 
    public static void testDataLabeller() throws FileNotFoundException, IOException, ClassNotFoundException{
        if (inputFile == null) {
                throw new IllegalArgumentException ("You must include `--input FILE ...' in order to specify a"+
                                                        "file containing the instances, one per line.");
        }
        
        BufferedReader br = new BufferedReader(new FileReader(inputFile.value));
        String line;
        
        BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile));
        int lineno = 0;
        while((line=br.readLine())!=null){
            if(line.contains("<utterance")){
                line = br.readLine();
                String[] words = line.split("\\s+");
                StringBuilder outLine = new StringBuilder();
                for(int i = 0;i<words.length;i++){
                    if(emptyLinePat.matcher(words[i]).find()){
                        // Matched empty string
                    }
                    else if(xPat.matcher(words[i]).find()){

                    }
                    else if(strPat.matcher(words[i]).find()){
                        outLine.append(ngrams(words[i]));
                    }
                    else{
                        
                    }
                }
                if(outLine.length()==0){ // No string meant for classification
                    System.out.println("Length is zero for line number " + (lineno+1));
                }else{
                    bw.write(outLine+"\n");
                }
            }
            else{
                
            }
            lineno++;
        }
        bw.close();
        br.close();
       
        String[] classifierArgs = {"--input",tempFile,"--output",tempMalletFile,
                                    "--line-regex","(.*)", "--classifier",
                                    sentClassifierFile.valueToString(),"--name","0","--data","1","--label","0","--report-majority","TRUE"};
        System.out.println("CommandLine: "+Arrays.toString(classifierArgs));
        Csv2Classify.main(classifierArgs);
        initializeClassifiers();
        wordLabeler();
    }
    static void goldTagger() throws FileNotFoundException{
        BufferedReader br = new BufferedReader(new FileReader(goldSentenceTags.value));
    }
    
    static HashMap<String,HashSet<String>> hmap = new HashMap<>();
    static HashMap<String,HashSet<String>> namedEntities = new HashMap<>();
   
    
    static void loadTrainingData() throws FileNotFoundException, IOException{
        BufferedReader br = new BufferedReader(new FileReader(trainingFile.value));
        String line;
        
        BufferedReader lbr = new BufferedReader(new FileReader(labelFile.value));
        
        while((line=br.readLine())!=null){
            String labelLine = lbr.readLine();
            if(line.contains("<utterance")){
                line = br.readLine();
                labelLine = lbr.readLine();
                String[] words = line.split("\\s+");
                String[] labels = labelLine.split("\\s+");
                StringBuilder outLine = new StringBuilder();
                for(int i = 0;i<words.length;i++){
                    if(emptyLinePat.matcher(words[i]).find()){
                        // Matched empty string
                    }
                    if(!hmap.containsKey(words[i].toLowerCase())){
                        HashSet<String> hset = new HashSet<>();
                        hmap.put(words[i].toLowerCase(),hset);
                    }
                    hmap.get(words[i].toLowerCase()).add(labels[i]);
                    
                    if(labels[i].startsWith("NE") || labels[i].startsWith("MIX")){
                        if(!namedEntities.containsKey(words[i].toLowerCase()))
                        {
                            HashSet<String> hset = new HashSet<>();
                            namedEntities.put(words[i].toLowerCase(),hset);
                        } 
                        namedEntities.get(words[i].toLowerCase()).add(labels[i]);
                    }
                }
            }
        }
        lbr.close();
        br.close();
    }
    static void trainingStats() throws IOException{
        
        loadTrainingData();
        System.out.println("Summary");
        if(showNEs.value)
        {
            for(String key:namedEntities.keySet()){
                 System.out.println(key+namedEntities.get(key).toString());
            }
        }
    }
    static void testNEStats() throws IOException{
        loadTrainingData();
        BufferedReader br = new BufferedReader(new FileReader(inputFile.value));
        String line;
        while((line=br.readLine())!=null){
            if(line.contains("<utterance")){
                line = br.readLine();
                String[] words = line.split("\\s+");
                StringBuilder outLine = new StringBuilder();
                for(int i = 0;i<words.length;i++){
                    if(emptyLinePat.matcher(words[i]).find()){
                        // Matched empty string
                    }
                    if((words[i].length()>3 || words[i].equals("bjp")|| words[i].equals("aap")) && namedEntities.containsKey(words[i].toLowerCase())){
                        System.out.println("The tag associated with word " + words[i] + " is " + namedEntities.get(words[i].toLowerCase()).toString());
                    }
                }
            }
            else{
                
            }
        }
        br.close();
    }
    static StringBuilder ngrams(String word){
        word = word.toLowerCase();
        StringBuilder strNgrams = new StringBuilder();
        int j =0;
        if(no1grams.value){
            j=2;
        }
        else{
            j=1;
        }
        for(;j<=numGrams.value;j++){
            if(word.length()-j>=0)
            for(int k=0;k<=word.length()-j;k++){
                strNgrams.append( word.substring(k, k+j)+" ");
            }
        }
        return strNgrams;
    }
    static void wordLabeler() throws IOException{
        BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile.value));
        String sentenceLabelFile;
        BufferedReader lr; 
        if(goldSentenceTags.wasInvoked()){
            lr = new BufferedReader(new FileReader(goldSentenceTags.value));
        }else{
            lr = new BufferedReader(new FileReader(tempMalletFile));
        }
        
        //  BufferedWriter deb = new BufferedWriter(new FileWriter("debugfile"));
        loadTrainingData();
        BufferedReader br = new BufferedReader(new FileReader(inputFile.value));
        String line=null;
        int lineno = 0;
        int numInstances = 0;
        while((line=br.readLine())!=null){
            if(line.contains("<utterance")){
                bw.write(line+"\n");
                line = br.readLine();
                String[] words = line.split("\\s+");
                String[] labels = new String[words.length];
                StringBuilder lineNgrams = new StringBuilder("");
                boolean flag = false;
                for(int i = 0;i<words.length;i++){
                    if(emptyLinePat.matcher(words[i]).find()){
                        // Matched empty string
                    }
                    else if((words[i].length()>3 ||words[i].toLowerCase().equals("bjp")||words[i].toLowerCase().equals("aap"))&& hmap.containsKey(words[i].toLowerCase())&&namedEntities.containsKey(words[i].toLowerCase())){
                        {
                           HashSet<String> hset = namedEntities.get(words[i].toLowerCase());
                           if(hset.size()>=1){
                               logger.fine(" Confusion for the entry " + words[i] + " being "+ hset.toString());
                               int len = 0;
                               for(String label:hset){
                                   if(label==null)
                                       System.out.println("Something going on here ");
                                   if(label.length()>len){
                                       len = label.length();
                                       labels[i] = label;
                                   }
                               }
                               logger.fine(" Labeller has chosen label " + labels[i]);
                           }
                           
                           
                        }
                    }
                    else if(xPat.matcher(words[i]).find()){
                        labels[i] = "X";
                    }
                    else if(strPat.matcher(words[i]).find()){
                        lineNgrams.append(ngrams(words[i]));
                        lineNgrams.append("\n");
                        numInstances++;
                        labels[i] = "L";
                        flag = true;
                    }
                    else{
                        labels[i] = "O";
                      //  deb.write(words[i]+" ");
                    }
                }
                lineno++;
                if(flag){
                    String label = lr.readLine();
                    Classifier classifier = classifiers.get(label);
                    StringReader sr = new StringReader(lineNgrams.toString());
                    Iterator<Instance> csvIterator = new CsvIterator (sr, Pattern.compile("^(.*)$"), 1, 0, 0);
                    if(classifier==null)
                        System.out.println("Something wrong " + lineno + label +" " +lineNgrams.toString());
                    Iterator<Instance> iterator = classifier.getInstancePipe().newIteratorFrom(csvIterator);
                    classifier.getInstancePipe().getDataAlphabet().stopGrowth();
                    classifier.getInstancePipe().getTargetAlphabet().stopGrowth();
                    int index = 0;
                    while(iterator.hasNext()){
                        while(labels[index]!="L") index++;
                        Instance inst = iterator.next();
                        Labeling labeling = classifier.classify(inst).getLabeling();
                        labels[index] = labeling.getBestLabel().toString();
                    }
                    
                }
                
                bw.write("\t\t"+ String.join(" ", Arrays.copyOfRange(labels, 1, labels.length))+"\n");
         //       deb.write("\n");
            }
            else{
                bw.write(line+"\n");
            }
        }
        bw.close();
        br.close();
        lr.close();
        //deb.close();
    }
    static void goldWordLabeler() throws IOException{
        BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile.value));
        BufferedReader lr = new BufferedReader(new FileReader(tempMalletFile));
      //  BufferedWriter deb = new BufferedWriter(new FileWriter("debugfile"));
        loadTrainingData();
        BufferedReader br = new BufferedReader(new FileReader(inputFile.value));
        String line=null;
        int lineno = 0;
        int numInstances = 0;
        while((line=br.readLine())!=null){
            if(line.contains("<utterance")){
                bw.write(line+"\n");
                line = br.readLine();
                String[] words = line.split("\\s+");
                String[] labels = new String[words.length];
                StringBuilder lineNgrams = new StringBuilder("");
                boolean flag = false;
                for(int i = 0;i<words.length;i++){
                    if(emptyLinePat.matcher(words[i]).find()){
                        // Matched empty string
                    }
                    else if((words[i].length()>3 ||words[i].toLowerCase().equals("bjp")||words[i].toLowerCase().equals("aap"))&& hmap.containsKey(words[i].toLowerCase())&&namedEntities.containsKey(words[i].toLowerCase())){
                        {
                           HashSet<String> hset = namedEntities.get(words[i].toLowerCase());
                           if(hset.size()>=1){
                               logger.fine(" Confusion for the entry " + words[i] + " being "+ hset.toString());
                               int len = 0;
                               for(String label:hset){
                                   if(label==null)
                                       System.out.println("Something going on here ");
                                   if(label.length()>len){
                                       len = label.length();
                                       labels[i] = label;
                                   }
                               }
                               logger.fine(" Labeller has chosen label " + labels[i]);
                           }
                           
                           
                        }
                    }
                    else if(xPat.matcher(words[i]).find()){
                        labels[i] = "X";
                    }
                    else if(strPat.matcher(words[i]).find()){
                        lineNgrams.append(ngrams(words[i]));
                        lineNgrams.append("\n");
                        numInstances++;
                        labels[i] = "L";
                        flag = true;
                    }
                    else{
                        labels[i] = "O";
                      //  deb.write(words[i]+" ");
                    }
                }
                lineno++;
                if(flag){
                    String label = lr.readLine();
                    Classifier classifier = classifiers.get(label);
                    StringReader sr = new StringReader(lineNgrams.toString());
                    Iterator<Instance> csvIterator = new CsvIterator (sr, Pattern.compile("^(.*)$"), 1, 0, 0);
                    if(classifier==null)
                        System.out.println("Something wrong " + lineno + label +" " +lineNgrams.toString());
                    Iterator<Instance> iterator = classifier.getInstancePipe().newIteratorFrom(csvIterator);
                    classifier.getInstancePipe().getDataAlphabet().stopGrowth();
                    classifier.getInstancePipe().getTargetAlphabet().stopGrowth();
                    int index = 0;
                    while(iterator.hasNext()){
                        while(labels[index]!="L") index++;
                        Instance inst = iterator.next();
                        Labeling labeling = classifier.classify(inst).getLabeling();
                        labels[index] = labeling.getBestLabel().toString();
                    }
                    
                }
                
                bw.write("\t\t"+ String.join(" ", Arrays.copyOfRange(labels, 1, labels.length))+"\n");
         //       deb.write("\n");
            }
            else{
                bw.write(line+"\n");
            }
        }
        bw.close();
        br.close();
        lr.close();
        //deb.close();
    }
    static void initializeClassifiers() throws FileNotFoundException, IOException, ClassNotFoundException{
        String[] classifierFiles = new String[]{"bn.cl",  "en.cl",  "gu.cl",  "hi.cl",  "kn.cl" , "ml.cl" , "mr.cl",  "ta.cl",  "te.cl"};
        String[] classifierLabels = new String[]{"bn_en_","en_","gu_en_","hi_en_","kn_en_","ml_en_","mr_en_","ta_en_","te_en_"};
        for(int ci = 0;ci<classifierFiles.length;ci++){
            ObjectInputStream ois =
                            new ObjectInputStream (new BufferedInputStream(new FileInputStream (wordClassifierFolder.value+"/"+classifierFiles[ci])));
            
            classifiers.put(classifierLabels[ci], (Classifier) ois.readObject());
        }
    }
    
    
    
    static void appendNGrams() throws FileNotFoundException, IOException{
        BufferedReader br = new BufferedReader(new FileReader(trainingFile.value));
        BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile.value));
        String line;
       
        while((line=br.readLine())!=null){
            if(line.contains("<utterance")){
                line = br.readLine();
                String[] words = line.split("\\s+");
                StringBuilder outLine = new StringBuilder();
                String labelStr = words[1];
                for(int i = 2;i<words.length;i++){
                    if(emptyLinePat.matcher(words[i]).find()){
                        // Matched empty string
                    }
                    else if(xPat.matcher(words[i]).find()){
                        
                    }
                    else if(strPat.matcher(words[i]).find()){
                        outLine.append(ngrams(words[i]).append(" "));
                    }
                }
                bw.write(labelStr+","+outLine+"\n");
            }
        }
        br.close();
        bw.close();
    }
    static void generateNGrams() throws FileNotFoundException, IOException{
        BufferedReader br = new BufferedReader(new FileReader(trainingFile.value));
        String line;
        BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile.value));
        
        BufferedReader lbr = new BufferedReader(new FileReader(labelFile.value));
        
        while((line=br.readLine())!=null){
            String labelLine = lbr.readLine();
            if(line.contains("<utterance")){
                line = br.readLine();
                labelLine = lbr.readLine();
                String[] words = line.split("\\s+");
                String[] labels = labelLine.split("\\s+");
                StringBuilder outLine = new StringBuilder();
                String labelStr = "";
                for(int i = 0;i<words.length;i++){
                    if(emptyLinePat.matcher(words[i]).find()){
                        // Matched empty string
                    }
                    else if(xPat.matcher(words[i]).find()){
                        
                    }
                    else if(strPat.matcher(words[i]).find()){
                        outLine.append(ngrams(words[i]).append(" "));
                    }
                }
                String[] classifierLabels = new String[]{"bn","gu","hi","kn","ml","mr","ta","te","en"};
                for(int ci = 0;ci<classifierLabels.length;ci++){
                    for(int i = 0;i<labels.length;i++){
                        if(labels[i].equals(classifierLabels[ci]))
                        {
                            labelStr += classifierLabels[ci]+"_";
                            break;
                        }
                    }
                }
                if(labelStr.length()==3 && !labelStr.equals("en_"))
                    labelStr+="en_";
                if(labelStr.startsWith("bn_hi_"))
                    labelStr = "bn_en_";
                if(labelStr.length()>=3)
                bw.write(labelStr+","+outLine+"\n");
            }
        }
        lbr.close();
        br.close();
        bw.close();
    }
}
