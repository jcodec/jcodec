/*
 * Copyright (c) 2009, 2010 Sun Microsystems, Inc. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met: -
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer. - Redistributions in binary
 * form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials provided
 * with the distribution. - Neither the name of Sun Microsystems nor the names
 * of its contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

/*
 * deployJava.js
 * 
 * This file is part of the Deployment Toolkit. It provides functions for web
 * pages to detect the presence of a JRE, install the latest JRE, and easily run
 * applets or Web Start programs. More Information on usage of the Deployment
 * Toolkit can be found in the Deployment Guide at:
 * http://java.sun.com/javase/6/docs/technotes/guides/jweb/index.html
 * 
 * The "live" copy of this file may be found at :
 * http://java.com/js/deployJava.js. For web pages provisioned using https, you
 * may want to access the copy at: https://java.com/js/deployJava.js.
 * 
 * You are encouraged to link directly to the live copies. The above files are
 * stripped of comments and whitespace for performance, You can access this file
 * w/o the whitespace and comments removed at:
 * http://java.com/js/deployJava.txt.
 * 
 * @(#)deployJava.txt 1.8 10/04/11
 */

var deployJava = {
	debug : null,

	firefoxJavaVersion : null,

	myInterval : null,
	preInstallJREList : null,
	returnPage : null,
	brand : null,
	locale : null,
	installType : null,

	EAInstallEnabled : false,
	EarlyAccessURL : null,

	// GetJava page
	getJavaURL : 'http://java.sun.com/webapps/getjava/BrowserRedirect?host=java.com',

	// Apple redirect page
	appleRedirectPage : 'http://www.apple.com/support/downloads/',

	// mime-type of the DeployToolkit plugin object
	oldMimeType : 'application/npruntime-scriptable-plugin;DeploymentToolkit',
	mimeType : 'application/java-deployment-toolkit',

	// location of the Java Web Start launch button graphic
	launchButtonPNG : 'http://java.sun.com/products/jfc/tsc/articles/swing2d/webstart.png',

	browserName : null,
	browserName2 : null,

	/**
	 * Returns an array of currently-installed JRE version strings. Version
	 * strings are of the form #.#[.#[_#]], with the function returning as much
	 * version information as it can determine, from just family versions
	 * ("1.4.2", "1.5") through the full version ("1.5.0_06").
	 * 
	 * Detection is done on a best-effort basis. Under some circumstances only
	 * the highest installed JRE version will be detected, and JREs older than
	 * 1.4.2 will not always be detected.
	 */
	getJREs : function() {
		var list = new Array();
		if (deployJava.isPluginInstalled()) {
			var plugin = deployJava.getPlugin();
			var VMs = plugin.jvms;
			for ( var i = 0; i < VMs.getLength(); i++) {
				list[i] = VMs.get(i).version;
			}
		} else {
			var browser = deployJava.getBrowser();

			if (browser == 'MSIE') {
				if (deployJava.testUsingActiveX('1.7.0')) {
					list[0] = '1.7.0';
				} else if (deployJava.testUsingActiveX('1.6.0')) {
					list[0] = '1.6.0';
				} else if (deployJava.testUsingActiveX('1.5.0')) {
					list[0] = '1.5.0';
				} else if (deployJava.testUsingActiveX('1.4.2')) {
					list[0] = '1.4.2';
				} else if (deployJava.testForMSVM()) {
					list[0] = '1.1';
				}
			} else if (browser == 'Netscape Family') {
				deployJava.getJPIVersionUsingMimeType();
				if (deployJava.firefoxJavaVersion != null) {
					list[0] = deployJava.firefoxJavaVersion;
				} else if (deployJava.testUsingMimeTypes('1.7')) {
					list[0] = '1.7.0';
				} else if (deployJava.testUsingMimeTypes('1.6')) {
					list[0] = '1.6.0';
				} else if (deployJava.testUsingMimeTypes('1.5')) {
					list[0] = '1.5.0';
				} else if (deployJava.testUsingMimeTypes('1.4.2')) {
					list[0] = '1.4.2';
				} else if (deployJava.browserName2 == 'Safari') {
					if (deployJava.testUsingPluginsArray('1.7.0')) {
						list[0] = '1.7.0';
					} else if (deployJava.testUsingPluginsArray('1.6')) {
						list[0] = '1.6.0';
					} else if (deployJava.testUsingPluginsArray('1.5')) {
						list[0] = '1.5.0';
					} else if (deployJava.testUsingPluginsArray('1.4.2')) {
						list[0] = '1.4.2';
					}
				}
			}
		}

		if (deployJava.debug) {
			for ( var i = 0; i < list.length; ++i) {
				alert('We claim to have detected Java SE ' + list[i]);
			}
		}

		return list;
	},

	/**
	 * Triggers a JRE installation. The exact effect of triggering an
	 * installation varies based on platform, browser, and if the Deployment
	 * Toolkit plugin is installed.
	 * 
	 * In the simplest case, the browser window will be redirected to the
	 * java.com JRE installation page, and (if possible) a redirect back to the
	 * current URL upon successful installation. The return redirect is not
	 * always possible, as the JRE installation may require the browser to be
	 * restarted.
	 * 
	 * In the best case (when the Deployment Toolkit plugin is present), this
	 * function will immediately cause a progress dialog to be displayed as the
	 * JRE is downloaded and installed.
	 */
	installLatestJRE : function() {
		if (deployJava.isPluginInstalled()) {
			if (deployJava.getPlugin().installLatestJRE()) {
				deployJava.refresh();
				if (deployJava.returnPage != null) {
					document.location = deployJava.returnPage;
				}
				return true;
			} else {
				return false;
			}
		} else {
			var browser = deployJava.getBrowser();
			var platform = navigator.platform.toLowerCase();
			if ((deployJava.EAInstallEnabled == 'true')
					&& (platform.indexOf('win') != -1)
					&& (deployJava.EarlyAccessURL != null)) {

				deployJava.preInstallJREList = deployJava.getJREs();
				if (deployJava.returnPage != null) {
					deployJava.myInterval = setInterval("deployJava.poll()",
							3000);
				}

				location.href = deployJava.EarlyAccessURL;

				// we have to return false although there may be an install
				// in progress now, when complete it may go to return page
				return false;
			} else {
				if (browser == 'MSIE') {
					return deployJava.IEInstall();
				} else if ((browser == 'Netscape Family')
						&& (platform.indexOf('win32') != -1)) {
					return deployJava.FFInstall();
				} else {
					location.href = deployJava.getJavaURL
							+ ((deployJava.returnPage != null) ? ('&returnPage=' + deployJava.returnPage)
									: '')
							+ ((deployJava.locale != null) ? ('&locale=' + deployJava.locale)
									: '')
							+ ((deployJava.brand != null) ? ('&brand=' + deployJava.brand)
									: '');
				}
				// we have to return false although there may be an install
				// in progress now, when complete it may go to return page
				return false;
			}
		}
	},

	/**
	 * Returns true if there is a matching JRE version currently installed
	 * (among those detected by getJREs()). The versionPattern string is of the
	 * form #[.#[.#[_#]]][+|*], which includes strings such as "1.4", "1.5.0*",
	 * and "1.6.0_02+". A star (*) means "any version within this family" and a
	 * plus (+) means "any version greater or equal to the specified version".
	 * "1.5.0*" matches 1.5.0_06 but not 1.6.0_01, whereas "1.5.0+" matches
	 * both.
	 * 
	 * If the versionPattern does not include all four version components but
	 * does not end with a star or plus, it will be treated as if it ended with
	 * a star. "1.5" is exactly equivalent to "1.5*", and will match any version
	 * number beginning with "1.5".
	 * 
	 * If getJREs() is unable to detect the precise version number, a match
	 * could be ambiguous. For example if getJREs() detects "1.5", there is no
	 * way to know whether the JRE matches "1.5.0_06+". versionCheck() compares
	 * only as much of the version information as could be detected, so
	 * versionCheck("1.5.0_06+") would return true in in this case.
	 * 
	 * Invalid versionPattern will result in a JavaScript error alert.
	 * versionPatterns which are valid but do not match any existing JRE release
	 * (e.g. "32.65+") will always return false.
	 */
	versionCheck : function(versionPattern) {
		var index = 0;
		var regex = "^(\\d+)(?:\\.(\\d+)(?:\\.(\\d+)(?:_(\\d+))?)?)?(\\*|\\+)?$";

		var matchData = versionPattern.match(regex);

		if (matchData != null) {
			var familyMatch = true;

			var patternArray = new Array();

			for ( var i = 1; i < matchData.length; ++i) {
				// browser dependency here.
				// Fx sets 'undefined', IE sets '' string for unmatched groups
				if ((typeof matchData[i] == 'string') && (matchData[i] != '')) {
					patternArray[index] = matchData[i];
					index++;
				}
			}

			if (patternArray[patternArray.length - 1] == '+') {
				familyMatch = false;
				patternArray.length--;
			} else {
				if (patternArray[patternArray.length - 1] == '*') {
					patternArray.length--;
				}
			}

			var list = deployJava.getJREs();
			for ( var i = 0; i < list.length; ++i) {
				if (deployJava.compareVersionToPattern(list[i], patternArray,
						familyMatch)) {
					return true;
				}
			}

			return false;
		} else {
			alert('Invalid versionPattern passed to versionCheck: ' + versionPattern);
			return false;
		}
	},

	// obtain JPI version using navigator.mimeTypes array
	// if found, set the version to deployJava.firefoxJavaVersion
	getJPIVersionUsingMimeType : function() {
		// Walk through the full list of mime types.
		for ( var i = 0; i < navigator.mimeTypes.length; ++i) {
			var s = navigator.mimeTypes[i].type;
			// The jpi-version is the plug-in version. This is the best
			// version to use.
			var m = s.match(/^application\/x-java-applet;jpi-version=(.*)$/);
			if (m != null) {
				deployJava.firefoxJavaVersion = m[1];
				break;
			}
		}
	},

	/*
	 * returns true if the ActiveX or XPI plugin is installed
	 */
	isPluginInstalled : function() {
		var plugin = deployJava.getPlugin();
		if (plugin && plugin.jvms) {
			return true;
		} else {
			return false;
		}
	},

	/*
	 * returns true if the plugin is installed and AutoUpdate is enabled
	 */
	isAutoUpdateEnabled : function() {
		if (deployJava.isPluginInstalled()) {
			return deployJava.getPlugin().isAutoUpdateEnabled();
		}
		return false;
	},

	/*
	 * sets AutoUpdate on if plugin is installed
	 */
	setAutoUpdateEnabled : function() {
		if (deployJava.isPluginInstalled()) {
			return deployJava.getPlugin().setAutoUpdateEnabled();
		}
		return false;
	},

	/*
	 * sets the preferred install type : null, online, kernel
	 */
	setInstallerType : function(type) {
		deployJava.installType = type;
		if (deployJava.isPluginInstalled()) {
			return deployJava.getPlugin().setInstallerType(type);
		}
		return false;
	},

	/*
	 * sets additional package list - to be used by kernel installer
	 */
	setAdditionalPackages : function(packageList) {
		if (deployJava.isPluginInstalled()) {
			return deployJava.getPlugin().setAdditionalPackages(packageList);
		}
		return false;
	},

	/*
	 * sets preference to install Early Access versions if available
	 */
	setEarlyAccess : function(enabled) {
		deployJava.EAInstallEnabled = enabled;
	},

	/*
	 * Determines if the next generation plugin (Plugin II) is default
	 */
	isPlugin2 : function() {
		if (deployJava.isPluginInstalled()) {
			if (deployJava.versionCheck('1.6.0_10+')) {
				try {
					return deployJava.getPlugin().isPlugin2();
				} catch (err) {
					// older plugin w/o isPlugin2() function -
				}
			}
		}
		return false;
	},

	allowPlugin : function() {
		deployJava.getBrowser();

		// Chrome, Safari, and Opera browsers find the plugin but it
		// doesn't work, so until we can get it to work - don't use it.
		var ret = ('Chrome' != deployJava.browserName2
				&& 'Safari' != deployJava.browserName2 && 'Opera' != deployJava.browserName2);
		return ret;
	},

	getPlugin : function() {
		deployJava.refresh();

		var ret = null;
		if (deployJava.allowPlugin()) {
			ret = document.getElementById('deployJavaPlugin');
		}
		return ret;
	},

	compareVersionToPattern : function(version, patternArray, familyMatch) {
		var regex = "^(\\d+)(?:\\.(\\d+)(?:\\.(\\d+)(?:_(\\d+))?)?)?$";
		var matchData = version.match(regex);

		if (matchData != null) {
			var index = 0;
			var result = new Array();

			for ( var i = 1; i < matchData.length; ++i) {
				if ((typeof matchData[i] == 'string') && (matchData[i] != '')) {
					result[index] = matchData[i];
					index++;
				}
			}

			var l = Math.min(result.length, patternArray.length);

			if (familyMatch) {
				for ( var i = 0; i < l; ++i) {
					if (result[i] != patternArray[i])
						return false;
				}

				return true;
			} else {
				for ( var i = 0; i < l; ++i) {
					if (result[i] < patternArray[i]) {
						return false;
					} else if (result[i] > patternArray[i]) {
						return true;
					}
				}

				return true;
			}
		} else {
			return false;
		}
	},

	getBrowser : function() {

		if (deployJava.browserName == null) {
			var browser = navigator.userAgent.toLowerCase();

			if (deployJava.debug) {
				alert('userAgent -> ' + browser);
			}

			// order is important here. Safari userAgent contains mozilla,
			// and Chrome userAgent contains both mozilla and safari.
			if (browser.indexOf('msie') != -1) {
				deployJava.browserName = 'MSIE';
				deployJava.browserName2 = 'MSIE';
			} else if (browser.indexOf('firefox') != -1) {
				deployJava.browserName = 'Netscape Family';
				deployJava.browserName2 = 'Firefox';
			} else if (browser.indexOf('chrome') != -1) {
				deployJava.browserName = 'Netscape Family';
				deployJava.browserName2 = 'Chrome';
			} else if (browser.indexOf('safari') != -1) {
				deployJava.browserName = 'Netscape Family';
				deployJava.browserName2 = 'Safari';
			} else if (browser.indexOf('mozilla') != -1) {
				deployJava.browserName = 'Netscape Family';
				deployJava.browserName2 = 'Other';
			} else if (browser.indexOf('opera') != -1) {
				deployJava.browserName = 'Netscape Family';
				deployJava.browserName2 = 'Opera';
			} else {
				deployJava.browserName = '?';
				deployJava.browserName2 = 'unknown';
			}

			if (deployJava.debug) {
				alert('Detected browser name:' + deployJava.browserName + ', '
						+ deployJava.browserName2);
			}
		}
		return deployJava.browserName;
	},

	testUsingActiveX : function(version) {
		var objectName = 'JavaWebStart.isInstalled.' + version + '.0';

		if (!ActiveXObject) {
			if (deployJava.debug) {
				alert('Browser claims to be IE, but no ActiveXObject object?');
			}
			return false;
		}

		try {
			return (new ActiveXObject(objectName) != null);
		} catch (exception) {
			return false;
		}
	},

	testForMSVM : function() {
		var clsid = '{08B0E5C0-4FCB-11CF-AAA5-00401C608500}';

		if (typeof oClientCaps != 'undefined') {
			var v = oClientCaps.getComponentVersion(clsid, "ComponentID");
			if ((v == '') || (v == '5,0,5000,0')) {
				return false;
			} else {
				return true;
			}
		} else {
			return false;
		}
	},

	testUsingMimeTypes : function(version) {
		if (!navigator.mimeTypes) {
			if (deployJava.debug) {
				alert('Browser claims to be Netscape family, but no mimeTypes[] array?');
			}
			return false;
		}

		for ( var i = 0; i < navigator.mimeTypes.length; ++i) {
			s = navigator.mimeTypes[i].type;
			var m = s
					.match(/^application\/x-java-applet\x3Bversion=(1\.8|1\.7|1\.6|1\.5|1\.4\.2)$/);
			if (m != null) {
				if (deployJava.compareVersions(m[1], version)) {
					return true;
				}
			}
		}
		return false;
	},

	testUsingPluginsArray : function(version) {
		if ((!navigator.plugins) || (!navigator.plugins.length)) {
			return false;
		}
		var platform = navigator.platform.toLowerCase();

		for ( var i = 0; i < navigator.plugins.length; ++i) {
			s = navigator.plugins[i].description;
			if (s.search(/^Java Switchable Plug-in (Cocoa)/) != -1) {
				// Safari on MAC
				if (deployJava.compareVersions("1.5.0", version)) {
					return true;
				}
			} else if (s.search(/^Java/) != -1) {
				if (platform.indexOf('win') != -1) {
					// still can't tell - opera, safari on windows
					// return true for 1.5.0 and 1.6.0
					if (deployJava.compareVersions("1.5.0", version)
							|| deployJava.compareVersions("1.6.0", version)) {
						return true;
					}
				}
			}
		}
		// if above dosn't work on Apple or Windows, just allow 1.5.0
		if (deployJava.compareVersions("1.5.0", version)) {
			return true;
		}
		return false;

	},

	IEInstall : function() {

		location.href = deployJava.getJavaURL
				+ ((deployJava.returnPage != null) ? ('&returnPage=' + deployJava.returnPage)
						: '')
				+ ((deployJava.locale != null) ? ('&locale=' + deployJava.locale)
						: '')
				+ ((deployJava.brand != null) ? ('&brand=' + deployJava.brand)
						: '')
				+ ((deployJava.installType != null) ? ('&type=' + deployJava.installType)
						: '');

		// should not actually get here
		return false;
	},

	done : function(name, result) {
	},

	FFInstall : function() {

		location.href = deployJava.getJavaURL
				+ ((deployJava.returnPage != null) ? ('&returnPage=' + deployJava.returnPage)
						: '')
				+ ((deployJava.locale != null) ? ('&locale=' + deployJava.locale)
						: '')
				+ ((deployJava.brand != null) ? ('&brand=' + deployJava.brand)
						: '')
				+ ((deployJava.installType != null) ? ('&type=' + deployJava.installType)
						: '');

		// should not actually get here
		return false;
	},

	// return true if 'installed' (considered as a JRE version string) is
	// greater than or equal to 'required' (again, a JRE version string).
	compareVersions : function(installed, required) {

		var a = installed.split('.');
		var b = required.split('.');

		for ( var i = 0; i < a.length; ++i) {
			a[i] = Number(a[i]);
		}
		for ( var i = 0; i < b.length; ++i) {
			b[i] = Number(b[i]);
		}
		if (a.length == 2) {
			a[2] = 0;
		}

		if (a[0] > b[0])
			return true;
		if (a[0] < b[0])
			return false;

		if (a[1] > b[1])
			return true;
		if (a[1] < b[1])
			return false;

		if (a[2] > b[2])
			return true;
		if (a[2] < b[2])
			return false;

		return true;
	},

	enableAlerts : function() {
		// reset this so we can show the browser detection
		deployJava.browserName = null;
		deployJava.debug = true;
	},

	poll : function() {

		deployJava.refresh();
		var postInstallJREList = deployJava.getJREs();

		if ((deployJava.preInstallJREList.length == 0)
				&& (postInstallJREList.length != 0)) {
			clearInterval(deployJava.myInterval);
			if (deployJava.returnPage != null) {
				location.href = deployJava.returnPage;
			}
			;
		}

		if ((deployJava.preInstallJREList.length != 0)
				&& (postInstallJREList.length != 0)
				&& (deployJava.preInstallJREList[0] != postInstallJREList[0])) {
			clearInterval(deployJava.myInterval);
			if (deployJava.returnPage != null) {
				location.href = deployJava.returnPage;
			}
		}

	},

	writePluginTag : function() {
		var browser = deployJava.getBrowser();

		if (browser == 'MSIE') {
			document
					.write('<'
							+ 'object classid="clsid:CAFEEFAC-DEC7-0000-0000-ABCDEFFEDCBA" '
							+ 'id="deployJavaPlugin" width="0" height="0">'
							+ '<' + '/' + 'object' + '>');
		} else if (browser == 'Netscape Family' && deployJava.allowPlugin()) {
			deployJava.writeEmbedTag();
		}
	},

	refresh : function() {
		navigator.plugins.refresh(false);

		var browser = deployJava.getBrowser();
		if (browser == 'Netscape Family' && deployJava.allowPlugin()) {
			var plugin = document.getElementById('deployJavaPlugin');
			// only do this again if no plugin
			if (plugin == null) {
				deployJava.writeEmbedTag();
			}
		}
	},

	writeEmbedTag : function() {
		var written = false;
		if (navigator.mimeTypes != null) {
			for ( var i = 0; i < navigator.mimeTypes.length; i++) {
				if (navigator.mimeTypes[i].type == deployJava.mimeType) {
					if (navigator.mimeTypes[i].enabledPlugin) {
						document
								.write('<' + 'embed id="deployJavaPlugin" type="' + deployJava.mimeType + '" hidden="true" />');
						written = true;
					}
				}
			}
			// if we ddn't find new mimeType, look for old mimeType
			if (!written)
				for ( var i = 0; i < navigator.mimeTypes.length; i++) {
					if (navigator.mimeTypes[i].type == deployJava.oldMimeType) {
						if (navigator.mimeTypes[i].enabledPlugin) {
							document
									.write('<' + 'embed id="deployJavaPlugin" type="' + deployJava.oldMimeType + '" hidden="true" />');
						}
					}
				}
		}
	},

	do_initialize : function() {
		deployJava.writePluginTag();
		if (deployJava.locale == null) {
			var loc = null;

			if (loc == null)
				try {
					loc = navigator.userLanguage;
				} catch (err) {
				}

			if (loc == null)
				try {
					loc = navigator.systemLanguage;
				} catch (err) {
				}

			if (loc == null)
				try {
					loc = navigator.language;
				} catch (err) {
				}

			if (loc != null) {
				loc.replace("-", "_")
				deployJava.locale = loc;
			}
		}
	}

};

