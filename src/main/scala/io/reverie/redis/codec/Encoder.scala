package io.reverie.redis.codec

import java.nio.charset.StandardCharsets.UTF_8

trait Encoder[In, Out] {
  def encode(in: In): Out

  def contramap[A](f: A => In): Encoder[A, Out] =
    (a: A) => encode(f(a))

  def map[B](f: Out => B): Encoder[In, B] =
    (in: In) => f(encode(in))
}

object Encoder {
  def apply[A, B](using ev: Encoder[A, B]): Encoder[A, B] = ev

  def fromFunction[A, B](f: A => B): Encoder[A, B] =
    (a: A) => f(a)

  given utf8Encoder: Encoder[String, Array[Byte]] =
    Encoder.fromFunction(_.getBytes(UTF_8))
}
