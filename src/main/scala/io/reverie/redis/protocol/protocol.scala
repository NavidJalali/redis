package io.reverie.redis.protocol

def string(value: String): String = s"+$value\r\n"
def integer(value: Int): String = s":$value\r\n"
def error(value: String): String = s"-$value\r\n"