deployJava.do_initialize();

/*
 * JCodec demos
 * 
 * Player applet wrapper script
 * 
 * @Author Jay Codec
 */

Function.prototype.bind = function(context) {
	var __method = this;
	return function() {
		return __method.apply(context, arguments);
	}
}

function removeChildren(elem) {
	if (elem.hasChildNodes()) {
		while (elem.childNodes.length >= 1) {
			elem.removeChild(elem.firstChild);
		}
	}
}

function addLoadEvent(func) {
	var oldEvent = window.onload;
	window.onload = function(e) {
		var t;
		try {
			func();
		} catch (e) {
			t = e;
		}
		if (oldEvent)
			oldEvent.call(e);
		if (t)
			throw t;
	};
}

function watchElement(id, func) {
	var el = document.getElementById(id);
	if (!el) {
		addLoadEvent(function() {
			var el = document.getElementById(id);
			func(el);
		});
	} else {
		func(el);
	}
}

function addEventListener(el, name, func) {
	if (el.addEventListener)
		el.addEventListener(name, func, false);
	else if (el.attachEvent)
		el.attachEvent('on' + name, func);
	else
		throw 'Can not add event listener';
}

function removeEventListener(el, name, func) {
	if (el.removeEventListener)
		el.removeEventListener(name, func, false);
	else if (el.detachEvent)
		el.detachEvent('on' + name, func);
	else
		throw 'Can not remove event listener';
}

