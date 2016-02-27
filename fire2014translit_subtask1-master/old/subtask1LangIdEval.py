#!/usr/bin/python
# FIRE 2014 Shared Task on Transliterated Search
# Subtask 1/Language Identification
# Evaluation script for language identification
#
# Gokul Chittaranjan, gokulchittaranjan@gmail.com

import sys;

import numpy as np;
from normalize import check_equivalence;
from utils import readAnnotationFile;

def printResult(results):
    
    outs = [];
    outs.append("Status\t%s" %(results["message"]));
    outs.append("Rows\t%d" %(results["rows"]));
    outs.append("Tokens\t%d" %(results["tokens"]));
    outs.append("Micro Average Accuracy\t%2.3f" %(results["micro_accuracy"]));
    outs.append("Macro Average Accuracy\t%2.3f" %(results["macro_accuracy"]));
    outs.append("Macro Average Precision\t%2.3f" %(results["p_macro"]));
    outs.append("Macro Average Recall\t%2.3f" %(results["r_macro"]));
    outs.append("Macro Average F-measure\t%2.3f" %(results["f_macro"]));
    for k in results["p_perClass"].keys():
        outs.append("\nClass %s\n=========" %(k));
        
        outs.append("Precision\t%2.3f" %(results["p_perClass"][k]));
        outs.append("Recall\t%2.3f" %(results["r_perClass"][k]));
        outs.append("F-measure\t%2.3f" %(results["f_perClass"][k]));
        #outs.append("Accuracy\t%2.3f" %(results["acc_perClass"][k]));
        outs.append("Tokens\t%d" %(results["tokens_perClass"][k]));
    outs.append("\nNo Mix\n=======");
    outs.append("Macro Average Precision\t%2.3f" %(results["p_noMix"]));
    outs.append("Macro Average Recall\t%2.3f" %(results["r_noMix"]));
    outs.append("Macro Average F-measure\t%2.3f" %(results["f_noMix"]));
    #outs.append("No Mix: Macro Average Accuracy\t%2.3f" %(results["macro_acc_noMix"]));
    outs.append("Micro Average Accuracy\t%2.3f" %(results["micro_acc_noMix"]));
    outs.append("Tokens\t%d" %(results["tokens_noMix"]));

    outs.append("\nNo NE\n=======");
    outs.append("Macro Average Precision\t%2.3f" %(results["p_noNe"]));
    outs.append("Macro Average Recall\t%2.3f" %(results["r_noNe"]));
    outs.append("Macro Average F-measure\t%2.3f" %(results["f_noNe"]));
    #outs.append("No NE: Macro Average Accuracy\t%2.3f" %(results["macro_acc_noNe"]));
    outs.append(" Micro Average Accuracy\t%2.3f" %(results["micro_acc_noNe"]));
    outs.append("Tokens\t%d" %(results["tokens_noNe"]));

    outs.append("\nNo Mix and NE\n==========")
    outs.append("Macro Average Precision\t%2.3f" %(results["p_noMixAndNe"]));
    outs.append("Macro Average Recall\t%2.3f" %(results["r_noMixAndNe"]));
    outs.append("Macro Average F-measure\t%2.3f" %(results["f_noMixAndNe"]));
    #outs.append("No Mix and NE: Macro Average Accuracy\t%2.3f" %(results["macro_acc_noMixAndNe"]));
    outs.append("Micro Average Accuracy\t%2.3f" %(results["micro_acc_noMixAndNe"]));
    outs.append("Tokens\t%d" %(results["tokens_noMixAndNe"]));
    outs.append("\nTransliterations\n===============");
    outs.append("Correct transliterations: %d" %(results["correctTranslits"]));
    outs.append("No transliterations in GT: %d" %(results["noTranslits"]));

    return "\n".join(outs);

