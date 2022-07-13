# Echo API

A maven library of a `/echo` API that echo HTTP request info back to the caller

## Demo

https://echook.azurewebsites.net/echo

Note 1: This site is hosted on Azure free tier. If it is a while since it last executes, it may take some time to respond. 
Otherwise you can clone this project and run the application locally

Note 2: Please refer to https://github.com/geraldnguyen/echo-server for a sample integration of this library

## Integration

pom.xml

```xml
<dependency>
   <groupId>io.github.geraldnguyen</groupId>
   <artifactId>echo</artifactId>
   <version>0.0.2</version>
</dependency>
```

EchoServerApplication.java

```java
@SpringBootApplication
@ComponentScan("nguyen.gerald.echo")
public class EchoServerApplication {
	public static void main(String[] args) {
		SpringApplication.run(EchoServerApplication.class, args);
	}
}
```

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

