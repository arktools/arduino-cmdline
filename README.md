###Building

``` console
ant
```

###Testing

``` console
ant test
```

###Cleaning

``` console
ant clean
```

###Example Usage:

``` console
java -jar dist/arduino-cmdline.jar --sketch=test/Blink --output=build/Blink --port=/dev/ttyUSB0 --target=mega2560 --preferences=lib/preferences.txt --preprocess
```
