package ImageComparison;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

import com.amazonaws.mturk.addon.HITProperties;
import com.amazonaws.mturk.addon.HITQuestion;
import com.amazonaws.mturk.requester.Assignment;
import com.amazonaws.mturk.requester.HIT;
import com.amazonaws.mturk.service.axis.RequesterService;
import com.amazonaws.mturk.service.exception.InvalidStateException;
import com.amazonaws.mturk.service.exception.ValidationException;
import com.amazonaws.mturk.util.PropertiesClientConfig;

public class ImageComparison {
	private RequesterService service;

	// Defining the location of the file containing the QAP and the properties of the HIT
	public static String rootDir = "./data/ImageComparison";
	public static String questionFile = rootDir + "/ImageComparison.question";
	private String expandedQuestionFile = rootDir + "/ExpandedImageComparison.question";
	private String propertiesFile = rootDir + "/ImageComparison.properties";

	// 120912 Number of questions per HIT
	public static int NumQuestionsPerHIT = 10; // 5;
	public static int NumPhotos = 42; //8;
	public static String OutputFileYN = "outputYN.txt";
	
	// 120913 Deploy schedule
	public static int BatchSize = 1; 
	public static int NumRepeats = 40;
	
	// 120919 Total number of questions
	public static int NumQuestions = 125; // just to match price
	public static String GoldStandard = rootDir + "/GYfull_GS_2.txt"; 
	
	
	/**
	 * Constructor
	 *
	 */
	public ImageComparison() {
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
	
	public void expandQuestion(int repeat) throws IOException {
		Scanner scanner = new Scanner(new FileReader(questionFile));
		BufferedWriter writer = new BufferedWriter(new FileWriter(expandedQuestionFile));
		StringBuffer mainQuestion = new StringBuffer();
		boolean write = false;
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			if (line.equals("===PREFIX==="))
				write = true;
			else if (line.equals("===QUESTION==="))
				write = false;
			else if (line.equals("===SUFFIX===")) {
				for (int i = 0; i < repeat; i++)
					writer.write(mainQuestion.toString().replaceAll("\\$id", String.valueOf(i)));
				write = true;
			} else if (write)
				writer.write(line.replaceAll("\\$num", String.valueOf(repeat)) + "\n");
			else
				mainQuestion.append(line + "\n");
		}
		writer.close();
		scanner.close();
	}

	public void createImageComparison(Map<String, String> input, BufferedWriter out) {
		try {
			//Loading the HIT properties file.  HITProperties is a helper class that contains the 
			//properties of the HIT defined in the external file.  This feature allows you to define
			//the HIT attributes externally as a file and be able to modify it without recompiling your code.
			//In this sample, the qualification is defined in the properties file.
			HITProperties props = new HITProperties(propertiesFile);
			
			//Loading the question (QAP) file.
			HITQuestion question = new HITQuestion(expandedQuestionFile);

			// Validate the question (QAP) against the XSD Schema before making the call.
			// If there is an error in the question, ValidationException gets thrown.
			// This method is extremely useful in debugging your QAP.  Use it often.
			//String qStr = question.getQuestion(input);
			//QAPValidator.validate(qStr);
						
			// Create a HIT using the properties and question files
			HIT hit = service.createHIT(null, // HITTypeId 
					props.getTitle(), 
					props.getDescription(), props.getKeywords(), // keywords 
					question.getQuestion(input),
					props.getRewardAmount(), props.getAssignmentDuration(),
					props.getAutoApprovalDelay(), props.getLifetime(),
					props.getMaxAssignments(), props.getAnnotation(), // requesterAnnotation 
					props.getQualificationRequirements(),
					null // responseGroup
					);

			
			System.out.println("Created HIT: " + hit.getHITId());
			out.write(hit.getHITId() + "\n");

			System.out.println("You may see your HIT with HITTypeId '" 
					+ hit.getHITTypeId() + "' here: ");

			System.out.println(service.getWebsiteURL() 
					+ "/mturk/preview?groupId=" + hit.getHITTypeId());
		} catch (ValidationException e) {
			//The validation exceptions will provide good insight into where in the QAP has errors.  
			//However, it is recommended to use other third party XML schema validators to make 
			//it easier to find and fix issues.
			System.err.println("QAP contains an error: " + e.getLocalizedMessage());  

		} catch (Exception e) {
			System.err.println(e.getLocalizedMessage());
		}
	}
	
