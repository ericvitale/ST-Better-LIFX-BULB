/**
 * Better LIFX Bulb
 *
 * Copyright 2016 Eric Vitale
 *
 * Version 1.1.0 - Updated to use the ST Beta Asynchronous API. (06/21/17)
 * Version 1.0.6 - Added the transitionLevel(), apiFlash(), & runEffect() methods. (06/16/2017)
 * Version 1.0.5 - Added saturation:0 to setColorTemperature per LIFX's recommendation. (05/22/2017)
 * Verison 1.0.4 - Fixed an issue with setColor() introduced by an api change. (05/19/2017)
 * Version 1.0.3 - Updated the scheduling settings (04/18/2017)
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

include 'asynchttp_v1'

import java.text.DecimalFormat;
 
metadata {
	definition (name: "Better LIFX Bulb Async", namespace: "ericvitale", author: "ericvitale@gmail.com") {
		capability "Actuator"
		capability "Color Control"
		capability "Color Temperature"
		capability "Configuration"
		capability "Polling"
		capability "Refresh"
		capability "Sensor"
		capability "Switch"
		capability "Switch Level"
        
        command "transitionLevel"
        command "runEffect"
        command "apiFlash"
        
        attribute "lastRefresh", "string"
        attribute "refreshText", "string"
	}
    
    preferences {
       	section("Bulb Configuration") {
            input "token", "text", title: "API Token", required: true
            input title: "LFIX API", description: "The LIFX API token is required to use your account. Navigate to https://cloud.lifx.com, sign in and then go to the settings section and select generate new token.", type: "paragraph", element: "paragraph"

            input "bulb", "text", title: "Bulb Name", required: true
            input title: "bulbHelp", description: "This is the name of the bulb found in the LIFX app, it is case sensitive. It must be entered exactly as you named it in the LIFX app.", type: "paragraph", element: "paragraph"
        }
       
       	section("Settings") {
	        input "logging", "enum", title: "Log Level", required: false, defaultValue: "INFO", options: ["TRACE", "DEBUG", "INFO", "WARN", "ERROR"]
            input "turnOnWithAdjustments", "bool", title: "Turn on lights when making adjustments?", required: true, defaultValue: true
            input "bRefreshOnSchedule", "bool", title: "Use Schedule", required: false, defaultValue: true
          	input "frequency", "number", title: "Frequency?", required: false, range: "1..*", defaultValue: 15
   	 		input "startHour", "number", title: "Schedule Start Hour", required: false, range: "0..23", defaultValue: 7
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
    
    log("Scheduling initial refresh...", "INFO")
    runIn(2, refresh)
    
    log("bRefreshOnSchedule = ${bRefreshOnSchedule}.", "DEBUG")
    
    setScheduleRefreshEnabled(bRefreshOnSchedule)
    
    if(isScheduledRefreshEnabled()) {
		log("Scheduling auto refresh.", "INFO")
		setupSchedule()
	}
    
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
    	commandLIFX(bulb, "PUT", [color: "kelvin:${colorTemperature} saturation:0", power: "on"])
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
	commandLIFX(bulb, "GET", [])
    log("End refresh().", "DEBUG")
}

def on() {
	log("Begin on().", "DEBUG")
    log("Turning bulb on.", "INFO")
    commandLIFX(bulb, "PUT", ["power" : "on"])
    sendEvent(name: "switch", value: "on")
    refresh()
	log("End on().", "DEBUG")
}

def off() {
	log("Begin off().", "DEBUG")
    log("Turning bulb off.", "INFO")
    commandLIFX(bulb, "PUT", ["power" : "off"])
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

def setLevel(value, duration) {
	log("Begin setting groups level to ${value} over ${duration} seconds.", "DEBUG")
    
    commandLIFX(bulb, "PUT", ["brightness": brightnessPercent, "power": "on", "duration": duration])
    sendEvent(name: "level", value: brightness)
    if(value > 0) {
	    sendEvent(name: "switch", value: "on")
	} else {
    	sendEvent(name: "switch", value: "off")
    }
	
    log("End setting groups level.", "DEBUG")
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

	def params = [
        uri: rawURL,
		path: rawPath,
		headers: rawHeaders,
        body: commands
    ]
    
    log("Full URL/Path = ${rawPath}.", "DEBUG")
    log("rawHeaders = ${rawHeaders}.", "DEBUG")
    log("body = ${commands}.", "DEBUG")
    
    if(method=="GET") {
    	asynchttp_v1.get('getResponseHandler', params)
    } else if(method=="PUT") {
    	asynchttp_v1.put('putResponseHandler', params)
    } else if(method=="POST") {
    	asynchttp_v1.post('postResponseHandler', params)
    }   
}

def postResponseHandler(response, data) {

    if(response.getStatus() == 200 || response.getStatus() == 207) {
		log("LFIX Success.", "DEBUG")
        updateDeviceLastActivity(new Date())
    } else {
    	log("LIFX failed to adjust bulb. LIFX returned ${response.getStatus()}.", "ERROR")
        log("Error = ${response.getErrorData()}", "ERROR")
    }
}

def putResponseHandler(response, data) {

    if(response.getStatus() == 200 || response.getStatus() == 207) {
		log("LFIX Success.", "DEBUG")
        updateDeviceLastActivity(new Date())
    } else {
    	log("LIFX failed to adjust bulb. LIFX returned ${response.getStatus()}.", "ERROR")
        log("Error = ${response.getErrorData()}", "ERROR")
    }
}

def getResponseHandler(response, data) {

    if(response.getStatus() == 200 || response.getStatus() == 207) {
		log("LFIX Success.", "DEBUG")
        updateDeviceLastActivity(new Date())
        
       	response.getJson().each {
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
    } else {
    	log("LIFX failed to adjust bulb. LIFX returned ${response.getStatus()}.", "ERROR")
        log("Error = ${response.getErrorData()}", "ERROR")
    }
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
    
    if(isScheduledRefreshEnabled()) {
        
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

private isScheduledRefreshEnabled() {
	if(state.scheduledRefresh == null) {
    	state.scheduledRefresh = false
    }
    return state.scheduledRefresh
}

private setScheduleRefreshEnabled(value) {
	state.scheduledRefresh = value
}
