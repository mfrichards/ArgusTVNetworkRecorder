# Argus TV Network Recorder
Ceton InfiniTV Recorder for ArgusTV PVR Software

## Description

Argus TV Network Recorder is a recorder program for the Argus TV Smart TV recording suite that allows Argus TV to use Ceton InfiniTV cable card devices. It is a standalone Spring Boot application written in Java and Groovy.

## Requirements

- You must have an Argus TV server installed and running on your local network. The recorder does not need to run on the same machine as the Argus TV server, however, they must both be accessible on the same network.
- You must have Java 1.8 installed.
- You must have a Ceton InfiniTV PCI card installed on the same machine as Argus TV Network Recorder. The card must fully setup and activated by your cable provider.
- You must be running Windows 7 or greater.

## Limitations

- The recorder can only record unencrypted channels. Depending on your cable provider, this usually means that you will not be able to record from "premium" channels such as HBO.
- Live TV is currently not supported.

## Setup and Configuration

When the network recorder starts up, it reads configuration settings from a file named "NetworkRecorder.properties". It looks for this file in the Argus TV "Settings" directory, which is located in:

%ProgramData%\ARGUS TV\Settings

Refer to the sample "NetworkRecorder.properties" file in this project for a description of the available settings.

To start the network recorder, run the following command from the Windows command line:

%JAVA_HOME%\bin\javaw.exe -jar ArgusTVNetworkRecorder-1.0.jar

With the network recorder running, you must configure the Argus TV server to use the recorder. This is done as follows:

1) Launch the Argus TV Scheduler Console application.
2) Select Administration > Recorders.
3) Click the "Create New" button, and select "Other".
4) In the new recorder plugin entry that is created, enter the following information:

Name: [enter a name for the recorder]<br>
Service URL: http://localhost:8080/Network/Recorder

If the network recorder is not running on the same machine as the Argus TV server, replace "localhost" with the IP address of the machine where the network recording is running.

5) Click the "Ping" button to test the connection. You should see the message "Server succesfully connected to NetworkRecorder". If an error message is displayed instead, first make sure the network recorder is running and has been configured as described above.

## Troubleshooting

The network recorder will write a log file called "network-recorder.log" to the "logs" subdirectory under the directory where the network recorder is installed. If you experience any issues using the network recorder, this file should contain detailed error information.

## License

This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any later version.