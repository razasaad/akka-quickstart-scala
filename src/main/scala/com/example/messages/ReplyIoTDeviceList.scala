package com.example.messages

final case class ReplyIoTDeviceList(requestId: Long, deviceIds: Set[String])
