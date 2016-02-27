#!/usr/bin/python

"""

Evaluation Script for Subtask1 in FIRE 2015.

Usage: 2015_subtask1Eval.py [options]

Options:
  -h, --help            show this help message and exit
  -g GTDIR, --gt-dir=GTDIR
                        Directory contining labelled data
  -s SUBDIR, --sub-dir=SUBDIR
                        Directory containing the submissions
  -o OUTPUTFILE, --output-file=OUTPUTFILE
                        Output file to write to...

Gokul Chittaranjan, gokulchittaranjan@gmail.com

"""

from optparse import OptionParser;
import glob;
import os;

import numpy as np;
import logging;
from HTMLParser import HTMLParser;

class AnnotationFileParser(HTMLParser):

	def __init__(self, inputType="annotation"):
		HTMLParser.__init__(self);
		self.utterances = dict();
		self.utteranceId = "0";
		self.inputType = inputType;
		

	def handle_starttag(self, tag, attrs):
		if tag=="utterance":
			attrs = dict(attrs);
			self.utteranceId = attrs["id"];
    
	def handle_endtag(self, tag):
		pass;
    
	def handle_data(self, data):
		d = data.replace("\n","").replace("\r","").replace("\t","");
		if d!="":
			self.utterances[self.utteranceId] = dict();
			self.utterances[self.utteranceId][self.inputType] = filter(None, d.split(" "));
		#print "Data", data.replace("\n","").replace("\r","").replace("\t","")

	def getData(self):
		return self.utterances;

	def resetParser(self):
		self.utterances = dict();
		self.utteranceId = "0";

class FIRE2015:

	def __init__(self):
		self.logger = Logging.getLogger("evaluator");

	def matchFiles(self, submissionFile, gtFile):
		annotationType = "annotation";
		annotationFileParser = AnnotationFileParser(annotationType);
		annotationFileParser.feed(open(submissionFile).read());
		subData = annotationFileParser.getData();
		annotationFileParser.resetParser();
		annotationFileParser.feed(open(gtFile).read());
		gtData = annotationFileParser.getData();

		
		toksInGt = dict();
		toksLookup = dict();
		cnt = 0;
		for k,v in gtData.items():
			gtToks = v[annotationType];
			for tok in gtToks:
				if not tok in toksInGt:
					toksInGt[tok] = cnt;
					toksLookup[cnt] = tok;
					cnt += 1;
		toksInGt["Others"] = cnt;
		toksLookup[cnt] = "Others";

		self.logger.debug("Tokens in GT %s" %(",".join(toksInGt.keys())))

		uniqToks = len(toksInGt);

		confusionMatrix = np.zeros([uniqToks, uniqToks])

		utterances = 0;
		utterancesCorrect = 0;
		utterancesProblem = 0;

		tokens = 0;
		tokensCorrect = 0;

		for k,v in subData.items():
			utterances += 1;
			if not k in gtData:
				self.logger.error("utternace %s does not exist in GT" %(k));
				utterancesProblem += 1;
				continue;
			gt = gtData[k][annotationType];
			sub = v[annotationType];
			if len(gt)!=len(sub):
				self.logger.error("Submission utterance %s has incorrect number of labels (GT: %d, Sub: %d)" %(k, len(gt), len(sub)));
				utterancesProblem += 1;
				continue;
			if gt==sub:
				utterancesCorrect += 1;

			tokens += len(gt);
			for gtToken, subToken in zip(gt, sub):
				if gtToken==subToken:
					tokensCorrect += 1;
				if not subToken in toksInGt:
					self.logger.debug("Submission utterance %s has token %s that is not in GT" %(k, subToken));
					subToken = "Others";

				confusionMatrix[toksInGt[gtToken], toksInGt[subToken]] += 1;

		self.logger.debug(confusionMatrix);
		
		precisionByClass = np.zeros([uniqToks, 1]);
		recallByClass = np.zeros([uniqToks, 1]);
		fmeasureByClass = np.zeros([uniqToks, 1]);

		tot = confusionMatrix.sum();
		for ii in xrange(0, uniqToks):
			tp = confusionMatrix[ii,ii];
			fp = confusionMatrix[ii,:].sum() - confusionMatrix[ii,ii];
			fn = confusionMatrix[:,ii].sum() - confusionMatrix[ii,ii];
			tn = tot - (tp + fp + fn);

			precisionByClass[ii] = tp/(tp + fp + 1e-10);
			recallByClass[ii] = tp/(tp + fn + 1e-10);
			fmeasureByClass[ii] = 2*precisionByClass[ii]*recallByClass[ii]/(precisionByClass[ii] + recallByClass[ii] + 1e-10)

		stats = dict();
		stats["utterances"] = utterances;
		stats["utterancesCorrect"] = utterancesCorrect;
		stats["utterancesProblem"] = utterancesProblem;
		stats["utterancesAccuracy"] = utterancesCorrect*100/(utterances + 1e-10);
		stats["tokens"] = tokens;
		stats["tokensCorrect"] = tokensCorrect;
		stats["tokensAccuracy"] = tokensCorrect*100/(tokens + 1e-10);

		for ii in xrange(0, uniqToks):
			stats["precision %s" %(toksLookup[ii])] = precisionByClass[ii][0];
			stats["recall %s" %(toksLookup[ii])] = recallByClass[ii][0];
			stats["f-measure %s" %(toksLookup[ii])] = fmeasureByClass[ii][0];
		#print stats;
		return stats;



