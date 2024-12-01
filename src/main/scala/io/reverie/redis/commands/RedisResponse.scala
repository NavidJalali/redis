package io.reverie.redis.commands

import io.reverie.redis.protocol.*
import io.reverie.redis.codec.Encoder

sealed trait RedisResponse extends Product with Serializable

object RedisResponse {
  final case class Error(message: String) extends RedisResponse
  case object Pong extends RedisResponse

  given Encoder[RedisResponse, RESP] = {
    Encoder.fromFunction {
      case Pong => RESP.Str("PONG")
      case Error(message) => RESP.Err(message)
    }
  }
}
