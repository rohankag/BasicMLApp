# BasicMLApp

To build an application which recognize human activities using machine learning technique. To recognize the activities, gathering datasets related to human actions in daily life would be required. Fortunately, we could solve this gathering issue using the smartphone. Nowadays, most people have their smartphone and the smartphones have many sensors such as accelerometer, gyroscope, orientation, GPS, proximity, etc. By using these sensors, we can obtain the datasets required for identifying human movements. 

Part A
	In this part, you should generate the training database file to classify three different activities: walking, running, and eating. Each activity duration should be 5 seconds. The data sampling frequency should be over 10 Hz for the proper accuracy. Also, the number of each activity in the training dataset should be over 20. According to recent research papers, accelerometer sensor datasets (X, Y, and Z axes) are very useful for human activity recognition, so we suggest to use these accelerometer sensors as input dataset among many sensors in the smartphone. The below table is an example of database scheme. 

Part B
	Based on the database which you generated (Part A), your application should classify the activities using Support Vector Machine. You can download free java SVM library from (http://www.csie.ntu.edu.tw/~cjlin/libsvm). You can download the android SVM application using the library (https://github.com/cnbuff410/Libsvm-androidjni). Also, it is fine to use other third party library or implement it yourself if you want. In your app, the SVM parameters and test accuracy should be displayed. The accuracy should be over 60%. For the validated test accuracy, we suggest to use ‘K-fold cross-validation technique’ which is supported by the library. The ‘K’ should be between three and five.

Steps to Use Application : 
1.) Install the SVM application.
2.) Copy "training_set" to "/storage/emulated/0/databaseFolder/" directory of your phone.
3.) Run Application
4.) Click Train button to train SVM.
5.) After training the SVM successfully, click "Classify" button to calculate accuracy based on given data.

Calculated Accuracy with given database : 61.11 %