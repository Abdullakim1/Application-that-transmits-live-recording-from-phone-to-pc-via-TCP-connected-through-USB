# Security over transmission project. It is course based project.

## Introduction
This project allows to **transmit live recordings from phone to a PC** using a TCP connection over USB. It enables real-time streaming of audio data from a mobile device directly to a 
desktop environment for monitoring, processing.  

## Purpose
The main goal of this project is to provide a **simple and reliable way to stream live recordings** from a phone to a PC. It can be useful for testing, development, 
remote monitoring, or any scenario where direct phone-to-PC data transfer is needed.  

## Getting Started
1. Connecting phone to the PC via USB.
2. Run the application on PC this automatically installs app and starts live recording.
3. Start streaming and monitor the data in real time on PC.
4. adb -d forward tcp:5000 tcp:5000 this command connects both in port 5000
5. nc 127.0.0.1 5000 | pacat --format=s16le --rate=44100 --channels=1 --latency-msec=100 this is ip address of PC and connection port 5000
---

*Note:* This project is designed to be lightweight and easy to use. It focuses on **functionality and live transmission**, rather than advanced features or extensive configuration.
