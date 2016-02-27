/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fire;

import cc.mallet.classify.Classifier;
import cc.mallet.classify.MaxEntTrainer;
import cc.mallet.classify.Trial;
import cc.mallet.classify.evaluate.ConfusionMatrix;
import cc.mallet.classify.tui.Csv2Classify;
import cc.mallet.classify.tui.Csv2Vectors;
import cc.mallet.pipe.iterator.CsvIterator;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.Labeling;
import cc.mallet.util.CommandOption;
import cc.mallet.util.MalletLogger;
import static fire.FIRE.initializeClassifiers;
import static fire.FIRE.inputFile;
import static fire.FIRE.outputFile;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
public class FIREWordLabeller {
    private static Logger logger = MalletLogger.getLogger(FIREWordLabeller.class.getName());
    static CommandOption.File trainingDataFile = new CommandOption.File
		(FIREWordLabeller.class, "training-data", "FILE", true, null,
		 "The text file containing training data in FIRE utterance format", null);
    
    static CommandOption.File trainingLabelFile = new CommandOption.File
		(FIREWordLabeller.class, "training-label", "FILE", true, null,
		 "The label file containing labels for training-data in FIRE utterance format", null);    
    
    static CommandOption.File testingDataFile = new CommandOption.File
		(FIREWordLabeller.class, "testing-data", "FILE", true, null,
		 "The text file containing testing data in FIRE utterance format", null);
    
    static CommandOption.File testingLabelFile = new CommandOption.File
		(FIREWordLabeller.class, "testing-label", "FILE", true, null,
		 "The label file containing labels for training-data in FIRE utterance format", null);

    static CommandOption.File outputFile = new CommandOption.File
		(FIREWordLabeller.class, "output", "FILE", true, new File("output"),
		 "Write predictions to this file; Using - indicates stdout.", null);
   
    
    static CommandOption.Integer numGrams = new CommandOption.Integer
		(FIREWordLabeller.class, "ngrams", "INT", true, 5,
		 "number of grams used for sentence and word classification", null);
    
    static CommandOption.String wordClassifierFolder = new CommandOption.String
		(FIREWordLabeller.class, "word-classifier", "STRING", true, null,
		 "Folder Containing word level classifers", null);

    static CommandOption.Boolean no1grams = new CommandOption.Boolean
		(FIRE.class, "no1grams", "BOOLEAN", true, false, "Flag indicating that 1grams to be omitted ", null);
    
    static Pattern emptyLinePat = Pattern.compile("^\\s*$");
    
    static Pattern xPat = Pattern.compile("^http|^www|[.]com$i|^\\d$|^\\d+.\\d*$|^#|^@|[.|*!\"'?:;,\\/()\\[\\]_\\-&$+\\u2026%\\u2018\\u2019\\u201c\\u201d]+"); // includes urls digits, # tags @ mentions
    
    static Pattern strPat = Pattern.compile("^[a-zA-Z]+.[a-zA-Z]*$|^.[a-zA-Z]+|[a-zA-Z]");
    
    static HashMap<String,Classifier> classifiers = new HashMap<String,Classifier>();
    
    static boolean isTestData = false;
    
    static int [] linenos = new int[100];
    
