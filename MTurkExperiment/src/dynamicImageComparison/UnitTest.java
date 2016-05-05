package dynamicImageComparison;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.distribution.PoissonDistribution;

import com.amazonaws.mturk.requester.HIT;
import com.amazonaws.mturk.requester.HITStatus;

class TestUtility {
	static void testAnnotation() {
		List<List<Integer>> pairs = new ArrayList<List<Integer>>();
		for (int i = 0; i < 2; i++) {
			List<Integer> pair = new ArrayList<Integer>();
			pair.add(i * 2); pair.add(i * 2 + 1);
			pairs.add(pair);
		}
		String ret = Utility.getAnnotation(pairs);
		if (!ret.equals("ABCD")) {
			Logger.log("Test Get Annotation Failed!");
			return;
		}
		List<List<Integer>> retPairs = Utility.getPairs(ret);
		if (!retPairs.equals(pairs)) {
			Logger.log("Test Get Annotation Failed!");
			return;			
		}
		Logger.log("Test Get Annotation Passed!");
	}
	
	static void testSetExclude() {
		List<Integer> pairA = new ArrayList<Integer>(); pairA.add(1); pairA.add(4);
		List<Integer> pairB = new ArrayList<Integer>(); pairB.add(41); pairB.add(42);
		List<Integer> pairC = new ArrayList<Integer>(); pairC.add(2); pairC.add(3);
		List<Integer> pairD = new ArrayList<Integer>(); pairD.add(1); pairD.add(2);
		
		List<List<Integer>> source = new ArrayList<List<Integer>>();
		List<List<Integer>> exclude = new ArrayList<List<Integer>>();
		source.add(pairA); source.add(pairB); source.add(pairC);
		exclude.add(pairD); exclude.add(pairB);
		List<List<Integer>> result = Utility.setExclude(source, exclude, 43);
		if (result.size() != 2) {
			Logger.log("Test Set Exclude Failed!");
			return;
		}
		if (!result.contains(pairA) || !result.contains(pairC)) {
			Logger.log("Test Set Exclude Failed!");
			return;
		}
		Logger.log("Test Set Exclude Passed!");
	}
	
	static void testVerifyAnswer() {
		List<Integer> pairA = new ArrayList<Integer>(); pairA.add(0); pairA.add(3);
		List<Integer> pairB = new ArrayList<Integer>(); pairB.add(40); pairB.add(41);
		List<Integer> pairC = new ArrayList<Integer>(); pairC.add(1); pairC.add(2);
		List<Integer> pairD = new ArrayList<Integer>(); pairD.add(0); pairD.add(1);
		String mockAnswer = "PREFIX<SelectionIdentifier>1</SelectionIdentifier>MIDDLE<SelectionIdentifier>1</SelectionIdentifier>"
				+ "<SelectionIdentifier>1</SelectionIdentifier>MIDDLE<SelectionIdentifier>1</SelectionIdentifier>SUFFIX";
		List<List<Integer>> pairs = new ArrayList<List<Integer>>();
		pairs.add(pairA); pairs.add(pairB); pairs.add(pairC); pairs.add(pairD);
		Map<Integer, Integer> entityMapping = DynamicImageComparison.getEntityMapping();
		if (Utility.getNumOfRightAnswers(entityMapping, mockAnswer, pairs) != 3) {
			Logger.log("Test Verify Answer Failed!");
			return;
		}
		mockAnswer = "PREFIX<SelectionIdentifier>1</SelectionIdentifier>MIDDLE<SelectionIdentifier>2</SelectionIdentifier>"
				+ "<SelectionIdentifier>1</SelectionIdentifier>MIDDLE<SelectionIdentifier>2</SelectionIdentifier>SUFFIX";
		if (Utility.getNumOfRightAnswers(entityMapping, mockAnswer, pairs) != 1) {
			Logger.log("Test Verify Answer Failed!");
			return;			
		}
		Logger.log("Test Verify Answer Passed!");
	}
}

class TestMTurkService {
	static void testInitialize() {
		ServiceInterface app = new MTurkService("./mturk.properties");
		double balance = app.getAccountBalance();
		app = new MTurkService("./sandbox.properties");
		if (app.getAccountBalance() != 10000) {
			Logger.log("Test Initialize Service Failed!");
			return; 
		}
		app = new MTurkService("./mturk.properties");
		if (app.getAccountBalance() != balance || balance == 10000) {
			Logger.log("Test Initialize Service Failed!");
			return;
		}
		Logger.log("Test Initialize Service Passed!");
	}
	
