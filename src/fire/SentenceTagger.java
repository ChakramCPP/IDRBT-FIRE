/* Copyright (C) 2002 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */

package fire;

import java.util.Iterator;
import java.util.logging.*;
import java.util.regex.*;
import java.io.*;
import java.nio.charset.Charset;

import cc.mallet.classify.*;
import cc.mallet.pipe.iterator.*;
import cc.mallet.types.*;
import cc.mallet.util.*;

/**
 * Command line tool for classifying a sequence of  
 *  instances directly from text input, without
 *  creating an instance list.
 *  <p>
 * 
 *  @author David Mimno
 *  @author Gregory Druck
 */

public class SentenceTagger {

	private static Logger logger = MalletLogger.getLogger(SentenceTagger.class.getName());

	static CommandOption.File inputFile =	new CommandOption.File
		(SentenceTagger.class, "input", "FILE", true, null,
		 "The file containing data to be classified, one instance per line", null);

	static CommandOption.File outputFile = new CommandOption.File
		(SentenceTagger.class, "output", "FILE", true, new File("output"),
		 "Write predictions to this file; Using - indicates stdout.", null);
        
        static CommandOption.File accuracyFile = new CommandOption.File
		(SentenceTagger.class, "accuracyFile", "FILE", true, new File("accuracy"),
		 "Write accuracy/f1 to this file; Using - indicates stdout.", null);

	static CommandOption.String lineRegex = new CommandOption.String
		(SentenceTagger.class, "line-regex", "REGEX", true, "^(\\S*)[\\s,]*(\\S*)[\\s,]*(.*)$",
		 "Regular expression containing regex-groups for label, name and data.", null);
	
	static CommandOption.Integer nameOption = new CommandOption.Integer
		(SentenceTagger.class, "name", "INTEGER", true, 1,
		 "The index of the group containing the instance name.\n" +
         "   Use 0 to indicate that the name field is not used.", null);
        static CommandOption.Integer labelOption = new CommandOption.Integer
		(SentenceTagger.class, "label", "INTEGER", true, 2,
		 "The index of the group containing the label string.\n" +
		 "   Use 0 to indicate that the label field is not used.", null);

	static CommandOption.Integer dataOption = new CommandOption.Integer
		(SentenceTagger.class, "data", "INTEGER", true, 3,
		 "The index of the group containing the data.", null);
	
	static CommandOption.File classifierFile = new CommandOption.File
		(SentenceTagger.class, "classifier", "FILE", true, new File("classifier"),
		 "Use the pipe and alphabets from a previously created vectors file.\n" +
		 "   Allows the creation, for example, of a test set of vectors that are\n" +
		 "   compatible with a previously created set of training vectors", null);

	static CommandOption.String encoding = new CommandOption.String
		(SentenceTagger.class, "encoding", "STRING", true, Charset.defaultCharset().displayName(),
		 "Character encoding for input file", null);
        
        static CommandOption.Boolean majorityLabel = new CommandOption.Boolean
                (SentenceTagger.class, "report-majority", "BOOLEAN", false, false,
		 "If True report only majority label rather than distribution", null);
        
        static CommandOption.Boolean reportAccuracy = new CommandOption.Boolean
                (SentenceTagger.class, "report-accuracy", "BOOLEAN", false, false,
		 "If True Accuaracy and confusion matrix for test data are displayed", null);
        static CommandOption.String inputString = new CommandOption.String
		(SentenceTagger.class, "input-string", "STRING", true, Charset.defaultCharset().displayName(),
		 "String containing the instance", null);