/*
 * Error console
 */
function ErrorConsole() {
	this.build();
}

ErrorConsole.prototype.build = function() {
	var root = document.createElement('div');
	root.style.position = 'absolute';
	root.style.border = '1px solid #000000';
	root.style.overflow = 'scroll';
	root.style.width = '200px';
	root.style.height = '200px';
	root.style.top = '0px';
	root.style.right = '10px';
	root.style.fontSize = '8px';
	this.root = root;

	if (document.body)
		document.body.appendChild(root);
	else
		addLoadEvent(function() {
			document.body.appendChild(root);
		});
}

ErrorConsole.prototype.log = function(msg) {
	this.root.innerHTML += msg + '<br>';
}

/*
 * Player control
 */
function Player(rootName, params) {
	this.listeners = [];

	this.video = params.video;
	this.baseUrl = params.baseUrl;
	this.jar = params.jar;
	this.width = params.width;
	this.height = params.height;
	this.console = params.console;
	this.preroll = params.preroll;

	this.codeString = 'org/jcodec/samples/player/ui/JAVPlayerApplet';

	if (params.errorMessage)
		this.errorMessage = params.errorMessage;
	else
		this.errorMessage = '<p>It appears Java 1.6 (or newer) is not available in your browser.</p>' + '<p>Java is necessary to have this clip played.</p>' + '<p>To get the latest Java simply click the link below.</p>';

	watchElement(rootName, function(el) {
		this.root = el;
		this.build();
	}.bind(this));
}

