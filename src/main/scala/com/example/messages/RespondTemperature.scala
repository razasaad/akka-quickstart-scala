package com.example.messages

final case class RespondTemperature(requestId: Long, value: Option[Double])
