package com.kircherelectronics.accelerationexplorer;

/**
 * An interface for classes that need to sample data from a sensor or other
 * input.
 * 
 * @author Kaleb
 * @version %I%, %G%
 */
public interface Sampler
{
	/**
	 * The state of the measurements.
	 * 
	 * @return The state of the measurements.
	 */
	public int getSampleState();
	
	/**
	 * Determine if the samples are being recorded.
	 * 
	 * @return True is samples are being recorded.
	 */
	public boolean isSampling();

	/**
	 * Indicate if samples are being recorded.
	 * 
	 * @param sampling
	 *            True if samples are being recorded.
	 */
	public void setSampling(boolean sampling);

	/**
	 * Indicate the sample measurement state.
	 * 
	 * @param state
	 *            The sample measurement state.
	 */
	public void setSampleState(int state);
}
