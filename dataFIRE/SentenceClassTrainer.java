/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fire;

import cc.mallet.classify.Classifier;
import cc.mallet.classify.NaiveBayesEMTrainer;
import cc.mallet.classify.Trial;
import cc.mallet.classify.evaluate.ConfusionMatrix;
import cc.mallet.types.InstanceList;
import cc.mallet.types.Labeling;
import cc.mallet.util.CommandOption;
import cc.mallet.util.MalletLogger;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.BitSet;
import java.util.logging.Logger;

/**
 *
 * @author nageshbhattu
 */
public class SentenceClassTrainer {
    private static Logger logger = MalletLogger.getLogger(SentenceClassTrainer.class.getName());
    static CommandOption.File trainingFile = new CommandOption.File
		(SentenceClassTrainer.class, "training-file", "FILE", true, null,
		 "The file containing train data to be classified, one instance per line", null);
    //static CommandOption.File testingFile = new CommandOption.File
	//	(SentenceClassTrainer.class, "testing-file", "FILE", true, null,
	//	 "The file containing test data to be classified, one instance per line", null);
    static CommandOption.Integer numTestInst = new CommandOption.Integer
		(SentenceClassTrainer.class, "ntest-inst", "FILE", true, 0,
		 "The number of instances belonging to testdata", null);
    static CommandOption.String trainingDir = new CommandOption.String
		(SentenceClassTrainer.class, "training-dir", "STRING", true, null,
		 "The directory containing training data ", null);
    static CommandOption.String testinDir = new CommandOption.String
		(SentenceClassTrainer.class, "testing-dir", "STRING", true, null,
		 "The directory containing test data ", null);
    static CommandOption.String outputFile = new CommandOption.String
		(SentenceClassTrainer.class, "output", "STRING", true, null,
		 "The file into which classifier is written ", null);
    public static void main(String[] args) throws FileNotFoundException, IOException, ClassNotFoundException {

        CommandOption.setSummary (SentenceClassTrainer.class,
                                                              "A tool for classifying a stream of unlabeled instances");
        CommandOption.process (SentenceClassTrainer.class, args);

        // Print some helpful messages for error cases
        if (args.length == 0) {
                CommandOption.getList(SentenceTagger.class).printUsage(false);
                System.exit (-1);
        }
        InstanceList trainingList = InstanceList.load (trainingFile.value);
       // InstanceList testingList = InstanceList.load (testingFile.value);
        int trainingSize = trainingList.size() -numTestInst.value;
        
        BitSet bs = new BitSet(trainingList.size());
        bs.set(bs.size()-numTestInst.value, bs.size()-1);
        trainingList.hideSomeLabels(bs);
        
        
        NaiveBayesEMTrainer nbEM = new NaiveBayesEMTrainer();
        Classifier c = nbEM.train(trainingList);
        trainingList.unhideAllLabels();
        Trial testTrial = new Trial (c, trainingList.subList(trainingSize, trainingList.size()-1));
        ConfusionMatrix cm = new ConfusionMatrix(testTrial);
        System.out.println("Confusion Matrix: ");
        System.out.println(cm.toString());
        System.out.println("Accuracy is " + testTrial.getAccuracy());
        try {
            ObjectOutputStream oos = new ObjectOutputStream
                    (new FileOutputStream (outputFile.value));
            oos.writeObject (c);
            oos.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException ("Couldn't write classifier to filename "+
                                                                                    outputFile.value);
        }
        }
}
