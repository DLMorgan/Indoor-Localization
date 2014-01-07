Indoor-Localization
===================

UCSB Indoor Localization project

This project has several componenets each working together to provide a best estimate of the users current location. A Map overlay of Harold Frank Hall 4th floor at UCSB is used to show the users current position estimate. The following primary components are fused to provide the estimate.

Wi-Fi FingerPrinting -
This portion of the project uses a pre-built Wi-Fi map to periodically predict the users current position. This prediction is fused with the current estimate to provide a corrected location estimate.

Pedestrian Dead Reckoning -
A Pedometer algorithm has been implemented which uses primary accelerometer data to predict when the user takes a step. Using this information along with the current heading estimate provided by the google API, a real time estimate of the users trajectory is shown on the map.

Step Length Estimation -
Using WEKA (found at http://weka.wikispaces.com/Use+WEKA+in+your+Java+code) within the java program, the Length of each step the user takes is estimated by a prebuilt Linear Regression training set. Each sample relates a users hieght and current step frequency to the length of each step. During localization, this regression maodel is used to give the best estimate for each step a user takes.

Calibration -
The Pedometer algorithm is designed to look for certain patterns that a person has during walking. Therefore at the start of the tracking, the user is asked to take a fixed number of steps, such that the algorithm can set its parameters to best detect steps for a given user.

### Project list

google-play-services_lib - library needed for google maps API

Localized WiFi - integration of WiFi Demo, HFHMap, and WiFi Demo.
