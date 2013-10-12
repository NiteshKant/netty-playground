My Protocol Implementation
=============

An implementation of a custom protocol using netty.

Protocol
========

A simple text based protocol where every line is a name-value pair.
Name and values are separated by a colon(:) and end of line is marked by a carriage return or line feed character.

Implementation
============

This implementation contains:

### Encoder/Decoder

Encoder and Decoder implementations to handle the protocol.

### Server

Implementation main class: com.netflix.custom.server.CustomProtocolServer

A server that receives messages in this protocol and sends back responses it knows how to handle an attribute.
For any unknown attributes, it sends an "error" attribute with the error message.
By default, the server handles the following attributes
 ##### "ping" Returns an attribute "pong" with no value.
 ##### "stream" If the value is an integer 'n', then schedules to send an attribute "data" every 'n' seconds, otherwise every 1 second.

### Client

Implementation main class: com.netflix.custom.client.CustomProtocolClient

A simple client that sends user defined attributes to the server and echoes back the response.

This client expects a user to enter on standard input the attribute data it wants to send. The name and value must be
separated by a colon (:)
