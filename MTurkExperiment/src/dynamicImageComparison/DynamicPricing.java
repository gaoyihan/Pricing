package dynamicImageComparison;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.TimeZone;

import org.apache.commons.math3.distribution.PoissonDistribution;

//We use logged task completion data to evaluate arrival rate.
class ArrivalRateRegression {
	List<List<Integer>> history;
	ArrivalRateRegression() {
		history = new ArrayList<List<Integer>>();
		history.add(extractArrival("log_group_10_002_full.txt"));
		history.add(extractArrival("log_group_20_002_full.txt"));
		history.add(extractArrival("log_group_30_002.txt"));
		history.add(extractArrival("log_group_40_002.txt"));
		history.add(extractArrival("log_group_50_002.txt"));
	}
	
	private List<Integer> extractArrival(String filename) {
		List<Integer> ret = new ArrayList<Integer>();
		for (int i = 0; i < 14; i++)
			ret.add(0);
		List<Long> acceptList = new ArrayList<Long>();
		try {
			Scanner scanner = new Scanner(new FileReader(filename));
			while (scanner.hasNext()) {
				scanner.nextLine(); // id
				scanner.nextLine(); // workerId
				Long accept = Long.valueOf(scanner.nextLine());
				scanner.nextLine(); // submit time
				acceptList.add(accept);
			}
			scanner.close();
		} catch (FileNotFoundException e) {
			Logger.log(e.getMessage());
			e.printStackTrace();
		}
		for (Long time : acceptList) {
			Calendar date = Calendar.getInstance();
			date.setTimeZone(TimeZone.getTimeZone("PST"));
			date.setTime(new Date(time));
			int hour = date.get(Calendar.HOUR_OF_DAY) - 8;
			if (hour < 0) hour = 0;
			if (hour > 13) hour = 13;
			ret.set(hour, ret.get(hour) + 1);
		}
		return ret;
	}
	public List<Double> arrivalRate() {
		List<Double> ret = new ArrayList<Double>();
		for (int i = 0; i < 14; i++)
			ret.add(0.0);
		int[] truncate = {6, 8, 14, 14, 14};
		double[] sum = {499, 250, 86, 81, 91};
		for (int i = 4; i >= 0; i--) {
			if (i != 4) {
				double partialSum = 0;
				for (int j = 0; j < truncate[i]; j++)
					partialSum += ret.get(j);
				sum[i] = sum[i] / partialSum * 100;
			}
			for (int j = 0; j < truncate[i]; j++)
				ret.set(j, ret.get(j) / (5 - i) * (4 - i) + history.get(i).get(j) / sum[i] * 100 / (5 - i));
		}
		return ret;
	}
	
	public List<Double> multiplier() {
		List<Double> ret = new ArrayList<Double>();
		List<Double> arrival = arrivalRate();
		int[] truncate = {6, 8, 14, 14, 14};
		double[] sum = {499, 250, 86, 81, 91};

		for (int i = 0; i < 5; i++) {
			double partialSum = 0;
			for (int j = 0; j < truncate[i]; j++)
				partialSum += arrival.get(j);
			ret.add(sum[i] / partialSum);
		}
		return ret;
	}
	
	public List<Integer> taskSize() {
		List<Integer> ret = new ArrayList<Integer>();
		for (int i = 10; i <= 50; i += 10)
			ret.add(i);
		return ret;
	}
}


public class DynamicPricing {
	private List<Double> arrival;
	private List<Double> factor;
	private List<Integer> batch;
	private int totalNum;
	private int[][] decision;
	private double[][] opt;
	
	private void train(double penalty) {
		for (int i = 0; i <= totalNum; i++) {
			opt[0][i] = penalty * i;
			decision[0][i] = 0;
		}
				
		for (int i = 1; i <= arrival.size(); i++)
			for (int j = 0; j <= totalNum; j++) {
				opt[i][j] = Double.MAX_VALUE;
				for (int k = 0; k < batch.size(); k++) {
					double cost = 0;
					PoissonDistribution poisson = new PoissonDistribution(factor.get(k) * arrival.get(arrival.size() - i));
					for (int l = 0; l * batch.get(k) <= j; l++)
						cost += poisson.probability(l) * (opt[i - 1][j - l * batch.get(k)] + l);
					cost += (1 - poisson.cumulativeProbability(j / batch.get(k))) * ((j + batch.get(k) - 1) / batch.get(k));
					if (cost < opt[i][j]) {
						opt[i][j] = cost;
						decision[i][j] = k;
					}
				}
			}
	}
	
	public DynamicPricing(List<Double> arrivalRate, List<Double> multiplier, List<Integer> batchSize, int totalNum, double penaltyMultiplier) {
		this.arrival = arrivalRate;
		this.factor = multiplier;
		this.batch = batchSize;
		this.totalNum = totalNum;
		decision = new int[arrival.size() + 1][totalNum + 1];
		opt = new double[arrival.size() + 1][totalNum + 1];
		train(penaltyMultiplier);
	}
	
	public int getBatchSize(int remainNum, int remainTimeSlot) {
		if (remainNum <= totalNum && remainTimeSlot <= arrival.size())
			return batch.get(decision[remainTimeSlot][remainNum]);
		else
			return -1;
	}	
}