Player.prototype.addListener = function(listener) {
	this.listeners.push(listener);
}

Player.prototype.build = function() {

	if (!this.preroll) {
		this.showPlayer();
	} else {
		this.buildPreroll();
	}
}

Player.prototype.showPlayer = function() {

	if (deployJava.versionCheck('1.6+')) {
		this.buildPlayer();
		return true;
	} else {
		this.buildError();
		return false;
	}
}

Player.prototype.buildPlayerIE = function() {
	this.root.innerHTML = '<object classid="clsid:8AD9C840-044E-11D1-B3E9-00805F499D93"'
			+ ' codebase="http://java.sun.com/update/1.6.0/jinstall-6u20-windows-i586.cab#Version=1,6,0,0"'
			+ ' height="'
			+ this.height
			+ '" width="'
			+ this.width
			+ '" >'
			+ '<param name="code" value="'
			+ this.codeString
			+ '" />'
			+ '<param name="archive" value="'
			+ (this.baseUrl + this.jar)
			+ '" />'
			+ '<param name="persistState" value="false" />'
			+ '</object>';

	this.player = this.root.firstChild;
}

Player.prototype.buildPlayerNonIE = function() {

	this.root.innerHTML = '<embed code="' + this.codeString + '"' + ' width="'
			+ this.width + '"' + ' height="' + this.height + '"'
			+ ' type="application/x-java-applet;version=1.6"' + ' archive="'
			+ (this.baseUrl + this.jar) + '"' + '>';

	this.player = this.root.firstChild;
}

