/**
 *  GE Motion Dimmer Switch
 *	Author: Matt lebaugh (@mlebaugh)
 *
 * Based off of the Dimmer Switch under Templates in the IDE 
 * Copyright (C) Matt LeBaugh
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
	definition (name: "GE Motion Switch 26931", namespace: "mlebaugh", author: "Matt LeBaugh") {
		capability "Motion Sensor"
        capability "Actuator"
 		capability "Switch"
		capability "Polling"
		capability "Refresh"
		capability "Sensor"
		capability "Health Check"
		capability "Light"
        capability "Button"

		command "toggleMode"
        command "Occupancy"
        command "Vacancy"
        command "Manual"
        
        attribute "operatingMode", "enum", ["Manual", "Vacancy", "Occupancy"]

		fingerprint mfr:"0063", prod:"494D", model: "3032", deviceJoinName: "GE Z-Wave Plus Motion Wall Switch"
	}

	// simulator metadata
	simulator {
		status "on":  "command: 2003, payload: FF"
		status "off": "command: 2003, payload: 00"

		// reply messages
		reply "2001FF,delay 100,2502": "command: 2503, payload: FF"
		reply "200100,delay 100,2502": "command: 2503, payload: 00"
	}

	preferences {
        	input title: "", description: "Select your prefrences here, they will be sent to the device once updated.\n\nTo verify the current settings of the device, they will be shown in the 'recently' page once any setting is updated", displayDuringSetup: false, type: "paragraph", element: "paragraph"
			input (
                name: "operationmode",
                title: "Operating Mode",
                description: "Occupancy: Automatically turn on and off the light with motion\nVacancy: Manually turn on, automatically turn off light with no motion.",
                type: "enum",
                options: [
                    "1" : "Manual",
                    "2" : "Vacancy (auto-off)",
                    "3" : "Occupancy (auto-on/off)",
                ],
                required: false
            )
            input (
                name: "timeoutduration",
                title: "Timeout Duration",
                description: "Length of time after no motion for the light to shut off in Occupancy/Vacancy modes",
                type: "enum",
                options: [
                    "0" : "Test (5s)",
                    "1" : "1 minute",
                    "5" : "5 minutes (default)",
                    "15" : "15 minutes",
                    "30" : "30 minutes",
                    "255" : "disabled"
                ],
                required: false
            )
			input (
                name: "motionsensitivity",
                title: "Motion Sensitivity",
                description: "Motion Sensitivity",
                type: "enum",
                options: [
                    "1" : "High",
                    "2" : "Medium (default)",
                    "3" : "Low"
                ],
                required: false
            )
			input (
                name: "lightsense",
                title: "Light Sensing",
                description: "If enabled, Occupancy mode will only turn light on if it is dark",
                type: "enum",
                options: [
                    "0" : "Disabled",
                    "1" : "Enabled",
                ],
                required: false
            )
			
			input (
                name: "motion",
                title: "Motion Sensor",
                description: "Enable/Disable Motion Sensor.",
                type: "enum",
                options: [
                    "0" : "Disable",
                    "1" : "Enable",
                ],
                required: false
            )
            input (
                name: "invertSwitch",
                title: "Switch Orentation",
                type: "enum",
                options: [
                    "0" : "Normal",
                    "1" : "Inverted",
                ],
                required: false
            )
            input (
                name: "resetcycle",
                title: "Reset Cycle",
                type: "enum",
                options: [
                    "0" : "Disabled",
                    "1" : "10 sec",
                    "2" : "20 sec (default)",
                    "3" : "30 sec",
                    "4" : "45 sec",
                    "110" : "27 mins",
                ],
                required: false
            )
            input (
            type: "paragraph",
            element: "paragraph",
            title: "Configure Association Groups:",
            description: "Devices in association group 2 will receive Basic Set commands directly from the switch when it is turned on or off. Use this to control another device as if it was connected to this switch.\n\n" +
                         "Devices in association group 3 will receive Basic Set commands directly from the switch when it is double tapped up or down.\n\n" +
                         "Devices are entered as a comma delimited list of IDs in hexadecimal format."
        	)

        	input (
            	name: "requestedGroup2",
            	title: "Association Group 2 Members (Max of 5):",
            	type: "text",
            	required: false
        	)

        	input (
            	name: "requestedGroup3",
            	title: "Association Group 3 Members (Max of 4):",
            	type: "text",
            	required: false
        	)
    }

	tiles(scale: 2) {
		multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label: '${name}', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#00A0DC"
				attributeState "off", label: '${name}', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
			}
		}
        
		standardTile("motion","device.motion", inactiveLabel: false, width: 2, height: 2) {
                state "inactive",label:'no motion',icon:"st.motion.motion.inactive",backgroundColor:"#ffffff"
                state "active",label:'motion',icon:"st.motion.motion.active",backgroundColor:"#53a7c0"
		}

		standardTile("refresh", "device.switch", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}
              
		standardTile("operatingmode", "device.operatingmode", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label:'Mode: ${currentValue} tap to toggle', unit:"", action:"toggleMode"
		}

		main(["switch"])
		details(["switch", "motion", "operatingmode", "refresh"])

	}
}


def parse(String description) {
    def result = null
	if (description != "updated") {
		log.debug "parse() >> zwave.parse($description)"
		def cmd = zwave.parse(description, [0x20: 1, 0x25: 1, 0x56: 1, 0x70: 2, 0x72: 2, 0x85: 2, 0x71: 3])
		if (cmd) {
			result = zwaveEvent(cmd)
        }
	}
    if (!result) { log.warn "Parse returned ${result} for $description" }
    else {log.debug "Parse returned ${result}"}
	return result
	if (result?.name == 'hail' && hubFirmwareLessThan("000.011.00602")) {
		result = [result, response(zwave.basicV1.basicGet())]
		log.debug "Was hailed: requesting state update"
	}
	return result
}

def zwaveEvent(physicalgraph.zwave.commands.crc16encapv1.Crc16Encap cmd) {
	log.debug("zwaveEvent(): CRC-16 Encapsulation Command received: ${cmd}")
	def encapsulatedCommand = zwave.commandClass(cmd.commandClass)?.command(cmd.command)?.parse(cmd.data)
	if (!encapsulatedCommand) {
		log.debug("zwaveEvent(): Could not extract command from ${cmd}")
	} else {
		return zwaveEvent(encapsulatedCommand)
	}
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd) {
	[name: "switch", value: cmd.value ? "on" : "off", type: "physical"]
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd) {
    log.debug "---BASIC SET V1--- ${device.displayName} sent ${cmd}"
	def result = []
    result << createEvent([name: "switch", value: cmd.value ? "on" : "off", type: "physical"])
    if (cmd.value == 255) {
    	result << createEvent([name: "button", value: "pushed", data: [buttonNumber: "1"], descriptionText: "On/Up on (button 1) $device.displayName was pushed", isStateChange: true, type: "physical"])
    }
	else if (cmd.value == 0) {
    	result << createEvent([name: "button", value: "pushed", data: [buttonNumber: "2"], descriptionText: "Off/Down (button 2) on $device.displayName was pushed", isStateChange: true, type: "physical"])
    }
    return result
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationReport cmd) {
	log.debug "---ASSOCIATION REPORT V2--- ${device.displayName} sent groupingIdentifier: ${cmd.groupingIdentifier} maxNodesSupported: ${cmd.maxNodesSupported} nodeId: ${cmd.nodeId} reportsToFollow: ${cmd.reportsToFollow}"
    state.group3 = "1,2"
    if (cmd.groupingIdentifier == 3) {
    	if (cmd.nodeId.contains(zwaveHubNodeId)) {
        	sendEvent(name: "numberOfButtons", value: 2, displayed: false)
        }
        else {
        	sendEvent(name: "numberOfButtons", value: 0, displayed: false)
			sendHubCommand(new physicalgraph.device.HubAction(zwave.associationV2.associationSet(groupingIdentifier: 3, nodeId: zwaveHubNodeId).format()))
			sendHubCommand(new physicalgraph.device.HubAction(zwave.associationV2.associationGet(groupingIdentifier: 3).format()))
        }
    }
}

def zwaveEvent(physicalgraph.zwave.commands.configurationv2.ConfigurationReport cmd) {
	log.debug "---CONFIGURATION REPORT V2--- ${device.displayName} sent ${cmd}"
    def config = cmd.scaledConfigurationValue
    def result = []
    if (cmd.parameterNumber == 1) {
		def value = config == 0 ? "Test 5s" : config == 1 ? "1 minute" : config == 5 ? "5 minute" : config == 15 ? "15 minute" : config == 30 ? "30 minute" : "255 minute" 
    	result << createEvent([name:"TimeoutDuration", value: value, displayed:true, isStateChange:true])
    } else if (cmd.parameterNumber == 13) {
		def value = config == 1 ? "High" : config == 2 ? "Medium" : "Low"
    	result << createEvent([name:"MotionSensitivity", value: value, displayed:true, isStateChange:true])
	} else if (cmd.parameterNumber == 14) {
		def value = config == 0 ? "Disabled" : "Enabled"
    	result << createEvent([name:"LightSense", value: value, displayed:true, isStateChange:true])
    } else if (cmd.parameterNumber == 15) {
    	def value = config == 0 ? "Disabled" : config == 1 ? "10 sec" : config == 2 ? "20 sec" : config == 3 ? "30 sec" : config == 4 ? "45 sec" : "27 minute" 
    	result << createEvent([name:"ResetCycle", value: value, displayed:true, isStateChange:true])
    } else if (cmd.parameterNumber == 3) {
    	if (config == 1 ) {
        	result << createEvent([name:"operatingMode", value: "Manual", displayed:true, isStateChange:true])
         } else if (config == 2 ) {
        	result << createEvent([name:"operatingMode", value: "Vacancy", displayed:true, isStateChange:true])
        } else if (config == 3 ) {
        	result << createEvent([name:"operatingMode", value: "Occupancy", displayed:true, isStateChange:true])
        }
    } else if (cmd.parameterNumber == 6) {
    	def value = config == 0 ? "Disabled" : "Enabled"
    	result << createEvent([name:"MotionSensor", value: value, displayed:true, isStateChange:true])
    } else if (cmd.parameterNumber == 4) {
    	def value = config == 0 ? "Normal" : "Inverted"
    	result << createEvent([name:"SwitchOrientation", value: value, displayed:true, isStateChange:true])
    } 
   return result
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd) {
    log.debug "---BINARY SWITCH REPORT V1--- ${device.displayName} sent ${cmd}"
    createEvent([name: "switch", value: cmd.value ? "on" : "off", type: "digital"])
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
    log.debug "---MANUFACTURER SPECIFIC REPORT V2--- ${device.displayName} sent ${cmd}"
	log.debug "manufacturerId:   ${cmd.manufacturerId}"
	log.debug "manufacturerName: ${cmd.manufacturerName}"
    state.manufacturer=cmd.manufacturerName
	log.debug "productId:        ${cmd.productId}"
	log.debug "productTypeId:    ${cmd.productTypeId}"
	def msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
	updateDataValue("MSR", msr)	
    sendEvent([descriptionText: "$device.displayName MSR: $msr", isStateChange: false])
}

def zwaveEvent(physicalgraph.zwave.commands.versionv1.VersionReport cmd) {
	def fw = "${cmd.applicationVersion}.${cmd.applicationSubVersion}"
	updateDataValue("fw", fw)
	log.debug "---VERSION REPORT V1--- ${device.displayName} is running firmware version: $fw, Z-Wave version: ${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}"
}

def zwaveEvent(physicalgraph.zwave.commands.hailv1.Hail cmd) {
	[name: "hail", value: "hail", descriptionText: "Switch button was pressed", displayed: false]
}


def zwaveEvent(physicalgraph.zwave.commands.notificationv3.NotificationReport cmd)
{
	log.debug "---NOTIFICATION REPORT V3--- ${device.displayName} sent ${cmd}"
	def result = []
	if (cmd.notificationType == 0x07) {
		if ((cmd.event == 0x00)) { 
			result << createEvent(name: "motion", value: "inactive", descriptionText: "$device.displayName motion has stopped")
		} else if (cmd.event == 0x08) {
			result << createEvent(name: "motion", value: "active", descriptionText: "$device.displayName detected motion")
		} 
	} 
	result
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
    log.warn "${device.displayName} received unhandled command: ${cmd}"
}

def on() {
	//sendEvent(name: "switch", value: "on")
	delayBetween([
		zwave.basicV1.basicSet(value: 0xFF).format(),
		zwave.switchBinaryV1.switchBinaryGet().format()
	],100)
}

def off() {
	//sendEvent(name: "switch", value: "off")
	delayBetween([
		zwave.basicV1.basicSet(value: 0x00).format(),
		zwave.switchBinaryV1.switchBinaryGet().format()
	],100)
}

def poll() {
	delayBetween([
		zwave.switchBinaryV1.switchBinaryGet().format(),
		zwave.manufacturerSpecificV1.manufacturerSpecificGet().format()
	],100)
}

/**
  * PING is used by Device-Watch in attempt to reach the Device
**/
def ping() {
		refresh()
}

