Zeromq-apns
===========

A awesome performant iPhone push notification (APNs) system, that has an ZMQ interface.
The project is meant to be run as a standalone service, that maintains
persistent connections to Apple servers.  Clients of the service, simply need
to enqueue notifications requests in a ZMQ queue.
![Image](http://d1xzuxjlafny7l.cloudfront.net/wp-content/uploads/2011/05/Push-Overview.jpg)

How to add sbteclipse
=========================
sbt eclipse 

How to Compile and Run
=========================

The project is built using Scala (Scala 2.9.2), and uses sbt (sbt 0.11.3) for building:

Building:

    sbt update compile test

Running:

    sbt run  [configuration-file]