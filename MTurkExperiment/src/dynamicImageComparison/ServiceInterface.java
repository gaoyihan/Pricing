package dynamicImageComparison;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import com.amazonaws.mturk.requester.HITStatus;

public interface ServiceInterface {
	public double getAccountBalance();
	public void setHITProperty(String propertiesFile);
	public String publishImageComparisonHIT(String templateFile, List<List<Integer>> pairs, List<String> photoFile);
	public int getTotalNumHITsInAccount();
	public int getActiveNumHITsInAccount();
	public HITStatus getHITStatus(String hitId);
	public int expireHIT(String hitId);
	public int expireHITs(List<String> hitId);
	public int disposeHIT(String hitId);
	public int disposeHITs(List<String> hitId);
	public List<String> getAllHITs();
	public void clearAllHITs();
	public String getAnnotation(String hitId);
	public void approveOrRejectAllAssignments();	
}

class FakeService implements ServiceInterface {
	List<Double> arrival;
	List<Double> factor;
	List<Integer> batch;
	Map<Integer, List<List<Integer>>> hits;
	Map<Integer, Integer> status; //0 - Assignable, 1 - Unassignable, 2 - Expired, 3 - Unassignable & Expired, 4 - Reviewable,
	int maxIndex;
	int currentTimeSlot;
	
	public FakeService(List<Double> arrivalRate, List<Double> multiplier, List<Integer> batchSize) {
		this.arrival = arrivalRate;
		this.factor = multiplier;
		this.batch = batchSize;
		this.hits = new HashMap<Integer, List<List<Integer>>>();
		this.status = new HashMap<Integer, Integer>();
		this.maxIndex = 0;
		this.currentTimeSlot = 0;
	}
	
	@Override
	public double getAccountBalance() {
		return 10000;
	}

	@Override
	public void setHITProperty(String propertiesFile) {
		return;
	}

	@Override
	public String publishImageComparisonHIT(String templateFile,
			List<List<Integer>> pairs, List<String> photoFile) {
		ArrayList<List<Integer>> imagePairs = new ArrayList<List<Integer>>();
		imagePairs.addAll(pairs);
		hits.put(maxIndex, imagePairs);
		status.put(maxIndex, 0);
		return String.valueOf(maxIndex ++);
	}

	@Override
	public int getTotalNumHITsInAccount() {
		return hits.size();
	}
	
	@Override
	public int getActiveNumHITsInAccount() {
		int ret = 0;
		for (Entry<Integer, Integer> hitStatus : status.entrySet())
		if (hitStatus.getValue() == 0)
			ret ++;
		return ret;
	}	

	@Override
	public int expireHITs(List<String> hitId) {
		int expireCount = 0;
		for (int i = 0; i < hitId.size(); i++)
			expireCount += expireHIT(hitId.get(i));
		Logger.log("Expired " + expireCount + " HITs");
		return expireCount;
	}

	@Override
	public int disposeHITs(List<String> hitId) {
		int disposeCount = 0;
		for (int i = 0; i < hitId.size(); i++)
			disposeCount += disposeHIT(hitId.get(i));
		Logger.log("Disposed " + disposeCount + " HITs");
		return disposeCount;
	}

	@Override
	public List<String> getAllHITs() {
		List<String> ret = new ArrayList<String>();
		for (Integer i : hits.keySet())
			ret.add(String.valueOf(i));
		return ret;
	}

	@Override
	public void clearAllHITs() {
		List<String> hitId = getAllHITs();
		expireHITs(hitId);
		disposeHITs(hitId);
		Logger.log("Cleared all HITs");
	}

	@Override
	public int expireHIT(String hitId) {
		int index = Integer.valueOf(hitId);
		if (status.get(index) < 2) {
			status.put(index, status.get(index) + 2);
			return 1;
		} else
			return 0;
	}

	@Override
	public int disposeHIT(String hitId) {
		int index = Integer.valueOf(hitId);
		if (status.get(index) == 2) {
			status.remove(index);
			hits.remove(index);
			return 1;
		} else
			return 0;
	}

	@Override
	public String getAnnotation(String hitId) {
		int index = Integer.valueOf(hitId);
		return Utility.getAnnotation(hits.get(index));
	}	
	
	//For test use
	public void timeJump(double timeSlotFactor) {
		Random gen = new Random();
		for (Entry<Integer, Integer> hitStatus : status.entrySet())
		if (hitStatus.getValue() == 1 || hitStatus.getValue() == 3)
		if (gen.nextBoolean())
			hitStatus.setValue(hitStatus.getValue() - 1);
		else
			hitStatus.setValue(4);
		
		double remain = arrival.get(currentTimeSlot ++) * timeSlotFactor;
		Logger.log("Completed " + remain + " Percentage");
		for (int i = 0; i < batch.size(); i++) {
			for (Integer key : hits.keySet())
				if (hits.get(key).size() == batch.get(i) && status.get(key) == 0) {
					if (remain >= 1 / factor.get(i)) {
						remain -= 1 / factor.get(i);
						status.put(key, 4);
					}
					else {
						remain = 0;
						status.put(key, 1);
						return;
					}
				}
		}
	}

	@Override
	public HITStatus getHITStatus(String hitId) {
		int index = Integer.valueOf(hitId);
		if (status.get(index) == 0) return HITStatus.Assignable;
		if (status.get(index) == 1) return HITStatus.Unassignable;
		if (status.get(index) == 2) return HITStatus.Unassignable;
		if (status.get(index) == 3) return HITStatus.Unassignable;
		if (status.get(index) == 4) return HITStatus.Reviewable;
		return null;
	}

	@Override
	public void approveOrRejectAllAssignments() {
	}
}