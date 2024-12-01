package io.reverie.redis

import io.reverie.redis.network.*
import io.reverie.redis.codec.*
import io.reverie.redis.commands.*
import scala.util.*
import chaining.*
import org.slf4j.LoggerFactory

import java.net.Socket

object Server {
  private val logger = LoggerFactory.getLogger(classOf[Server.type])

  private def handleSocket(socket: Socket, redis: Redis) =
    Using.resources(
      socket,
      socket.getInputStream,
      socket.getOutputStream
    ) { (_, inputStream, outputStream) =>
      val response = Redis.decoder.decode(inputStream) match
        case Left(error) =>
          logger.error(s"Failed to decode command: $error")
          RedisResponse.Error(error.toString)
        case Right(request) =>
          val response = redis.request(request)
          logger.info(s"Request: $request Response: $response")
          response
      outputStream.write(Redis.encoder.encode(response))
    }

  private def server(address: InetSocketAddress, redis: Redis) =
    bind(address) match
      case Left(error) =>
        logger.error(s"Failed to bind to $address: $error")
        System.exit(1)
      case Right(serverSocket) =>
        logger.info(s"Server is running on ${serverSocket.getLocalSocketAddress}")
        while true do
          val socket = serverSocket.accept()
          logger.info(s"Accepted connection from ${socket.getRemoteSocketAddress}")
          Try(handleSocket(socket, redis)) pipe {
            case Failure(exception) =>
              exception.printStackTrace()
            case _ =>
          }

  def main(args: Array[String]): Unit = {
    val address = InetSocketAddress(Host("localhost"), Port(6379))
    val redis = Redis()
    server(address, redis)
  }
}
