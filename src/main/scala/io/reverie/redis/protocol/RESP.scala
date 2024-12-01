package io.reverie.redis.protocol

import cats.syntax.either._
import io.reverie.redis.codec.Encoder
import io.reverie.redis.codec.Decoder

enum RESP {
  case Str(value: String)
  case Bin(value: String)
  case Err(value: String)
  case Int(value: scala.Int)
  case Arr(values: Seq[RESP])
}

object RESP {
  private def write(sb: StringBuilder)(resp: RESP): StringBuilder =
    resp match
      case RESP.Str(value) =>
        sb.append(s"+$value\r\n")
      case RESP.Bin(value) =>
        sb.append(s"$$${value.length}\r\n$value\r\n")
      case RESP.Err(value) =>
        sb.append(s"-$value\r\n")
      case RESP.Int(value) =>
        sb.append(s":$value\r\n")
      case RESP.Arr(values) =>
        sb.append(s"*${values.length}\r\n")
        values.foreach(write(sb))
        sb

  given Encoder[RESP, Array[Byte]] = {
    Encoder.fromFunction { resp =>
      write(new StringBuilder)(resp).toString().getBytes
    }
  }

  given Decoder[Array[Byte], RESP] =
    Decoder.utf8 emap { raw =>
      for {
        lines <- Decoder.lines.decode(raw)
        firstLine <-
          lines
            .headOption
            .toRight(
              Decoder.DecodeError.ExhaustedInput(raw)
            )
        firstChar <-
          firstLine
            .headOption
            .toRight(
              Decoder.DecodeError.ExhaustedInput(raw)
            )
        resp <-
          firstChar match
            // simple string
            case '+' =>
              val value = firstLine.tail
              RESP.Str(value).asRight
            // simple error
            case '-' =>
              val value = firstLine.tail
              RESP.Err(value).asRight
            // integer
            case ':' =>
              firstLine.tail
                .strip()
                .toIntOption
                .toRight(Decoder.DecodeError.BadInput("Cannot be cast into Int", firstLine))
                .map(RESP.Int.apply)
            case '$' =>
              for {
                length <-
                  firstLine
                    .tail
                    .toIntOption
                    .toRight(
                      Decoder.DecodeError.BadInput("Cannot parse length", firstLine)
                    )
                data <-
                  Either.cond(
                    lines.length >= 2,
                    lines(1),
                    Decoder.DecodeError.ExhaustedInput(raw)
                  )
                result <-
                  Either.cond(
                    data.length == length,
                    RESP.Bin(data),
                    Decoder.DecodeError.BadInput(
                      s"Expected $length bytes, got ${data.length}",
                      data
                    )
                  )
              } yield result
            case '*' =>
              for {
                length <-
                  firstLine
                    .tail
                    .toIntOption
                    .toRight(
                      Decoder.DecodeError.BadInput("Cannot parse length", firstLine)
                    )

                data = lines.drop(1)

              } yield ???
      } yield resp
    }
}