	public static void main (String[] args) throws FileNotFoundException, IOException {

		// Process the command-line options
            CommandOption.setSummary (SentenceTagger.class,
                                                              "A tool for classifying a stream of unlabeled instances");
            CommandOption.process (SentenceTagger.class, args);

            // Print some helpful messages for error cases
            if (args.length == 0) {
                    CommandOption.getList(SentenceTagger.class).printUsage(false);
                    System.exit (-1);
            }
            if (inputFile == null&& !inputString.wasInvoked()) {
                    throw new IllegalArgumentException ("You must include `--input FILE ...' in order to specify a"+
                                                            "file containing the instances, one per line.");
            }
	    	
	  // Read classifier from file
                
            Classifier classifier = null;
            try {
                    ObjectInputStream ois =
                            new ObjectInputStream (new BufferedInputStream(new FileInputStream (classifierFile.value)));

                    classifier = (Classifier) ois.readObject();
                    ois.close();
            } catch (Exception e) {
                    throw new IllegalArgumentException("Problem loading classifier from file " + classifierFile.value +
                                                       ": " + e.getMessage());
            }
		
            // Read instances from the file
            Reader fileReader;
            if (inputFile.value.toString().equals ("-")) {
                fileReader = new InputStreamReader (System.in);
            }
            else {
                    fileReader = new InputStreamReader(new FileInputStream(inputFile.value), encoding.value);
            }
            Iterator<Instance> csvIterator = 
                    new CsvIterator (fileReader, Pattern.compile(lineRegex.value),
                    dataOption.value, 0, nameOption.value);
            Iterator<Instance> iterator = 
                    classifier.getInstancePipe().newIteratorFrom(csvIterator);

            // Write classifications to the output file
            PrintStream out = null;

            if (outputFile.value.toString().equals ("-")) {
                    out = System.out;
            }
            else {
                    out = new PrintStream(outputFile.value, encoding.value);
            }
            PrintStream accuracyStream = null;
            if (reportAccuracy.value && accuracyFile.value.toString().equals ("-")) {
                    accuracyStream = System.out;
            }
            else {
                    accuracyStream = new PrintStream(accuracyFile.value, encoding.value);
            }

            // gdruck@cs.umass.edu
            // Stop growth on the alphabets. If this is not done and new
            // features are added, the feature and classifier parameter
            // indices will not match.  
            classifier.getInstancePipe().getDataAlphabet().stopGrowth();
            classifier.getInstancePipe().getTargetAlphabet().stopGrowth();

            InstanceList ilist = new InstanceList(classifier.getInstancePipe());
            Alphabet targetAlphabet = ilist.getTargetAlphabet();
            int numClasses = ilist.getTargetAlphabet().size();
            if(labelOption.value==0){ // no need of accuracy evaluation
                while(iterator.hasNext()){
                    Instance inst = iterator.next();
                    Labeling l = (Labeling) classifier.classify(inst).getLabeling();
                    if(majorityLabel.wasInvoked()){
                            out.println(l.getBestLabel());
                    }else{
                        StringBuilder output = new StringBuilder();
                        output.append(inst.getName());

                        for (int location = 0; location < l.numLocations(); location++) {
                                output.append("\t" + l.labelAtLocation(location));
                                output.append("\t" + l.valueAtLocation(location));
                        }

                        out.println(output);
                    }
                }

            }
            else{
                while(iterator.hasNext()){
                    ilist.add(iterator.next());
                }
                Trial trial = new Trial (classifier, ilist);

                for (int ii = 0; ii< ilist.size();ii++) {
                        Instance instance = ilist.get(ii);

                        Labeling labeling = trial.get(ii).getLabeling();

                        StringBuilder output = new StringBuilder();
                        output.append(instance.getName());
                        if(!majorityLabel.value)
                        for (int location = 0; location < labeling.numLocations(); location++) {
                                output.append("\t" + labeling.labelAtLocation(location));
                                output.append("\t" + labeling.valueAtLocation(location));
                        }
                        else{
                            output.append("\t"+labeling.getBestLabel());
                        }
                        out.println(output);
                }
                for(int i=0;i<numClasses;i++){
                    Labeling l = (Labeling)targetAlphabet.lookupObject(i);
                    accuracyStream.println(targetAlphabet.lookupObject(i) +":" + trial.getAccuracy() + ":" + trial.getF1(i));
                }
                accuracyStream.println(trial.toString());

                if (! outputFile.value.toString().equals ("-")) {
                        out.close();
                }
                if (! accuracyFile.value.toString().equals ("-")) {
                        accuracyStream.close();
                }
            }
            
	}
}

    

