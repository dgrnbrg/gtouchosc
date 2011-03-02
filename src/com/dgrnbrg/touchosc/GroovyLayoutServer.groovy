package com.dgrnbrg.touchosc

//We use a helper b/c NanoHTTPD starts when it's constructed
class GroovyLayoutServer {
  String xmlString
  String name
  def helper

  void setName(String name) {
    this.name = name
    if (!name.endsWith(".touchosc"))
      this.name += ".touchosc"
  }

  void startService() {
    if (helper == null)
      helper = new GroovyLayoutServerHelper(xmlString: xmlString, name: name)
  }

  void stopService() {
    helper?.stop()
    helper = null
  }
}

class GroovyLayoutServerHelper extends NanoHTTPD {
  String xmlString
  String name

  GroovyLayoutServerHelper() {
    super(9658);
  }

  public NanoHTTPD.Response serve(String uri, String method, Properties header, Properties parms)
  {
    println "sending data: $xmlString"
    NanoHTTPD.Response r = new NanoHTTPD.Response(this, "200 OK", "application/touchosc", xmlString)
    r.addHeader("Content-Disposition", "attachment; filename=\"" + name + "\"")
    return r
  }
}
