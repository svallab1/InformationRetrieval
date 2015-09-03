package com.srinija.ir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;

public class Retrieve {

	public static void main(String[] args) {
		if(args.length < 1){
			System.out.println("Please mention the query terms."+args.length);
			return;
		}
		String query = "";
		for(int i = 0; i< args.length;i++){
			args[i] = args[i].toLowerCase();
			query = query.concat(args[i]+" ");
		}
		System.out.println("query: "+query);
		
		File postingsFile = new File("resources/postingsFile.txt");
		File dictionaryFile = new File("resources/dictionaryFile.txt");

		//Check for inverted files. If they do not exist, generate them.
		if(!(postingsFile.exists() && dictionaryFile.exists())){
			System.out.println("Generating index files");
			Index indexer = new Index();
			indexer.indexCorpus(new String[]{"files",""});
		}
		
		List<String> dictionaryArray = null;
		try {
			dictionaryArray = Files.readAllLines(Paths.get(dictionaryFile.getAbsolutePath()),Charset.defaultCharset());
		} catch (IOException e) {
			System.out.println("Error reading dictionary file \n"+e.getMessage());
		}
		//For each of the query terms, extract the postings file information and number of occurrences.
		Map<String,int[]> termInfoMap = new HashMap<String, int[]>();
		for(int i = 0;i < args.length;i++){
			if(dictionaryArray.contains(args[i])){
				int termIndex = dictionaryArray.indexOf(args[i]);
				int documentCount = Integer.parseInt(dictionaryArray.get(termIndex+1));
				int postingLocation = Integer.parseInt(dictionaryArray.get(termIndex+2));
				termInfoMap.put(args[i], new int[]{documentCount,postingLocation});
			}
		}
		dictionaryArray.clear(); dictionaryArray = null;
		
		List<String> queryTermArray = Arrays.asList(args);
		Map<String, Float> queryTermWeights = new HashMap<String,Float>();
		//Calculate tf*idf for each term of the query.
		//Assuming N = 503 constant.
		for(String term: termInfoMap.keySet()){
			int df = termInfoMap.get(term)[0];
			Float idf = new Float(Math.log(503/df));
			Float tf = (float)Collections.frequency(queryTermArray, term)/queryTermArray.size();
			queryTermWeights.put(term, Float.valueOf(tf*idf));
		}
		
		// Get the information on postings file for each query term.
		Table<String,String, Float> results = TreeBasedTable.create();
		List<String> postingsArray = loadPostingsFile(postingsFile);
		Map<String,ArrayList<Float>> docWtArrayMap = new HashMap<>();
		for(String term: queryTermWeights.keySet()){
			ArrayList<Float> docWtArray = new ArrayList<Float>();
			for(int j = 0;j < termInfoMap.get(term)[0];j++){
				String documentWeight = postingsArray.get(termInfoMap.get(term)[1]+j);
				String[] data = documentWeight.split(",");
				results.put(term, data[0], Float.valueOf(data[1].trim())*queryTermWeights.get(term));
				if(!docWtArrayMap.containsKey(data[0])){
					docWtArrayMap.put(data[0], new ArrayList<Float>());
				}
				docWtArrayMap.get(data[0]).add(Float.valueOf(data[1].trim()));
			}
		}
		
		float queryWtMagnitude = 0f;
		//magnitude of query terms in the query
		for(String term:queryTermWeights.keySet()){
			queryWtMagnitude += queryTermWeights.get(term)*queryTermWeights.get(term);
		}
		queryWtMagnitude = (float)Math.sqrt(queryWtMagnitude);
		Map<String,Float> docRanking = new HashMap<String,Float>();
		for(String file: results.columnKeySet()){
			float weight = 0f;
			float docWtMagnitude = 0f;
			//magnitude of query terms in the document.
			for(int i = 0;i<docWtArrayMap.get(file).size();i++){
				docWtMagnitude += docWtArrayMap.get(file).get(i)*docWtArrayMap.get(file).get(i);
			}
			docWtMagnitude = (float)Math.sqrt(docWtMagnitude);
			for(String term: results.column(file).keySet()){
				weight += (results.get(term, file));
			}
			weight /=docWtMagnitude;
			weight /= queryWtMagnitude;
			docRanking.put(file, weight);
		}
		if(docRanking.isEmpty()){
			System.out.println("No search results found");
			return;
		}
		
		ValueComparator bvc =  new ValueComparator(docRanking);
        TreeMap<String,Float> sorted_map = new TreeMap<String,Float>(bvc);
        sorted_map.putAll(docRanking);
		Iterator<String> it = sorted_map.keySet().iterator();
		
        for(int i = 0; it.hasNext() && i<10;i++){
        	String file = it.next();
        	System.out.println(file+"\t"+docRanking.get(file));
        }
	}
	
	private static List<String> loadPostingsFile(File file){
		try {
			return Files.readAllLines(Paths.get(file.getAbsolutePath()), Charset.defaultCharset());
		} catch (IOException e) {
			System.out.println("Error reading dictionary file \n"+e.getMessage());
		}
		return null;
		
	}
	
}
class ValueComparator implements Comparator<String> {

    Map<String, Float> base;
    public ValueComparator(Map<String, Float> base) {
        this.base = base;
    }

    // Note: this comparator imposes orderings that are inconsistent with equals.    
    public int compare(String a, String b) {
        if (base.get(a) >= base.get(b)) {
            return -1;
        } else {
            return 1;
        } // returning 0 would merge keys
    }
}