def refresh() {
	log.debug "refresh() is called"
    delayBetween([
        zwave.switchBinaryV1.switchBinaryGet().format(),
		zwave.notificationV3.notificationGet(notificationType: 7).format()    
	],100)
}

def toggleMode() {
	log.debug("Toggling Mode") 
    def cmds = []
    if (device.currentValue("operatingMode") == "Manual") { 
    	cmds << zwave.configurationV1.configurationSet(configurationValue: [2] , parameterNumber: 3, size: 1)
    }
    else if (device.currentValue("operatingMode") == "Vacancy") {
    	cmds << zwave.configurationV1.configurationSet(configurationValue: [3], parameterNumber: 3, size: 1)
    }
    else if (device.currentValue("operatingMode") == "Occupancy") {
    	cmds << zwave.configurationV1.configurationSet(configurationValue: [1], parameterNumber: 3, size: 1)
    }
    cmds << zwave.configurationV1.configurationGet(parameterNumber: 3)
        sendHubCommand(cmds.collect{ new physicalgraph.device.HubAction(it.format()) }, 1000)

}

def SetModeNumber(value) {
	log.debug("Setting mode by number: ${value}") 
    def cmds = []
    	cmds << zwave.configurationV1.configurationSet(configurationValue: [value] , parameterNumber: 3, size: 1)
  		cmds << zwave.configurationV1.configurationGet(parameterNumber: 3)
    sendHubCommand(cmds.collect{ new physicalgraph.device.HubAction(it.format()) }, 1000)

}

