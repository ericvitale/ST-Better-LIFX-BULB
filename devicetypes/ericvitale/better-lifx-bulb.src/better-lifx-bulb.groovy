/**
 *  Better LIFX Bulb
 *
 *  Copyright 2016 Eric Vitale
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
metadata {
	definition (name: "Better LIFX Bulb", namespace: "ericvitale", author: "Eric Vitale") {
		capability "Actuator"
		capability "Color Control"
		capability "Color Temperature"
		capability "Configuration"
		capability "Polling"
		capability "Refresh"
		capability "Sensor"
		capability "Switch"
		capability "Switch Level"
	}
    
    preferences {
       	section("Bulb Configuration") {
            input "token", "text", title: "API Token", required: true
            input title: "LFIX API", description: "The LIFX API token is required to use your account. Navigate to https://cloud.lifx.com, sign in and then go to the settings section and select generate new token.", type: "paragraph", element: "paragraph"

            input "bulb", "text", title: "Bulb Name", required: true
            input title: "bulbHelp", description: "This is the name of the bulb found in the LIFX app, it is case sensitive. It must be entered exactly as you named it in the LIFX app.", type: "paragraph", element: "paragraph"
        }
       
       	section("Settings") {
	        input "logging", "enum", title: "Log Level", required: false, defaultValue: "DEBUG", options: ["TRACE", "DEBUG", "INFO", "WARN", "ERROR"]
            input "turnOnWithAdjustments", "bool", title: "Turn on lights when making adjustments?", required: true, defaultValue: true
    	    input "useSchedule", "bool", title: "Use Schedule", required: false, defaultValue: false
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
        }
        
        valueTile("Brightness", "device.level", width: 2, height: 1) {
        	state "level", label: 'Brightness ${currentValue}%'
        }
        
        controlTile("levelSliderControl", "device.level", "slider", width: 4, height: 1) {
        	state "level", action:"switch level.setLevel"
        }

        /*standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat") {
			state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
		}*/
        
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
        
        //standardTile("sceneOne", "device.sceneOne", inactiveLabel: false, decoration: "flat", height: 2, width: 2) {
		//	state "default", label:"Scene One", action:"sceneOne", icon:"http://hosted.lifx.co/smartthings/v1/196xOn.png"
		//}
       
        //standardTile("sceneTwo", "device.sceneTwo", inactiveLabel: false, decoration: "flat", height: 2, width: 2) {
		//	state "default", label:"Scene Two", action:"sceneTwo", icon:"http://hosted.lifx.co/smartthings/v1/196xOn.png"
		//}
        
        //standardTile("sceneThree", "device.sceneThree", inactiveLabel: false, decoration: "flat", height: 2, width: 2) {
		//	state "default", label:"Scene Three", action:"sceneThree", icon:"http://hosted.lifx.co/smartthings/v1/196xOn.png"
		//}

        main(["switch"])
        //details(["switch", "Brightness", "levelSliderControl", "colorTemp", "colorTempSliderControl", "rgbSelector", "poll", "sceneOne", "sceneTwo", "sceneThree", "refresh"])
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
    setupSchedule()
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
    	commandLIFX(bulb, "PUT", [color: "saturation:${saturation}+hue:${hue}", power: "on"])
    	sendEvent(name: "color", value: setColor.hex)
        sendEvent(name: "switch", value: "on")
    } else {
        commandLIFX(bulb, "PUT", [color: "saturation:${saturation}+hue:${hue}"])
    	sendEvent(name: "color", value: setColor.hex)
    }
    
    log("End setColor(${value}).", "DEBUG")
}

