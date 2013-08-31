blue-pi
=======

Steer a [Pololu 3pi robot](http://www.pololu.com/catalog/product/975 "robot") with an Android device, over Bluetooth, using either the Android device's accelerometer or with button presses.

Load the [serial slave program](http://www.pololu.com/docs/0J21/10.a "serial slave program") onto the 3pi to communicate with it.

For Bluetooth connectivity, I put a Bluetooth Arduino atop the 3pi and connected the serial lines,
with the Arduino programmed to simply pass along commands.

To steer with the accelerometer, hold the device sideways.  Tilt it forward or back to drive
forward or reverse; degree of tilt controls speed.  Tilt left or right to change direction.
