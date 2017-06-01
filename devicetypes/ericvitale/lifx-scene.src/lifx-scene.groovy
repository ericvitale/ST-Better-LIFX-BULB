/**
 *  LIFX Scene
 *
 *  Copyright 2016 Eric Vitale
 *
 *  Version 1.0.0 - Initial Release (05/31/2017)
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
	definition (name: "LIFX Scene", namespace: "ericvitale", author: "ericvitale@gmail.com") {
		capability "Actuator"
		capability "Button"
		capability "Momentary"
		capability "Sensor"
        
        command "activateScene"
	}
    
    preferences {
       	section("LIFX Configuration") {
            input "token", "text", title: "API Token", required: true
            input "scene", "text", title: "Scene UUID", required: true
            input "defaultTransition", "decimal", title: "Default Transition Time", required: true, defaultValue: 1.0
        }
       
       	section("Settings") {
	        input "logging", "enum", title: "Log Level", required: false, defaultValue: "INFO", options: ["TRACE", "DEBUG", "INFO", "WARN", "ERROR"]
        }
    }

	tiles {
		standardTile("button", "device.button", width: 2, height: 2, canChangeIcon: true) {
        	state "default", label: "Push", action: "push", icon: "st.unknown.zwave.remote-controller", backgroundColor: "#ffffff"
			state "pushed", label: 'Push', action: "push", backgroundColor: "#ffffff"
		}
        
		main "button"
		details "button"
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

def parse(String description) {
}

def push() {
	activateScene(uuid, defaultTransition)
	sendEvent(name: "button", value: "pushed", isStateChange: true)
    log("Scened uuid: ${uuid} activated.", "DEBUG")
}

def activateScene(uuid, duration=defaultTransition) {
	commandLIFX(uuid, "PUT", "duration=${duration}")
}

////////////   BEGIN LIFX COMMANDS ///////////
def commandLIFX(light, method, commands) {
    def rawURL = "https://api.lifx.com"
    def rawPath = ""
    
    rawPath = "/v1/scenes/scene_id:" + scene + "/activate"
    
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
		httpPut(pollParams) { resp ->
        	log("response: ${resp}", "DEBUG")
        	return parseResponse(resp)
        }
    } catch(Exception e) {
        log(e, "ERROR")
        
        if(e?.getMessage()?.toUpperCase() == "NOT FOUND") {
        	log("LIFX did not understand the scene name. It needs to match uuid from the api.", "ERROR")
        } else if(e?.getMessage()?.toUpperCase() == "UNAUTHORIZED") {
        	log("The API token you entered is not correct and LFIX will not authorize your remote call.", "ERROR")
        }
    }
}

private parseResponse(resp) {
    if (resp.status != 200 && resp.status != 207) {
        log("LIFX Service Unreachable!", "ERROR")
		return []
	}
    
    if(resp.data.results[0] != null) {
    	log("Results: "+resp.data.results[0], "DEBUG")
        if(resp.data.results[0]["status"] != "ok") {
        	log("Something didn't work...", "ERROR")
        }
        return []
    }  
    
    return resp
}
////////////// END LIFX COMMANDS /////////////