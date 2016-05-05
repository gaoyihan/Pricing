/*
 * Copyright 2007-2012 Amazon Technologies, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 * 
 * http://aws.amazon.com/apache2.0
 * 
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */ 

package text_categorization;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import com.amazonaws.mturk.addon.HITDataCSVReader;
import com.amazonaws.mturk.addon.HITDataCSVWriter;
import com.amazonaws.mturk.addon.HITDataInput;
import com.amazonaws.mturk.addon.HITDataOutput;
import com.amazonaws.mturk.addon.HITProperties;
import com.amazonaws.mturk.addon.HITQuestion;
import com.amazonaws.mturk.addon.QAPValidator;
import com.amazonaws.mturk.requester.Assignment;
import com.amazonaws.mturk.requester.HIT;
import com.amazonaws.mturk.requester.QualificationType;
import com.amazonaws.mturk.service.axis.RequesterService;
import com.amazonaws.mturk.service.exception.InvalidStateException;
import com.amazonaws.mturk.service.exception.ValidationException;
import com.amazonaws.mturk.util.PropertiesClientConfig;

public class TextCategorization {

	private RequesterService service;

	// Defining the location of the file containing the QAP and the properties of the HIT
	private String rootDir = "./data/text_categorization";
	private String questionFile = rootDir + "/question.txt";
	private String propertiesFile = rootDir + "/properties.txt";
	private String inputFile = rootDir + "/input.txt";
	private String rewardInputFile = rootDir + "/reward.txt";
	
	private String title, keywords, description; 
	private double reward;
	
	private List<String> document = new ArrayList<String>();
	private List<List<String>> topic = new ArrayList<List<String>>();
	
	private String question;
	
	private long autoApprovalDelay = 1296000;
	private long assignmentDuration = 3600;
	private long lifeTime = 259200;
	
	private String[] selectedTopic = {"grain", "wheat", "corn", "veg-oil", "soybean", "oilseed", "coffee", "sugar", "livestock"};	
	
	/**
	 * Constructor
	 */
	public TextCategorization() {
		service = new RequesterService(new PropertiesClientConfig("./mturk.properties"));
	}
	
	/**
	 * Check to see if your account has sufficient funds
	 * @return true if there are sufficient funds. False if not.
	 */
	public boolean hasEnoughFund() {
		double balance = service.getAccountBalance();
		System.out.println("Got account balance: " + RequesterService.formatCurrency(balance));
		return balance > 0;
	}
	
	public void readInputFile() throws IOException {
		Scanner reader = new Scanner(new File(inputFile));
		StringBuffer buffer = new StringBuffer();
		while (reader.hasNextLine()) {
			String line = reader.nextLine();
			if (line.equals("<NewDocument>")) {
				line = reader.nextLine();
				String[] split = line.split(";");
				topic.add(Arrays.asList(split));
				buffer = new StringBuffer();
			} else if (line.equals("</NewDocument>")) {
				document.add(buffer.toString());
			} else
				buffer.append(line + "\n");
		}
		reader.close();		
	}
	
	public void readQuestionFile() throws IOException {
		Scanner reader = new Scanner(new File(questionFile));
		StringBuffer buffer = new StringBuffer();
		while (reader.hasNextLine()) {
			String line = reader.nextLine();
			buffer.append(line + "\n");
		}
		this.question = buffer.toString(); 
		reader.close();				
	}
	
	/**
	 * Substitute parameters into question template
	 */
	private String substituteTemplate(String[] document, String question) {

		int[] index = new int[document.length];
		for (int i = 0; i < index.length; i++)
			index[i] = question.indexOf("$" + (i + 1));
		StringBuffer ret = new StringBuffer();
		for (int i = 0; i < index.length; i++) {
			int lastPosition = (i == 0 ? 0 : index[i - 1] + 2);
			ret.append(question.substring(lastPosition, index[i]));

			String docStr = document[i];
			//Replace spaces by &nbsp;, end-of-line by <br>
			while (docStr.contains("  "))
				docStr = docStr.replaceAll("  ", " &nbsp; ");
			while (docStr.contains("&nbsp; &nbsp;"))
				docStr = docStr.replaceAll("&nbsp; &nbsp;", "&nbsp;&nbsp;");
			docStr = docStr.replaceAll("\n", " <br/> \n ");
			ret.append(docStr);
		}
		ret.append(question.substring(index[index.length - 1] + 2));
		
		return ret.toString();
	}
  
