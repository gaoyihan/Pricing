package dynamicImageComparison;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.amazonaws.mturk.requester.HITStatus;

interface Timer {
	/*
	 * Note that this method sleeps until the last logged time plus parameter $numMinutes
	 * The first logged time is set in the constructor.
	 */
	public void sleep(int numMinutes);
}

class SystemTimer implements Timer {
	private Calendar logTime;
	public SystemTimer() {
		logTime = Calendar.getInstance();
	}
	@Override
	public void sleep(int numMinutes) {
		try {
			long offset = Calendar.getInstance().getTimeInMillis() - logTime.getTimeInMillis();
			Thread.sleep(numMinutes * 60 * 1000 - offset);
		} catch (InterruptedException e) {
			Logger.log(e.getMessage());
			e.printStackTrace();
		}
		logTime = Calendar.getInstance();
	}
}

class FakeTimer implements Timer{
	FakeService target;
	public FakeTimer(FakeService service) {
		this.target = service;
	}
	@Override
	public void sleep(int numMinutes) {
		target.timeJump(0.9);
	}
}

public class DynamicImageComparison {

	public static String rootDir = "./data/ImageComparison";
	public static String questionFile = rootDir + "/ImageComparison.question";
	public static String propertiesFile = rootDir + "/ImageComparison.properties";
	public static String GoldStandard = rootDir + "/GYfull_GS_2.txt";
	public static List<String> photoFile;
	public static int numPhotos = 42;
	public static int numRepeats = 40;
	public static int numQuestions = 125;
	
	private Timer timer;	
	private ServiceInterface service;
	
	public DynamicImageComparison(ServiceInterface service, Timer timer) {
		this.timer = timer;
		this.service = service;
		DynamicImageComparison.photoFile = generatePhotoFile();
	}	
	
	private static List<String> generatePhotoFile() {
		List<String> ret = new ArrayList<String>();
		for (int i = 1; i <= numPhotos; i++)
			ret.add("http://www.stanford.edu/~manasrj/SCOOP/Worker_Eval/" + i + ".jpg");			
		return ret;
	}
	
	public static Map<Integer, Integer> getEntityMapping() {
		Map<Integer, Integer> entityMapping = new HashMap<Integer, Integer>();		
		
		try {
			FileInputStream fstream = new FileInputStream(GoldStandard);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			
			// for each answer
			String line = "";
			int eid = 0;
			while ((line = br.readLine()) != null){
				String [] tokens = line.split(",");
				for(int i = 0; i < tokens.length; i++)
					entityMapping.put(Integer.valueOf(tokens[i]) - 1, eid);
				eid++;
			}
			
			br.close();
		} catch (Exception e) {
			Logger.log(e.getMessage());
			e.printStackTrace();
			return null;
		}
		
		return entityMapping;
	}
	
