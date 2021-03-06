package it.unipr.aotlab.mapreduce.countword;

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
 * Launch the application with files inside the "CountWordMassive" folder
 * this is an example of map-reduce with strings, files in input are quite big (around 200Mb)
 * so the example is very complicated, the execution of the program may be long.
 *
 */
public class BootMassiveCountWord {
	public static void main(String[] args) {
		// params
		final int workers = 3;
		//final int blockSize = 1024;
		final int blockSize = 50 * 1024 * 1024;
		final int bufferedContextSize = 50 * 1024 * 1024;
		//final String inputPath = "resources/CountWordMassive/";
		final String inputPath = "resources/Strings/";
		final String outputPath = "output/CountWordMassive/";
		final MapJob mapJob = new CountWordMap();
		final ReduceJob reduceJob = new CountWordReduce();

		Configuration c = Controller.INSTANCE.getConfiguration();
		c.setScheduler(ThreadPoolScheduler.class.getName());
		c.setCreator(Master.class.getName());
		c.setArguments(workers, inputPath, outputPath, blockSize, mapJob, reduceJob, bufferedContextSize);
		c.setFilter(Logger.ALLLOGS);
		c.addWriter(ConsoleWriter.class.getName(), TextualFormatter.class.getName(), null);
		Controller.INSTANCE.run();
	}
}
