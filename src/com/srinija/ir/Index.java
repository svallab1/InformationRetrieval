package com.srinija.ir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import com.google.common.collect.Table;

public class Index {

	private  Table<String, String, Float> termTable;
	public void indexCorpus(String[] args){
		if(args.length < 2){
			System.out.println("Insufficient argument list.\n"
					+ "Please run the program with format ProgramName InputFilesDirectory OutputFilesDirectory.");
		}
		Calcwts calcwts = new Calcwts();
		try {
			termTable = calcwts.getTermWeights(args);
			if(termTable != null){
				generatePostingsFile(args);
			}
		} catch (IOException e) {
			System.out.println("Unable to read/write files.");
			e.printStackTrace();
		}
	}
	
	private  void generatePostingsFile(String[] args){
		File postingsFile = new File("resources/"+args[1]+"/postingsFile.txt");
		File dictionaryFile = new File("resources/"+args[1]+"/dictionaryFile.txt");
		
		FileWriter fileWriter, dictionaryWriter;
		long lineCounter = 1;  // Starting position in postings file.

		try {
			if(!postingsFile.exists()){
				postingsFile.createNewFile();
			}
			if(!dictionaryFile.exists()){
				dictionaryFile.createNewFile();
			}
			fileWriter = new FileWriter(postingsFile);
			dictionaryWriter = new FileWriter(dictionaryFile);

			for (String word : termTable.rowKeySet()) {
				Map<String, Float> wordMap = termTable.row(word);
				
				dictionaryWriter.write(word + "\n");
				dictionaryWriter.write(wordMap.size()+"\n");
				dictionaryWriter.write(lineCounter+"\n");
				for (String file : wordMap.keySet()) {
					fileWriter.write(file + ",\t" + wordMap.get(file)+"\n");
					lineCounter++;
				}

			}
			fileWriter.close();
			dictionaryWriter.close();
		} catch (IOException e) {
			System.out.println("Unable to open file: postingsFile.txt");
			e.printStackTrace();
		}
		
	}
}