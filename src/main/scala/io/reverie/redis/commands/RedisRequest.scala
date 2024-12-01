package io.reverie.redis.commands

import cats.syntax.either.*
import io.reverie.redis.codec.Decoder
import io.reverie.redis.protocol.RESP

import scala.collection.immutable.{AbstractSeq, LinearSeq}

sealed trait RedisRequest extends Product with Serializable {
  type Response
}

object RedisRequest {
  type WithResponse[A] = RedisRequest {type Response = A}

  case object Ping extends RedisRequest {
    type Response = RedisResponse.Pong.type
  }

  given Decoder[RESP, RedisRequest] =
    Decoder.fromFunction {
      case in@RESP.Arr(elements) =>
        elements match
          case Seq(RESP.Bin("PING")) => Ping.asRight
          case _ => Decoder.DecodeError.BadInput("Unknown Request", in).asLeft
      case other => Decoder.DecodeError.BadInput(other.toString, other).asLeft
    }
}
