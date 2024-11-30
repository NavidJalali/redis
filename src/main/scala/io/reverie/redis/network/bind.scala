package io.reverie.redis.network

import java.io.IOException
import java.net.ServerSocket

enum BindError {
  case AlreadyBound(e: IOException)
  case SecurityError(e: SecurityException)
}

def bind(address: InetSocketAddress) = {
  val serverSocket = ServerSocket()
  try {
    serverSocket.bind(address.asJava)
    Right(serverSocket)
  } catch {
    case e: IOException =>
      serverSocket.close()
      Left(BindError.AlreadyBound(e))
    case e: SecurityException =>
      serverSocket.close()
      Left(BindError.SecurityError(e))
  }
}