    public static void main(String[] args) throws FileNotFoundException, IOException, ClassNotFoundException {

        CommandOption.setSummary (FIREWordLabeller.class, "A tool for identifying the language at word level");
        CommandOption.process (FIREWordLabeller.class, args);

        // Print some helpful messages for error cases
        if (args.length == 0) {
                CommandOption.getList(SentenceTagger.class).printUsage(false);
                System.exit (-1);
        }
        String ngramsTrainFile = "ngramsTrain.txt";
        String ngramsTrainMallFile = "ngramsTrain.mall";
        
        generateNGrams(trainingDataFile.value.toString(),trainingLabelFile.value.toString(),ngramsTrainFile);
        
        isTestData = true;
        String ngramsTestFile = "ngramsTest.txt";
        String ngramsTestMallFile = "ngramsTest.mall";
        generateNGrams(testingDataFile.value.toString(),testingLabelFile.value.toString(),ngramsTestFile);
        String[] inputCmdArgs = {"--input",ngramsTrainFile,"--output",ngramsTrainMallFile,
                                 "--line-regex","(\\S+)[,](.*)", "--name","0","--data","2","--label","1"};
        System.out.println("CommandLine: "+Arrays.toString(inputCmdArgs));
        
        Csv2Vectors.main(inputCmdArgs);
        
        inputCmdArgs = new String[]{"--input",ngramsTestFile,"--output",ngramsTestMallFile,
                                 "--line-regex","(\\S+)[,](.*)", "--name","0","--data","2","--label","1","--use-pipe-from",ngramsTrainMallFile};
        Csv2Vectors.main(inputCmdArgs);
        // Build the Sentence level classifier using MaxEntropy
        
        MaxEntTrainer maxent = new MaxEntTrainer();
        InstanceList trainingList = InstanceList.load(new File(ngramsTrainMallFile));
        InstanceList testingList = InstanceList.load(new File(ngramsTestMallFile));
        Classifier c = maxent.train(trainingList);
        
        // Test the sentence level classifier on the test set 
        Trial testTrial = new Trial (c, testingList);
        ConfusionMatrix cm = new ConfusionMatrix(testTrial);
        System.out.println("Confusion Matrix for Sentence Level Classifier is: ");
        System.out.println(cm.toString());
        System.out.println("Accuracy is " + testTrial.getAccuracy());
        
        // Write the classifier into 
        String sentClassifierFile = "s_maxent.cl";
         try {
            ObjectOutputStream oos = new ObjectOutputStream
                    (new FileOutputStream (new File(sentClassifierFile)));
            oos.writeObject (c);
            oos.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException ("Couldn't write classifier to filename "+
                                                                                    sentClassifierFile);
        }
        String sentClassLabelFile = "s_maxent_labels.txt";
        String[] classifierArgs = {"--input",ngramsTestFile,"--output", sentClassLabelFile,
                                    "--line-regex","(.*)", "--classifier", sentClassifierFile
                                    ,"--name","0","--data","1","--label","0","--report-majority","TRUE"};
        System.out.println("CommandLine: "+Arrays.toString(classifierArgs));
        Csv2Classify.main(classifierArgs);
        initializeClassifiers();
        wordLabeler(sentClassLabelFile,testingDataFile.value.toString(),testingLabelFile.value.toString());
        
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
    
    
    static void wordLabeler(String sentClassLabelsFile, String testDataFile, String testLabelFile) throws IOException{
        BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile.value));
        String sentenceLabelFile;
        BufferedReader lr; 
        lr = new BufferedReader(new FileReader(sentClassLabelsFile));
        
        //  BufferedWriter deb = new BufferedWriter(new FileWriter("debugfile"));
        loadTrainingData();
        BufferedReader br = new BufferedReader(new FileReader(testDataFile));
        String line=null;
        int lineno = 0;
        int numInstances = 0;
        int ind = 0;
        while((line=br.readLine())!=null){
            if(line.contains("<utterance")){
                bw.write(line+"\n");
                line = br.readLine();
                String[] words = line.split("\\s+");
                String[] labels = new String[words.length];
                StringBuilder lineNgrams = new StringBuilder("");
                boolean flag = false;
                if(lineno==linenos[ind]){
                    flag = false;
                }
                for(int i = 0;i<words.length;i++){
                    if(emptyLinePat.matcher(words[i]).find()){
                        // Matched empty string
                    }
                    else if(xPat.matcher(words[i]).find()){
                        labels[i] = "X";
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
                if(lineno==linenos[ind]){
                    flag = false;
                    ind++;
                }
                lineno++;
                
                if(flag){
                    String label = lr.readLine();
                    if(label==null)
                        System.out.println("Something wrong");
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
    
    static void generateNGrams(String dataFile, String labelFile,String outputFile) throws FileNotFoundException, IOException{
        BufferedReader br = new BufferedReader(new FileReader(new File(dataFile)));
        String line;
        BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outputFile)));
        
        BufferedReader lbr = new BufferedReader(new FileReader(new File(labelFile)));
        int l = 0;
        int ind = 0;
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
                else {
                    System.out.println("Empty sentence at Line Number " + l);
                    if(isTestData)
                        linenos[ind++]=l;
                }
                l++;
            }
            
        }
        lbr.close();
        br.close();
        bw.close();
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
    static HashMap<String,HashSet<String>> hmap = new HashMap<>();
    static HashMap<String,HashSet<String>> namedEntities = new HashMap<>();
    
    static void loadTrainingData() throws FileNotFoundException, IOException{
        BufferedReader br = new BufferedReader(new FileReader(trainingDataFile.value));
        String line;
        
        BufferedReader lbr = new BufferedReader(new FileReader(trainingLabelFile.value));
        
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
}
