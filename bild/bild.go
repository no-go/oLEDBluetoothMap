package main

import (
	"github.com/andlabs/ui"
)

type MyHandler struct {
}

func (h *MyHandler) Draw(a *ui.Area, dp *ui.AreaDrawParams) {
	// draw a filled rectangle with dodger blue color
	bt := []ui.GradientStop{ ui.GradientStop{0.0,0.0,0.0,0.0,0.0} }
	brush := ui.Brush{ui.Solid,0,0,0,0, 0,0,1,1,1, bt}
	path := ui.NewPath(ui.Winding)
	path.AddRectangle(0, 0, dp.AreaWidth, dp.AreaHeight)
	path.End()
	dp.Context.Fill(path, &brush)
	dp.Context.Clip(path)
	path.Free()
}
func (h *MyHandler) MouseEvent(a *ui.Area, me *ui.AreaMouseEvent) {
	
}
func (h *MyHandler) MouseCrossed(a *ui.Area, left bool) {
	
}
func (h *MyHandler) DragBroken(a *ui.Area) {
	
}

func (h *MyHandler) KeyEvent(a *ui.Area, ke *ui.AreaKeyEvent) (handled bool) {
	return false
}



func main() {
	ui.Main(func() {
		window := ui.NewWindow("boo", 200, 200, false)
		box := ui.NewVerticalBox()
		mh := MyHandler{}
		ar := ui.NewArea(&mh)
		box.Append(ar, false)
		window.SetChild(box)
		window.OnClosing(func(*ui.Window) bool {
			ui.Quit()
			return true
		})
		window.Show()
	})
}
