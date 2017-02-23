#define OLED_DC      5
#define OLED_CS     12
#define OLED_RESET   6

#include <Adafruit_GFX.h>
#include "Adafruit_SSD1331.h"
#include <SPI.h>

#include "Adafruit_BLE.h"
#include "Adafruit_BluefruitLE_SPI.h"
#include "BluefruitConfig.h"

// TextSize 0: 8 lines, 15 chars, Pixel: 96x64 waveshare
Adafruit_SSD1331 oled = Adafruit_SSD1331(OLED_CS, OLED_DC, OLED_RESET); 
Adafruit_BluefruitLE_SPI ble(BLUEFRUIT_SPI_CS, BLUEFRUIT_SPI_IRQ, BLUEFRUIT_SPI_RST);

bool isFirst = true;
byte last;
short color;

void setup(void) {
  oled.begin();
  //oled.fillScreen(0x0050);
  //oled.print("\n oLED Bluetooth\nMap and Picture\n\n\nHello, dude!");
  //delay(3000);
  oled.fillScreen(0x0000);
  
  ble.begin(false);
  ble.echo(false);
  ble.sendCommandCheckOK("AT+HWModeLED=BLEUART");
  ble.setMode(BLUEFRUIT_MODE_DATA);
}

void loop() {
  delay(40);
  if (ble.isConnected()) {
  while ( ble.available() ) {
    byte inbyte = (byte) ble.read();
    if (!isFirst) {
      color = last;
      color = color<<8;
      color += inbyte;
      oled.pushColor(color);
      isFirst = true;
    } else {
      last = inbyte;
      isFirst = false;
    };
  }
}
}
