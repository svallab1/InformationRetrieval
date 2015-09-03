
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;

/* 
 * Author: Srinija Vallabhaneni
 * Email: svallab1@umbc.edu
 * Description: CMSC676 phase5
 * This program assumes that the output of phase2 is available as input folder
 */
public class Cluster {

	private static Table<String, String, Float> termWeights;
	private static  Map<String, HashMap<String, Float>> similarityMatrix;
	private static Map<String, Integer> clusterCounts = new HashMap<String, Integer>();
	private static String cluster1 =null,cluster2 = null;
	private static Map<String, Float> corpusCentroid = new HashMap<String, Float>();
	
	public static void main(String[] args) {
		if (args.length < 1) {
			System.out
					.println("Insufficient argument list.\n"
							+ "Please run the program with format ProgramName InputFilesDirectory.");
		}
		
		Calcwts ct = new Calcwts();
		try {
			termWeights = ct.getTermWeights(args);
		} catch (IOException e) {
			System.out.println(e.getMessage());
		
		}
		
		computeSimilarityMatrix();
		for(String fileName: termWeights.columnKeySet()){
			clusterCounts.put(fileName, 1);
		}
		computeCorpusCentroid();

		//find doc closest to centroid
		System.out.println("Closest doc to corpus centrod: "+findClosestDocToCorpusCentroid());
		while(findMaxSimilarPair() >= 0.4){
			System.out.println("C1: "+cluster1+"\tC2: "+cluster2+"\tsim: "+similarityMatrix.get(cluster1).get(cluster2));
			
			reComputeSimilarityMatrix(recalculateTermWeights(cluster1,cluster2));
			
		}
		System.out.println("similarity matrix final: "+similarityMatrix);
	}
	
	private static String findClosestDocToCorpusCentroid(){
		String closestFile = null;
		float maxSim = 0f;
		for(String fileName: termWeights.columnKeySet()){
			int termCountFileName = termWeights.column(fileName).size();
			Set<String> commonTerms = new HashSet<String>(termWeights.column(fileName).keySet());
			commonTerms.retainAll(corpusCentroid.keySet());
			float jacardSim = (float)commonTerms.size()/(float)(corpusCentroid.size()+termCountFileName-commonTerms.size());
			if(maxSim < jacardSim){
				closestFile = fileName;
			}
		}
		return closestFile;
	}
	
	private static String recalculateTermWeights(String c1, String c2){
		Map<String,Float> termWeightC1 = termWeights.column(c1);
		Map<String,Float> termWeightsC2 = termWeights.column(c2);
		Map<String,Float> termWeightC1C2 = new HashMap<String,Float>();
		int newClusterSize = clusterCounts.get(c1)+ clusterCounts.get(c2);
		for(String term: termWeightC1.keySet()){
			termWeightC1C2.put(term, clusterCounts.get(c1)*termWeightC1.get(term)/newClusterSize);
		}
		for(String term: termWeightsC2.keySet()){
			if(termWeightC1C2.containsKey(term)){
				float temp = termWeightC1C2.get(term);
				termWeightC1C2.put(term, temp+(clusterCounts.get(c2)*termWeightsC2.get(term)/newClusterSize));
			}else{
				termWeightC1C2.put(term, clusterCounts.get(c2)*termWeightsC2.get(term)/newClusterSize);
			}
		}
		clusterCounts.remove(c1);
		clusterCounts.remove(c2);
		String newClusterName = c1+"|"+c2;
		clusterCounts.put(newClusterName, newClusterSize);
		for(String term: termWeightC1C2.keySet()){
			termWeights.remove(term, c1);
			termWeights.remove(term, c2);
			termWeights.put(term, newClusterName, termWeightC1C2.get(term));
			
		}
		similarityMatrix.remove(c1);
		similarityMatrix.remove(c2);
		for(String temp: similarityMatrix.keySet()){
			if(similarityMatrix.get(temp).containsKey(c1)|| similarityMatrix.get(temp).containsKey(c2)){
				similarityMatrix.get(temp).remove(c1);
				similarityMatrix.get(temp).remove(c2);
			}
		}
		return newClusterName;
	}
	
	private static void reComputeSimilarityMatrix(String cName){

		
		Set<String> fileNames = new HashSet<String>(termWeights.columnKeySet());
		HashMap<String,Float> newSimMap = new HashMap<String, Float>();
		int termCountCluster = termWeights.column(cName).size();
		for(String fileName: fileNames){
			int termCountFileName = termWeights.column(fileName).size();
			Set<String> commonTerms = new HashSet<String>(termWeights.column(cName).keySet());
			commonTerms.retainAll(termWeights.column(fileName).keySet());
			float jacardSim = (float)commonTerms.size()/(float)(termCountCluster+termCountFileName-commonTerms.size());
			newSimMap.put(fileName, jacardSim);
		}
		similarityMatrix.put(cName, newSimMap);
	
	}
	
	private static void computeSimilarityMatrix(){
		similarityMatrix = new HashMap<String,HashMap<String,Float>>();
		Set<String> fileNames = new HashSet<String>(termWeights.columnKeySet());
		for(Iterator<String> filesIterator = fileNames.iterator(); filesIterator.hasNext();){
			//Calculate similarity wrt others
			String fileA = filesIterator.next();
			filesIterator.remove();
			Map<String, Float> fileAweights = termWeights.column(fileA);
			similarityMatrix.put(fileA, new HashMap<String, Float>());
			for(String fileB: fileNames){
				Map<String,Float> fileBweights = termWeights.column(fileB);
				ArrayList<String> termA = new ArrayList<String>(fileAweights.keySet());
				ArrayList<String> termB = new ArrayList<String>(fileBweights.keySet());
				ArrayList<String> termAB = new ArrayList<String>(termA);
				termAB.retainAll(termA);
				termAB.retainAll(termB);
				float jacardSim = (float)termAB.size()/(float)(termA.size()+termB.size() - termAB.size());
				similarityMatrix.get(fileA).put(fileB, jacardSim);
			}
		}
		System.out.println(similarityMatrix);
	}
	
	private static float findMaxSimilarPair(){
		cluster1 = cluster2 = null;
		Float simMax = 0f;
		for(String fileA: similarityMatrix.keySet()){
			if(cluster1 == null){
				cluster1 = fileA;
			}
			for(String fileB: similarityMatrix.get(fileA).keySet()){
				if(cluster2 == null){
					cluster2 = fileB;
				}
				if(fileA.equals(fileB)){
					continue;
				}
				float temp = similarityMatrix.get(fileA).get(fileB);
				if( temp > simMax){
					cluster1 = fileA;
					cluster2 = fileB;
					simMax = temp; 
				}
			}
			
		}
		return simMax;
		
	}
	
	private static void computeCorpusCentroid(){
		int corpusSize =termWeights.columnKeySet().size();
		for(String term: termWeights.rowKeySet()){
			float weight =0f;
			for( Map.Entry<String, Float> temp: termWeights.row(term).entrySet()){
				weight += temp.getValue();
			}
			corpusCentroid.put(term, weight/corpusSize);
		}
	}
}
