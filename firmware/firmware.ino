#define sclk   13
#define mosi   11
#define cs     10
#define rst    9
#define dc     8

#include <Adafruit_GFX.h>
#include <Adafruit_SSD1331.h>
#include <SPI.h>

// TextSize 0: 8 lines, 15 chars, Pixel: 96x64 waveshare
Adafruit_SSD1331 oled = Adafruit_SSD1331(cs, dc, rst); 

bool isFirst = true;
byte last;
short color;

void setup(void) {
  oled.begin();
  //oled.fillScreen(0x0050);
  //oled.print("\n oLED Bluetooth\nMap and Picture\n\n\nHello, dude!");
  //delay(3000);
  oled.fillScreen(0x0000);
  Serial.begin(9600);
}

void serialEvent() {
  while (Serial.available()) {
    byte inbyte = (byte) Serial.read();
    
    if (!isFirst) {
      color = last;
      color = color<<8;
      color += inbyte;
      oled.pushColor(color);
      isFirst = true;
    } else {
      last = inbyte;
      isFirst = false;
    }
  }
}

void loop() {
  delay(40); // ?!?
}
