Hi-Chan Project
Lab 6 Submission
E.T.C

Project Overview
Hi-Chan is a java designed command-line designed to assist truck drivers traveling along I-90 in Washington & Idaho.
The program determines and displays the appropriate communication channel identifier based on the drivers current
location and direction of travel.
The system provides accurate channel lookup information to support communication and routing requirements for truck
drivers operating within the covered regions.

Project Goal
The purpose of Hi-Chan is to provide truck drivers with the channel identifier corresponding to their current location
and direction of travel along I-90 in Washington and Idaho. The command-line is intended to deliver accurate and timely
channel information, supporting communication and operational needs while meeting end-user requirements.

Source Code Modules
Main.java - Entry point of the application, coordinates program execution
DataLoader.java - loads location and channel data from source files
LocationProcessor.java - processes user location and direction input
ChannelLookup.java - performs lookup of the correct channel based on location data
ChannelResult.java - stores and represents channel lookup results
LocationResult.java - stores location processing results
OutputManager.java - handles display and output formatting
LocationNotFoundException.java - custom exception thrown when a location cannot be matched

Traceability Matrix
Accept user location input = Main.java, LocationProcessor.java
Accept direction of travel = Main.java, LocationProcessor.java
Load I-90 location/channel data = DataLoader.java
Search for matching location = ChannelLookup.java
Determine appropriate channel identifier = ChannelLookup.java
Store channel lookup results = ChannelResult.java
Store processed location information = LocationResult.java
Display lookup result to user = OutputManager.java
Handle invalid or unknown locations = LocationNotFoundException.java, ChannelLookup.java
Coordinate application workflow = Main.java

Program Flow
1. User enters current location and direction of travel
2. DataLoader loads avaliable location/channel information
3. LocationProcessor validates and processes user input
4. ChannelLookup searches for hte appropriate channel
5. ChannelResult stores the selected channel information
6. OutputManager displays results
7. If a location cannot be found, LocationNotFoundException is thrown and handled appropriately

Unit Tests
- Data loading functionality
- Location processing logic
- Channel lookup accuracy
- Result object creation
- Exception handling 
- Output formatting behavior
