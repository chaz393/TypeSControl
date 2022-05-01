# TypeSControl

This is an app with Tasker integration for turning on and off a set of ProSeries underglow LEDs made by TypeS
I use it to automatically turn on the LEDs when I start my car (I have a Tasker profile that triggers when my Bluetooth audio reciever powers on and connects to my phone)
If you want to use this app to control your own set of underglow, all you'll need is the mac addresses of your modules and the value used to turn on and off your modules (I assume it's different for every set since TypeS makes you set a password during setup). You can get these values from enabling Bluetooth snooping in the developer options and taking a bug report. You can open the Bluetooth log in Wireshark and find the values relatively easily
It would also be a good idea to double check that the service and characteristic UUID is the same for your modules. The easiest way to check that would probably be to use the nRF Connect app and read the services/characteristics of one of your modules