Player.prototype.buildPlayer = function() {
	removeChildren(this.root);
	var browserName = '' + navigator.appName; // Force conversion to string
	if (browserName.toLowerCase().indexOf('explorer') != -1) {
		this.buildPlayerIE();
	} else {
		this.buildPlayerNonIE();
	}

	this.notifyListeners(null);

	this.timer = setInterval(this.statusCheck.bind(this), 1000);
}

Player.prototype.buildPreroll = function() {
	removeChildren(this.root);
	this.root.innerHTML = '<img src="' + this.preroll + '" border="0" />';
}

Player.prototype.notifyListeners = function(status) {
	for ( var i = 0; i < this.listeners.length; i++) {
		try {
			this.listeners[i](this.player, status);
		} catch (e) {
			if (console)
				console.log('From listener: ' + e);
		}
	}
}

Player.prototype.buildError = function() {
	removeChildren(this.root);
	var div = document.createElement('div');
	div.className = 'error-message';
	div.style.width = '' + this.width + 'px';
	div.style.height = '' + this.height + 'px';
	div.innerHTML = this.errorMessage;
	var a = document.createElement('a');
	a.innerHTML = 'Install Java';
	addEventListener(a, 'click', this.installJava.bind(this), false);
	a.href = '#';

	div.appendChild(a);
	this.root.appendChild(div);
}

