package main

import (
	"flag"
	"log"
	"net/http"
	"io/ioutil"
	
	"fmt"

	"github.com/gorilla/websocket"
	"github.com/andlabs/ui"
)

var addr = flag.String("addr", "0.0.0.0:65000", "http service address")

var upgrader = websocket.Upgrader{} // use default options

var box = ui.NewVerticalBox()
//var windowExists = false
var window = ui.NewWindow("WebsocketByte2PNG", 200, 100, false)
var messages = 0

func popUp(msg string) {
	messages++
	box = ui.NewVerticalBox()
	box.Append(ui.NewLabel(fmt.Sprintf("%s %d",msg, messages)), false)
	window.SetChild(box)
	//if (windowExists == false) {
		ui.Main(func() {
	//		windowExists = true
			window.OnClosing(func(*ui.Window) bool {
				//windowExists = false
				ui.Quit()
				return true
			})
			window.Show()
		})
	//}
}

func echo(w http.ResponseWriter, r *http.Request) {
	c, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		log.Print("upgrade:", err)
		return
	}
	defer c.Close()
	for {
		_, message, err := c.ReadMessage()
		if err != nil {
			break
		}
		//log.Printf("recv")
		err = ioutil.WriteFile("out.png", []byte(message), 0644)
		popUp("new message")
		if err != nil {
			log.Println("error:", err)
			break
		}
	}
}

func main() {
	flag.Parse()
	log.SetFlags(0)
	http.HandleFunc("/", echo)
	log.Fatal(http.ListenAndServe(*addr, nil))
}
