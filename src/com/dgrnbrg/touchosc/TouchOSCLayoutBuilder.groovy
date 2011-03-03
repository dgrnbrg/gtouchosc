package com.dgrnbrg.touchosc
import groovy.xml.MarkupBuilder

enum Orientation {
  HORIZ, VERT
}

enum Device {
  IPHONE, IPAD, IPOD
}

enum BuilderPhase {
  TAB, SPAN, TAB_SPAN
}

class TouchOSCLayoutBuilder {
  def bytesOS
  def mb
  int width, height, padding = 5
  def receiver
  def phase = BuilderPhase.TAB
  def hasTabs = false
  def orientation

  //size in pixels of various things we need for computing the layout
  static def touchOSCBar = 40
  static def iPodVert = [320, 480]
  static def iPadVert = [768, 1024]

  TouchOSCLayoutBuilder() {
    bytesOS = new ByteArrayOutputStream()
    mb = new MarkupBuilder(new PrintWriter(new OutputStreamWriter(bytesOS)))
    mb.doubleQuotes = true
  }

  void build(Device device, Orientation orient, Closure c) {
    def correctMode = [(Device.IPOD): 0, (Device.IPAD): 1, (Device.IPHONE): 0][device]
    def correctOrientation = [(Orientation.VERT): 'horizontal', (Orientation.HORIZ): 'vertical'][orient]

    orientation = orient

    def dim
    switch (device) {
    case Device.IPOD:
    case Device.IPHONE:
      dim = iPodVert
      break
    case Device.IPAD:
      dim = iPadVert
      break
    default: assert false
    }
    switch (orient) {
    case Orientation.HORIZ:
      dim = [dim[1], dim[0]]
    case Orientation.VERT:
      break
    default: assert false
    }
    dim[1] -= touchOSCBar
    width = dim[0]
    height = dim[1]

    c.delegate = builderDelegate
    mb.layout([version: 8, mode: correctMode, orientation: correctOrientation]){
      c()
    }
  }

  String getLayoutXML() {
    return '<?xml version="1.0" encoding="UTF-8"?>\n' + bytesOS.toString()
  }

  def fixFills(Orientation orient, List controls) {
    //compute total proportion we'll be dividing between
    def totalProportion = controls.collect{ it.fill ?: 0 }.sum()
    assert totalProportion <= 1
    //now, we divide the remaining proportion to the ones that didn't have any
    def numNeedingSpace = 0
    controls.each { if (!it.fill) numNeedingSpace++ }
    controls.each {
      if (!it.fill) it.fill = ((1.0 - totalProportion) / numNeedingSpace)
    }
  }

  def doRealLayout(hierarchy) {
    if (!hasTabs) {
      mb.tabpage(name: 1) {
        doRealLayoutHelper(hierarchy, width, height, 0, 0)
      }
    } else {
      doRealLayoutHelper(hierarchy, width, height, 0, 0)
    }
  }

  def doRealLayoutHelper(hierarchy, int width, int height, int hoff, int voff) {
    if (hierarchy.spanControls != null) {
      //it's a span
      def pos = hierarchy.orient == Orientation.HORIZ ? hoff : voff
      hierarchy.spanControls.each {
        switch (hierarchy.orient) {
        case Orientation.HORIZ:
          def w = width * it.fill
          doRealLayoutHelper(it, w as int, height, pos as int, voff)
          pos += w
          break
        case Orientation.VERT:
          def h = height * it.fill
          doRealLayoutHelper(it, width, h as int, hoff, pos as int)
          pos += h
          break
        default: assert false
        }
      }
    } else {
      //it's a control
      if (hierarchy.ignore) return
      switch (orientation) {
      case Orientation.VERT:
        hierarchy.w = width - 2*padding
        hierarchy.h = height - 2*padding
        hierarchy.x = hoff + padding
        hierarchy.y = voff + padding
        break
      case Orientation.HORIZ:
        hierarchy.h = width - 2*padding
        hierarchy.w = height - 2*padding
        hierarchy.y = hoff + padding
        hierarchy.x = this.height - (voff + height - padding)
        break
      default:
        assert false
      }
      hierarchy.remove('fill')
      hierarchy.remove('bind')
      hierarchy.remove('bindReal')
      mb.control(hierarchy)
    }
  }

