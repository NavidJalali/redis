package io.reverie.redis.commands

import io.reverie.redis.codec.Decoder

sealed trait RedisRequest extends Product with Serializable {
  type Response
}

object RedisRequest {
  type WithResponse[A] = RedisRequest { type Response = A }

  case object Ping extends RedisRequest {
    type Response = RedisResponse.Pong.type
  }
}