Player.prototype.installJava = function() {
	deployJava.installLatestJRE();
}

Player.prototype.statusCheck = function() {
	try {
		var statusJSON = this.player.getStatus();
	} catch (e) {
		if (console)
			console.log('Player, getStatus: ' + e);
	}
	// Hello IE 6!
	var json;
	try {
		json = JSON;
	} catch (e) {
		json = null;
	}

	try {
		var status;
		if (json) {
			status = json.parse(statusJSON);
		} else {
			status = eval('(' + statusJSON + ')');
		}
		this.notifyListeners(status);
	} catch (e) {
		if (console)
			console.log('JSON parse: ' + e);
	}
}

/*
 * Buffer level visualizaiton
 */
function LevelBar(rootName, playerCtl) {
	this.console = playerCtl.console;

	watchElement(rootName, function(el) {
		this.container = el;
		this.build();
		playerCtl.addListener(this.onPlayer.bind(this));
	}.bind(this));
}

LevelBar.prototype.build = function() {
	removeChildren(this.container);

	this.root = document.createElement('div');
	this.root.className = 'level inactive';
	this.root.style.width = '100%';
	this.root.style.height = '100%';
	this.root.style.position = 'relative';

	this.level = document.createElement('div');
	this.level.className = 'progress';

	this.root.appendChild(this.level);
	this.container.appendChild(this.root);
}

