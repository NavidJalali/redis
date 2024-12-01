package io.reverie.redis

import io.reverie.redis.network.*
import io.reverie.redis.codec.*
import io.reverie.redis.commands.*
import io.reverie.redis.protocol.RESP

import scala.util.*
import org.slf4j.LoggerFactory

import java.net.Socket

object Server {
  private val logger = LoggerFactory.getLogger(classOf[Server.type])

  private def handleSocket(socket: Socket, redis: Redis): Unit =
    Using.resource(socket.getInputStream) { inputStream =>
      Using.resource(socket.getOutputStream) { outputStream =>
        Redis.decoder.decode(inputStream) match
          case Left(decodeError) =>
            logger.error(s"Failed to decode command: $decodeError")
            val error = RedisResponse.Error("Failed to decode command")
            outputStream.write(Redis.encoder.encode(error))
          case Right(request) =>
            val response = redis.request(request)
            logger.info(s"Request: $request Response: $response")
            outputStream.write(Redis.encoder.encode(response))
      }
    }

  private def server(address: InetSocketAddress, redis: Redis): Unit =
    bind(address) match
      case Left(error) =>
        logger.error(s"Failed to bind to $address: $error")
        System.exit(1)
      case Right(serverSocket) =>
        logger.info(s"Server is running on ${serverSocket.getLocalSocketAddress}")
        while true do
          Using.resource(serverSocket.accept()) { clientSocket =>
            logger.info(s"Accepted connection from ${clientSocket.getRemoteSocketAddress}")
            handleSocket(clientSocket, redis)
          }

  def main(args: Array[String]): Unit = {
    val address = InetSocketAddress(Host("localhost"), Port(6379))
    val redis = Redis()
    server(address, redis)
  }
}
