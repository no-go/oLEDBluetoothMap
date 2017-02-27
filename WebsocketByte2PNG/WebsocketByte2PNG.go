package main

import (
	"flag"
	"log"
	"net/http"
	"io/ioutil"

	"github.com/gorilla/websocket"
)

var addr = flag.String("addr", "0.0.0.0:65000", "http service address")

var upgrader = websocket.Upgrader{} // use default options

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
