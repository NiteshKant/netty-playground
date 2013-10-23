My Protocol Implementation
=============

An implementation of SSE (Server sent events) using netty.

Protocol
========

This is intended to be an implementation of SSE however it is not completed as yet. See TODOs for list of missing items.

Implementation
============

This implementation contains:

### Encoder/Decoder

Encoder and Decoder implementations to handle the protocol.

### Server

Implementation main class: com.netflix.sse.server.SSEServer

A server that receives messages in this protocol and sends back responses it knows how to handle an attribute.
For any unknown attributes, it sends an "error" attribute with the error message.
By default, the server handles the following attributes
 ##### "ping" Returns an attribute "pong" with no value.
 ##### "stream" If the value is an integer 'n', then schedules to send an attribute "data" every 'n' seconds, otherwise every 1 second.

### Client

Implementation main class: com.netflix.sse.client.SSEClient

A simple client that sends user defined attributes to the server and echoes back the response.

This client expects a user to enter on standard input the attribute data it wants to send. The name and value must be
separated by a colon (:)

TODO
===========

The SSE protocol is not completely implemented. It is using the same encoder/decoder as Custom protocol module in this
project.