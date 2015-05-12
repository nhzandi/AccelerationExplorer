package com.kircherelectronics.accelerationexplorer.filter;

public interface ImuLinearAccelerationInterface
{
	/**
	 * Get the linear acceleration of the device. This method can be called
	 * *only* after setAcceleration(), setMagnetic() and getGyroscope() have
	 * been called.
	 * 
	 * @return float[] an array containing the linear acceleration of the device
	 *         where [0] = x, [1] = y and [2] = z with respect to the Android
	 *         coordinate system.
	 */
	public float[] getLinearAcceleration();

	/**
	 * The acceleration of the device. Presumably from Sensor.TYPE_ACCELERATION.
	 * 
	 * @param acceleration
	 *            The acceleration of the device.
	 */
	public void setAcceleration(float[] acceleration);

	/**
	 * Set the gyroscope rotation. Presumably from Sensor.TYPE_GYROSCOPE
	 * 
	 * @param gyroscope
	 *            the rotation of the device.
	 * @param timeStamp
	 *            the time the measurement was taken.
	 */
	public void setGyroscope(float[] gyroscope, long timeStamp);

	/**
	 * The complementary filter coefficient, a floating point value between 0-1,
	 * exclusive of 0, inclusive of 1.
	 * 
	 * @param filterCoefficient
	 */
	public void setFilterCoefficient(float filterCoefficient);

	/**
	 * Set the magnetic field... presumably from Sensorr.TYPE_MAGNETIC_FIELD.
	 * 
	 * @param magnetic
	 *            the magnetic field
	 */
	public void setMagnetic(float[] magnetic);
}