	public ArrayList<ArrayList<Integer>> preparePairs() throws IOException {
		ArrayList<ArrayList<Integer>> pairs = new ArrayList<ArrayList<Integer>>();
		ArrayList<ArrayList<Integer>> tmpPairs = new ArrayList<ArrayList<Integer>>();

		for(int i = 1; i <= NumPhotos; i++){
			for(int j = i+1; j <= NumPhotos; j++){
				ArrayList<Integer> pair = new ArrayList<Integer>();
				pair.add(i);
				pair.add(j);
				tmpPairs.add(pair);
			}
		}
		
		Random rand = new Random();
		rand.setSeed(910109);
		Collections.shuffle(tmpPairs, rand);
		
		// read gold standard file and construct clusters of matching records
		Map<Integer, Integer> entityMapping = new HashMap<Integer, Integer>();
		FileInputStream fstream = new FileInputStream(GoldStandard);
		
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		
		// for each answer
		String line = "";
		int eid = 0;
		while ((line = br.readLine()) != null){
			String [] tokens = line.split(",");
			for(int i = 0; i < tokens.length; i++)
				entityMapping.put(Integer.valueOf(tokens[i]), eid);
			eid++;
		}
		
		br.close();
		
		ArrayList<ArrayList<Integer>> selectedPairs = new ArrayList<ArrayList<Integer>>();
		int numYes = 0;
		int numNo = 0;
		for(int i = 0; i < tmpPairs.size(); i++){
			ArrayList<Integer> pair = tmpPairs.get(i);
			int id1 = pair.get(0);
			int id2 = pair.get(1);
			// if pair is Yes and Yes is needed, add pair
			if(entityMapping.get(id1) == entityMapping.get(id2) && numYes < NumQuestions/2){
				selectedPairs.add(pair);
				numYes++;
			// if pair is No and No is needed, add pair
			} else if (entityMapping.get(id1) != entityMapping.get(id2) && numNo < (NumQuestions + 1)/2) {
				selectedPairs.add(pair);
				numNo++;
			}
		}
		// re-shuffle selectedPairs because it probably has all its No's up front. 
		Collections.shuffle(selectedPairs, rand);
		
		for(int i = 0; i < NumRepeats; i++){
			for(ArrayList<Integer> pair : selectedPairs){
				pairs.add(pair);
			}
		}
		return pairs;
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
	
	public void createBatchHIT() throws IOException {
		ArrayList<ArrayList<Integer>> pairs = preparePairs();
		
		Map<String, String> input = null; 
		
		int numHITs = 0;
		System.out.println(pairs.size());
		for(int i = 0; i < pairs.size(); i += NumQuestionsPerHIT){
			// in case application stops in middle
			FileWriter fstreamYN = new FileWriter(OutputFileYN, true);
			BufferedWriter outYN = new BufferedWriter(fstreamYN);

			input = new HashMap<String, String>();
			for(int j = 0; j < NumQuestionsPerHIT; j++){
				ArrayList<Integer> pair = pairs.get(i+j);
				
				//System.out.print(pair.get(0) + "," + pair.get(1) + ", ");
				outYN.write(pair.get(0) + "," + pair.get(1) + ", ");
				
				input.put("photo_" + j + "_0" , "http://www.stanford.edu/~manasrj/SCOOP/Worker_Eval/" + pair.get(0) + ".jpg");
				input.put("photo_" + j + "_1" , "http://www.stanford.edu/~manasrj/SCOOP/Worker_Eval/" + pair.get(1) + ".jpg");
			}

			if (hasEnoughFund()) {
				System.out.println("Posting HITs for 50YN interface");
				expandQuestion(NumQuestionsPerHIT);
				createImageComparison(input, outYN);
			}
			
			outYN.close();
			
			numHITs++;
			/*if(numHITs % BatchSize == 0){
				java.util.Date date = new java.util.Date();
				
				System.out.print("Posted " + numHITs + " HITs (Time: " + date.toString() + "), waiting 1 hr..");
				Thread.sleep(3600000);
				System.out.println("done");
			}*/
		}		
		System.out.println("Total " + numHITs + " HITs created");
	}
	
	public void approveAssignments() {
		HIT[] hits = service.searchAllHITs();
		for (int i = 0; i < hits.length; i++) {
			Assignment[] assignments = service.getAllAssignmentsForHIT(hits[i].getHITId());
			for (int j = 0; j < assignments.length; j++) {
				System.out.println("Approve assignment for task " + hits[i].getHITId());
				service.approveAssignment(assignments[j].getAssignmentId(), null);
			} 
		}
	}
	
	public void logAssignments() throws IOException {
		
		BufferedWriter writer = new BufferedWriter(new FileWriter("log_group_10_002_full.txt"));		
		
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
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws IOException, InterruptedException {

		ImageComparison app = new ImageComparison();
		
		//System.out.println("Sleeping for 15 min");
		//Thread.sleep(900000);		
		
		//System.out.println("Clearing Existing HITs");
		//app.clearExistingHIT();
		
		System.out.println("Approving Assignments");
		app.approveAssignments();
		
		System.out.println("Logging Assignments");
		app.logAssignments();
				
		//System.out.println("Creating Batch HITs");
		//app.createBatchHIT();
	}
}
