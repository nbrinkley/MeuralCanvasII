# MeuralCanvasII
Hubitat Device Driver for Meural Canvas II

Implements the following capabilities:

	capability "Refresh"
	capability "Light" - Added to expose at a light in Alexa
	capability "Switch" - Off: Suspends the Meural Canvas, On: Resumes teh Meural Canvas
	capability "SwitchLevel" - Overrides the automatic brightness adjustment.
      		Manually set brightness level as a percent. Meural restricts the range to 9 through 94.  Use 0 or 100 to restore automatic level setting.
	capability "Actuator" - To ensure that lux is exposed for use as a light meter

Meural Canvas II v0.00 Beta

	Install as a virtual device and after saving add your Meural IP Address.  A static IP is required.

  	Known issues:
	1. Setting a "Power Level" in the Alexa app results in "Unresponsive".  The level is, however, correctly changed.
	2. Alexa does not display the current power level.
