package com.dgrnbrg.touchosc
import com.illposed.osc.*
import com.illposed.osc.utility.*

class OSCReceiver {
  int port = 15200
  def runningThread
  def converter = new OSCByteArrayToJavaConverter();
  def listeners = [:]

  void addListener(String path, Closure listener) {
    listeners[path] = listener
  }

  boolean removeListener(String key) {
    return listeners.remove(key)
  }

  def handlePacket(byte[] buf, int length) {
    def oscPacket = converter.convert(buf, length)
    def packets = []
    def timestamp
    switch (oscPacket) {
      case OSCBundle:
        timestamp = oscPacket.timeStamp
        packets = oscPacket.packets
        break
      case OSCMessage:
        timestamp = null
        packets = [oscPacket]
        break
      default:
        assert false
    }
    listeners.each { path, closure ->
      def pattern = ~path
      packets.each { packet ->
        if (pattern.matcher(packet.address).matches()) {
          closure(timestamp, packet)
        }
      }
    }
  }

  void startListening() {
    byte[] buf = new byte[4096]
    DatagramPacket dp = new DatagramPacket(buf, buf.length)
    DatagramSocket s = new DatagramSocket(port)
    runningThread = Thread.start { ->
      while(true) {
        s.receive(dp)
        handlePacket(buf, dp.length)
      }
    }
  }

  void stopListening() {
    runningThread.interrupt()
  }
}
