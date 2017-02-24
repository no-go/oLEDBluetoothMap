# oLED Bluetooth Map

Get position from smartphone, generates a map from osm, send map via UART bluefruit to a RGB display.
App can take pictures and sends them, too! You can switch between nRF (bluefruit) or HM10 (CC2541)
Bluetooth module. For feather M0 you need my SSD1331 lib (Adafruit based) and switch the app to *nRF* and *slow*.

If you garant the App notification access, it will send App icon and notification!

Support me: <a href="https://flattr.com/submit/auto?fid=o6wo7q&url=https%3A%2F%2Fgithub.com%2Fno-go%2FoLEDBluetoothMap" target="_blank">![Flattr This](stuff/flattr.png)</a>

[APK File](https://raw.githubusercontent.com/no-go/oLEDBluetoothMap/hm10_cc2541/app/app-release.apk) or (in future) get the App from [google play](https://play.google.com/store/apps/details?id=click.dummer.oLEDBluetoothMap).

## App Icon

![logo](app/src/main/res/mipmap/logo.png)

## Circuit

![](stuff/circuit.png)

## Screenshots

![](stuff/screenshot1.jpg)

![](stuff/screenshot2.jpg)

![feather M0 and SSD1331 oLED](stuff/screenshot3.jpg)

![](stuff/screenshot4.jpg)

Examples with Notification access (you need to change your android settings).

## Privacy policy

Google Play requires me to disclose this App will take camera pictures and needs access to your position. 
This App caches the map on your phone. Thus it needs access to your files. The files or your position is not send to me and everything is realy private :-D Ok, OpenStreet Map needs the position to generate the map. Ask them, what they are doing with that stuff :-/ The cam picture does not send it to a network (just bluetooth) or is stored permanently on your phone.
