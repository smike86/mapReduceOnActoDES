package it.unipr.aotlab.mapreduce.stub;

import it.unipr.aotlab.actodes.configuration.Configuration;
import it.unipr.aotlab.actodes.logging.ConsoleWriter;
import it.unipr.aotlab.actodes.logging.Logger;
import it.unipr.aotlab.actodes.logging.TextualFormatter;
import it.unipr.aotlab.actodes.runtime.Controller;
import it.unipr.aotlab.actodes.runtime.active.ThreadPoolScheduler;
import it.unipr.aotlab.mapreduce.Master;
import it.unipr.aotlab.mapreduce.context.MapJob;
import it.unipr.aotlab.mapreduce.context.ReduceJob;

/**
 * Simple boot for dummy example
 */
public class BootStub {
	public static void main(String[] args) {
		// params
		final int workers = 3;
		final int blockSize = 1024;
		final String inputPath = "resources/stub/";
		final String outputPath = "output/stub/output.txt";
		final MapJob mapJob = new WaitAndEchoJob();
		final ReduceJob reduceJob = new WaitAndEchoReduceJob();

		Configuration c = Controller.INSTANCE.getConfiguration();
		c.setScheduler(ThreadPoolScheduler.class.getName());
		c.setCreator(Master.class.getName());
		c.setArguments(workers, inputPath, outputPath, blockSize, mapJob, reduceJob);
		c.setFilter(Logger.ALLLOGS);
		c.addWriter(ConsoleWriter.class.getName(), TextualFormatter.class.getName(), null);
		Controller.INSTANCE.run();
	}
}
