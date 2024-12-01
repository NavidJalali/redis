package io.reverie.redis.codec

import java.nio.charset.StandardCharsets.UTF_8

trait Encoder[In, Out] {
  self =>
  def encode(in: In): Out

  def contramap[A](f: A => In): Encoder[A, Out] =
    (a: A) => self.encode(f(a))

  def map[B](f: Out => B): Encoder[In, B] =
    (in: In) => f(self.encode(in))

  infix def ~>[A](that: Encoder[Out, A]): Encoder[In, A] =
    (in: In) => that.encode(self.encode(in))

}

object Encoder {
  def apply[A, B]: Apply[A, B] = new Apply[A, B]

  final class Apply[A, B] {
    def apply()(using instance: Encoder[A, B]): Encoder[A, B] = instance

    def via[C](using ac: Encoder[A, C], cb: Encoder[C, B]): Encoder[A, B] =
      ac ~> cb
  }

  def fromFunction[A, B](f: A => B): Encoder[A, B] =
    (a: A) => f(a)

  given utf8Encoder: Encoder[String, Array[Byte]] =
    Encoder.fromFunction(_.getBytes(UTF_8))
}
