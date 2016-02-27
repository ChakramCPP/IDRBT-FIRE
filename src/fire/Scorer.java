/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fire;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

/**
 *
 * @author nageshbhattu
 */

public class Scorer {
    HashSet<Integer> queryids = new HashSet<>();
    HashSet<Integer> docids = new HashSet<>();
    HashMap<Pair,Integer> queryRels = new HashMap<>();
    HashMap<Pair,Integer> runRels = new HashMap<>();
    void loadQRELFile(String qrelFile) throws FileNotFoundException, IOException{
        BufferedReader br = new BufferedReader(new FileReader(qrelFile));
        String line;
        while((line =br.readLine())!=null){
            String[] words = line.split("\\s+");
            Pair p = new Pair(words[0],words[2]);
            queryRels.put(p, Integer.parseInt(words[3]));
        }
        
    }
    void loadRunFile(String qrelFile) throws FileNotFoundException, IOException{
        BufferedReader br = new BufferedReader(new FileReader(qrelFile));
        String line;
        while((line =br.readLine())!=null){
            String[] words = line.split("\\s+");
            Pair p = new Pair(words[0],words[2]);
            Integer rel = 0;
            if(queryRels.containsKey(p))
                rel = queryRels.get(p);
            Pair p2 = new Pair(words[0],words[3]);
            runRels.put(p2, rel);
        }
        
    }
    void computeMAPMRR(){
        for(Pair p:queryRels.keySet()){
            
        }
    }
}
class Pair{
    String queryid;
    String docid;
    public Pair(String qid, String did){
        queryid = qid;
        docid = did;
    }
    @Override
    public boolean equals(Object o) {
         if (o == this) {
            return true;
        }
 
        /* Check if o is an instance of Complex or not
          "null instanceof [type]" also returns false */
        if (!(o instanceof Pair)) {
            return false;
        }
        Pair p = (Pair) o;
        
        return p.docid.equals(docid) && p.queryid.equals(docid);
    }
    
}