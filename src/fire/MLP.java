/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fire;

import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureVector;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import static fire.FIRE.inputFile;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

/**
 *
 * @author nageshbhattu
 */
public class MLP {
    public static void main(String[] args) throws FileNotFoundException, IOException{
        String inputFileName = "training.mall";
        //BufferedReader br = new BufferedReader(new FileReader(inputFileName));
        BufferedWriter bw = new BufferedWriter(new FileWriter("input.mlp"));
        InstanceList ilist = InstanceList.load(new File(inputFileName));
        Alphabet dataAlphabet = ilist.getDataAlphabet();
        Alphabet labelAlphabet = ilist.getTargetAlphabet();
        for(int ii = 0;ii<ilist.size();ii++){
            Instance instance = ilist.get(ii);
            FeatureVector fv = (FeatureVector) instance.getData();
            double[] features = new double[dataAlphabet.size()];
            for(int li = 0;li<fv.numLocations();li++){
                features[fv.indexAtLocation(li)] = fv.valueAtLocation(li);
            }
            StringBuffer bs = new StringBuffer("");
            for(int fi= 0;fi<features.length;fi++)
                bs.append(features[fi]+",");
            int[] labels = new int[labelAlphabet.size()];
            
            labels[instance.getLabeling().getBestIndex()] = 1;
            for(int li= 0;li<labels.length;li++)
                if(li==labels.length-1)
                    bs.append(labels[li]);
                else
                    bs.append(labels[li]+",");
            
            bw.write(bs.toString()+"\n");
        }
    }
}
