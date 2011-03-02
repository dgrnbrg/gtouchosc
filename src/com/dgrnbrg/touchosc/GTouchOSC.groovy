package com.dgrnbrg.touchosc
import com.illposed.osc.*

class GTouchOSC {

  static void main(args) {
    def receiver = new OSCReceiver()
    def b = new TouchOSCLayoutBuilder(receiver: receiver)
    b.build(Device.IPOD, Orientation.VERT){ 
      tab {
        span(orient: Orientation.VERT) {
          spacer()
          span {
            label(text: 'hello, david!')
            push(color: 'yellow', bind: {time, msg -> println "push: $msg.arguments"})
            led(color: 'blue')
            toggle(color: 'blue')
          }
          span {
            fader(orient: Orientation.VERT)
            rotary(orient: Orientation.VERT)
            xy()
          }
          span {
            multifader(8, orient: Orientation.VERT)
            multitoggle(8,8, bind: {time,msg -> println "mt: $msg.arguments"})
          }
        }
      }
      tab {
        span(orient: Orientation.VERT) {
          multifader(4, fill: 0.3)
          multitoggle(16,16)
        }
      }
    }

    def c = new TouchOSCLayoutBuilder()
    c.build(Device.IPOD, Orientation.VERT){
      span{ push(); rotary() }
    }
    net.hexler.touchosc.zeroconf.ZeroConfManager zcm = new net.hexler.touchosc.zeroconf.ZeroConfManager()
    def ls = new GroovyLayoutServer(xmlString: b.getLayoutXML(), name: "Test.touchosc")
    ls.startService()
    receiver.startListening()

    zcm.startService()
//    zcm.stopService()
//    ls.stopService()
  }
}