	private static List<List<Integer>> preparePairs(int begin, int num) {
		List<List<Integer>> pairs = new ArrayList<List<Integer>>();
		for (int i = 0; i < num; i++) {
			List<Integer> pair = new ArrayList<Integer>();
			pair.add(i * 2 + begin); pair.add(i * 2 + 1 + begin);
			pairs.add(pair);
		}
		return pairs;
	}
	
	private static List<String> prepareImage(int num) {
		List<String> image = new ArrayList<String>();
		for (int i = 0; i < num; i++)
			image.add("http://fakewebsite/image_" + i + ".jpg");
		return image;
	}
		
	static void testPublishHIT() {
		MTurkService app = new MTurkService("./sandbox.properties");
		String templateFile = DynamicImageComparison.questionFile;
		List<List<Integer>> pairs = preparePairs(0, 30);
		List<String> replace = prepareImage(60);
		app.setHITProperty(DynamicImageComparison.propertiesFile);
		
		int numHIT = app.getTotalNumHITsInAccount();
		
		HIT hit = app.getService().getHIT(app.publishImageComparisonHIT(templateFile, pairs, replace));
		
		if (app.getTotalNumHITsInAccount() != numHIT + 1) {
			Logger.log("Test Publish HIT Failed!");
			return;
		}
		
		if (app.getHITStatus(hit.getHITId()) != HITStatus.Assignable) {
			Logger.log("Test Publish HIT Failed!");
			return;			
		}
		
		int[] Pos = new int[60];
		String question = hit.getQuestion();
		for (int i = 0; i < 60; i++)
			if (!question.contains(replace.get(i))) {
				Logger.log("Test Publish HIT Failed!");
				return;
			} else {
				Pos[i] = question.indexOf(replace.get(i));
			}
		for (int i = 1; i < 60; i++)
			if (Pos[i] < Pos[i - 1]) {
				Logger.log("Test Publish HIT Failed!");
				return;
			}
		if (!question.contains("There are 30 pairs of photos to compare.")) {
			Logger.log("Test Publish HIT Failed!");
			return;
		}
		Logger.log("Test Publish HIT Passed!");
	}

	static void testClearHIT() {
		MTurkService app = new MTurkService("./sandbox.properties");
		String templateFile = DynamicImageComparison.questionFile;
		List<List<Integer>> pairs = preparePairs(0, 30);
		List<String> replace = prepareImage(60);
		app.setHITProperty(DynamicImageComparison.propertiesFile);

		Logger.log("Active Number of HIT: " + app.getActiveNumHITsInAccount());
		app.clearAllHITs();
		int num = app.getTotalNumHITsInAccount();
		List<String> createdHIT = new ArrayList<String>();
		for (int i = 0; i < 10; i++)
			createdHIT.add(app.publishImageComparisonHIT(templateFile, pairs, replace));
		if (num + 10 != app.getTotalNumHITsInAccount() || app.getActiveNumHITsInAccount() != 10) {
			Logger.log("Test Clear HIT Failed!");
			return;
		}
		app.expireHITs(createdHIT.subList(0, 5));
		int ret = app.disposeHITs(createdHIT.subList(0, 5));
		if (num + 5 != app.getTotalNumHITsInAccount() || ret != 5 || app.getActiveNumHITsInAccount() != 5) {
			Logger.log("Test Clear HIT Failed!");
			return;
		}
		
		app.clearAllHITs();
		//Note that the number of HITs in account is not necessarily zero, some HITs may not be disposable.
		if (app.getTotalNumHITsInAccount() != num || app.getActiveNumHITsInAccount() != 0) {
			Logger.log("Test Clear HIT Failed!");
			return;
		}
		if (app.disposeHITs(app.getAllHITs()) != 0) {
			Logger.log("Test Clear HIT Failed!");
			return;			
		}
		Logger.log("Test Clear HIT Passed!");		
	}
}

class TestDynamicPricing {
	static void testOptimal() {
		List<Double> arrival = new ArrayList<Double>();
		List<Integer> batch = new ArrayList<Integer>();
		List<Double> factor = new ArrayList<Double>();
		for (int i = 0; i < 10; i ++)
			arrival.add(i + 1.0);
		for (int i = 1; i <= 4; i++)
			batch.add(i);
		for (int i = 1; i <= 4; i++)
			factor.add(1.0);
		DynamicPricing test = new DynamicPricing(arrival, factor, batch, 20, 1000);
		for (int i = 10; i <= 20; i++)
			for (int j = 1; j <= 10; j++)
				if (test.getBatchSize(i, j) != 4) {
					Logger.log(i + "," + j + "," + test.getBatchSize(i, j));
					Logger.log("Test Optimal Pricing Failed!");
					return;
				}
		Logger.log("Test Optimal Pricing Passed!");
	}
	
