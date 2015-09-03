package com.srinija.ir;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Tokenizer {
	private static File outputFolder = null;
	private static File inputFolder = null;
	private static ArrayList<String> completeList = new ArrayList<String>();
	public static void main(String[] args) throws IOException {
		inputFolder = new File("resources/" + args[0]);
		outputFolder = new File("resources/"+args[1]);
		
		try{
			if(!outputFolder.exists()){
				boolean result = outputFolder.mkdir();
				if(!result){
					System.out.println("Couldnot create output directory. Please check for permissions");
					return;
				}
			}
		}
		catch(Exception e ){
			System.out.println(e.getMessage());
		}
		for (File inputFile : inputFolder.listFiles()) {
			readFile(inputFile,inputFile.getName().replace("html", "txt"));
		}
		executeShellScript("sh ./bashscript.sh "+outputFolder.getName());

	}

	private static void executeShellScript(String command) {
		System.out.println("Calling the sort script");
		Process p = null;
		try{
			p = Runtime.getRuntime().exec(command, null,new File(outputFolder.getParent()));
			p.waitFor();
			
			BufferedReader scriptOutput = new BufferedReader(new InputStreamReader(p.getInputStream()));

			String outputLine = "";
			while((outputLine = scriptOutput.readLine()) != null){
				System.out.println(outputLine);
			}
		}catch (Exception e) {
			System.out.println(e.getMessage());
		}
		p.destroy();
	}

	public static void readFile(File fileName,String outputFile) {
		FileReader in = null;
		FileWriter out = null;
		try {
			in = new FileReader(fileName);
			out = new FileWriter(outputFolder.getAbsolutePath()+"/"+outputFile);
			BufferedReader buff = new BufferedReader(in);
			String fileContent = new String(Files.readAllBytes(Paths.get(fileName.getAbsolutePath())));

			fileContent = removeNonText(fileContent);
			List<String> wordList =  Arrays.asList(fileContent.toLowerCase().split("\\s"));
			ArrayList<String> wordArrayList = new ArrayList<String>(wordList);
			try{
				wordArrayList.removeAll(Arrays.asList(null,""));
			}catch(UnsupportedOperationException ue){
				System.out.println(ue.getMessage());
			}
			for (String string : wordArrayList) {
				if (string != "") {
					out.write(string + "\n");
				}
			}
			completeList.addAll(wordArrayList);
			
			buff.close();
			out.close();
			in.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException i) {
			System.out.println(i.getMessage());
		}
	}
	
	public static String removeNonText(String text) {
		text = text.replaceAll("<[/!]?[-]*[a-zA-Z \"#=0-9.:/]*[\"-]*>", " "); // Complete tags
		
		text = text.replaceAll("<[/!]?[-]*[a-zA-Z \"#=0-9.:/]*[\"-]*", " "); // Open tags
		
		text = text.replaceAll("[/!]?[-]*[a-zA-Z \"#=0-9.:/]*[\"-]*>", " "); // Closing tags
		// Remove the following special characters |,'[]"
		text = text.replaceAll("\\|*[,'\\[\\]\";]*", "");
		// Remove &,acute;,#146; and other special characters.
		//text = text.replaceAll("&*(&#146)*_*%*=*\\(\\)", "");
		text = text.replaceAll("&#146", "").replaceAll("_*%*=*\\(", " ").replaceAll("\\)", " ");
		
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
		return text;
	}


}
