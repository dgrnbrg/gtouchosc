package com.dgrnbrg.touchosc
import com.illposed.osc.*

class GTouchOSC {

  static void main(args) {
    def receiver = new OSCReceiver()
    def b = new TouchOSCLayoutBuilder(receiver: receiver)
    b.build(Device.IPOD, Orientation.VERT) {
      tab {
        span (orient: Orientation.VERT) {
          push(fill: 0.3, bind: { pressed -> println "pushed button $pressed" })
          span {
            xy(fill: 0.8, bind: {x,y -> println "xy $x $y" })
            label(orient: Orientation.VERT, text: "Hello Alec!")
          }
        }
      }
      tab {
        span{toggle(bind: {pressed -> println "toggle $pressed"})}
      }
    }
/*
    b.build(Device.IPOD, Orientation.HORIZ){
      tab {
        span(orient: Orientation.VERT) {
          spacer()
          span {
            label(text: 'hello, david!')
            push(color: 'yellow', bind: {pushed -> println "push: $pushed"})
            led(color: 'blue')
            toggle(color: 'blue')
          }
          span {
            fader(orient: Orientation.VERT)
            rotary(orient: Orientation.VERT)
          }
          span {
            multifader(8, orient: Orientation.VERT, bind: {i, v -> println "multifader $i $v"})
            multitoggle(8,8, bind: {x, y, pushed -> println "mt: $x $y $pushed"})
          }
        }
      }
      tab {
        span(orient: Orientation.VERT) {
          multifader(4, fill: 0.3)
//          multitoggle(16,16)
            xy(bind: {x,y -> println "xy: $x $y"})
        }
      }
    }
*/

//    receiver.addListener('/') {time,msg -> println "$msg.address: $msg.arguments"}

    net.hexler.touchosc.zeroconf.ZeroConfManager zcm = new net.hexler.touchosc.zeroconf.ZeroConfManager()
    def ls = new GroovyLayoutServer(xmlString: b.getLayoutXML(), name: "bar.touchosc")
    ls.startService()
    receiver.startListening()

    zcm.startService()
//    zcm.stopService()
//    ls.stopService()
  }
}