	static void testCompletion() {
		List<Double> arrival = new ArrayList<Double>();
		List<Integer> batch = new ArrayList<Integer>();
		List<Double> factor = new ArrayList<Double>();
		for (int i = 0; i < 10; i ++)
			arrival.add(2.0 * (i + 1));
		for (int i = 1; i <= 4; i++)
			batch.add(i);
		for (int i = 1; i <= 4; i++)
			factor.add(1.0 * Math.exp(-i));
		DynamicPricing test = new DynamicPricing(arrival, factor, batch, 20, 10000);
		double[][] possibility = new double[11][21];
		for (int i = 1; i <= 20; i++)
			possibility[0][i] = 1;
		for (int i = 1; i <= 20; i++)
			for (int j = 1; j <= 10; j++) {
				int decision = test.getBatchSize(i, j);
				decision = 1;
				PoissonDistribution poisson = new PoissonDistribution(factor.get(decision - 1) * arrival.get(10 - j));
				for (int k = 0; k <= 20; k++)
					possibility[j][i] += possibility[j - 1][Math.max(i - k * decision, 0)] * poisson.probability(k);
			}
		if (possibility[10][20] > 0.001) {
			Logger.log("Test Completion Failed!");
			return;
		}
		Logger.log("Test Completion Passed!");		
	}
}

class TestExperiment {
	static void testPreparePair() {
		List<List<Integer>> result = DynamicImageComparison.preparePairs();
		if (result.size() != DynamicImageComparison.numRepeats * DynamicImageComparison.numQuestions) {
			Logger.log("Test Prepare Pairs Failed!");
			return;
		}
		for (List<Integer> pair : result) {
			if (pair.size() != 2 || pair.get(0) < 0 || pair.get(0) >= DynamicImageComparison.numPhotos || pair.get(1) < 0 || pair.get(1) >= DynamicImageComparison.numPhotos) {
				Logger.log("Test Prepare Pairs Failed!");
				return;				
			}
		}
		Logger.log("Test Prepare Pairs Passed!");
	}	
	
	static void simulateExperiment() {	
		ArrivalRateRegression instance = new ArrivalRateRegression();
		FakeService service = new FakeService(instance.arrivalRate(), instance.multiplier(), instance.taskSize());
		Timer faketimer = new FakeTimer(service);
		DynamicImageComparison experiment = new DynamicImageComparison(service, faketimer);
		experiment.completeHIT(instance.arrivalRate(), instance.multiplier(), instance.taskSize(), 60);

		//Output Statistics
		Logger.log("Total Cost: " + service.getTotalNumHITsInAccount());		
		int completedTasks = 0;
		for (String hitId : service.getAllHITs())
			completedTasks += Utility.getPairs(service.getAnnotation(hitId)).size();
		Logger.log("Total Completed Pairs: " + completedTasks);

		if (service.getActiveNumHITsInAccount() != 0)
			Logger.log("Test Simulation Failed!");
		else
			Logger.log("Test Simulation Passed!");
	}
	
	static void testTimer() {
		Calendar start = Calendar.getInstance();
		Timer a = new SystemTimer(), b = new SystemTimer();
		a.sleep(1); 
		if (Math.abs(Calendar.getInstance().getTimeInMillis() - start.getTimeInMillis() - 60000) > 1000)
			Logger.log("Test Timer Failed!");
		b.sleep(2);
		if (Math.abs(Calendar.getInstance().getTimeInMillis() - start.getTimeInMillis() - 120000) < 1000)
			Logger.log("Test Timer Passed!");
		else
			Logger.log("Test Timer Failed!");
	}
}

class UnitTest {
	public static void main(String[] args) {
		TestUtility.testAnnotation();
		TestUtility.testSetExclude();
		TestUtility.testVerifyAnswer();
		TestMTurkService.testInitialize();
		TestMTurkService.testPublishHIT();
		TestMTurkService.testClearHIT();
		TestDynamicPricing.testOptimal();
		TestDynamicPricing.testCompletion();
		TestExperiment.testPreparePair();
		//TestExperiment.testTimer();
		TestExperiment.simulateExperiment();
	}
}
