package io.reverie.redis

import cats.syntax.all.*
import io.reverie.redis.network.*
import io.reverie.redis.codec.*
import io.reverie.redis.commands.*
import java.util.concurrent.*
import scala.util.*
import org.slf4j.LoggerFactory
import java.net.Socket

object Server {
  val logger = LoggerFactory.getLogger(classOf[Server.type])

  private def handleSocket(socket: Socket, redis: Redis) =
    Using.resource(socket.getInputStream) { inputStream =>
      Using.resource(socket.getOutputStream) { outputStream =>
        val bytes = inputStream.readAllBytes()
        val command =
          Decoder[Array[Byte], RedisRequest].decode(bytes)
        command match
          case Left(error) =>
            logger.error(s"Failed to decode command: $error")
            outputStream.write(protocol.error("Failed to decode command").getBytes)
          case Right(request) =>
            val response = redis.request(request)
            logger.info(s"Request: $request Response: $response")
            val encodedResponse =
              Encoder[commands.RedisResponse, Array[Byte]].encode(response)
            outputStream.write(encodedResponse)
      }
    }

  private def server(addess: InetSocketAddress, redis: Redis) =
    bind(addess) match
      case Left(error) =>
        logger.error(s"Failed to bind to $addess: $error")
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
