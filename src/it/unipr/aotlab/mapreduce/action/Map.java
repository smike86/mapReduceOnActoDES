/**
 * 
 */
package it.unipr.aotlab.mapreduce.action;

import it.unipr.aotlab.actodes.interaction.Action;
import it.unipr.aotlab.mapreduce.context.MapJob;
import it.unipr.aotlab.mapreduce.resources.InputLinesReader;
import it.unipr.aotlab.mapreduce.resources.ResourcesHandler;

/**
 * @author Omi087
 *
 */
public class Map implements Action {

	private ResourcesHandler rh;
	private int mapBlock;
	private MapJob job;

	public Map(ResourcesHandler fh, int mapBlock, MapJob job) {
		this.rh = fh;
		this.mapBlock = mapBlock;
		this.job = job;
	}

	public void executeBlock() throws Exception {
		try (InputLinesReader lr = rh.getInputLinesReader(mapBlock)) {
			String line = null;
			while ((line = lr.readLine()) != null) {
				// esegui elaborazione sulla riga
				job.execute(line, rh.getMapContext());
				// fine
			}
		}
	}

}
