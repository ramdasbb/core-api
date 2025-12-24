package com.smartvillage.ratingakka

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import scala.util.{Failure, Success}

object Server {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem(Behaviors.empty, "rating-akka-system")
    implicit val ec = system.executionContext

    val route = pathPrefix("async") {
      path("ping") {
        get {
          complete("pong")
        }
      }
    }

    val binding = Http().newServerAt("0.0.0.0", 9000).bind(route)
    binding.onComplete {
      case Success(b) => println(s"Rating Akka server bound to ${b.localAddress}")
      case Failure(e) =>
        println(s"Failed to bind Akka HTTP server: $e")
        system.terminate()
    }
  }
}