def Occupancy() {
def cmds = []
    cmds << zwave.configurationV1.configurationSet(configurationValue: [3] , parameterNumber: 3, size: 1)
    cmds << zwave.configurationV1.configurationGet(parameterNumber: 3)
    sendHubCommand(cmds.collect{ new physicalgraph.device.HubAction(it.format()) }, 1000)

}

def Vacancy() {
def cmds = []
    cmds << zwave.configurationV1.configurationSet(configurationValue: [2] , parameterNumber: 3, size: 1)
    cmds << zwave.configurationV1.configurationGet(parameterNumber: 3)
        sendHubCommand(cmds.collect{ new physicalgraph.device.HubAction(it.format()) }, 1000)

}

def Manual() {
def cmds = []
    cmds << zwave.configurationV1.configurationSet(configurationValue: [1] , parameterNumber: 3, size: 1)
    cmds << zwave.configurationV1.configurationGet(parameterNumber: 3)
        sendHubCommand(cmds.collect{ new physicalgraph.device.HubAction(it.format()) }, 1000)

}

def installed() {
	// Device-Watch simply pings if no device events received for 32min(checkInterval)
	sendEvent(name: "checkInterval", value: 2 * 15 * 60 + 2 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID])
}

def updated() {
	sendEvent(name: "checkInterval", value: 2 * 15 * 60 + 2 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID])
    if (state.lastUpdated && now() <= state.lastUpdated + 3000) return
    state.lastUpdated = now()

	def cmds = []
	//switch and dimmer settings
        if (settings.timeoutduration) {cmds << zwave.configurationV1.configurationSet(configurationValue: [settings.timeoutduration.toInteger()], parameterNumber: 1, size: 1)}
        cmds << zwave.configurationV1.configurationGet(parameterNumber: 1)
        if (settings.motionsensitivity) {cmds << zwave.configurationV1.configurationSet(configurationValue: [settings.motionsensitivity.toInteger()], parameterNumber: 13, size: 1)}
        cmds << zwave.configurationV1.configurationGet(parameterNumber: 13)
        if (settings.lightsense) {cmds << zwave.configurationV1.configurationSet(configurationValue: [settings.lightsense.toInteger()], parameterNumber: 14, size: 1)}
        cmds << zwave.configurationV1.configurationGet(parameterNumber: 14)
        if (settings.resetcycle) {cmds << zwave.configurationV1.configurationSet(configurationValue: [settings.resetcycle.toInteger()], parameterNumber: 15, size: 1)}
        cmds << zwave.configurationV1.configurationGet(parameterNumber: 15)
        if (settings.operationmode) {cmds << zwave.configurationV1.configurationSet(configurationValue: [settings.operationmode.toInteger()], parameterNumber: 3, size: 1)}
        cmds << zwave.configurationV1.configurationGet(parameterNumber: 3)
        if (settings.motion) {cmds << zwave.configurationV1.configurationSet(configurationValue: [settings.motion.toInteger()], parameterNumber: 6, size: 1)}
        cmds << zwave.configurationV1.configurationGet(parameterNumber: 6)
        if (settings.invertSwitch) {cmds << zwave.configurationV1.configurationSet(configurationValue: [settings.invertSwitch.toInteger()], parameterNumber: 4, size: 1)}
        cmds << zwave.configurationV1.configurationGet(parameterNumber: 4)
		
        // Make sure lifeline is associated - was missing on a dimmer:
		cmds << zwave.associationV1.associationSet(groupingIdentifier:0, nodeId:zwaveHubNodeId)
        cmds << zwave.associationV1.associationSet(groupingIdentifier:1, nodeId:zwaveHubNodeId)
		cmds << zwave.associationV1.associationSet(groupingIdentifier:2, nodeId:zwaveHubNodeId)
		cmds << zwave.associationV1.associationSet(groupingIdentifier:3, nodeId:zwaveHubNodeId)
        
        //association groups
		def nodes = []
		if (settings.requestedGroup2 != state.currentGroup2) {
    	    nodes = parseAssocGroupList(settings.requestedGroup2, 2)
        	cmds << zwave.associationV2.associationRemove(groupingIdentifier: 2, nodeId: [])
        	cmds << zwave.associationV2.associationSet(groupingIdentifier: 2, nodeId: nodes)
        	cmds << zwave.associationV2.associationGet(groupingIdentifier: 2)
        	state.currentGroup2 = settings.requestedGroup2
    	}

    	if (settings.requestedGroup3 != state.currentGroup3) {
        	nodes = parseAssocGroupList(settings.requestedGroup3, 3)
        	cmds << zwave.associationV2.associationRemove(groupingIdentifier: 3, nodeId: [])
        	cmds << zwave.associationV2.associationSet(groupingIdentifier: 3, nodeId: nodes)
        	cmds << zwave.associationV2.associationGet(groupingIdentifier: 3)
        	state.currentGroup3 = settings.requestedGroup3
    	}
        
        sendHubCommand(cmds.collect{ new physicalgraph.device.HubAction(it.format()) }, 500)
}

