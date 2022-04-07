# Echo API

## Demo

https://echook.azurewebsites.net/echo

Note: This site is hosted on Azure free tier. If it is a while since it last executes, it may take some time to respond. 
Otherwise you can clone this project and run the application locally

## Features

The following information are echo back in JSON format:
- Path
- Protocol
- Headers
- Query string
- URL-encoded form submission
- Multi-part form submission

## Limitation

The parsing of query string and URL-encoded body depends on a deprecated class `HttpUtils` which supports only default 
encoding. The extraction code also does not handle malformed string well.

## Hash fragment (#abc)

Browser does not include hash fragment in the HTTP message. Java servlet specification does not provide any way to extract
original URL either.

Hence there is no way to extract and echo hash fragment back.

