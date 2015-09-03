/*
 * Program to compute Term weights for a given Corpus of documents 
 * Author: Srinija Vallabhaneni 
 * UMBC ID: svallab1
 * */
package com.srinija.ir;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;

public class Calcwts {
	private  File outputFolder = null;
	private  File inputFolder = null;
	private  ArrayList<String> stopWords  = null;
	private  Table<String, String, Float> termTable = null ;
	private  float fileCount = 0;
	
	public Table<String,String, Float> getTermWeights(String[] args) throws IOException {
		inputFolder = new File("resources/" + args[0]);
		outputFolder = new File("resources/"+args[1]);
		loadStopWords();
		try{
			if(!outputFolder.exists()){
				boolean result = outputFolder.mkdir();
				if(!result){
					System.out.println("Couldnot create output directory. Please check for permissions");
					return null;
				}
			}
		}
		catch(Exception e ){
			System.out.println(e.getMessage());
		}
		termTable = TreeBasedTable.create();
		fileCount = inputFolder.listFiles().length;
		// Pre-process each file and load it into the table
		for (File inputFile : inputFolder.listFiles()) {
			readFile(inputFile,inputFile.getName().replace("html", "txt"));
		}
		//Compute term weights
		termWeight();
		return termTable;
		
	}

	/*
	 * Function to Load the stop words list from a file into an array
	 * */
	private void loadStopWords() throws IOException {
		stopWords = (ArrayList<String>) Files.readAllLines(Paths.get("resources/stoplist.txt"), StandardCharsets.US_ASCII);
		return;
	}

	/*
	 * Function to read and tokenize each file*/
	public  void readFile(File fileName,String outputFile) {
		FileReader in = null;
		try {
			in = new FileReader(fileName);
			BufferedReader buff = new BufferedReader(in);
			String fileContent = new String(Files.readAllBytes(Paths.get(fileName.getAbsolutePath())));

			fileContent = removeNonText(fileContent);
			List<String> wordList =  Arrays.asList(fileContent.toLowerCase().split("\\s"));
			ArrayList<String> wordArrayList = new ArrayList<String>(wordList);
			try{
				// For removing any strings of length 1;
				for (Iterator<String> iter = wordArrayList.iterator(); iter.hasNext();) {
				      String s = iter.next();
				      if(s.length()==1){
				    	  iter.remove();
				      }
				    }

				wordArrayList.removeAll(Arrays.asList(null,""));
				wordArrayList.removeAll(stopWords);
				
		
			}catch(UnsupportedOperationException ue){
				System.out.println(ue.getMessage());
			}
			
			buff.close();
			in.close();
			loadTable(wordArrayList,outputFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException i) {
			System.out.println(i.getMessage());
		}
	}
	
	/*
	 * Function to remove HTML tags and other non alpha numeric characters.
	 * */
	public  String removeNonText(String text) {
		text = text.replaceAll("<[/!]?[-]*[a-zA-Z \"#=0-9.:/]*[\"-]*>", " "); // Complete tags
		
		text = text.replaceAll("<[/!]?[-]*[a-zA-Z \"#=0-9.:/]*[\"-]*", " "); // Open tags
		
		text = text.replaceAll("[/!]?[-]*[a-zA-Z \"#=0-9.:/]*[\"-]*>", " "); // Closing tags
		// Remove the following special characters |,'[]"
		text = text.replaceAll("\\|*[,'\\[\\]\";]*", "");
		// Remove &,acute;,#146; and other special characters.
		//text = text.replaceAll("&*(&#146)*_*%*=*\\(\\)", "");
		text = text.replaceAll("&#146", "");
		
		Pattern phonetic = Pattern.compile("&(.)acute");
		Matcher matcher = phonetic.matcher(text);
		if (matcher.find()) {
			text = text.replaceAll("&.acute", matcher.group(1));
		}
		// Remove period from sentences but not from URLs.
		 Pattern periodPattern = Pattern.compile("([a-z])\\.\\s");
		 matcher = periodPattern.matcher(text);
		if (matcher.find()) {
			text = text.replaceAll("([a-z])\\.\\s", matcher.group(1));
		}
		text = text.replaceAll("\\p{P}", " ");

		return text;
	}
	
	/*
	 * Populates the TDM after each file processing
	 * Assuming the files are processed in numerical order, 
	 * the location of the first record for a word in the postings file occurs when the row size is 0.
	 * */
	private  void loadTable(ArrayList<String> wordArray, String fileName){
		HashSet<String> uniqueSet = new HashSet<String>(wordArray);
		float arrayLength = wordArray.size();
		for (String word : uniqueSet) {
			termTable.put(word,fileName,new Float(Collections.frequency(wordArray, word)/arrayLength));
		}
	 return;
	}
	
	/*
	 * Function for processing the table to give term weights.
	 * */
	private  float termWeight(){
		Set<String> terms = termTable.rowKeySet();
		for (Iterator<String> iter = terms.iterator(); iter.hasNext();) {
			String term = iter.next();
			if (termTable.row(term).size() == 1) {
				iter.remove();
				termTable.row(term).clear();
			} else {

				double idf = (double) (Math.log1p(fileCount
						/ termTable.row(term).size()) / Math.log(2));
				Map<String, Float> filesWithTerm = termTable.row(term);
				for (String file : filesWithTerm.keySet()) {
					termTable.put(term, file,
							(float) (filesWithTerm.get(file) * idf));
				}
			}
		}
		return 0;
		
	}


}