LevelBar.prototype.onPlayer = function(player, status) {

	if (!this.player)
		this.player = player;

	if (this.player && status && status.source
			&& status.source.name == 'jcodec' && status.source.bufferLevel) {
		this.root.className = 'level';
		var bl = status.source.bufferLevel;
		this.level.style.height = bl + '%';
	} else {
		this.root.className = 'level inactive';
	}
}

/*
 * Play pause toggle button
 */
function PlayPauseToggle(rootName, playerCtl) {
	this.video = playerCtl.video;
	this.baseUrl = playerCtl.baseUrl;
	this.console = playerCtl.console;
	this.playerCtl = playerCtl;

	watchElement(rootName, function(el) {
		this.root = el;
		this.build();
		this.enableButton();
	}.bind(this));

}

PlayPauseToggle.prototype.onPlayer = function(player, status) {
	if (!this.player) {
		this.player = player;

		if (player && this.waitingForPlayer) {
			this.openAndPlay();
		}
	}

	if (!this.player)
		return;

	this.enableButton();

	if (status) {
		if (!status.playing || status.endOfStream) {
			this.button.src = this.baseUrl + 'img/play.png';
		} else {
			this.button.src = this.baseUrl + 'imt/pause.png';
		}
	}
}

PlayPauseToggle.prototype.enableButton = function() {
	if (!this.listenerBound) {
		this.listenerBound = this.playPause.bind(this);
		addEventListener(this.button, 'click', this.listenerBound, false);
		this.button.src = this.baseUrl + 'img/play.png';
	}
}

PlayPauseToggle.prototype.disableButton = function() {
	if (this.listenerBound) {
		removeEventListener(this.button, 'click', this.listenerBound, false);
		this.button.src = this.baseUrl + 'img/play_d.png';
		this.listenerBound = null;
	}
}

PlayPauseToggle.prototype.build = function() {
	removeChildren(this.root);

	this.button = document.createElement('img');
	this.button.border = '0';

	this.button.src = this.baseUrl + 'img/play_d.png';
	this.root.appendChild(this.button);
}

PlayPauseToggle.prototype.playPause = function() {
	if (!this.player) {
		this.waitingForPlayer = true;
		this.playerCtl.addListener(this.onPlayer.bind(this));
		if (!this.playerCtl.showPlayer()) {
			this.disableButton();
		} else {
			this.button.src = this.baseUrl + 'img/pause.png';
		}

		return;
	}

	try {
		var pipeline = this.player.getPipeline();
	} catch (e) {
		if (console)
			console.log('Player, getPipeline: ' + e);
	}
	if (!pipeline || pipeline.isEndOfStream()) {
		this.openAndPlay();
	} else {
		if (pipeline.isPlaying()) {
			this.button.src = this.baseUrl + 'img/play.png';
			this.player.pause();
		} else {
			this.button.src = this.baseUrl + 'img/pause.png';
			this.player.play();
		}
	}
}

