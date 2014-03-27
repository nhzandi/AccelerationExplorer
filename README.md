AccelerationExplorer
====================

Read the full description in this [blog article](http://blog.kircherelectronics.com/blog/index.php/85-blog/android-articles/android-sensor-articles/73-android-accelerometer-noise-offset-skew).

Download the free application from the [Goolge Play Store](https://play.google.com/store/apps/details?id=com.kircherelectronics.accelerationexplorer&hl=en)

Acceleration Explorer will plot the acceleration sensor outputs in real time to the screen. The acceleration sensor outputs can also be logged to a .CSV file and be opened in any spreedsheet application.

Analog gauges of the acceleration and rotation of the device are also displayed to help further visualize the acceleration sensor outputs.

Discover the noise, offset and skew associated with your Android devices acceleration sensor! 

Acceleration Explorer allows users to investigate the noise, offset and skew associated with the accelerometer sensor on Android devices. After a quick calibration process, Acceleration Explorer will calculate the magnitude of each axis and the noise associated with it. Acceleration Explorer also determines the minimum and maximum amplitudes of each axis along with the update frequency of the acceleration sensor.

Why would you want to know about noise, offset and skew?

We want to know about noise, offset and skew because they are aspects that make the sensor less accurate that we can partially compensate for. For example, you may want to implement a low-pass filter or mean filter to smooth the acceleration sensors output and knowing how much noise the sensor has is very useful. Or you may want to know how accurately your device measures gravity, tilt or acceleration.

Compare devices!

Acceleration Explorer makes it easy to compare the performance of the acceleration sensors of multiple devices. Not every Android device is the same and knowing the range of how acceleration sensors perform can be very helpful.

Write better code!

Acceleration Explorer can help fine tune your acceleration sensor algorithms, especially low-pass filters, mean filters and normalizing sensor outputs.

Noise:

It is known that most Android devices do some form of black-box (meaning we have no idea what is actually going on under the hood) filtering with their sensors before providing an output, and some sensors even have filters designed into them. However, it would be useful to at least some idea of what kind of filtering is already occurring in cases where we would like to do filtering of our own. Knowing how noisy the output of the sensor should be is an excellent place to start.

A fair amount of information can be gained by determining what hardware is being used on the Android device in question and then referring to data sheets to determine the expected noise density of the sensor. The noise density, denoted in units of ug/sqrt(Hz), is defined as the noise per unit of square root bandwidth and can be used to determine the expected noise output from a sensor. To determine the expected noise output from noise density, we take the equivalent noise bandwidth, B, of the output filter. The equivalent noise bandwidth of a filter is the -3dB bandwidth multiplied by a coefficient corresponding to the order of the filter.

Offset:

Offset occurs when the same measurement, say gravity, is taken with both the positive and negative axis of an accelerometer sensor and a difference is apparent in the measurements. In one dimension, it means that the center of the measurement will not occur at 0,0 but rather a point that is not centered. This is apparent when you look at the results from measuring gravity with the positive side of the accelerometer and the same measurement with the negative side of the accelerometer.

Sensor Skew and the Ellipse:

The really bad possibility is that the accelerometer values are skewed from a sphere into an ellipse with two different radii (and offset from center). To understand why a skewed ellipse can distort accelerometer data so much, consider that in a sphere the diameter in the x-axis and y-axis are always equal to each other and always orthogonal (90 degrees) to each other. So, as you rotate the device through the axes, the lengths of the axes will remain the same and equal. If the lengths of the axes are equal, the magnitude of the acceleration will always be the same, equal to gravity, as the device is rotated through the axes. That means you can measure the tilt of the device with the accelerometers accurately, for instance (assuming the device is not accelerating).

When the accelerometer values are skewed into an ellipse, the length of one axis is shorter or longer than the other axis. That means that as you rotate the device through the axes the lengths of the axis will constantly be changing. The values of each axis will also change in very non-linear way, making all kinds of applications much more difficult to implement. Measuring the tilt of the device accurately is nearly impossible with an ellipse.

![Alt text](http://blog.kircherelectronics.com/blog/images/screenshot.png "Android Acceleration Explorer Screenshot")

Written by [Kircher Electronics](https://www.kircherelectronics.com).
Released by [Boki Software](https://www.bokisoftware.com).
