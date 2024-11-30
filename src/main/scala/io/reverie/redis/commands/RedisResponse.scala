package io.reverie.redis.commands

import io.reverie.redis.protocol.*
import io.reverie.redis.codec.Encoder

sealed trait RedisResponse extends Product with Serializable

object RedisResponse {
  case object Pong extends RedisResponse

  given Encoder[RedisResponse, Array[Byte]] = {
    Encoder.fromFunction { case Pong =>
      string("PONG").getBytes
    }
  }
}
