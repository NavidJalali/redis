package codecrafters_redis

import cats.syntax.all.*
import java.net.{ServerSocket}
import codecrafters_redis.network.*
import java.io.IOException
import scala.util.Using.Manager
import scala.util.Using.Releasable
import scala.util.Using

object network {
  opaque type Port = Int
  object Port {
    def apply(value: Int): Port = value
    extension (port: Port) {
      def value: Int = port
    }
  }

  opaque type Host = String
  object Host {
    def apply(value: String): Host = value
    extension (host: Host) {
      def value: String = host
    }
  }

  final case class InetSocketAddress(host: Host, port: Port) {
    import Port.*, Host.*
    def asJava: java.net.InetSocketAddress =
      new java.net.InetSocketAddress(host.value, port.value)
  }

  enum BindError {
    case AlreadyBound(e: IOException)
    case SecurityError(e: SecurityException)
  }

  given Releasable[ServerSocket] with
    def release(resource: ServerSocket): Unit = resource.close()
}

private def bind(address: InetSocketAddress) =
  val serverSocket = ServerSocket()
  try
    serverSocket.bind(address.asJava)
    Right(serverSocket)
  catch
    case e: IOException =>
      serverSocket.close()
      Left(BindError.AlreadyBound(e))
    case e: SecurityException =>
      serverSocket.close()
      Left(BindError.SecurityError(e))

object Server {
  def main(args: Array[String]): Unit = {
    val address = InetSocketAddress(Host("localhost"), Port(6379))
    bind(address) match
      case Left(error) =>
        Console.err.println(s"Failed to bind to $address: $error")
        System.exit(1)
      case Right(value) =>
        println(s"Successfully bound to $address")
        Using.resource(value) { serverSocket =>
          println(s"Server is running on ${serverSocket.getLocalSocketAddress}")
          val socket = serverSocket.accept()
          println(s"Accepted connection from ${socket.getInetAddress}")
          Thread.sleep(10000)
        }
  }
}
