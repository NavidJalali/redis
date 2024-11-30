package io.reverie.redis.codec

import cats.syntax.either._
import java.nio.charset.StandardCharsets.UTF_8

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
  def apply[A, B](using ev: Decoder[A, B]): Decoder[A, B] = ev

  def fromFunction[A, B](f: A => Either[DecodeError, B]): Decoder[A, B] =
    (a: A) => f(a)

  given utf8Decoder: Decoder[Array[Byte], String] =
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
