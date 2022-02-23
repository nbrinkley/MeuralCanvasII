/**
 *  Meural Canvas II v0.00 Beta
 *
 *  Copyright 2022 Norbert Brinkley
 *
 *  Thanks to Joel Wetzel for his iPhone WiFi Presence Sensor v1.03 driver that helped me get started on this
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
 *  Release Notes:
 *  v0.00 Beta
 *
 *  Future additions:
 *  /remote/get_galleries_json
 *  
 */

import groovy.json.*
import java.text.SimpleDateFormat 

	
metadata {
	definition (name: "Meural Canvas II", namespace: "norbertbrinkley", author: "Norbert Brinkley") {
		capability "Refresh"
		capability "Light"
        capability "Switch"
        capability "SwitchLevel"
        capability "Actuator"
        
        //{"status":"pass","response":{"product":"69L31175A0141","version":"2.2.10_2.0.13","als":true,"backlight":"69","lux":"50","gsensor":"landscape","server":{"state":"online","sync":"2022-01-12 07:12:36","error":""},"date":{"day":"Tuesday","month":"February","day_month":"22","time":"08:59","tz":"EST","epoch":"1645538373"},"orientation":"landscape","alias":"klimt-785","name":"Living Room Left","wifi_status":{"state":"wifi","name":"Brinkley","ip":"192.168.68.63","signal":"-39","freq":"5240","bit":"433.3","remote":false,"bands":"2","softap":{"name":"meural-klimt-785","password":"f434d47e","ip":"NA","state":"offline"}},"bt_status":{"state":"offline","ip":"","key":"535215"},"lan_status":{"state":"offline","ip":""},"free_space":3979,"boot_status":"image","sdcard":true,"language":"en","country":"US"}}
    
        attribute "product", "string"
        attribute "version", "string"
        //attribute "als", "boolean"  //misleading as it does not change with a manual override
        attribute "backlight", "number"
        attribute "lux", "number"
        attribute "asofdatetime", "string"
        attribute "name", "string"
        attribute "current_gallery_name", "string"
        
        attribute "level", "number"
        attribute "switch", "string"
        command "setLevel", [[name: "Set Brightness", type: "NUMBER", description: "Manually set brightness level as a percent. Meural restricts the range to 9 through 94.  Use 0 or 100 to restore automatic level setting"]]
        
        //Example command "testEnum", [[name:"Testing Enum", type: "ENUM", description: "Pick an option", constraints: ["one","two","three"] ] ]
	}

	preferences {
		section {
			input (
				type: "string",
				name: "ipAddress",
				title: "Meural IP Address",
				required: true				
			)
			input (
				type: "bool",
				name: "enableDebugLogging",
				title: "Enable Debug Logging?",
				required: true,
				defaultValue: true
			)
		}
	}
}


def log(msg) {
	if (enableDebugLogging) {
		log.debug(msg)	
	}
}

void logOff() {
    log.info "${device.displayName}.logOff()"
    device.updateSetting('enableDebugLogging',[value:'false',type:'bool'])
}

def installed() {
	log.info "${device.displayName}.installed()"
    refresh()
}

def initialize() {
    log.info "${device.displayName}.initialize()"
    refresh()
}

def updated() {
    log.info "${device.displayName}.updated()"
    
    /*if (level != null) {
        setBrightness(level)
    }*/
    refresh()
}

def refresh() {
    log.info "${device.displayName}.refresh()"
        
    updateState()
    unschedule()
    
    // disable logs in 30 minutes, placed after unschedule() to avoid turning this scheduled item off
    if (enableDebugLogging) {
        runIn(1800, logOff) 
    }

    runEvery5Minutes(updateState)
}

def updateState() {
    log "${device.displayName}.updateState()"
    
	if (ipAddress == null || ipAddress.size() == 0) {
		return
	}
	
	asynchttpGet("httpGetSystemInfo", [
		uri: "http://${ipAddress}",
        path: "/remote/control_check/system/",
        timeout: 10
	]);
    
	asynchttpGet("httpGetGalleryInfo", [
		uri: "http://${ipAddress}",
        path: "/remote/get_gallery_status_json",
        timeout: 10
	]);
}

def httpGetCallback(response, data) {
    meuralResponse = response.getJson()
    log "${device.displayName}: ${meuralResponse.status}"
}

def httpGetSystemInfo(response, data) {
    meuralResponse = response.getJson()
    log "${device.displayName}: ${meuralResponse.status}"
    
    //System info
    try {
        sendEvent(name: "product", value: meuralResponse.response.product)
        sendEvent(name: "version", value: meuralResponse.response.version)
        sendEvent(name: "backlight", value: meuralResponse.response.backlight)
        sendEvent(name: "lux", value: meuralResponse.response.lux)
        sendEvent(name: "name", value: meuralResponse.response.name)
        
        Date dateObj =  new Date(1000L * (meuralResponse.response.date.epoch as int))
        def cleanDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(dateObj)
        sendEvent(name: "asofdatetime", value: cleanDate)
    }
    catch (e)
    {
        log "${device.displayName}: ${e}"
    }
}

def httpGetGalleryInfo(response, data) {
    meuralResponse = response.getJson()
    log "${device.displayName}: ${meuralResponse.status}"
    
    //Gallery info
    try {
        sendEvent(name: "current_gallery_name", value: meuralResponse.response.current_gallery_name)
    }
    catch (e)
    {
        log "${device.displayName}: ${e}"
    }
}

def on() {
    log "${device.displayName}.on()"
    
	if (ipAddress == null || ipAddress.size() == 0) {
		return
	}
	
	asynchttpGet("httpGetCallback", [
		uri: "http://${ipAddress}",
        path: "/remote/control_command/resume",
        timeout: 10
	]);

    state.switch = "on"
    sendEvent(name: "switch", value: "on")
}

def off(){
    log "${device.displayName}.off()"
    
	if (ipAddress == null || ipAddress.size() == 0) {
		return
	}
	
	asynchttpGet("httpGetCallback", [
		uri: "http://${ipAddress}",
        path: "/remote/control_command/suspend",
        timeout: 10
	]);
    
    state.switch = "off"
    sendEvent(name: "switch", value: "off")
}

def setLevel(level) {
    setLevel(level, 0)
}

def setLevel(level, duration) {
    log "${device.displayName}.setLevel()"
    
	if (ipAddress == null || ipAddress.size() == 0 || level == null) {
		return
	}
    
    state.level = level
    sendEvent(name: "backlight", value: level)
	
    if (level == 0 || level == 100) {
        
	    asynchttpGet("httpGetCallback", [
		    uri: "http://${ipAddress}",
            path: "/remote/control_command/als_calibrate/off/",
            timeout: 10
        ]);
    }
    else {   
        
	    asynchttpGet("httpGetCallback", [
		    uri: "http://${ipAddress}",
            path: "/remote/control_command/set_backlight/${level + 1}/", //buggy meural is off by one
            timeout: 10
	    ]);
    }
}