	public static List<List<Integer>> preparePairs() {
		List<List<Integer>> pairs = new ArrayList<List<Integer>>();
		ArrayList<ArrayList<Integer>> tmpPairs = new ArrayList<ArrayList<Integer>>();

		for(int i = 0; i < numPhotos; i++){
			for(int j = i+1; j < numPhotos; j++){
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
		Map<Integer, Integer> entityMapping = getEntityMapping();
		
		ArrayList<ArrayList<Integer>> selectedPairs = new ArrayList<ArrayList<Integer>>();
		int numYes = 0;
		int numNo = 0;
		for(int i = 0; i < tmpPairs.size(); i++){
			ArrayList<Integer> pair = tmpPairs.get(i);
			int id1 = pair.get(0);
			int id2 = pair.get(1);
			// if pair is Yes and Yes is needed, add pair
			if(entityMapping.get(id1) == entityMapping.get(id2) && numYes < numQuestions/2){
				selectedPairs.add(pair);
				numYes++;
			// if pair is No and No is needed, add pair
			} else if (entityMapping.get(id1) != entityMapping.get(id2) && numNo < (numQuestions + 1)/2) {
				selectedPairs.add(pair);
				numNo++;
			}
		}
		// re-shuffle selectedPairs because it probably has all its No's up front. 
		Collections.shuffle(selectedPairs, rand);
		
		for(int i = 0; i < numRepeats; i++){
			for(ArrayList<Integer> pair : selectedPairs){
				pairs.add(pair);
			}
		}
		return pairs;
	}	
	
	public void completeHIT(List<Double> arrivalRate, List<Double> multiplier, List<Integer> batchSize, int slotMinutes) {		
		List<List<Integer>> pairs = preparePairs();
		DynamicPricing engine = new DynamicPricing(arrivalRate, multiplier, batchSize, pairs.size(), 10000);
		
		int currentBatchSize = engine.getBatchSize(pairs.size(), arrivalRate.size());
		Logger.log("Current Batch Size: " + currentBatchSize);
		for (int i = 0; i + currentBatchSize <= pairs.size(); i += currentBatchSize)
			service.publishImageComparisonHIT(questionFile, pairs.subList(i, i + currentBatchSize), photoFile);
		
		List<List<Integer>> remainPairs = new ArrayList<List<Integer>>();
		remainPairs.addAll(pairs.subList(pairs.size() - pairs.size() % currentBatchSize, pairs.size()));
		if (arrivalRate.size() == 1 && remainPairs.size() > 0)
			service.publishImageComparisonHIT(questionFile, remainPairs, photoFile);
		int remainTasks = pairs.size(), activeHITs = pairs.size() / currentBatchSize;

		timer.sleep(slotMinutes);
		
		for (int i = 1; i < arrivalRate.size(); i++) {
			int currentActiveHITs = service.getActiveNumHITsInAccount();
			int estimateCompletion = (activeHITs - currentActiveHITs) * currentBatchSize;
			Logger.log("Estimated Remaining Pairs: " + (remainTasks - estimateCompletion));
			currentBatchSize = engine.getBatchSize(
					remainTasks - estimateCompletion, 
					arrivalRate.size() - i);
			Logger.log("Current Batch Size: " + currentBatchSize);
			if (currentBatchSize == -1) {
				//Something goes wrong, clean up every thing
				service.clearAllHITs();
				service.approveOrRejectAllAssignments();
				service.clearAllHITs();
				return;
			}

			remainTasks = remainPairs.size();
			List<String> hitIdList = service.getAllHITs();
			for (String hitId : hitIdList) {
				if (service.getHITStatus(hitId) == HITStatus.Assignable) 
					service.expireHIT(hitId);
				List<List<Integer>> hitPairs = Utility.getPairs(service.getAnnotation(hitId));
				if (service.disposeHIT(hitId) == 1) {
					remainPairs.addAll(hitPairs);
					remainTasks += hitPairs.size();
				}
				
				while (remainPairs.size() >= currentBatchSize) {
					service.publishImageComparisonHIT(questionFile, remainPairs.subList(0, currentBatchSize), photoFile);
					remainPairs.subList(0, currentBatchSize).clear();
				}
			}

			if (i == arrivalRate.size() - 1 && remainPairs.size() > 0)
				service.publishImageComparisonHIT(questionFile, remainPairs, hitIdList);
			activeHITs = remainTasks / currentBatchSize;
			
			Logger.log("Remaining Pairs: " + remainTasks);
			System.gc();
			Logger.log("Sleep " + slotMinutes + " minutes");
			timer.sleep(slotMinutes);
		}
		service.clearAllHITs();
	}
	
	public static void doExperiment() {
		ArrivalRateRegression instance = new ArrivalRateRegression();
		MTurkService service = /*new MTurkService("./sandbox.properties");*/new MTurkService("./mturk.properties");
		service.setHITProperty(DynamicImageComparison.propertiesFile);
		Timer timer = new SystemTimer();
		DynamicImageComparison experiment = new DynamicImageComparison(service, timer);
		
		experiment.completeHIT(instance.arrivalRate(), instance.multiplier(), instance.taskSize(), 60);
		
		//Output Statistics
		Logger.log("Total Cost: " + service.getTotalNumHITsInAccount());		
		int completedTasks = 0;
		for (String hitId : service.getAllHITs())
			completedTasks += Utility.getPairs(service.getAnnotation(hitId)).size();
		Logger.log("Total Completed Pairs: " + completedTasks);
	}
	
	public static void doFixedPricingExperiment() {
		ArrivalRateRegression instance = new ArrivalRateRegression();
		//MTurkService service = new MTurkService("./sandbox.properties");
		MTurkService service = new MTurkService("./mturk.properties");
		service.setHITProperty(DynamicImageComparison.propertiesFile);
		Timer timer = new SystemTimer();
		DynamicImageComparison experiment = new DynamicImageComparison(service, timer);
		
		// In order to achieve fixed pricing, we need to hack some value.
		List<Double> hackedMultiplier = instance.multiplier();
		hackedMultiplier.set(0, 0.001);
		hackedMultiplier.set(1, 0.001);
		hackedMultiplier.set(2, 0.001);
		hackedMultiplier.set(3, 0.001);
		hackedMultiplier.set(4, 1.001);
		experiment.completeHIT(instance.arrivalRate(), hackedMultiplier, instance.taskSize(), 6000);
	}
	
	public static void cleanUp() {
		//MTurkService service = new MTurkService("./sandbox.properties");
		MTurkService service = new MTurkService("./mturk.properties");
		service.clearAllHITs();
		service.approveOrRejectAllAssignments();
		service.clearAllHITs();
	}
		
	public static void main(String[] args) {
		System.out.println("UnitTest or Run or CleanUp");
		if (args.length > 0) {
			if (args[0].equals("UnitTest"))
				UnitTest.main(args);
			else if (args[0].equals("Run"))
				doExperiment();
			else if (args[0].equals("CleanUp"))
				cleanUp();
			return;
		}
		//doFixedPricingExperiment();
		cleanUp();
	}
}
