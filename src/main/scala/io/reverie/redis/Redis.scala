package io.reverie.redis

import io.reverie.redis.codec.{Decoder, Encoder}
import io.reverie.redis.commands.RedisRequest
import io.reverie.redis.commands.RedisResponse
import io.reverie.redis.commands.RedisRequest.Ping
import io.reverie.redis.protocol.RESP

import java.io.InputStream

final class Redis {
  self =>
  def request(in: RedisRequest): RedisResponse = {
    in match {
      case in: Ping.type => self.ping(in)
    }
  }

  def ping(in: Ping.type): RedisResponse.Pong.type = RedisResponse.Pong
}

object Redis {
  given encoder: Encoder[RedisResponse, Array[Byte]] =
    Encoder[RedisResponse, Array[Byte]].via[RESP]

  given decoder: Decoder[InputStream, RedisRequest] =
    Decoder[InputStream, RedisRequest].via[RESP]
}
