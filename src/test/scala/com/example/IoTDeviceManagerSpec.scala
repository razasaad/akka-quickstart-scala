package com.example

import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestProbe}
import com.example.messages._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class IoTDeviceManagerSpec(_system: ActorSystem) extends TestKit(_system)
  with Matchers
  with WordSpecLike
  with BeforeAndAfterAll {

  def this() = this(ActorSystem("IoTDeviceManager"))

  override def afterAll: Unit = {
    shutdown(system)
  }

  "be able to register a device actor" in {
    val probe = TestProbe()
    val managerActor = system.actorOf(IoTDeviceManager.props())

    managerActor.tell(RequestTrackIoTDevice("group", "device1"), probe.ref)
    probe.expectMsg(DeviceRegistered)
    val firstDeviceActor = probe.lastSender

    managerActor.tell(RequestTrackIoTDevice("group", "device2"), probe.ref)
    probe.expectMsg(DeviceRegistered)
    val secondDeviceActor = probe.lastSender

    firstDeviceActor should !==(secondDeviceActor)

    firstDeviceActor.tell(RecordTemperature(requestId = 0L, value = 15.6), probe.ref)
    probe.expectMsg(TemperatureRecorded(requestId = 0L))

    secondDeviceActor.tell(RecordTemperature(requestId = 5L, value = 24.1), probe.ref)
    probe.expectMsg(TemperatureRecorded(requestId = 5L))
  }

  "be able to return the registered DeviceGroup Actors" in {
    val probe = TestProbe()
    val managerActor = system.actorOf(IoTDeviceManager.props())

    managerActor.tell(RequestTrackIoTDevice("group", "device1"), probe.ref)
    probe.expectMsg(DeviceRegistered)

    managerActor.tell(RequestTrackIoTDevice("group", "device2"), probe.ref)
    probe.expectMsg(DeviceRegistered)

    managerActor.tell(RequestIoTDeviceGroupList(0L), probe.ref)
    probe.expectMsg(ReplyIoTDeviceGroupList(0L, Set("group")))
  }
}
