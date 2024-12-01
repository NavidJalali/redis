package io.reverie.redis.protocol

import io.reverie.redis.codec.{Decoder, Encoder}
import munit.FunSuite

import java.io.InputStream

final class RESPTest extends FunSuite {
  test("encode simple string") {
    val str = "PING"
    val expected = s"+${str}\r\n".getBytes
    val result = Encoder[RESP, Array[Byte]]().encode(RESP.Str(str))
    assert(result.sameElements(expected))
  }
  test("decode simple string") {
    val str = "PING"
    val inputStream = makeInputStream(s"+${str}\r\n")

    val result = Decoder[InputStream, RESP]().decode(inputStream)
    assertEquals(result, Right(RESP.Str(str)))
  }

  private def makeInputStream(value: String) = new InputStream {
    private val bytes = value.getBytes
    private var index = 0

    override def read(): Int = {
      if (index >= bytes.length) -1
      else {
        val byte = bytes(index)
        index += 1
        byte
      }
    }
  }
}