def setColorTemperature(colorTemperature) {
	log("Begin on().", "DEBUG")
    if(turnOnWithAdjustments) {
    	commandLIFX(bulb, "PUT", [color: "kelvin:${colorTemperature}", power: "on"])
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
	handleResponse(commandLIFX(bulb, "GET", ""))
    //commandLIFX(bulb, "GET", "")
    log("End refresh().", "DEBUG")
}

def on() {
	log("Begin on().", "DEBUG")
    commandLIFX(bulb, "PUT", "power=on")
    sendEvent(name: "switch", value: "on")
	log("End on().", "DEBUG")
}

def off() {
	log("Begin off().", "DEBUG")
    commandLIFX(bulb, "PUT", "power=off")
    sendEvent(name: "switch", value: "off")
	log("End off().", "DEBUG")
}

def setLevel(brightness) {
	log("Begin setLevel(...)", "DEBUG")
    log("Brightness level selected = ${brightness}.", "DEBUG")
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

////////////   BEGIN LIFX COMMANDS ///////////

def commandLIFX(light, method, commands) {
    def rawURL = "https://api.lifx.com"
    //def rawPath = "/v1/lights/label:" + light + "/state"
    def rawPath = ""
    if(method == "PUT") {
    	rawPath = "/v1/lights/label:" + light + "/state"
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
    Response: [[product:[company:LIFX, name:Color 1000, capabilities:[has_variable_color_temp:true, has_color:true], identifier:lifx_color_a19], brightness:1.0, id:d073d512e65d, location:[id:0f111fc37110c0462c0dc86f62acedf0, name:Turkey Foot], color:[saturation:0.0, kelvin:4362, hue:246.38864728770886], connected:true, power:off, label:Office Light, uuid:029c74a5-263d-47cd-9b35-182b9610eb40, last_seen:2016-07-31T20:20:51.168+01:00, group:[id:924792021a11aebc749879571138f74f, name:Office], seconds_since_seen:0.00232187]]
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
         	[id:0f111fc37110c0462c0dc86f62acedf0, 
         	name:Turkey Foot],
         color:
         	[saturation:0.0, kelvin:4362, hue:246.38864728770886], 
         connected:true, 
         power:off, 
         label:Office Light, 
         uuid:029c74a5-263d-47cd-9b35-182b9610eb40, 
         last_seen:2016-07-31T20:20:51.168+01:00, 
         group:
         	[id:924792021a11aebc749879571138f74f, name:Office], 
         seconds_since_seen:0.00232187]]
    
    */    
    resp.data.each {
    
    	log("${it.label} is ${it.power}.", "DEBUG")
        log("Bulb Type: ${it.product.name}.", "TRACE")
        log("Capabilities? Color Temperature = ${it.product.capabilities.has_variable_color_temp}, Is Color = ${it.product.capabilities.has_color}.", "TRACE")
        log("Brightness = ${it.brightness}.", "TRACE")
        log("Color = [saturation:${it.color.saturation}], kelvin:${it.color.kelvin}, hue:${it.color.hue}.", "TRACE")
        
    	if(it.power == "on") {
        	sendEvent(name: "switch", value: "on")
			if(it.color.saturation == 0.0) {
            	log("Saturation is 0.0, setting color temperature.", "DEBUG")
                sendEvent(name: "colorTemperature", value: it.color.kelvin)
       			sendEvent(name: "color", value: "#ffffff")
                sendEvent(name: "level", value: (it.brightness * 100).toInteger())
        		sendEvent(name: "switch", value: "on")
            } else {
            	log("Saturation is > 0.0, setting color.", "DEBUG")
                //def hex = hslToHex(it.color.hue, it.color.saturation, it.brightness)
                //log("hex = ${hex}.", "DEBUG")
              	//sendEvent(name: "color", value: hex)
                sendEvent(name: "level", value: (it.brightness * 100).toInteger())
		        sendEvent(name: "switch", value: "on")
            }
        } else if(it.power == "off") {
	        sendEvent(name: "switch", value: "off")
        }
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

private hslToHex(hue, sat, lum) {
	log("hue = ${hue}.", "DEBUG")
	def tR = (hue / 360) + 0.333
    def tG = (hue / 360)
    def tB = (hue / 360) - 0.333
    def red
    def green
    def blue
    def t1// = lum * (1.0 + 0.67)
    def t2 = 2 * lum - 0.4676
    
    if(lum < 0.5) {
    	t1 = lum * (1.0 + sat)
    } else {
    	t1 = lum + sat - lum * sat
    }
    
    //Calculate R
    red = (doCrazyMath(tR, t1, t2) * 255).toInteger()
    green = (doCrazyMath(tG, t1, t2) * 255).toInteger()
    blue = (doCrazyMath(tB, t1, t2) * 255).toInteger()
    
    log("red = ${red}.", "DEBUG")
    log("green = ${green}.", "DEBUG")
    log("blue = ${blue}.", "DEBUG")
    
    def hex = "#" + Integer.toHexString(red) + Integer.toHexString(green) + Integer.toHexString(blue)
    log("hex = ${hex}.", "DEBUG")
    return hex
    //return "#ff0000"
}

def doCrazyMath(temp, t1, t2) {
	if((6 * temp) < 1) {
    	return t2 + (t1 - t2) * 6 * temp
    } else if((2 * temp) < 1) {
    	return t1
    } else if((3 * temp) < 2) {
    	return t2 + (t1 - t2) * (0.666 - temp) * 6
    } else {
    	return t2
    }
}