PlayPauseToggle.prototype.openAndPlay = function() {
	try {
		this.player.open(this.baseUrl + this.video, 25);
		this.player.play();
		this.button.src = this.baseUrl + 'img/pause.png';
	} catch (e) {
		if (console)
			console.log('Player, play: ' + e);
	}
}

/*
 * FPS counter
 */
function FPSCounter(rootName, playerCtl) {
	watchElement(rootName, function(el) {
		this.container = el;
		playerCtl.addListener(this.onPlayer.bind(this));
	}.bind(this));
}

FPSCounter.prototype.onPlayer = function(player, status) {
	if (status && status.fps) {
		this.container.innerHTML = status.fps;
	}
}

/*
 * Generic slider control
 */

function Slider(dragger, slider, startPos, onChange) {
	this.dragger = dragger;
	this.slider = slider;
	if (onChange)
		this.onChange = onChange;
	this.addEvents();
	if (startPos)
		this.move(startPos);
	else
		this.move(0);
}

Slider.prototype.setEnabled = function(enabled) {
	this.enabled = enabled;
}

Slider.prototype.move = function(newLoc) {
	var draggerW = this.dragger.clientWidth;
	var sliderW = this.slider.clientWidth;
	var pix = Math.round((sliderW * newLoc) / 100 - (draggerW / 2));
	this.dragger.style.left = pix + 'px';
}

Slider.prototype.addEvents = function() {

	var release = function(e) {
		this.dragging = false;
		document.stopObserving('mouseup', release);
		document.stopObserving('mousemove', move);
		e.stopPropagation();
		e.preventDefault();
	}.bind(this);

	var move = function(e) {
		if (this.dragging) {
			var nPos = e.pageX - this.sliderX - this.halfDragger;
			if (nPos < -this.halfDragger)
				nPos = -this.halfDragger;
			var maxPos = this.sliderW - this.halfDragger;
			if (nPos > maxPos) {
				nPos = maxPos;
			}

			var nPers = ((nPos + this.halfDragger) * 100) / this.sliderW;
			if (this.onChange != null) {
				setTimeout( function() {
					this.onChange(nPers);
				}.bind(this), 5);
			}

			this.dragger.style.left = nPos + 'px';
		}
		e.stopPropagation();
		e.preventDefault();
	}.bind(this);

	var engage = function(e) {
		if (this.enabled) {
			if (this.onSelect)
				this.onSelect();

			this.startX = e.pageX;
			this.sliderX = this.slider.cumulativeOffset()[0];
			this.draggerW = this.dragger.clientWidth;
			this.halfDragger = Math.round(this.draggerW / 2);
			this.sliderW = this.slider.clientWidth;
			this.dragging = true;
			document.observe('mouseup', release);
			document.observe('mousemove', move);
			e.stopPropagation();
			e.preventDefault();
		}
	}.bind(this);

	this.dragger.observe('mousedown', engage);

	this.dragger.observe('click', function(e) {
		e.stopPropagation();
		e.preventDefault();
	});

	this.slider.observe('mousedown',
			function(e) {
				if (this.enabled) {
					if (this.onSelect)
						this.onSelect();

					var offset = e.pointerX()
							- this.slider.cumulativeOffset()[0];
					var draggerW = this.dragger.clientWidth;
					this.dragger.style.left = (offset - Math
							.round(draggerW / 2)) + 'px';

					var nPers = (offset * 100) / this.slider.clientWidth;
					if (this.onChange) {
						this.onChange(nPers);
					}
					e.stopPropagation();
					e.preventDefault();
				}
			}.bind(this));
}

Slider.prototype.select = function() {
	this.dragger.addClassName('selected');
}

Slider.prototype.deselect = function() {
	this.dragger.removeClassName('selected');
}

/*
 * Timeline
 */
function Timeline(rootName, playerCtl) {
	watchElement(rootName, function(el) {
		this.container = el;
		this.build();
		playerCtl.addListener(this.onPlayer.bind(this));
	}.bind(this));
}

Timeline.prototype.build = function() {
	var timeline = document.createElement('div');
	timeline.className = 'timeline';

	var slider = document.createElement('slider');
	slider.className = 'slider';

	timeline.appendChild(slider);

	this.containe.appendChild(timeline);

	this.slider = new Slider(slider, timeline, 0, this.seek.bind(this));
	this.slider.setEnabled(true);
	this.slider.select();
}

Timeline.prototype.onPlayer = function(player, status) {
	// Player event
	this.slider.move(0);
}

Timeline.prototype.seek = function(newLoc) {
	// Seek in player
}