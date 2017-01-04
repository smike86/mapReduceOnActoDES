package stub;

import java.util.Random;

import it.unipr.aotlab.mapreduce.context.MapJob;
import it.unipr.aotlab.mapreduce.context.Context;
import it.unipr.aotlab.mapreduce.context.ReduceJob;

public class WaitAndEchoReduceJob implements ReduceJob{

	private static Random random = new Random();
	private int i = 0;
	
	@Override
	public void execute(String line, Context context) throws Exception {
		Thread.sleep(1000 + random.nextInt(1000));
		int nextKey = getNextKey(); 
		context.put(nextKey, line.substring(0,3));
		System.out.println("reduce " + nextKey + " - " + line.substring(0,3));
	}

	private synchronized int getNextKey() {
		return i++;
	}

}