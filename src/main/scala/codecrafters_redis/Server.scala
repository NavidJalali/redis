package codecrafters_redis

import cats.syntax.all._
import java.net.{ServerSocket}
import codecrafters_redis.network._
import java.io.IOException
import scala.util.Using.Manager
import scala.util.Using.Releasable
import scala.util.Using

object network {
  type Port = Int
  object Port {
    def apply(value: Int): Port = value
  }

  type Host = String
  object Host {
    def apply(value: String): Host = value
  }

  final case class InetSocketAddress(host: Host, port: Port) {
    def asJava: java.net.InetSocketAddress =
      new java.net.InetSocketAddress(host, port)
  }

  sealed trait BindError extends Product with Serializable
  object BindError {
    final case class AlreadyBound(e: IOException) extends BindError
    final case class SecurityError(e: SecurityException) extends BindError
  }

  implicit val releaseSocket: Releasable[ServerSocket] = _.close()

  def bind(address: InetSocketAddress) = {
    val serverSocket = new ServerSocket()
    try {
      serverSocket.bind(address.asJava)
      Right(serverSocket)
    } catch {
      case e: IOException =>
        serverSocket.close()
        Left(BindError.AlreadyBound(e))
      case e: SecurityException =>
        serverSocket.close()
        Left(BindError.SecurityError(e))
    }
  }
}

object Server {
  def main(args: Array[String]): Unit = {
    val address = InetSocketAddress(Host("localhost"), Port(6379))
    bind(address) match {
      case Left(error) =>
        Console.err.println(s"Failed to bind to $address: $error")
        System.exit(1)
      case Right(value) =>
        println(s"Successfully bound to $address")
        Using.resource(value) { serverSocket =>
          println(s"Server is running on ${serverSocket.getLocalSocketAddress}")
          val socket = serverSocket.accept()
          println(s"Accepted connection from ${socket.getInetAddress}")
        }
    }
  }
}
