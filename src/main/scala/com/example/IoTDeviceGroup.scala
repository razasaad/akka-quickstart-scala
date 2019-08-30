package com.example

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import com.example.messages.{ReplyIoTDeviceList, RequestIoTDeviceList, RequestTrackIoTDevice}


object IoTDeviceGroup {
  def props(groupId: String): Props = Props(new IoTDeviceGroup(groupId))
}

class IoTDeviceGroup(groupId: String) extends Actor with ActorLogging {
  var deviceIdToActor = Map.empty[String, ActorRef]

  override def preStart(): Unit = log.info("DeviceGroup {} started", groupId)

  override def postStop(): Unit = log.info("DeviceGroup {} stopped", groupId)

  override def receive: Receive = {
    case trackMsg@RequestTrackIoTDevice(`groupId`, _) =>
      deviceIdToActor.get(trackMsg.deviceId) match {
        case Some(deviceActor) => deviceActor.forward(trackMsg)
        case None =>
          log.info("No Actor registered for handling this. Creating new device actor for {}", trackMsg.deviceId)
          val deviceActor = context.actorOf(IoTDevice.props(trackMsg.groupId, trackMsg.deviceId))
          context.watch(deviceActor)
          deviceIdToActor += trackMsg.deviceId -> deviceActor
          deviceActor.forward(trackMsg)
      }
    case RequestTrackIoTDevice(groupId, _) => log.warning("This Actor is only responsible " +
      "for {} but not {}", this.groupId, groupId)

    case RequestIoTDeviceList(requestId) => sender() ! ReplyIoTDeviceList(requestId, deviceIdToActor.keySet)

    case Terminated(deviceActor) =>
      log.info("Actor with ref {} has been terminated", deviceActor)
      val actorRefToDeviceId = for ((k, v) <- deviceIdToActor) yield (v, k)
      deviceIdToActor -= actorRefToDeviceId(deviceActor)
  }
}
