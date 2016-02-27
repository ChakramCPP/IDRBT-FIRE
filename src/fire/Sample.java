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
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import es.upv.nlel.translit.TransliterationMiner;

public class Sample{
	public static void main(String[] args) throws Exception {
		TransliterationMiner tm = new TransliterationMiner();
		int[] g = {1,2};
		tm.setGrams(g);
		
		
		tm.loadModel1Network("obj/fineenrsmweights20linear", true, "linear");
		System.out.println("[info] Autoencoder Loaded.");
		tm.loadFeatureIndex("obj/feature-id-model1.txt");
		System.out.println("[info] Feature Index Loaded.");
		
		tm.loadData("obj/termIndexModel1Songs.obj","obj/projectedDataModel1Songs.obj","obj/lexicon.txt", 1);	


		System.out.println("[info] Data Loaded.\n\n");
	
		while(true) {
			Scanner in = new Scanner(System.in);
			System.out.print("Input Term: ");
			String input = in.nextLine();
			/** topTerms Parameters
			 * String - the input term for which variations are required
			 * String - distance metric ("cosine" or "euclidean")
			 * double - similarity threshold (normally 0.95 works pretty well)
			 * boolean - verbose (if true, the variants will be printed on the console)
			 */
			
			List<String> topTerms = tm.topTerms(input, "cosine", 0.95, false);
			for(String s: topTerms) {
				System.out.println(s);
			}
		}
	}
}