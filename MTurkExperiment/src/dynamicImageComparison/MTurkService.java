package dynamicImageComparison;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.mturk.addon.HITProperties;
import com.amazonaws.mturk.addon.HITQuestion;
import com.amazonaws.mturk.requester.Assignment;
import com.amazonaws.mturk.requester.HIT;
import com.amazonaws.mturk.requester.HITStatus;
import com.amazonaws.mturk.service.axis.RequesterService;
import com.amazonaws.mturk.service.exception.InvalidStateException;
import com.amazonaws.mturk.util.PropertiesClientConfig;

//This Class include all helpful functions related to MTurk service class
public class MTurkService implements ServiceInterface {

	private final String expandedTemplateFilename = "expandedTemplate.question";
	private RequesterService service;
	private HITProperties props;
	
	public MTurkService(String propertyPath) {
		service = new RequesterService(new PropertiesClientConfig(propertyPath));
	}	
	
	@Override
	public double getAccountBalance() {
		return service.getAccountBalance();
	}

	@Override
	public void setHITProperty(String propertiesFile) {
		try {
			props = new HITProperties(propertiesFile);
		} catch (IOException e) {
			Logger.log(e.getMessage());
			e.printStackTrace();
		}
	}

	@Override
	public String publishImageComparisonHIT(String templateFile,
			List<List<Integer>> pairs, List<String> photoFile) {
		Utility.expandQuestion(templateFile, expandedTemplateFilename, pairs.size());
		HashMap<String, String> input = new HashMap<String, String>();
		for (int i = 0; i < pairs.size(); i++) {
			List<Integer> pair = pairs.get(i);
									
			input.put("photo_" + i + "_0" , photoFile.get(pair.get(0)));
			input.put("photo_" + i + "_1" , photoFile.get(pair.get(1)));
		}
		try {
			HITQuestion question = new HITQuestion(expandedTemplateFilename);

			HIT hit = service.createHIT(null, // HITTypeId 
					props.getTitle(), 
					props.getDescription(), props.getKeywords(), // keywords 
					question.getQuestion(input),
					props.getRewardAmount(), props.getAssignmentDuration(),
					props.getAutoApprovalDelay(), props.getLifetime(),
					props.getMaxAssignments(), Utility.getAnnotation(pairs), // requesterAnnotation 
					props.getQualificationRequirements(),
					null // responseGroup
					);
			
			Logger.log("Created HIT: " + hit.getHITId());
			return hit.getHITId();
		} catch (Exception e) {
			Logger.log(e.getMessage());
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public int getTotalNumHITsInAccount() {
		return service.getTotalNumHITsInAccount();
	}
	
	@Override
	public int getActiveNumHITsInAccount() {
		HIT[] hits = service.searchAllHITs();
		int count = 0;
		for (int i = 0; i < hits.length; i++)
		if (hits[i].getHITStatus().equals(HITStatus.Assignable))
			count ++;
		return count;
	}	

	@Override
	public int expireHIT(String hitId) {
		HITStatus status = service.getHIT(hitId).getHITStatus();
		if (status.equals(HITStatus.Assignable)||status.equals(HITStatus.Unassignable)) {
			service.forceExpireHIT(hitId);
			return 1;
		}
		return 0;
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
	public int disposeHIT(String hitId) {
		try {
			service.disposeHIT(hitId);
			return 1;
		} catch (InvalidStateException e) {
			return 0;
		}
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
		HIT[] hits = service.searchAllHITs();
		List<String> hitId = new ArrayList<String>();
		for (int i = 0; i < hits.length; i++)
			hitId.add(hits[i].getHITId());
		return hitId;
	}

	@Override
	public void clearAllHITs() {
		List<String> hitId = getAllHITs();
		expireHITs(hitId);
		disposeHITs(hitId);
		Logger.log("Cleared all HITs");
	}
	
	@Override
	public String getAnnotation(String hitId) {
		return service.getHIT(hitId).getRequesterAnnotation();
	}
	
	//For Test Purpose
	protected RequesterService getService() {
		return service;
	}
	
	public void approveOrRejectAllAssignments() {
		HIT[] hits = service.searchAllHITs();
		Map<Integer, Integer> entityMapping = DynamicImageComparison.getEntityMapping();
		for (int i = 0; i < hits.length; i++) {
			Assignment[] assignments = service.getAllAssignmentsForHIT(hits[i].getHITId());
			for (int j = 0; j < assignments.length; j++) {
				List<List<Integer>> pairs = Utility.getPairs(hits[i].getRequesterAnnotation());
				int rightAnswers = Utility.getNumOfRightAnswers(entityMapping, assignments[j].getAnswer(), pairs);
				if (rightAnswers > pairs.size() * 0.6) {
					Logger.log("Approve Assignment for HIT" + assignments[j].getHITId());
					Logger.log("Acceptance Time: " + assignments[j].getAcceptTime().getTimeInMillis());
					Logger.log("Num of right answers " + rightAnswers);
					Logger.log("Worker Id: " + assignments[j].getWorkerId());
					Logger.log("Submit Time: " + assignments[j].getSubmitTime().getTimeInMillis());
					service.approveAssignment(assignments[j].getAssignmentId(), null);
				} else {
					Logger.log("Reject Assignment for HIT" + assignments[j].getHITId());
					Logger.log("Num of right answers " + rightAnswers);
					Logger.log("Worker Id " + assignments[j].getWorkerId());
					Logger.log("Answer String " + assignments[j].getAnswer());
					service.rejectAssignment(assignments[j].getAssignmentId(), "poor quality");					
				}
			}
		}
	}

	@Override
	public HITStatus getHITStatus(String hitId) {
		return service.getHIT(hitId).getHITStatus();
	}	
}
