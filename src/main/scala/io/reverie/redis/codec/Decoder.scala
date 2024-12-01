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

  def ~>[A](that: Decoder[Out, A]): Decoder[In, A] =
    emap(that.decode)
}

object Decoder {
  def apply[A, B]: Apply[A, B] = new Apply[A, B]

  final class Apply[A, B] {
    def apply()(using instance: Decoder[A, B]): Decoder[A, B] = instance

    def via[C](using ac: Decoder[A, C], cb: Decoder[C, B]): Decoder[A, B] =
      ac ~> cb
  }

  def fromFunction[A, B](f: A => Either[DecodeError, B]): Decoder[A, B] =
    (a: A) => f(a)

  sealed trait DecodeError extends Product with Serializable

  object DecodeError {
    final case class ExhaustedInput[Message](message: Message) extends DecodeError
    final case class BadInput[Input](reason: String, input: Input) extends DecodeError
  }
}
