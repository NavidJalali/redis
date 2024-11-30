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

  given Decoder[Array[Byte], RedisRequest] =
    Decoder.utf8Decoder.emap(string =>
      string match {
        case "PING" => Right(Ping)
        case _      => Left(Decoder.DecodeError.BadInput(string))
      }
    )
}
