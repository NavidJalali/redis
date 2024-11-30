package codecrafters_redis

import cats.syntax.all._
import java.net.{ServerSocket}
import codecrafters_redis.network._
import java.io.IOException
import scala.util.Using.Manager
import scala.util.Using.Releasable
import scala.util.Using
import cats.MonadError
import cats.Invariant
import cats.arrow.FunctionK
import codecrafters_redis.codec._
import java.nio.charset.StandardCharsets.UTF_8

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

object codec {
  trait Encoder[In, Out] {
    def encode(in: In): Out

    def contramap[A](f: A => In): Encoder[A, Out] =
      (a: A) => encode(f(a))

    def map[B](f: Out => B): Encoder[In, B] =
      (in: In) => f(encode(in))
  }

  object Encoder {
    def apply[A, B](implicit ev: Encoder[A, B]): Encoder[A, B] = ev

    def fromFunction[A, B](f: A => B): Encoder[A, B] =
      (a: A) => f(a)

    implicit val utf8Encoder: Encoder[String, Array[Byte]] =
      Encoder.fromFunction(_.getBytes(UTF_8))
  }

  trait Decoder[In, Out] {
    def decode(bytes: In): Either[Decoder.DecodeError, Out]

    def map[A](f: Out => A): Decoder[In, A] =
      (bytes: In) => decode(bytes).map(f)

    def emap[A](f: Out => Either[Decoder.DecodeError, A]): Decoder[In, A] =
      (bytes: In) => decode(bytes).flatMap(f)

    def contramap[B](f: B => In): Decoder[B, Out] =
      (b: B) => decode(f(b))

    def flatMap[A](f: Out => Decoder[Out, A]): Decoder[In, A] =
      (bytes: In) => decode(bytes).flatMap(out => f(out).decode(out))
  }

  object Decoder {
    def apply[A, B](implicit ev: Decoder[A, B]): Decoder[A, B] = ev

    def fromFunction[A, B](f: A => Either[DecodeError, B]): Decoder[A, B] =
      (a: A) => f(a)

    implicit val utf8Decoder: Decoder[Array[Byte], String] =
      Decoder.fromFunction(bytes =>
        Either
          .catchNonFatal(new String(bytes, UTF_8))
          .leftMap(_ => DecodeError.NotUtf8)
      )

    sealed trait DecodeError extends Product with Serializable
    object DecodeError {
      case object NotUtf8 extends DecodeError
      final case class BadInput(s: String) extends DecodeError
    }
  }
}

object protocol {
  def string(value: String): String = s"+$value\r\n"
  def integer(value: Int): String = s":$value\r\n"
  def error(value: String): String = s"-$value\r\n"
}

object Log {
  def info(message: String): Unit = println(s"[INFO] $message")
  def error(message: String): Unit = Console.err.println(s"[ERROR] $message")
  def debug(message: String): Unit = println(s"[DEBUG] $message")
}

object commands {
  sealed trait RedisCommand extends Product with Serializable {
    type Response
  }

  object RedisCommand {
    type WithResponse[A] = RedisCommand { type Response = A }

    case object Ping extends RedisCommand {
      type Response = RedisResponse.Pong.type
    }

    implicit val decoder: Decoder[Array[Byte], RedisCommand] =
      Decoder.utf8Decoder.emap(string =>
        string match {
          case "PING" => Right(Ping)
          case _      => Left(Decoder.DecodeError.BadInput(string))
        }
      )
  }

  sealed trait RedisResponse extends Product with Serializable
  object RedisResponse {
    case object Pong extends RedisResponse

    implicit val encoder: Encoder[RedisResponse, Array[Byte]] = {
      import protocol._
      Encoder.fromFunction { case Pong =>
        string("PONG").getBytes
      }
    }
  }
}

object Server {

  def handleConnection(
      inputStream: java.io.InputStream,
      outputStream: java.io.OutputStream
  ) = {
    val bytes = inputStream.readAllBytes()
    val command =
      Decoder[Array[Byte], commands.RedisCommand].decode(bytes)

    command match {
      case Left(error) =>
        Log.error(s"Failed to decode command: $error")
        outputStream.write(protocol.error("Failed to decode command").getBytes)
      case Right(value) =>
        Log.debug(s"Received command: $value")
        val response = value match {
          case commands.RedisCommand.Ping =>
            commands.RedisResponse.Pong
        }

        val encodedResponse =
          Encoder[commands.RedisResponse, Array[Byte]].encode(response)

        outputStream.write(encodedResponse)
    }
  }

  def main(args: Array[String]): Unit = {
    val address = InetSocketAddress(Host("localhost"), Port(6379))
    bind(address) match {
      case Left(error) =>
        Log.error(s"Failed to bind to $address: $error")
        System.exit(1)
      case Right(value) =>
        Log.info(s"Successfully bound to $address")
        Using.resource(value) { serverSocket =>
          Log.info(
            s"Server is running on ${serverSocket.getLocalSocketAddress}"
          )
          while (true) {
            Using.resource(serverSocket.accept()) { clientSocket =>
              Log.info(
                s"Accepted connection from ${clientSocket.getRemoteSocketAddress}"
              )

              Using.resource(clientSocket.getInputStream) { inputStream =>
                Using.resource(clientSocket.getOutputStream) { outputStream =>
                  handleConnection(inputStream, outputStream)
                }
              }
            }
          }
        }
    }
  }
}
