#define OLED_DC      5
#define OLED_CS     12
#define OLED_RESET   6

#define VBATPIN     A7  // A7 = D9 !!

// Color definitions
#define BLACK           0x0000
#define BLUE            0x0050
#define GREEN           0x07E0
#define RED             0xF800
#define WHITE           0xFFFF

#include <Adafruit_GFX.h>
#include "Adafruit_SSD1331.h"
#include <SPI.h>

#include "Adafruit_BLE.h"
#include "Adafruit_BluefruitLE_SPI.h"
#include "BluefruitConfig.h"

// TextSize 0: 8 lines, 15 chars, Pixel: 96x64 waveshare
Adafruit_SSD1331 oled = Adafruit_SSD1331(OLED_CS, OLED_DC, OLED_RESET); 

Adafruit_BluefruitLE_SPI ble(BLUEFRUIT_SPI_CS, BLUEFRUIT_SPI_IRQ, BLUEFRUIT_SPI_RST);

byte pix;

bool isFirst = true;
byte last;
short color;

int readVcc() {
  float mv = analogRead(VBATPIN);
  mv *= 2;
  mv *= 3.3;
  return mv;
}

void setup(void) {
  oled.begin();
  oled.fillScreen(BLUE);
  oled.setTextColor(GREEN);
  oled.setCursor(6,20);
  oled.print("oLED Bluetooth");
  oled.setTextColor(WHITE);
  oled.setCursor(4,35);
  oled.print("Map and Picture");
  oled.setCursor(30,55);
  oled.setTextColor(RED, BLACK);
  oled.print(readVcc());
  oled.print(" mV");
  delay(3000);
  oled.fillScreen(0x0000);
  oled.goHome();
  
  ble.begin(false);
  ble.echo(false);
  ble.sendCommandCheckOK("AT+HWModeLED=BLEUART");
  ble.sendCommandCheckOK("AT+GAPDEVNAME=oLED Feather");
  ble.sendCommandCheckOK("ATE=0");
  ble.sendCommandCheckOK("AT+BAUDRATE=115200");
  ble.sendCommandCheckOK("AT+BLEPOWERLEVEL=4");
  ble.sendCommandCheckOK("ATZ");
  ble.setMode(BLUEFRUIT_MODE_DATA);
  ble.verbose(false);
}

void loop() {
  if (ble.isConnected()) {
    while ( ble.available() ) {
      pix = (byte) ble.read();
      if (!isFirst) {
        isFirst = true;
        color = last;
        color = color<<8;
        color += pix;
        oled.pushColor(color);
      } else {
        isFirst = false;
        last = pix;
      }
    }
  } else {
    delay(100);
  }
}
