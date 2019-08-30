package com.example

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import com.example.messages.{ReplyIoTDeviceGroupList, RequestIoTDeviceGroupList, RequestTrackIoTDevice}

object IoTDeviceManager {
  def props(): Props = Props(new IoTDeviceManager)
}

class IoTDeviceManager extends Actor with ActorLogging {
  var groupIdToActor = Map.empty[String, ActorRef]

  override def preStart(): Unit = log.info("IoTDeviceManager started")

  override def postStop(): Unit = log.info("IoTDeviceManager stopped")

  override def receive: Receive = {
    case trackMsg@RequestTrackIoTDevice(groupId, _) =>
      groupIdToActor.get(groupId) match {
        case Some(ref) => ref.forward(trackMsg)
        case None =>
          log.info("Creating an IoTDeviceGroup Actor for group {}", groupId)
          val groupActor = context.actorOf(IoTDeviceGroup.props(groupId), "group-" + groupId)
          context.watch(groupActor)
          groupActor.forward(trackMsg)
          groupIdToActor += groupId -> groupActor
      }

    case RequestIoTDeviceGroupList(requestId) => sender() ! ReplyIoTDeviceGroupList(requestId, groupIdToActor.keySet)

    case Terminated(groupActor) =>
      log.info("Actor with ref {} has been terminated", groupActor)
      val actorRefToGroupId = for ((k, v) <- groupIdToActor) yield (v, k)
      groupIdToActor -= actorRefToGroupId(groupActor)
  }
}
