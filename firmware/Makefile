PROZ=m328p
UISP=avrdude
PROGRAMMER=usbasp

all:
	$(UISP) -B 300 -c $(PROGRAMMER) -p $(PROZ) -U flash:w:firmware.ino.with_bootloader.eightanaloginputs.hex

fuse:
	$(UISP) -B 300 -c $(PROGRAMMER) -p $(PROZ) -U hfuse:w:0xDA:m -U lfuse:w:0xEE:m -U efuse:w:0xFD:m