	/**
	 * Creates the Text Categorization HIT using input and property files
	 * If any HIT already exist in the pool, then it will not be created again.
	 */
	public void createTextCategorization() {
		String hitTypeId = service.registerHITType(autoApprovalDelay, assignmentDuration, reward, title, keywords, description, null);
		
		//Filter out already completed HITs
		HIT[] hits = service.searchAllHITs();
		boolean[] completed = new boolean[document.size()];
		for (int i = 0; i < document.size(); i++)
			completed[i] = false;
		for (int i = 0; i < hits.length; i++) {
			String indexString = hits[i].getRequesterAnnotation().split(";")[0];
			if (indexString.startsWith("Data Index:"))
				completed[Integer.valueOf(indexString.substring("Data Index:".length()))] = true;
		}
		
		//Upload other HITs
		for (int i = 0; i < 100; i++)
		if (!completed[i]) {
			System.out.println("Creating HIT #" + i);
			String[] documentList = new String[2];
			document.subList(i * 2, (i + 1) * 2).toArray(documentList);
			service.createHIT(hitTypeId, null, null, null, substituteTemplate(documentList, question)
					, null, null, null, lifeTime, 3, "Data Index:" + String.valueOf(i) + ";Reward:" + "0.01", null, null);
		}
	    System.out.println(service.getWebsiteURL() 
	              + "/mturk/preview?groupId=" + hitTypeId);	
	}
	
	/**
	 * Clear all exist HIT: Force expire them, then dispose 
	 * them if they don't have any ongoing or not reviewed assignments.
	 */
	public void clearExistingHIT() {
		HIT[] hits = service.searchAllHITs();
		for (int i = 0; i < hits.length; i++) {
			try {
				service.forceExpireHIT(hits[i].getHITId());
			} catch (InvalidStateException e) {
				System.out.println("HIT " + hits[i].getHITId() + " unable to force expire");
			}
		}
		for (int i = 0; i < hits.length; i++) {
			try {
				service.disposeHIT(hits[i].getHITId());
			} catch (InvalidStateException e) {
				System.out.println("HIT " + hits[i].getHITId() + " unable to dispose");
			} 
		}
	}
	
	public void readProperties() throws IOException {
		Scanner reader = new Scanner(new File(propertiesFile));
		while (reader.hasNextLine()) {
			String line = reader.nextLine();
			String[] split = line.split(":");
			if (split[0].equals("title")) title = split[1];
			if (split[0].equals("description")) description = split[1];
			if (split[0].equals("keywords")) keywords = split[1];
			if (split[0].equals("reward")) reward = Double.valueOf(split[1]);			
		}
		reader.close();
	}
	
	private String[] parseAnswer(String answer) {
		String[] split = answer.split("</?SelectionIdentifier>");
		String[] ret = new String[split.length / 2];
		for (int i = 1; i < split.length; i += 2)
			ret[i / 2] = split[i];
		return ret;
	}
	
	public void approveAssignments() {
		HIT[] hits = service.searchAllHITs();
		for (int i = 0; i < hits.length; i++)
		{
			Assignment[] assignments = service.getAllAssignmentsForHIT(hits[i].getHITId());
			String[] split = hits[i].getRequesterAnnotation().split(";");
			double reward = Double.valueOf(split[1].substring("Reward:".length()));
			int index = Integer.valueOf(split[0].substring("Data Index:".length()));
			for (int j = 0; j < assignments.length; j++) {
				if (true) {
					System.out.println("Approve assignment for task " + index);
					service.approveAssignment(assignments[j].getAssignmentId(), null);
					//if (reward > 0.01) {
					//	System.out.println("Grant bonus " + (reward - 0.01));
					//	service.grantBonus(assignments[j].getWorkerId(), reward - 0.01, assignments[j].getAssignmentId(), "Regular bonus");
					//}
				} /*else {
					System.out.println("Reject assignment for task " + index);
					service.rejectAssignment(assignments[j].getAssignmentId(), null);
				}*/
			}
		}
	}
	
	public void logAssignments() throws IOException {
		
		BufferedWriter writer = new BufferedWriter(new FileWriter("log_group_2_001.txt"));		
		
		HIT[] hits = service.searchAllHITs();
		for (int i = 0; i < hits.length; i++) {
			Assignment[] assignments = service.getAllAssignmentsForHIT(hits[i].getHITId());
			for (int j = 0; j < assignments.length; j++)
				writer.write(i + "\n" + assignments[j].getWorkerId() + "\n" + assignments[j].getAcceptTime().getTimeInMillis() + "\n" + assignments[j].getSubmitTime().getTimeInMillis() + "\n");
		}		
		writer.close();
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {

		TextCategorization app = new TextCategorization();
		
		System.out.println("Reading Property File");
		app.readProperties();
		System.out.println("Reading Input File");
		app.readInputFile();
		System.out.println("Reading Question File");
		app.readQuestionFile();
		
		//System.out.println("Approving assignments");
		//app.approveAssignments();
		
		//System.out.println("Logging assignments");
		//app.logAssignments();
		
		System.out.println("Clearing Existing HITs");
		app.clearExistingHIT();
		
		//if (app.hasEnoughFund()) {
			//app.createTextCategorization();
		//}
	}
}
