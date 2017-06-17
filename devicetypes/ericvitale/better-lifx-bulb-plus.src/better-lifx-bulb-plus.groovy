/**
 * Better LIFX Bulb Plus
 *
 * Copyright 2016 Eric Vitale
 *
 * Version 1.0.6 - Added the transitionLevel(), apiFlash(), & runEffect() methods. (06/16/2017)
 * Version 1.0.4 - Added saturation:0 to setColorTemperature per LIFX's recommendation. (05/22/2017)
 * Version 1.0.3 - Fixed an issue with setColor() introduced by an api change. (05/19/2017)
 * Version 1.0.2 - More accuracy for setLevel (12/17/2016)
 * Version 1.0.1 - Added additonal logging on refresh method (12/17/2016)
 * Version 1.0.0 - Initial Release (08/08/2016)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
import java.text.DecimalFormat;
 
metadata {
	definition (name: "Better LIFX Bulb Plus", namespace: "ericvitale", author: "ericvitale@gmail.com") {
		capability "Actuator"
		capability "Color Control"
		capability "Color Temperature"
		capability "Configuration"
		capability "Polling"
		capability "Refresh"
		capability "Sensor"
		capability "Switch"
		capability "Switch Level"
        
        attribute "lastRefresh", "string"
        attribute "refreshText", "string"
        attribute "irState", "string"
        
        command "irOn", ["number"]
        command "irOff"
        command "transitionLevel"
        command "runEffect"
        command "apiFlash"
	}
    
    preferences {
       	section("Bulb Configuration") {
            input "token", "text", title: "API Token", required: true
            input title: "LFIX API", description: "The LIFX API token is required to use your account. Navigate to https://cloud.lifx.com, sign in and then go to the settings section and select generate new token.", type: "paragraph", element: "paragraph"

            input "bulb", "text", title: "Bulb Name", required: true
            input title: "Bulb Name Help", description: "This is the name of the bulb found in the LIFX app, it is case sensitive. It must be entered exactly as you named it in the LIFX app.", type: "paragraph", element: "paragraph"
            
            input "ir", "decimal", title: "IR Level", required: true, range: "0..1"
        }
       
       	section("Settings") {
	        input "logging", "enum", title: "Log Level", required: false, defaultValue: "INFO", options: ["TRACE", "DEBUG", "INFO", "WARN", "ERROR"]
            input "turnOnWithAdjustments", "bool", title: "Turn on lights when making adjustments?", required: true, defaultValue: true

			input title: "Schedule Help", description: "This setting will schedule the device handler to request updates from the LIFX cloud to keep the SmartThings device up to date if you use other means (LIFX App) to set the device.", type: "paragraph", element: "paragraph"
            input "useSchedule", "bool", title: "Refresh on Schedule?", required: false, defaultValue: true, submitOnChange: true
            input "frequency", "number", title: "Every X Minutes", required: false, range: "1..*", defaultValue: 15
       	 	input "startHour", "number", title: "Schedule Start Hour", required: false, range: "0..23", defaultValue: 0
       		input "endHour", "number", title: "Schedule End Hour", required: false, range: "0..23", defaultValue: 23
        }
    }

	tiles(scale: 2) {
    	multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label:'${name}', action:"switch.off", icon:"http://hosted.lifx.co/smartthings/v1/196xOn.png", backgroundColor:"#79b821"//, nextState:"turningOff"
				attributeState "off", label:'${name}', action:"switch.on", icon:"http://hosted.lifx.co/smartthings/v1/196xOff.png", backgroundColor:"#ffffff"//, nextState:"turningOn"
			}
            
            tileAttribute ("device.level", key: "SECONDARY_CONTROL") {
				attributeState "default", label:'${currentValue}%'
			}
            
            tileAttribute ("device.level", key: "SLIDER_CONTROL") {
        		attributeState "default", action:"switch level.setLevel"
            }
        }
        
        multiAttributeTile(name:"switchDetails", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label:'${name}', action:"switch.off", icon:"http://hosted.lifx.co/smartthings/v1/196xOn.png", backgroundColor:"#79b821"
				attributeState "off", label:'${name}', action:"switch.on", icon:"http://hosted.lifx.co/smartthings/v1/196xOff.png", backgroundColor:"#ffffff"
			}
            
            tileAttribute ("device.lastActivity", key: "SECONDARY_CONTROL") {
				attributeState "default", label:'Last activity: ${currentValue}', action: "refresh.refresh"
			}
        }
        
        valueTile("Brightness", "device.level", width: 2, height: 1) {
        	state "level", label: 'Brightness ${currentValue}%'
        }
        
        controlTile("levelSliderControl", "device.level", "slider", width: 4, height: 1) {
        	state "level", action:"switch level.setLevel"
        }
        
        valueTile("colorTemp", "device.colorTemperature", inactiveLabel: false, decoration: "flat", height: 1, width: 2) {
			state "colorTemp", label: '${currentValue}K'
		}
        
		controlTile("colorTempSliderControl", "device.colorTemperature", "slider", height: 1, width: 4, inactiveLabel: false, range:"(2500..9000)") {
			state "colorTemp", action:"color temperature.setColorTemperature"
		}
        
        controlTile("rgbSelector", "device.color", "color", height: 3, width: 3, inactiveLabel: false) {
            state "color", action:"setColor"
        }
        
        standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat", height: 3, width: 3) {
			state "default", label:"", action:"refresh.refresh", icon: "st.secondary.refresh"
		}

        main(["switch"])
        details(["switchDetails", "Brightness", "levelSliderControl", "colorTemp", "colorTempSliderControl", "rgbSelector", "refresh"])
    }
}

private determineLogLevel(data) {
    switch (data?.toUpperCase()) {
        case "TRACE":
            return 0
            break
        case "DEBUG":
            return 1
            break
        case "INFO":
            return 2
            break
        case "WARN":
            return 3
            break
        case "ERROR":
        	return 4
            break
        default:
            return 1
    }
}

def log(data, type) {
    data = "LIFX -- ${device.label} -- ${data ?: ''}"
        
    if (determineLogLevel(type) >= determineLogLevel(settings?.logging ?: "INFO")) {
        switch (type?.toUpperCase()) {
            case "TRACE":
                log.trace "${data}"
                break
            case "DEBUG":
                log.debug "${data}"
                break
            case "INFO":
                log.info "${data}"
                break
            case "WARN":
                log.warn "${data}"
                break
            case "ERROR":
                log.error "${data}"
                break
            default:
                log.error "LIFX -- ${device.label} -- Invalid Log Setting of ${type}."
                log.error "Message = ${data}."
        }
    }
}

def installed() {
	log("Begin installed().", "DEBUG")
	initialize()
    log("End installed().", "DEBUG")
}

def updated() {
	log("Begin updated().", "DEBUG")
	initialize()
    log("End updated().", "DEBUG")
}

def initialize() {
	log("Begin initialize.", "DEBUG")
    runIn(2, refresh)
    log("Scheduling initial refresh...", "INFO")
    setupSchedule()
    log("Setup refresh schedule...", "INFO")
	log("End initialize.", "DEBUG")
}

def configure() {
	log.debug "Executing 'configure'"
}

def parse(String description) {
	log("Parsing '${description}'". "DEBUG")
}

def setHue(val) {
	log("setHue() is unsuppored by this DH.", "WARN")
}

def setSaturation() {
	log("setSaturation() is unsuppored by this DH.", "WARN")
}

def setColor(setColor) {
	log("Begin setColor(${setColor}).", "DEBUG")
    log("Color HEX: ${setColor.hex}.", "DEBUG")

    def hue = setColor.hue * 3.6
    def saturation = setColor.saturation / 100
    
    if(turnOnWithAdjustments) {
    	commandLIFX(bulb, "PUT", [color: "saturation:${saturation} hue:${hue}", power: "on"])
    	sendEvent(name: "color", value: setColor.hex)
        sendEvent(name: "switch", value: "on")
    } else {
        commandLIFX(bulb, "PUT", [color: "saturation:${saturation} hue:${hue}"])
    	sendEvent(name: "color", value: setColor.hex)
    }
    
    log("End setColor(${value}).", "DEBUG")
}

def setColorTemperature(colorTemperature) {
	log("Begin on().", "DEBUG")
    log("Color temperature selected = ${colorTemperature}K.", "INFO")
    if(turnOnWithAdjustments) {
    	commandLIFX(bulb, "PUT", [color: "kelvin:${colorTemperature saturation:0}", power: "on"])
    	sendEvent(name: "colorTemperature", value: colorTemperature)
        sendEvent(name: "color", value: "#ffffff")
        sendEvent(name: "switch", value: "on")
    } else {
        commandLIFX(bulb, "PUT", "color=kelvin:${colorTemperature}")
    	sendEvent(name: "colorTemperature", value: colorTemperature)
    }

	log("End on().", "DEBUG")
}

def poll() {
	log("Poll().", "DEBUG")
    refresh()
}

def refresh() {
	log("Begin refresh().", "DEBUG")
    log("Beginning device update...", "INFO")
	handleResponse(commandLIFX(bulb, "GET", ""))
    log("End refresh().", "DEBUG")
}

def on() {
	log("Begin on().", "DEBUG")
    log("Turning bulb on.", "INFO")
    commandLIFX(bulb, "PUT", "power=on")
    //commandLIFX(bulb, "PUT", ["power": "on", "infrared": 0.8])
    runIn(10, irOff)
    sendEvent(name: "switch", value: "on")
    refresh()
	log("End on().", "DEBUG")
}

def off() {
	log("Begin off().", "DEBUG")
    log("Turning bulb off, IR = off.", "INFO")
    commandLIFX(bulb, "PUT", "power=off")
    runIn(10, irOn)
    sendEvent(name: "switch", value: "off")
	log("End off().", "DEBUG")
}

def transitionLevel(value, duration=1.0) {
	log("transitionLevel(${value}, ${duration})", "DEBUG")
	setLevel(value, duration)
}

def setLevel(brightness) {
	log("Begin setLevel(...)", "DEBUG")
    log("Brightness level selected = ${brightness}.", "INFO")
    def brightnessPercent = brightness / 100
    
    if(turnOnWithAdjustments) {
	    commandLIFX(bulb, "PUT", ["brightness": brightnessPercent, "power": "on"])
        sendEvent(name: "level", value: brightness)
        sendEvent(name: "switch", value: "on")
    } else {
    	commandLIFX(bulb, "PUT", ["brightness": brightnessPercent])
        sendEvent(name: "level", value: brightness)
    }
    
	log("End setLevel(...)", "DEBUG")
}

def irOn() {
	irOn(ir)
}

def irOn(val) {
	log("Begin irOn(${val}).", "DEBUG")
    DecimalFormat df = new DecimalFormat("###,##0.#")
    def newVal = df.format(val)
    log("newVal = ${newVal}.", "DEBUG")
    commandLIFX(bulb, "PUT", ["infrared": val])
    val = val * 100
    sendEvent(name: "irState", value: "on", descriptionText: "Infrared is at ${val}%.")
	log("End irOn().", "DEBUG")
}

def irOff() {
	log("Begin irOff().", "DEBUG")
    commandLIFX(bulb, "PUT", "infrared=0")
    sendEvent(name: "irState", value: "off", descriptionText: "Infrared is at 0%.")
	log("End irOff().", "DEBUG")
}

def runEffect(effect="pulse", color="blue", from_color="red", cycles=5, period=0.5, brightness=0.5) {
	log("runEffect(effect=${effect}, color=${color}: 1.0, from_color=${from_color}, cycles=${cycles}, period=${period}, brightness=${brightness}.", "INFO")

	if(effect != "pulse" && effect != "breathe") {
    	log("${effect} is not a value effect, defaulting to pulse.", "ERROR")
        effect = "pulse"
    }
	
    commandLIFX(bulb, "POST", ["color" : "${color.toLowerCase()} brightness:${brightness}", "from_color" : "${from_color.toLowerCase()} brightness:${brightness}", "cycles" : "${cycles}" ,"period" : "${period}"], effect)
}

def apiFlash(cycles=5, period=0.5, brightness1=1.0, brightness2=0.0) {
    if(brightness1 < 0.0) {
    	brightness1 = 0.0
    } else if(brightness1 > 1.0) {
    	brightness1 = 1.0
    }
    
    if(brightness2 < 0.0) {
    	brightness2 = 0.0
    } else if(brightness2 > 1.0) {
    	brightness2 = 1.0
    }
    
    log("The Group is: ${state.groupsList}", "DEBUG")
    
    commandLIFX(	bulb, 
    				"POST",
                    ["color" : "brightness:${brightness1}", "from_color" : "brightness:${brightness2}", "cycles" : "${cycles}" ,"period" : "${period}"],
                    "pulse"
               )
}

////////////   BEGIN LIFX COMMANDS ///////////

def commandLIFX(light, method, commands, effect=null) {
    def rawURL = "https://api.lifx.com"
    def rawPath = ""
    
    if(method == "PUT") {
    	rawPath = "/v1/lights/label:" + light + "/state"
    } else if (effect == "pulse" || effect == "breathe") {
    	rawPath = "/v1/lights/label:" + light + "/effects/" + effect 
    } else {
    	rawPath = "/v1/lights/label:" + light
    }
    
    def rawHeaders = ["Content-Type": "application/x-www-form-urlencoded", "Authorization": "Bearer ${token}"]
    def pollParams = [
        uri: rawURL,
		path: rawPath,
		headers: rawHeaders,
        body: commands
    ]
    
    log("Full URL/Path = ${rawURL}${rawPath}.", "DEBUG")
    log("rawHeaders = ${rawHeaders}.", "DEBUG")
    log("body = ${commands}.", "DEBUG")
    
    try {
        if(method=="GET") {
            httpGet(pollParams) { resp ->
            	log("response: ${resp}", "DEBUG")
                return parseResponse(resp)
            }
        } else if(method=="PUT") {
            httpPut(pollParams) { resp ->
            	log("response: ${resp}", "DEBUG")
                return parseResponse(resp)
            }
        } else if(method=="POST") {
            httpPost(pollParams) { resp ->            
                parseResponse(resp)
            }
        }
    } catch(Exception e) {
        log(e, "ERROR")
        if(e?.getMessage()?.toUpperCase() == "NOT FOUND") {
        	log("LIFX did not understand the bulb names. It needs to match what is in your LIFX app and they are case sensitive.", "ERROR")
        } else if(e?.getMessage()?.toUpperCase() == "UNAUTHORIZED") {
        	log("The API token you entered is not correct and LFIX will not authorize your remote call.", "ERROR")
        }
    }
}

private parseResponse(resp) {
    if (resp.status == 404) {
		sendEvent(name: "switch", value: "unreachable")
        log("LIFX Service Unreachable!", "ERROR")
		return []
	}
    
    if(resp.data.results[0] != null) {
    	log("Results: "+resp.data.results[0], "DEBUG")
        return []
    }  
    
    return resp
}

def handleResponse(resp) {
    log("Response: " + resp.data, "DEBUG")
    
    if (resp.status == 404) {
		sendEvent(name: "switch", value: "unreachable")
        log("LIFX Service Unreachable!", "INFO")
		return []
	}
    
    /*
   
    [[product:
    	[company:LIFX, 
         name:Color 1000, 
         capabilities:
         	[has_variable_color_temp:true, 
            has_color:true], 
         identifier:lifx_color_a19], 
         brightness:1.0, 
         id:d073d512e65d, 
         location:
         	[id:x, 
         	name:Turkey Foot],
         color:
         	[saturation:0.0, kelvin:4362, hue:246.38864728770886], 
         connected:true, 
         power:off, 
         label:Office Light, 
         uuid:xxx-263d-47cd-9b35-182b9610eb40, 
         last_seen:2016-07-31T20:20:51.168+01:00, 
         group:
         	[id:xx, name:Office], 
         seconds_since_seen:0.00232187]]
    
    */    
    resp.data.each {
    
    	log("${it.label} is ${it.power}.", "TRACE")
        log("Bulb Type: ${it.product.name}.", "TRACE")
        log("Capabilities? Color Temperature = ${it.product.capabilities.has_variable_color_temp}, Is Color = ${it.product.capabilities.has_color}.", "TRACE")
        log("Brightness = ${it.brightness}.", "TRACE")
        log("Color = [saturation:${it.color.saturation}], kelvin:${it.color.kelvin}, hue:${it.color.hue}.", "TRACE")
        
        def refreshT = "${it.label} is ${it.power}"
        
        DecimalFormat df = new DecimalFormat("###,##0.0#")
        DecimalFormat dfl = new DecimalFormat("###,##0.000")
        DecimalFormat df0 = new DecimalFormat("###,##0")
        
        if(it.power == "on") {
        	refreshT += " with a brightness of ${df.format(it.brightness * 100)}%. Color @ [saturation:${df.format(it.color.saturation)}], kelvin:${it.color.kelvin}, hue:${dfl.format(it.color.hue)}."
        } else {
        	refreshT += "."
        }
        
        log("${refreshT}", "INFO")
        
        sendEvent(name: "lastRefresh", value: new Date())
        sendEvent(name: "refreshText", value: refreshT)
        
    	if(it.power == "on") {
        	sendEvent(name: "switch", value: "on")
			if(it.color.saturation == 0.0) {
            	log("Saturation is 0.0, setting color temperature.", "DEBUG")
                
                def b = df0.format(it.brightness * 100)
                
                sendEvent(name: "colorTemperature", value: it.color.kelvin)
       			sendEvent(name: "color", value: "#ffffff")
                sendEvent(name: "level", value: b)
        		sendEvent(name: "switch", value: "on")
            } else {
            	log("Saturation is > 0.0, setting color.", "DEBUG")
                def h = df.format(it.color.hue)
                def s = df.format(it.color.saturation)
                def b = df0.format(it.brightness * 100)
                
                log("h = ${h}, s = ${s}.", "INFO")
                
                sendEvent(name: "hue", value: h, displayed: true)
                sendEvent(name: "saturation", value: s, displayed: true)
                sendEvent(name: "kelvin", value: it.color.kelvin, displayed: true)
                sendEvent(name: "level", value: b)
		        sendEvent(name: "switch", value: "on")
            }
        } else if(it.power == "off") {
	        sendEvent(name: "switch", value: "off")
        }
    }
    
    log("Device update received from LIFX...", "INFO")
    updateDeviceLastActivity(new Date())
}

////////////// END LIFX COMMANDS /////////////

def setupSchedule() {
	log("Begin setupSchedule().", "DEBUG")
    
    try {
	    unschedule(refresh)
    } catch(e) {
    	log("Failed to unschedule!", "ERROR")
        log("Exception ${e}", "ERROR")
        return
    }

	log("useSchedule = ${useSchedule}.", "DEBUG")
    
    if(useSchedule) {
        
        try {
        	schedule("17 0/${frequency.toString()} ${startHour.toString()}-${endHour.toString()} * * ?", refresh)
            log("Refresh scheduled to run every ${frequency.toString()} minutes between hours ${startHour.toString()}-${endHour.toString()}.", "INFO")
        } catch(e) {
        	log("Failed to set schedule!", "ERROR")
            log("Exception ${e}", "ERROR")
        } 
    }
    
    log("End setupSchedule().", "DEBUG")
}

def updateDeviceLastActivity(lastActivity) {
	def finalString = lastActivity?.format('MM/d/yyyy hh:mm a',location.timeZone)    
	sendEvent(name: "lastActivity", value: finalString, display: false , displayed: false)
}