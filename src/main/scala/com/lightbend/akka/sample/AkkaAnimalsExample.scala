package com.lightbend.akka.sample

import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.util.ByteString

import scala.util.Failure
import scala.util.Success

object AkkaAnimalsExample {

  case class Animal(animal: String, breed: String, color: String, weight: String)


  def main(args: Array[String]): Unit = {

    // actor system and implicit materializer
    implicit val system: ActorSystem = ActorSystem("Sys")
    implicit val materializer: ActorMaterializer = ActorMaterializer()

    val SpeciesPattern = """(CAT|COW|DOG|HORSE).*""".r

    // read lines from a animals file
    val animalsFile = Paths.get("src/main/resources/animals.txt")

    FileIO.fromPath(animalsFile)
      // parse chunks of bytes into lines
      .via(Framing.delimiter(ByteString(System.lineSeparator), maximumFrameLength = 512, allowTruncation = true))
      .map(_.utf8String)
      .map(line => {
        val split: Vector[String] = line.split(",").toVector
        split.head match {
          case animal @ SpeciesPattern(species) =>
            (species, Animal(animal, split(1), split(2), split(3)))
          case animal @ other               =>
            ("OTHER", Animal(animal, split(1), split(2), split(3)))
        }
      })
      // group them by species
      .groupBy(5, _._1)
      .fold(("", List.empty[Animal])) {
        case ((_, list), (species, animal)) => (species, animal :: list)
      }
      // write lines of each group to a separate file
      .mapAsync(parallelism = 5) {
      case (species, groupList) =>
        Source(groupList.reverse).map(line => ByteString(line + "\n")).runWith(FileIO.toPath(Paths.get(s"target/animal-$species.txt")))
    }
      .mergeSubstreams
      .runWith(Sink.onComplete {
        case Success(_) =>
          println("Output complete, check /target folder.")
          system.terminate()
        case Failure(e) =>
          println(s"Failure: ${e.getMessage}")
          system.terminate()
      })
  }
}