  def spanStack = []
  def counter = 0
  def countup = {-> counter++}
  void handleMethodMissing(String name, args) {

    def argMap = args.find{it instanceof Map} ?: [:]
    def okKeysList = ['fill', 'osc_cs', 'ignore']
    def argClosure = args.find{it instanceof Closure} ?: {->}

    //default params related fixers
    def defaultify = { defaults ->
      okKeysList += defaults.keySet()
      defaults.putAll(argMap)
      argMap = defaults
    }
    def fixOriented = { ->
      defaultify([orient: orientation == Orientation.HORIZ ? Orientation.VERT : Orientation.HORIZ])
      argMap.type = "$argMap.type${argMap.orient == Orientation.HORIZ ? 'h' : 'v'}"
    }
    def fixScale = {->
      defaultify([scalef: 0.0, scalet: 1.0])
    }
    def fixBasics = {->
      defaultify([type: name])
      defaultify([name: "$argMap.type${countup()}".toString(), color: 'red'])
    }
    def fixLocal = {->
      defaultify([local_off: false])
    }
    def fixInvert = {
      if (it == 'xy') {
        defaultify([inverted_x: true, inverted_y: false])
      } else {
        defaultify([inverted: true])
      }
    }
    def fixSlide = { ->
      fixBasics()
      fixScale()
      fixOriented()
      fixInvert()
      defaultify([centered: false])
    }

    def handleBinding = { Closure wrapper ->
      if (receiver != null)
        okKeysList += ['bind', 'bindReal']
      if (argMap.bind != null) {
        argMap.bindReal = wrapper.curry(argMap.bind)
      }
    }

    //adds current item to build hierarchy
    def pushSpan = {->
      if (phase == BuilderPhase.TAB) {
        if (hasTabs) throw new RuntimeException("Cannot use span outside of tab if using tabs!")
        phase = BuilderPhase.SPAN
      }
      if (spanStack == []) {
        mb.control(argMap)
      } else {
        spanStack[-1] << argMap
      }
    }

    defaultify([osc_cs: "/addr${countup()}".toString()])

    switch (name) {
    case 'span':
      defaultify([orient: Orientation.HORIZ])
      argClosure.delegate = builderDelegate
      //we'll need the orientation and list of controls in the span
      spanStack << []
      argClosure()
      fixFills(argMap.orient, spanStack[-1])
      def oldSpan = spanStack.pop()
      okKeysList << 'spanControls'
      argMap.spanControls = oldSpan
      if (spanStack == []) {
        doRealLayout(argMap)
      } else {
        spanStack[-1] << argMap
      }
      break
    case 'label':
      defaultify([outline: false, background: true, size: 14])
      fixBasics()
      fixOriented()
      okKeysList << 'text'
      argMap.text = argMap.text.bytes.encodeBase64()
      pushSpan()
      break
    case 'push':
      fixBasics()
      fixLocal()
      fixScale()
      pushSpan()
      //true/false on press/release
      handleBinding { binding, time, msg -> binding(msg.arguments[0] == 1) }
      break
    case 'toggle':
      fixBasics()
      fixLocal()
      fixScale()
      pushSpan()
      //true/false on press/release
      handleBinding { binding, time, msg -> binding(msg.arguments[0] == 1) }
      break
    case 'xy':
      fixBasics()
      fixScale()
      fixInvert('xy')
      fixLocal()
      pushSpan()
      //gives x/y coords
      handleBinding { binding, time, msg -> binding(msg.arguments[0], msg.arguments[1]) }
      break
    case 'fader':
      fixSlide()
      pushSpan()
      //gives value
      handleBinding { binding, time, msg -> binding(msg.arguments[0]) }
      break
    case 'rotary':
      fixSlide()
      pushSpan()
      //gives value
      handleBinding { binding, time, msg -> binding(msg.arguments[0]) }
      break
    case 'multifader':
      args = args as List
      if (!(args.size() == 1 || args.size() == 2)) {
        throw new RuntimeException("usage: multifader(int faders, ...)")
      }
      def numArg = args.find { it.toString().isInteger() }
      if (numArg == null) {
        throw new RuntimeException("You need to pass multifader 1 integer")
      }
      defaultify(number: numArg)
      fixSlide()
      pushSpan()
      //gives index and value
      handleBinding { binding, time, msg ->
        def matcher = msg.address =~ '/\\p{Alnum}+/([0-9])+'
        assert matcher.matches()
        binding(matcher.group(1) as Integer, msg.arguments[0])
      }
      break
    case 'multitoggle':
      args = args as List
      if (!(args.size() == 2 || args.size() == 3) &&
          !(args[0].toString().isInteger() && args[1].toString().isInteger())) {
        throw new RuntimeException("usage: multitoggle(int rows, int cols, ...)")
      }
      def numArgs = args.findAll { it.toString().isInteger() }
      if (numArgs?.size() != 2) {
        throw new RuntimeException("You need to pass multitoggle 2 integers")
      }
      defaultify(number_x: numArgs[0], number_y: numArgs[1])
      fixBasics()
      fixScale()
      fixLocal()
      pushSpan()
      //gives index and pushed or not
      handleBinding { binding, time, msg ->
        def matcher = msg.address =~ '/\\p{Alnum}+/([0-9])+/([0-9])+'
        assert matcher.matches()
        binding(matcher.group(1) as Integer, matcher.group(2) as Integer, msg.arguments[0] == 1)
      }
      break
    case 'led':
      fixBasics()
      fixScale()
      pushSpan()
      break
    case 'tab':
      if (phase != BuilderPhase.TAB) {
        throw new RuntimeException("Can only make tabs as top level components (did you make a span or control top level? That implies that it's in the sole tab)")
      }
      hasTabs = true
      phase = BuilderPhase.TAB_SPAN
      argClosure.delegate = builderDelegate
      defaultify([name: argMap.name ?: countup()])
      mb.tabpage(name: argMap.name) {
        argClosure()
      }
      phase = BuilderPhase.TAB
      break
    case 'spacer':
      argMap.ignore = true
      break
    default:
      throw new RuntimeException("Unknown command: $name")
    }

    if (receiver != null && argMap.bindReal != null) {
      //listen for the osc_cs path or any subpath
      receiver.addListener(argMap.osc_cs, argMap.bindReal)
    }

    def extraKeys = argMap.keySet() - okKeysList
    if (extraKeys.size() != 0) {
      throw new RuntimeException("Unknown keys: $extraKeys")
    }
  }

  def handlePropertyMissing(String name) {
    return mb."$name"
  }

  void handlePropertyMissing(String name, value) {
    mb."$name" = value
  }

  def builderDelegate = new Delegator(receiver: this)
}

class Delegator {
  def receiver
  def methodMissing(String name, args) {
    try {
      receiver.handleMethodMissing(name,args)
    } catch (Exception e) {e.printStackTrace(); System.exit(1)}
  }
  def propertyMissing(String name) {
    try {
      receiver.handlePropertyMissing(name)
    } catch (Exception e) {e.printStackTrace(); System.exit(1)}
  }
  def propertyMissing(String name, value) {
    try {
      receiver.handlePropertyMissing(name, value)
    } catch (Exception e) {e.printStackTrace(); System.exit(1)}
  }
}
