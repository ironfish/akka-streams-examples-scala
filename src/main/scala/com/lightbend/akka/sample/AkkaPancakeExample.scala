package com.lightbend.akka.sample

import akka.NotUsed
import akka.Done
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent.{ExecutionContextExecutor, Future}

object AkkaPancakeExample extends App {

  case class ScoopOfBatter()
  case class HalfCookedPancake()
  case class Pancake()

  val orders = List.fill(10)(ScoopOfBatter())

  val workLoad: Source[ScoopOfBatter, NotUsed] =
    Source(orders)

  // Takes a scoop of batter and creates a pancake with one side cooked
  val fryingPan1: Flow[ScoopOfBatter, HalfCookedPancake, NotUsed] =
    Flow[ScoopOfBatter].map { batter => HalfCookedPancake() }

  // Finishes a half-cooked pancake
  val fryingPan2: Flow[HalfCookedPancake, Pancake, NotUsed] =
    Flow[HalfCookedPancake].map { halfCooked => Pancake() }

  // With the two frying pans we can fully cook pancakes
  val pancakeChef: Flow[ScoopOfBatter, Pancake, NotUsed] =
    Flow[ScoopOfBatter].via(fryingPan1.async).via(fryingPan2.async)

  // Processing stage accepting and completing work
  val working: Sink[Pancake, Future[Done]] =
    Sink.foreach(x => println(x.toString))

  // Runnable flow with both ends attached (Source & Sink)
  val work: RunnableGraph[Future[Done]] = workLoad.via(pancakeChef).toMat(working)(Keep.right)

  implicit val system: ActorSystem = ActorSystem("AkkaStreamsPancakeExample")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val futureWork = work.run()

  implicit val ec: ExecutionContextExecutor = system.dispatcher

  futureWork.onComplete(_ => system.terminate())
}