class Logging:

	@staticmethod
	def defaults():
		logging.basicConfig(level=logging.DEBUG, format='%(name)-12s: %(asctime)s - %(levelname)s - %(message)s', datefmt='%m/%d/%Y %I:%M:%s %p');	
		return logging.getLogger("FIRE2015.root");

	@staticmethod
	def getLogger(name):
		return logging.getLogger("FIRE2015.%s" %(name));


def mergeStats(overall, chunk):
	for k,v in chunk.items():
		if not k in overall:
			overall[k] = [v];
		else:
			overall[k].append(v);
	
	overallK = set(overall.keys()).difference(set(chunk.keys()));
	for k in overallK:
		overall[k].append(0.0);

def computeAggregates(overall):
	averages = dict();
	stds = dict();
	median = dict();
	for k,v in overall.items():
		if k!="filename":
			npv = np.array(v);
			averages[k] = npv.mean();
			stds[k] = npv.std();
			median[k] = np.median(npv);
	overall["filename"].append("Aggregate_Mean");
	overall["filename"].append("Aggregate_Std_dev");
	overall["filename"].append("Aggregate_Median");

	for k,v in averages.items():
		overall[k].append(v);

	for k,v in stds.items():
		overall[k].append(v);

	for k,v in median.items():
		overall[k].append(v);



if __name__=="__main__":

	logger = Logging.defaults();

	parser = OptionParser()
	parser.add_option("-g", "--gt-dir", dest="gtDir", default="gtTest/", 
	                  help="Directory contining labelled data")
	parser.add_option("-s", "--sub-dir", dest="subDir", default="subTest/",
	                  help="Directory containing the submissions")
	parser.add_option("-o", "--output-file", dest="outputFile", default="performance.txt",
	                  help="Output file to write to...")

	(options, args) = parser.parse_args()

	logger.debug("Configuration")
	logger.debug("GT Dir %s" %(options.gtDir));
	logger.debug("Sub Dir %s" %(options.subDir));

	if not os.path.exists(options.gtDir):
		logger.error("GT Dir does not exist");

	if not os.path.exists(options.subDir):
		logger.error("Sub Dir does not exist");

	subFiles = glob.glob("%s%s*txt" %(options.subDir, os.sep));

	overallStats = dict();

	for subFile in subFiles:
		filename = os.path.basename(subFile);
		gtFile = "%s%s%s" %(options.gtDir, os.sep, filename);
		if not os.path.exists(gtFile):
			logger.error("%s does not have a corresponding GT file" %(filename));
			continue;
		logger.debug("Processing file %s" %(filename));
		fire2015 = FIRE2015();
		stats = fire2015.matchFiles(subFile, gtFile);
		stats["filename"] = filename;
		mergeStats(overallStats, stats);
	
	computeAggregates(overallStats);

	logger.debug("\n");
	ofid = open(options.outputFile,'a');
	print>>ofid, "%18s\t%s" %("filename", "\t".join([str(n) for n in overallStats["filename"]]));
	sortedKeys = sorted(overallStats.keys());
	for k in sortedKeys:
		v = overallStats[k];
		if k!="filename":
			print>>ofid, "%18s\t%s" %(k, "\t".join(["%2.4f" %(n) for n in v]));
	ofid.close();
