package jbpmtest

import akka.camel.{CamelExtension, CamelMessage, Consumer}
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext

class MyEndpoint extends Consumer {
  def endpointUri = "mina2:tcp://localhost:6200?textline=true"

  def receive = {
    case msg: CamelMessage => println(s"msg ---> $msg")
    case m => println(s"other stuff --> $m")
  }
}

object HelloCamelActor extends App {

  // start and expose actor via tcp

  import akka.actor.{ActorSystem, Props}
  import ExecutionContext.Implicits.global

  val system = ActorSystem("some-system")
  val camel = CamelExtension(system)
  val actorRef = system.actorOf(Props[MyEndpoint])
  val activationFuture = camel.activationFutureFor(actorRef)(timeout = 10 seconds, executor = system.dispatcher)
  activationFuture.onComplete {
    case Success(t) => println(s"ok $t")
    case Failure(e) => println(s"error $e")
  }
  println("press enter to end program")
  readLine()
  System.exit(0)
}