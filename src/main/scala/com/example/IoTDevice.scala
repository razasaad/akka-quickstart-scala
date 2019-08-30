package com.example

import akka.actor.{Actor, ActorLogging, Props}
import com.example.messages._

object IoTDevice {
  def props(groupId: String, deviceId: String): Props = Props(new IoTDevice(groupId = groupId, deviceId = deviceId))
}

class IoTDevice(groupId: String, deviceId: String) extends Actor with ActorLogging {

  var lastTemperatureReading: Option[Double] = None


  override def preStart(): Unit = log.info("Device actor {}-{} started", groupId, deviceId)

  override def postStop(): Unit = log.info("Device actor {}-{} stopped", groupId, deviceId)

  override def receive: Receive = {
    case RequestTrackIoTDevice(`groupId`, `deviceId`) => sender() ! DeviceRegistered

    case RequestTrackIoTDevice(groupId, deviceId) => log.warning("Ignoring TrackDevice Request for {}-{}. " +
      "This actor is responsible for {}-{}", groupId, deviceId, this.groupId, this.deviceId)

    case ReadTemperature(id) => sender() ! RespondTemperature(id, lastTemperatureReading)

    case RecordTemperature(id, value) =>
      lastTemperatureReading = Some(value)
      sender() ! TemperatureRecorded(id)
  }
}