def evaluateResult(gtfile, testfile):
    gtData = readAnnotationFile(gtfile);
    testData = readAnnotationFile(testfile);

    results = dict();
    results["micro_accuracy"] = -1;
    results["macro_accuracy"] = -1;
    results["p_macro"] = -1;
    results["r_macro"] = -1;
    results["f_macro"] = -1;
    
    results["p_perClass"] = dict();
    results["r_perClass"] = dict();
    results["f_perClass"] = dict();
    results["acc_perClass"] = dict();
    results["tokens_perClass"] = dict();
    
    results["p_noMix"] = -1;
    results["r_noMix"] = -1;
    results["f_noMix"] = -1;
    results["acc_noMix"] = -1;
    results["tokens_noMix"] = -1;
    results["micro_acc_noMix"] = -1;

    results["p_noNe"] = -1;
    results["r_noNe"] = -1;
    results["f_noNe"] = -1;
    results["macro_acc_noNe"] = -1;
    results["tokens_noNe"] = -1;
    results["micro_acc_noNe"] = -1;


    results["p_noMixAndNe"] = -1;
    results["r_noMixAndNe"] = -1;
    results["f_noMixAndNe"] = -1;
    results["macro_acc_noMixAndNe"] = -1;
    results["tokens_noMixAndNe"] = -1;
    results["micro_acc_noMixAndNe"] = -1;

    results["rows"] = -1;
    results["tokens"] = -1;
    results["message"] = "No Results.";
    results["correctTranslits"] = -1;
    results["noTranslits"] = -1;


    if len(gtData)!=len(testData):
        results["message"] += "GT and test files do not have the same number of rows.\n";
        return results;

    labels = [];
    for gtRow in gtData:
        for token in gtRow:
            if token[1]=="":
                continue;
            if not token[1] in labels:
                labels.append(token[1]);
    for testRow in testData:
        for token in testRow:
            if token[1]=="":
                continue;
            if not token[1] in labels:
                results["message"] += "WARNING: Test data contained a label %s that was not in GT.\n" %(token[1]);
                labels.append(token[1]);
    #if not "NE_" in labels:
    #    labels.append("NE_?");
    
    labels = sorted(labels);
    labels.append("OTHER/INVALID");
    #print labels
    nLab = len(labels);
    
    rcnt = 0;
    confMatrix = np.zeros([nLab, nLab]);
    tot = 0;

    correctTranslits = 0;
    noTranslits = 0;

    for gtRow, testRow in zip(gtData, testData):
        rcnt += 1;
        if len(gtRow) != len(testRow):
            results["message"] = "Row %s does not match." %(rcnt);
            print "GT: "
            print gtRow
            print "Test: "
            print testRow
            return results;
        
        for gtTok, testTok in zip(gtRow, testRow):
            tot += 1;
            if gtTok[1]=="":
                continue;
            
            gtIdx = labels.index(gtTok[1]);
            
            if not testTok[1] in labels:
                results["message"] = "Test data contains label %s in row %s, which is not defined in GT" %(testTok[1], rcnt);
                confMatrix[nLab-1][gtIdx] += 1;
                continue;
                #return results;
            if (gtTok[1]=="NE_GENERIC"):
                if testTok[1][0:3]=="NE_":
                    testTok[1] = "NE_GENERIC";
            testIdx = labels.index(testTok[1]);
            if gtTok[2]!="":
                if gtTok[2]==testTok[2] or check_equivalence(gtTok[2].encode('utf-8'), testTok[2].encode('utf-8')):
                    correctTranslits += 1;
                noTranslits += 1;
            confMatrix[testIdx][gtIdx] += 1;
           # tot += 1;
    
    confMatrix_tr = confMatrix.transpose();
    
    corr = 0.0;
    tp = np.zeros([nLab,1]);
    tn = np.zeros([nLab,1]);
    fp = np.zeros([nLab,1]);
    fn = np.zeros([nLab,1]);
    
    p = np.zeros([nLab,1]);
    r = np.zeros([nLab,1]);
    f = np.zeros([nLab,1]);
    acc = np.zeros([nLab,1]);

    p_perclass = dict();
    r_perclass = dict();
    f_perclass = dict();
    acc_perclass = dict();

    tokens_perclass = dict();

    p_noMix = [];
    r_noMix = [];
    f_noMix = [];
    acc_noMix = [];
    tokens_noMix = 0.0;
    corr_noMix = 0.0;

    p_noNe = [];
    r_noNe = [];
    f_noNe = [];
    acc_noNe = [];
    tokens_noNe = 0.0;
    corr_noNe = 0.0;

    p_noMixAndNe = [];
    r_noMixAndNe = [];
    f_noMixAndNe = [];
    acc_noMixAndNe = [];
    tokens_noMixAndNe = 0.0;
    corr_noMixAndNe = 0.0;


    for ii in xrange(0, nLab):
        corr += confMatrix[ii][ii];

        tp[ii] = confMatrix[ii][ii];
        fp[ii] = confMatrix[ii].sum() - tp[ii];
        fn[ii] = confMatrix_tr[ii].sum() - tp[ii];
        tn[ii] = confMatrix.sum() - (tp[ii]+fp[ii]+fn[ii]);
        if tp[ii]==0 and fp[ii]==0:
            p[ii] = 0;
        else:
            p[ii] = tp[ii]/(tp[ii] + fp[ii]);
        if (tp[ii]==0 and fn[ii]==0):
            r[ii] = 0;
        else:
            r[ii] = tp[ii]/(tp[ii]+fn[ii]);
        if (p[ii]==0 and r[ii]==0):
            f[ii] = 0;
        else:
            f[ii] = 2*p[ii]*r[ii]/(p[ii]+r[ii]);
        if np.isnan(f[ii]):
            f[ii] = 0;
        if (tot>0):
            acc[ii] = (tp[ii]+tn[ii])/tot;
        else:
            acc[ii] = 0;
        p_perclass[labels[ii]] = p[ii];
        r_perclass[labels[ii]] = r[ii];
        f_perclass[labels[ii]] = f[ii];
        acc_perclass[labels[ii]] = acc[ii];
        tokens_perclass[labels[ii]] = tp[ii] + fn[ii];
        isNE = False;
        isMix = False;
        if labels[ii][0:3]=="NE_":
            isNE = True;
        if labels[ii].upper()=="MIX":
            isMix = True;
        if not isNE:
            p_noNe.append(p[ii]);
            r_noNe.append(r[ii]);
            f_noNe.append(f[ii]);
            acc_noNe.append(acc[ii]);
            tokens_noNe += tp[ii] + fn[ii];
            corr_noNe += confMatrix[ii][ii];
        if not isMix:
            p_noMix.append(p[ii]);
            r_noMix.append(r[ii]);
            f_noMix.append(f[ii]);
            acc_noMix.append(acc[ii]);
            tokens_noMix += tp[ii] + fn[ii];
            corr_noMix += confMatrix[ii][ii];
        if (not isNE) and (not isMix):
            p_noMixAndNe.append(p[ii]);
            r_noMixAndNe.append(r[ii]);
            f_noMixAndNe.append(f[ii]);
            acc_noMixAndNe.append(acc[ii]);
            tokens_noMixAndNe += tp[ii] + fn[ii];
            corr_noMixAndNe += confMatrix[ii][ii];

    results["rows"] = rcnt;
    results["tokens"] = tot;
    if tot>0:
        results["micro_accuracy"] = corr/tot;
    else:
        results["micro_accuracy"] = 0;
    results["macro_accuracy"] = acc.mean();
    
    results["p_macro"] = p.mean();
    results["r_macro"] = r.mean();
    results["f_macro"] = f.mean();

    results["p_perClass"] = p_perclass;
    results["r_perClass"] = r_perclass;
    results["f_perClass"] = f_perclass;
    results["acc_perClass"] = acc_perclass;
    results["tokens_perClass"] = tokens_perclass;

    results["p_noMix"] = sum(p_noMix)/len(p_noMix);
    results["r_noMix"] = sum(r_noMix)/len(r_noMix);
    results["f_noMix"] = sum(f_noMix)/len(f_noMix);
    results["macro_acc_noMix"] = sum(acc_noMix)/len(acc_noMix);
    results["tokens_noMix"] = tokens_noMix;
    if (tokens_noMix):
        results["micro_acc_noMix"] = corr_noMix/tokens_noMix;
    else:
        results["micro_acc_noMix"] = 0;

    results["p_noNe"] = sum(p_noNe)/len(p_noNe);
    results["r_noNe"] = sum(r_noNe)/len(r_noNe);
    results["f_noNe"] = sum(f_noNe)/len(f_noNe);
    results["macro_acc_noNe"] = sum(acc_noNe)/len(acc_noNe);
    results["tokens_noNe"] = tokens_noNe;
    if (tokens_noNe>0):
        results["micro_acc_noNe"] = corr_noNe/tokens_noNe;
    else:
        results["micro_acc_noNe"] = 0;

    results["p_noMixAndNe"] = sum(p_noMixAndNe)/len(p_noMixAndNe);
    results["r_noMixAndNe"] = sum(r_noMixAndNe)/len(r_noMixAndNe);
    results["f_noMixAndNe"] = sum(f_noMixAndNe)/len(f_noMixAndNe);
    results["macro_acc_noMixAndNe"] = sum(acc_noMixAndNe)/len(acc_noMixAndNe);
    results["tokens_noMixAndNe"] = tokens_noMixAndNe;
    if (tokens_noMixAndNe):
        results["micro_acc_noMixAndNe"] = corr_noMixAndNe/tokens_noMixAndNe;
    else:
        results["micro_acc_noMixAndNe"] = 0;

    results["correctTranslits"] = correctTranslits;
    results["noTranslits"] = noTranslits;
    
    results["message"] = "Evaluation successful";

    return results

if __name__=="__main__":
    
    gtfile = sys.argv[1];
    testfile = sys.argv[2];
    
    
    results = evaluateResult(gtfile, testfile);
    p = printResult(results);
    print p;
