Zeromq-apns
===========

A awesome performant iPhone push notification (APNs) system, that has an ZMQ interface, 
use Redis as persistent storage. 
The project is meant to be run as a standalone service, that maintains
persistent connections to Apple servers.  Clients of the service, simply need
to enqueue notifications requests in a ZMQ queue.
![Image](http://d1xzuxjlafny7l.cloudfront.net/wp-content/uploads/2011/05/Push-Overview.jpg)

##Ubuntu
On Ubuntu, it is easy to build both packages from source, if you follow the proper recipe (below).
You should build the [Java bindings repository](https://launchpad.net/~tuomjarv/+archive/jzmq) from source because the
package archive has not been updated in over a year, so it is not up to date.

FYI, here is how to add the old ZeroMQ repository.
I don't think you should use it because you should build the Java bindings anyway, and the versions of the two packages should match.

````
sudo add-apt-repository ppa:chris-lea/zeromq
sudo aptitude install libzmq-dev
````

### Building from source
````
chmod 755 autogenZMQfromSrc.sh
./autogenZMQfromSrc.sh
````

## Mac
````
chmod 755 autogenZMQonMac.sh
./autogenZMQonMac.sh
````

##How to add sbteclipse
=========================
sbt eclipse 

##How to Compile and Run
=========================

The project is built using Scala (Scala 2.9.2), and uses sbt (sbt 0.11.3) for building:

Building:

    sbt clean reload update compile test

Running:

    sbt run  [configuration-file]
    
##Configuration Files
==========================
A configuration file is needed to set server bind address, and the redis host and port
is needed too, you also can set multi game push notification settings, contains the "certificate"
and "passwd" key.
the application.conf file would look like

apnserver {
  address = "tcp://0.0.0.0:5566"
  akka.loglevel = DEBUG
}
staticContentServer {
  akka.loglevel = DEBUG
  server-name = "staticContentServer"
  hostname = "0.0.0.0"
  port = 5567
  rootPath = "upload"
  tmpPath = "tmp"
}
my-pinned-dispatcher {
  type=PinnedDispatcher
  executor=thread-pool-executor
}
apnclient {
  address = "tcp://127.0.0.1:5566"
  akka.loglevel = DEBUG
}
redis {
  host = "127.0.0.1"
  port = 6379
}

##Expected Message Format
==========================
The Apple APNs server expects JSON messages, that contain these properties:
More info is here: http://developer.apple.com/library/ios/#documentation/NetworkingInternet/Conceptual/RemoteNotificationsPG/ApplePushService/ApplePushService.html#//apple_ref/doc/uid/TP40008194-CH100-SW9
alert  -   If this property is included, iOS displays a standard alert. You may specify a string as the value of alert or a dictionary as its value. If you specify a string, it becomes the message text of an alert with two buttons: Close and View. If the user taps View, the application is launched.
		   Alternatively, you can specify a dictionary as the value of alert. See Table 3-2 for descriptions of the keys of this dictionary.
badge  -   The number to display as the badge of the application icon. If this property is absent, any badge number currently shown is removed.
sound  -   The name of a sound file in the application bundle. The sound in this file is played as an alert. If the sound file doesn¡¯t exist or default is specified as the value, the default alert sound is played. The audio must be in one of the audio data formats that are compatible with system sounds; see ¡°Preparing Custom Alert Sounds¡± for details.

Child properties of the alert property:
body   			-  The text of the alert message.
action-loc-key 	-  If a string is specified, displays an alert with two buttons, whose behavior is described in Table 3-1. However, iOS uses the string as a key to get a localized string in the current localization to use for the right button¡¯s title instead of ¡°View¡±. If the value is null, the system displays an alert with a single OK button that simply dismisses the alert when tapped. See ¡°Localized Formatted Strings¡± for more information.
loc-key			-  A key to an alert-message string in a Localizable.strings file for the current localization (which is set by the user¡¯s language preference). The key string can be formatted with %@ and %n$@ specifiers to take the variables specified in loc-args. See ¡°Localized Formatted Strings¡± for more information.
loc-args		-  Variable string values to appear in place of the format specifiers in loc-key. See ¡°Localized Formatted Strings¡± for more information.
launch-image	-  The filename of an image file in the application bundle; it may include the extension or omit it. The image is used as the launch image when users tap the action button or move the action slider. If this property is not specified, the system either uses the previous snapshot,uses the image identified by the UILaunchImageFile key in the application¡¯s Info.plist file, or falls back to Default.png.
				   This property was added in iOS 4.0.

Sample message:
{
    "aps" : {
        "alert" : "You got your emails.",
        "badge" : 9,
        "sound" : "bingbong.aiff"
    },
    "acme1" : "bar",
    "acme2" : 42
}
{
    "aps" : {
        "alert" : { "loc-key" : "GAME_PLAY_REQUEST_FORMAT", "loc-args" : [ "Jenna", "Frank"] },
        "sound" : "chime"
    },
    "acme" : "foo"
}

##Running
````
chmod 755 server.sh
./server.sh
````