def Up() {
	sendEvent(name: "button", value: "pushed", data: [buttonNumber: "1"], descriptionText: "On/Up (button 1) on $device.displayName was pushed", isStateChange: true, type: "digital")
    on()
}

def Down() {
	sendEvent(name: "button", value: "pushed", data: [buttonNumber: "2"], descriptionText: "Off/Down (button 2) on $device.displayName was pushed", isStateChange: true, type: "digital")
    off()
}

def configure() {
        def cmds = []
		// Make sure lifeline is associated - was missing on a dimmer:
		cmds << zwave.associationV1.associationSet(groupingIdentifier:0, nodeId:zwaveHubNodeId)
        cmds << zwave.associationV1.associationSet(groupingIdentifier:1, nodeId:zwaveHubNodeId)
		cmds << zwave.associationV1.associationSet(groupingIdentifier:2, nodeId:zwaveHubNodeId)
		cmds << zwave.associationV1.associationSet(groupingIdentifier:3, nodeId:zwaveHubNodeId)
        sendHubCommand(cmds.collect{ new physicalgraph.device.HubAction(it.format()) }, 1000)
}

private parseAssocGroupList(list, group) {
    def nodes = group == 2 ? [] : [zwaveHubNodeId]
    if (list) {
        def nodeList = list.split(',')
        def max = group == 2 ? 5 : 4
        def count = 0

        nodeList.each { node ->
            node = node.trim()
            if ( count >= max) {
                log.warn "Association Group ${group}: Number of members is greater than ${max}! The following member was discarded: ${node}"
            }
            else if (node.matches("\\p{XDigit}+")) {
                def nodeId = Integer.parseInt(node,16)
                if (nodeId == zwaveHubNodeId) {
                	log.warn "Association Group ${group}: Adding the hub as an association is not allowed (it would break double-tap)."
                }
                else if ( (nodeId > 0) & (nodeId < 256) ) {
                    nodes << nodeId
                    count++
                }
                else {
                    log.warn "Association Group ${group}: Invalid member: ${node}"
                }
            }
            else {
                log.warn "Association Group ${group}: Invalid member: ${node}"
            }
        }
    }
    
    return nodes
}