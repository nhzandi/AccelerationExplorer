package com.kircherelectronics.accelerationexplorer;

/*
 * Acceleration Explorer
 * Copyright (C) 2013, Kaleb Kircher - Boki Software, Kircher Engineering, LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.LinkedList;

import org.apache.commons.math3.stat.StatUtils;

/**
 * An implementation to calculate variance from a rolling window.
 * 
 * @author Kaleb
 * @version %I%, %G%
 */
public class Variance
{
	private LinkedList<Double> varianceList = new LinkedList<Double>();
	private double variance;

	/**
	 * Add a sample to the rolling window.
	 * 
	 * @param value
	 *            The sample value.
	 * @return The variance of the rolling window.
	 */
	public double addSample(double value)
	{
		varianceList.addLast(value);

		enforceWindow();

		return calculateVariance();
	}

	/**
	 * Enforce the rolling window.
	 */
	private void enforceWindow()
	{
		if (varianceList.size() > AccelerationActivity.getSampleWindow())
		{
			varianceList.removeFirst();
		}
	}

	/**
	 * Calculate the variance of the rolling window.
	 * @return The variance of the rolling window.
	 */
	private double calculateVariance()
	{
		if (varianceList.size() > 5)
		{
			variance = StatUtils
					.variance(convertDoubleArray(new Double[varianceList.size()]));
		}

		return variance;
	}

	/**
	 * Transfer an array of Doubles to a primitive array of doubles.
	 * @param array Doubles[]
	 * @return doubles[]
	 */
	private double[] convertDoubleArray(Double[] array)
	{
		double[] d = new double[array.length];

		for (int i = 0; i < d.length; i++)
		{
			if (array[i] != null)
			{
				d[i] = array[i];
			}
		}

		return d;
	}
}
